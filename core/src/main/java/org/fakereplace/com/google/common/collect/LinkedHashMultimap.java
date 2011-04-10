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

package org.fakereplace.com.google.common.collect;

import org.fakereplace.com.google.common.annotations.GwtCompatible;
import org.fakereplace.com.google.common.annotations.VisibleForTesting;
import org.fakereplace.com.google.common.base.Preconditions;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;


/**
 * Implementation of {@code Multimap} that does not allow duplicate key-value
 * entries and that returns collections whose iterators follow the ordering in
 * which the data was added to the multimap.
 * <p/>
 * <p>The collections returned by {@code keySet}, {@code keys}, and {@code
 * asMap} iterate through the keys in the order they were first added to the
 * multimap. Similarly, {@code get}, {@code removeAll}, and {@code
 * replaceValues} return collections that iterate through the values in the
 * order they were added. The collections generated by {@code entries} and
 * {@code values} iterate across the key-value mappings in the order they were
 * added to the multimap.
 * <p/>
 * <p>The iteration ordering of the collections generated by {@code keySet},
 * {@code keys}, and {@code asMap} has a few subtleties. As long as the set of
 * keys remains unchanged, adding or removing mappings does not affect the key
 * iteration order. However, if you remove all values associated with a key and
 * then add the key back to the multimap, that key will come last in the key
 * iteration order.
 * <p/>
 * <p>The multimap does not store duplicate key-value pairs. Adding a new
 * key-value pair equal to an existing key-value pair has no effect.
 * <p/>
 * <p>Keys and values may be null. All optional multimap methods are supported,
 * and all returned views are modifiable.
 * <p/>
 * <p>This class is not threadsafe when any concurrent operations update the
 * multimap. Concurrent read operations will work correctly. To allow concurrent
 * update operations, wrap your multimap with a call to {@link
 * Multimaps#synchronizedSetMultimap}.
 *
 * @author Jared Levy
 */
@GwtCompatible(serializable = true)
public final class LinkedHashMultimap<K, V> extends AbstractSetMultimap<K, V> {
    private static final int DEFAULT_VALUES_PER_KEY = 8;

    @VisibleForTesting
    transient int expectedValuesPerKey = DEFAULT_VALUES_PER_KEY;

    /**
     * Map entries with an iteration order corresponding to the order in which the
     * key-value pairs were added to the multimap.
     */
    // package-private for GWT deserialization
    transient Collection<Map.Entry<K, V>> linkedEntries;

    /**
     * Creates a new, empty {@code LinkedHashMultimap} with the default initial
     * capacities.
     */
    public static <K, V> LinkedHashMultimap<K, V> create() {
        return new LinkedHashMultimap<K, V>();
    }

    /**
     * Constructs an empty {@code LinkedHashMultimap} with enough capacity to hold
     * the specified numbers of keys and values without rehashing.
     *
     * @param expectedKeys         the expected number of distinct keys
     * @param expectedValuesPerKey the expected average number of values per key
     * @throws IllegalArgumentException if {@code expectedKeys} or {@code
     *                                  expectedValuesPerKey} is negative
     */
    public static <K, V> LinkedHashMultimap<K, V> create(
            int expectedKeys, int expectedValuesPerKey) {
        return new LinkedHashMultimap<K, V>(expectedKeys, expectedValuesPerKey);
    }

    /**
     * Constructs a {@code LinkedHashMultimap} with the same mappings as the
     * specified multimap. If a key-value mapping appears multiple times in the
     * input multimap, it only appears once in the constructed multimap. The new
     * multimap has the same {@link Multimap#entries()} iteration order as the
     * input multimap, except for excluding duplicate mappings.
     *
     * @param multimap the multimap whose contents are copied to this multimap
     */
    public static <K, V> LinkedHashMultimap<K, V> create(
            Multimap<? extends K, ? extends V> multimap) {
        return new LinkedHashMultimap<K, V>(multimap);
    }

