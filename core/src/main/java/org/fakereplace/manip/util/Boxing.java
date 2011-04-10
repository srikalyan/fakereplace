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

package org.fakereplace.manip.util;

import javassist.bytecode.Bytecode;

/**
 * This class is responsible for generating bytecode fragments to box/unbox
 * whatever happens to be on the top of the stack.
 * <p/>
 * It is the calling codes responsibility to make sure that the correct type is
 * on the stack
 *
 * @author stuart
 */
public class Boxing {

    static public void box(Bytecode b, char type) {
        switch (type) {
            case 'I':
                boxInt(b);
                break;
            case 'J':
                boxLong(b);
                break;
            case 'S':
                boxShort(b);
                break;
            case 'F':
                boxFloat(b);
                break;
            case 'D':
                boxDouble(b);
                break;
            case 'B':
                boxByte(b);
                break;
            case 'C':
                boxChar(b);
                break;
            case 'Z':
                boxBoolean(b);
                break;
            default:
                throw new RuntimeException("Cannot box unkown primitive type: " + type);
        }

    }

    static public Bytecode unbox(Bytecode b, char type) {
        switch (type) {
            case 'I':
                return unboxInt(b);
            case 'J':
                return unboxLong(b);
            case 'S':
                return unboxShort(b);
            case 'F':
                return unboxFloat(b);
            case 'D':
                return unboxDouble(b);
            case 'B':
                return unboxByte(b);
            case 'C':
                return unboxChar(b);
            case 'Z':
                return unboxBoolean(b);
        }
        throw new RuntimeException("Cannot unbox unkown primitive type: " + type);
    }

    static public void boxInt(Bytecode bc) {
        bc.addInvokestatic("java.lang.Integer", "valueOf", "(I)Ljava/lang/Integer;");
    }

    static public void boxLong(Bytecode bc) {
        bc.addInvokestatic("java.lang.Long", "valueOf", "(J)Ljava/lang/Long;");
    }

    static public void boxShort(Bytecode bc) {
        bc.addInvokestatic("java.lang.Short", "valueOf", "(S)Ljava/lang/Short;");
    }

    static public void boxByte(Bytecode bc) {
        bc.addInvokestatic("java.lang.Byte", "valueOf", "(B)Ljava/lang/Byte;");
    }

    static public void boxFloat(Bytecode bc) {
        bc.addInvokestatic("java.lang.Float", "valueOf", "(F)Ljava/lang/Float;");
    }

    static public void boxDouble(Bytecode bc) {
        bc.addInvokestatic("java.lang.Double", "valueOf", "(D)Ljava/lang/Double;");
    }

    static public void boxChar(Bytecode bc) {
        bc.addInvokestatic("java.lang.Character", "valueOf", "(C)Ljava/lang/Character;");
    }

    static public void boxBoolean(Bytecode bc) {
        bc.addInvokestatic("java.lang.Boolean", "valueOf", "(Z)Ljava/lang/Boolean;");
    }

    // unboxing

    static public Bytecode unboxInt(Bytecode bc) {
        bc.addCheckcast("java.lang.Number");
        bc.addInvokevirtual("java.lang.Number", "intValue", "()I");
        return bc;
    }

    static public Bytecode unboxLong(Bytecode bc) {
        bc.addCheckcast("java.lang.Number");
        bc.addInvokevirtual("java.lang.Number", "longValue", "()J");
        return bc;
    }

    static public Bytecode unboxShort(Bytecode bc) {
        bc.addCheckcast("java.lang.Number");
        bc.addInvokevirtual("java.lang.Number", "shortValue", "()S");
        return bc;
    }

    static public Bytecode unboxByte(Bytecode bc) {
        bc.addCheckcast("java.lang.Number");
        bc.addInvokevirtual("java.lang.Number", "byteValue", "()B");
        return bc;
    }

    static public Bytecode unboxFloat(Bytecode bc) {
        bc.addCheckcast("java.lang.Number");
        bc.addInvokevirtual("java.lang.Number", "floatValue", "()F");
        return bc;
    }

    static public Bytecode unboxDouble(Bytecode bc) {
        bc.addCheckcast("java.lang.Number");
        bc.addInvokevirtual("java.lang.Number", "doubleValue", "()D");
        return bc;
    }

    static public Bytecode unboxChar(Bytecode bc) {
        bc.addCheckcast("java.lang.Character");
        bc.addInvokevirtual("java.lang.Character", "charValue", "()C");
        return bc;
    }

    static public Bytecode unboxBoolean(Bytecode bc) {
        bc.addCheckcast("java.lang.Boolean");
        bc.addInvokevirtual("java.lang.Boolean", "booleanValue", "()Z");
        return bc;
    }

}
