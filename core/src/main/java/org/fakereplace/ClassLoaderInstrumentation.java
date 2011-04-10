/*
 * Copyright 2011, Stuart Douglas
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.fakereplace;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;
import org.fakereplace.boot.Logger;
import org.fakereplace.classloading.ClassLookupManager;
import org.fakereplace.data.BaseClassData;
import org.fakereplace.data.ClassDataStore;
import org.fakereplace.data.MethodData;
import org.fakereplace.manip.FinalMethodManipulator;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class ClassLoaderInstrumentation {

    public ClassLoaderInstrumentation(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    final Instrumentation instrumentation;

    Set<Class<?>> trasformedClassLoaders = new CopyOnWriteArraySet<Class<?>>();

    Set<Class<?>> failedTransforms = new CopyOnWriteArraySet<Class<?>>();

    /**
     * This method instruments class loaders so that they can load our helper
     * classes.
     *
     * @param cl
     */
    public void redefineClassLoader(Class<?> cl) {
        try {
            if (trasformedClassLoaders.contains(cl) || failedTransforms.contains(cl)) {
                return;
            }
            // we are using the high level javassist bytecode here because we
            // have access to the class object
            CtClass cls = null;
            if (cl.getClassLoader() != null) {
                URL resource = cl.getClassLoader().getResource(cl.getName().replace('.', '/') + ".class");
                InputStream in = null;
                try {
                    in = resource.openStream();
                    cls = ClassPool.getDefault().makeClass(in);
                } catch (Exception e) {
                } finally {
                    try {
                        in.close();
                    } catch (IOException e) {
                    }
                }
            }
            if (cls == null) {
                cls = ClassPool.getDefault().getCtClass(cl.getName());
            }
            cls.defrost();
            CtClass str = ClassPool.getDefault().getCtClass("java.lang.String");
            CtClass[] arg = new CtClass[2];
            arg[0] = str;
            arg[1] = CtClass.booleanType;
            // now we instrument the loadClass
            // if the system requests a class from the generated class package
            // then
            // we check to see if it is already loaded.
            // if not we try and get the class definition from GlobalData
            // we do not need to delegate as GlobalData will only
            // return the data to the correct classloader.
            // if the data is not null then we define the class, link
            // it if requested and return it.

            CtMethod method = cls.getDeclaredMethod("loadClass", arg);
            method
                    .insertBefore("byte[] bdata = "
                            + ClassLookupManager.class.getName()
                            + ".getClassData($1,this); if(bdata != null){ try{ Class find = findLoadedClass($1); if(find != null) return find; Class c = defineClass($1,bdata,0,bdata.length); if($2) resolveClass(c); return c;  }catch(Throwable e) {e.printStackTrace(); return null;}}");
            BaseClassData data = ClassDataStore.getBaseClassData(cl.getClassLoader(), cl.getName());
            for (Object i : cls.getDeclaredMethods()) {
                CtMethod m = (CtMethod) i;
                MethodData dta = null;
                if (data != null) {
                    for (MethodData md : data.getMethods()) {
                        if (md.getDescriptor().equals(m.getMethodInfo().getDescriptor()) && md.getMethodName().equals(m.getName())) {
                            dta = md;
                            break;
                        }
                    }
                    if (dta == null) {
                        continue;
                    }
                    if (dta.isFinalMethod()) {
                        m.setModifiers(m.getModifiers() & ~AccessFlag.FINAL);
                    }
                }
            }
            FinalMethodManipulator.addClassLoader(cl.getName());
            // now reload the instrumented class loader
            ClassDefinition cd = new ClassDefinition(cl, cls.toBytecode());
            ClassDefinition[] ar = new ClassDefinition[1];
            ar[0] = cd;

            instrumentation.redefineClasses(ar);
            // make a note of the fact that we have transformed this class
            // loader
            trasformedClassLoaders.add(cl);
            System.out.println("INSTRUMENTED: " + cl);
        } catch (NotFoundException e) {
            if (cl.getSuperclass() != ClassLoader.class && !trasformedClassLoaders.contains(cl.getSuperclass()) && !failedTransforms.contains(cl)) {
                redefineClassLoader(cl.getSuperclass());
            }
            trasformedClassLoaders.add(cl);
        } catch (Exception e) {
            e.printStackTrace();
            failedTransforms.add(cl);
        } finally {
        }
    }

    /**
     * Checks to see if a class loader has already been instrumented and if not
     * instriments it
     *
     * @param loader
     */
    void instrumentClassLoaderIfNessesary(ClassLoader loader, String className) {
        if (loader != null) {
            if (!trasformedClassLoaders.contains(loader.getClass()) && !failedTransforms.contains(loader.getClass())) {
                redefineClassLoader(loader.getClass());
            }
            if (failedTransforms.contains(loader.getClass())) {
                Logger.log(this, "The class loader for " + className + " could not be instrumented");
            }
        }
    }
}