    private LinkedHashMultimap() {
        super(new LinkedHashMap<K, Collection<V>>());
        linkedEntries = Sets.newLinkedHashSet();
    }

    private LinkedHashMultimap(int expectedKeys, int expectedValuesPerKey) {
        super(new LinkedHashMap<K, Collection<V>>(expectedKeys));
        Preconditions.checkArgument(expectedValuesPerKey >= 0);
        this.expectedValuesPerKey = expectedValuesPerKey;
        linkedEntries = new LinkedHashSet<Map.Entry<K, V>>(
                expectedKeys * expectedValuesPerKey);
    }

    private LinkedHashMultimap(Multimap<? extends K, ? extends V> multimap) {
        super(new LinkedHashMap<K, Collection<V>>(
                Maps.capacity(multimap.keySet().size())));
        linkedEntries
                = new LinkedHashSet<Map.Entry<K, V>>(Maps.capacity(multimap.size()));
        putAll(multimap);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * <p>Creates an empty {@code LinkedHashSet} for a collection of values for
     * one key.
     *
     * @return a new {@code LinkedHashSet} containing a collection of values for
     *         one key
     */
    @Override
    Set<V> createCollection() {
        return new LinkedHashSet<V>(Maps.capacity(expectedValuesPerKey));
    }

    /**
     * {@inheritDoc}
     * <p/>
     * <p>Creates a decorated {@code LinkedHashSet} that also keeps track of the
     * order in which key-value pairs are added to the multimap.
     *
     * @param key key to associate with values in the collection
     * @return a new decorated {@code LinkedHashSet} containing a collection of
     *         values for one key
     */
    @Override
    Collection<V> createCollection(K key) {
        return new SetDecorator(key, createCollection());
    }

    private class SetDecorator extends ForwardingSet<V> {
        final Set<V> delegate;
        final K key;

        SetDecorator(K key, Set<V> delegate) {
            this.delegate = delegate;
            this.key = key;
        }

        @Override
        protected Set<V> delegate() {
            return delegate;
        }

        <E> Map.Entry<K, E> createEntry(E value) {
            return Maps.immutableEntry(key, value);
        }

        <E> Collection<Map.Entry<K, E>> createEntries(Collection<E> values) {
            // converts a collection of values into a list of key/value map entries
            Collection<Map.Entry<K, E>> entries
                    = Lists.newArrayListWithExpectedSize(values.size());
            for (E value : values) {
                entries.add(createEntry(value));
            }
            return entries;
        }

        @Override
        public boolean add(V value) {
            boolean changed = delegate.add(value);
            if (changed) {
                linkedEntries.add(createEntry(value));
            }
            return changed;
        }

        @Override
        public boolean addAll(Collection<? extends V> values) {
            boolean changed = delegate.addAll(values);
            if (changed) {
                linkedEntries.addAll(createEntries(delegate()));
            }
            return changed;
        }

        @Override
        public void clear() {
            linkedEntries.removeAll(createEntries(delegate()));
            delegate.clear();
        }

        @Override
        public Iterator<V> iterator() {
            final Iterator<V> delegateIterator = delegate.iterator();
            return new Iterator<V>() {
                V value;

                public boolean hasNext() {
                    return delegateIterator.hasNext();
                }

                public V next() {
                    value = delegateIterator.next();
                    return value;
                }

                public void remove() {
                    delegateIterator.remove();
                    linkedEntries.remove(createEntry(value));
                }
            };
        }

        @Override
        public boolean remove(Object value) {
            boolean changed = delegate.remove(value);
            if (changed) {
                /*
                * linkedEntries.remove() will return false when this method is called
                * by entries().iterator().remove()
                */
                linkedEntries.remove(createEntry(value));
            }
            return changed;
        }

        @Override
        public boolean removeAll(Collection<?> values) {
            boolean changed = delegate.removeAll(values);
            if (changed) {
                linkedEntries.removeAll(createEntries(values));
            }
            return changed;
        }

        @Override
        public boolean retainAll(Collection<?> values) {
            /*
            * Calling linkedEntries.retainAll() would incorrectly remove values
            * with other keys.
            */
            boolean changed = false;
            Iterator<V> iterator = delegate.iterator();
            while (iterator.hasNext()) {
                V value = iterator.next();
                if (!values.contains(value)) {
                    iterator.remove();
                    linkedEntries.remove(Maps.immutableEntry(key, value));
                    changed = true;
                }
            }
            return changed;
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * <p>Generates an iterator across map entries that follows the ordering in
     * which the key-value pairs were added to the multimap.
     *
     * @return a key-value iterator with the correct ordering
     */
    @Override
    Iterator<Map.Entry<K, V>> createEntryIterator() {
        final Iterator<Map.Entry<K, V>> delegateIterator = linkedEntries.iterator();

        return new Iterator<Map.Entry<K, V>>() {
            Map.Entry<K, V> entry;

            public boolean hasNext() {
                return delegateIterator.hasNext();
            }

            public Map.Entry<K, V> next() {
                entry = delegateIterator.next();
                return entry;
            }

            public void remove() {
                // Remove from iterator first to keep iterator valid.
                delegateIterator.remove();
                LinkedHashMultimap.this.remove(entry.getKey(), entry.getValue());
            }
        };
    }

    /**
     * {@inheritDoc}
     * <p/>
     * <p>If {@code values} is not empty and the multimap already contains a
     * mapping for {@code key}, the {@code keySet()} ordering is unchanged.
     * However, the provided values always come last in the {@link #entries()} and
     * {@link #values()} iteration orderings.
     */
    @Override
    public Set<V> replaceValues(
            K key, Iterable<? extends V> values) {
        return super.replaceValues(key, values);
    }

    /**
     * Returns a set of all key-value pairs. Changes to the returned set will
     * update the underlying multimap, and vice versa. The entries set does not
     * support the {@code add} or {@code addAll} operations.
     * <p/>
     * <p>The iterator generated by the returned set traverses the entries in the
     * order they were added to the multimap.
     * <p/>
     * <p>Each entry is an immutable snapshot of a key-value mapping in the
     * multimap, taken at the time the entry is returned by a method call to the
     * collection or its iterator.
     */
    @Override
    public Set<Map.Entry<K, V>> entries() {
        return super.entries();
    }

    /**
     * Returns a collection of all values in the multimap. Changes to the returned
     * collection will update the underlying multimap, and vice versa.
     * <p/>
     * <p>The iterator generated by the returned collection traverses the values
     * in the order they were added to the multimap.
     */
    @Override
    public Collection<V> values() {
        return super.values();
    }

    // Unfortunately, the entries() ordering does not determine the key ordering;
    // see the example in the LinkedListMultimap class Javadoc.

    /**
     * @serialData the number of distinct keys, and then for each distinct key:
     * the first key, the number of values for that key, and the key's values,
     * followed by successive keys and values from the entries() ordering
     */
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeInt(expectedValuesPerKey);
        Serialization.writeMultimap(this, stream);
        for (Map.Entry<K, V> entry : linkedEntries) {
            stream.writeObject(entry.getKey());
            stream.writeObject(entry.getValue());
        }
    }

    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        expectedValuesPerKey = stream.readInt();
        int distinctKeys = Serialization.readCount(stream);
        setMap(new LinkedHashMap<K, Collection<V>>(Maps.capacity(distinctKeys)));
        linkedEntries = new LinkedHashSet<Map.Entry<K, V>>(
                distinctKeys * expectedValuesPerKey);
        Serialization.populateMultimap(this, stream, distinctKeys);
        linkedEntries.clear(); // will clear and repopulate entries
        for (int i = 0; i < size(); i++) {
            @SuppressWarnings("unchecked") // reading data stored by writeObject
                    K key = (K) stream.readObject();
            @SuppressWarnings("unchecked") // reading data stored by writeObject
                    V value = (V) stream.readObject();
            linkedEntries.add(Maps.immutableEntry(key, value));
        }
    }

    private static final long serialVersionUID = 0;
}
