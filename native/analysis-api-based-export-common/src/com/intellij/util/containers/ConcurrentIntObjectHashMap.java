// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.intellij.util.containers;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * DO NOT MODIFY. THIS FILE WAS PORTED FROM THE OTHER REPOSITORY AND SHOULD BE TREATED AS GENERATED
 * Adapted from Doug Lea <a href="https://gee.cs.oswego.edu/dl/concurrency-interest/index.html">ConcurrentHashMap</a> to int keys
 * with following additions/changes:
 * - added hashing strategy argument
 * - added cacheOrGet convenience method
 * - Null values are NOT allowed
 * @author Doug Lea
 * @param <V> the type of mapped values
 * @deprecated Use {@link com.intellij.concurrency.ConcurrentCollectionFactory#createConcurrentIntObjectMap()} instead
 */
@Deprecated
@SuppressWarnings("ALL")
final class ConcurrentIntObjectHashMap<V> implements ConcurrentIntObjectMap<V> {
    private final ConcurrentHashMap<Integer, V> map = new ConcurrentHashMap<>();

    /* ---------------- Public operations -------------- */

    /**
     * Creates a new, empty map with the default initial table size (16).
     */
    ConcurrentIntObjectHashMap() {
    }

    /**
     * Creates a new, empty map with an initial table size
     * accommodating the specified number of elements without the need
     * to dynamically resize.
     *
     * @param initialCapacity The implementation performs internal
     * sizing to accommodate this many elements.
     * @throws IllegalArgumentException if the initial capacity of
     * elements is negative
     */
    ConcurrentIntObjectHashMap(int initialCapacity) {
    }

    // Original (since JDK1.2) Map methods

    /**
     * {@inheritDoc}
     */
    public int size() {
        return map.size();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code key.equals(k)},
     * then this method returns {@code v}; otherwise it returns
     * {@code null}.  (There can be at most one such mapping.)
     *
     * @throws NullPointerException if the specified key is null
     */
    public V get(int key) {
        return map.get(key);
    }

    /**
     * Tests if the specified object is a key in this table.
     *
     * @param  key possible key
     * @return {@code true} if and only if the specified object
     *         is a key in this table, as determined by the
     *         {@code equals} method; {@code false} otherwise
     * @throws NullPointerException if the specified key is null
     */
    public boolean containsKey(int key) {
        return map.containsKey(key);
    }

    /**
     * Returns {@code true} if this map maps one or more keys to the
     * specified value. Note: This method may require a full traversal
     * of the map, and is much slower than method {@code containsKey}.
     *
     * @param value value whose presence in this map is to be tested
     * @return {@code true} if this map maps one or more keys to the
     * specified value
     */
    @Override
    public boolean containsValue(@NotNull Object value) {
        return map.containsValue(value);
    }

    /**
     * Maps the specified key to the specified value in this table.
     * Neither the key nor the value can be null.
     *
     * <p>The value can be retrieved by calling the {@code get} method
     * with a key that is equal to the original key.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with {@code key}, or
     * {@code null} if there was no mapping for {@code key}
     */
    @Override
    public V put(int key, @NotNull V value) {
        return map.put(key, value);
    }

    /**
     * Removes the key (and its corresponding value) from this map.
     * This method does nothing if the key is not in the map.
     *
     * @param  key the key that needs to be removed
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}
     */
    @Override
    public V remove(int key) {
        return map.remove(key);
    }

    /**
     * Removes all of the mappings from this map.
     */
    public void clear() {
        map.clear();
    }


    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * The collection is backed by the map, so changes to the map are
     * reflected in the collection, and vice-versa.  The collection
     * supports element removal, which removes the corresponding
     * mapping from this map, via the {@code Iterator.remove},
     * {@code Collection.remove}, {@code removeAll},
     * {@code retainAll}, and {@code clear} operations.  It does not
     * support the {@code add} or {@code addAll} operations.
     *
     * <p>The view's iterators and spliterators are
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * <p>The view's {@code spliterator} reports {@link Spliterator#CONCURRENT}
     * and {@link Spliterator#NONNULL}.
     *
     * @return the collection view
     */
    public Collection<V> values() {
        return map.values();
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  The set supports element
     * removal, which removes the corresponding mapping from the map,
     * via the {@code Iterator.remove}, {@code Set.remove},
     * {@code removeAll}, {@code retainAll}, and {@code clear}
     * operations.
     *
     * <p>The view's iterators and spliterators are
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * <p>The view's {@code spliterator} reports {@link Spliterator#CONCURRENT},
     * {@link Spliterator#DISTINCT}, and {@link Spliterator#NONNULL}.
     *
     * @return the set view
     */
    public Set<Entry<V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the hash code value for this {@link Map}, i.e.,
     * the sum of, for each key-value pair in the map,
     * {@code key.hashCode() ^ value.hashCode()}.
     *
     * @return the hash code value for this map
     */
    public int hashCode() {
        return map.hashCode();
    }

    /**
     * Returns a string representation of this map.  The string
     * representation consists of a list of key-value mappings (in no
     * particular order) enclosed in braces ("{@code {}}").  Adjacent
     * mappings are separated by the characters {@code ", "} (comma
     * and space).  Each key-value mapping is rendered as the key
     * followed by an equals sign ("{@code =}") followed by the
     * associated value.
     *
     * @return a string representation of this map
     */
    public String toString() {
        return map.toString();
    }

    /**
     * Compares the specified object with this map for equality.
     * Returns {@code true} if the given object is a map with the same
     * mappings as this map.  This operation may return misleading
     * results if either map is concurrently modified during execution
     * of this method.
     *
     * @param o object to be compared for equality with this map
     * @return {@code true} if the specified object is equal to this map
     */
    @Override
    public boolean equals(Object o) {
        throw new UnsupportedOperationException();
    }

    // ConcurrentMap methods

    /**
     * {@inheritDoc}
     *
     * @return the previous value associated with the specified key,
     * or {@code null} if there was no mapping for the key
     */
    @Override
    public V putIfAbsent(int key, @NotNull V value) {
        return map.putIfAbsent(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(int key, @NotNull Object value) {
        return map.remove(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean replace(int key, @NotNull V oldValue, @NotNull V newValue) {
        return map.replace(key, oldValue, newValue);
    }

    /**
     * {@inheritDoc}
     *
     * @return the previous value associated with the specified key,
     * or {@code null} if there was no mapping for the key
     */
    public V replace(int key, @NotNull V value) {
        return map.replace(key, value);
    }

    // Overrides of JDK8+ Map extension method defaults

    /**
     * Returns the value to which the specified key is mapped, or the
     * given default value if this map contains no mapping for the
     * key.
     *
     * @param key the key whose associated value is to be returned
     * @param defaultValue the value to return if this map contains
     * no mapping for the given key
     * @return the mapping for the key, if present; else the default value
     */
    @Override
    public V getOrDefault(int key, V defaultValue) {
        return map.getOrDefault(key, defaultValue);
    }

    // Hashtable legacy methods

    /**
     * Tests if some key maps into the specified value in this table.
     *
     * <p>Note that this method is identical in functionality to
     * {@link #containsValue(Object)}, and exists solely to ensure
     * full compatibility with class {@link java.util.Hashtable},
     * which supported this method prior to introduction of the
     * Java Collections Framework.
     *
     * @param  value a value to search for
     * @return {@code true} if and only if some key maps to the
     *         {@code value} argument in this table as
     *         determined by the {@code equals} method;
     *         {@code false} otherwise
     * @throws NullPointerException if the specified value is null
     */
    public boolean contains(Object value) {
        return map.containsValue(value);
    }

    /**
     * Returns an enumeration of the keys in this table.
     *
     * @return an enumeration of the keys in this table
     */
    @Override
    public int [] keys() {
        throw new UnsupportedOperationException("keys() method is not supported in ConcurrentIntObjectHashMap");
    }

    /**
     * Returns an enumeration of the values in this table.
     *
     * @return an enumeration of the values in this table
     * @see #values()
     */
    @Override
    @NotNull
    public Enumeration<V> elements() {
        return map.elements();
    }

    // ConcurrentHashMap-only methods



    /* ---------------- Special Nodes -------------- */

    /**
     * @return value if there is no entry in the map, or corresponding value if entry already exists
     */
    @Override
    @NotNull
    public V cacheOrGet(final int key, @NotNull final V defaultValue) {
        V v = get(key);
        if (v != null) return v;
        V prev = putIfAbsent(key, defaultValue);
        return prev == null ? defaultValue : prev;
    }
}
