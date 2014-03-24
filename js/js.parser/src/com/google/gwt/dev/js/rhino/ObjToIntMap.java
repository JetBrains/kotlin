/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * The contents of this file are subject to the Netscape Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/NPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express oqr
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is Netscape
 * Communications Corporation.  Portions created by Netscape are
 * Copyright (C) 1997-2000 Netscape Communications Corporation. All
 * Rights Reserved.
 *
 * Contributor(s):
 * Igor Bukanov
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the GNU Public License (the "GPL"), in which case the
 * provisions of the GPL are applicable instead of those above.
 * If you wish to allow use of your version of this file only
 * under the terms of the GPL and not to allow others to use your
 * version of this file under the NPL, indicate your decision by
 * deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL.  If you do not delete
 * the provisions above, a recipient may use your version of this
 * file under either the NPL or the GPL.
 */
// Modified by Google

package com.google.gwt.dev.js.rhino;

import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Map to associate objects to integers.
 * The map does not synchronize any of its operation, so either use
 * it from a single thread or do own synchronization or perform all mutation
 * operations on one thread before passing the map to others
 *
 * @author Igor Bukanov
 *
 */

public class ObjToIntMap implements Serializable {

    public static final Object NULL_VALUE = new String("");

// Map implementation via hashtable,
// follows "The Art of Computer Programming" by Donald E. Knuth

// ObjToIntMap is a copy cat of ObjToIntMap with API adjusted to object keys

    public static class Iterator {

        Iterator(ObjToIntMap master) {
            this.master = master;
        }

        final void init(Object[] keys, int[] values, int keyCount) {
            this.keys = keys;
            this.values = values;
            this.cursor = -1;
            this.remaining = keyCount;
        }

        public void start() {
            master.initIterator(this);
            next();
        }

        public boolean done() {
            return remaining < 0;
        }

        public void next() {
            if (remaining == -1) Context.codeBug();
            if (remaining == 0) {
                remaining = -1;
                cursor = -1;
            }else {
                for (++cursor; ; ++cursor) {
                    Object key = keys[cursor];
                    if (key != null && key != DELETED) {
                        --remaining;
                        break;
                    }
                }
            }
        }

        public Object getKey() {
            Object key = keys[cursor];
            if (key == NULL_VALUE) { key = null; }
            return key;
        }

        public int getValue() {
            return values[cursor];
        }

        public void setValue(int value) {
            values[cursor] = value;
        }

        ObjToIntMap master;
        private int cursor;
        private int remaining;
        private Object[] keys;
        private int[] values;
    }

    public ObjToIntMap() {
        this(4);
    }

    public ObjToIntMap(int keyCountHint) {
        if (keyCountHint < 0) Context.codeBug();
        // Table grow when number of stored keys >= 3/4 of max capacity
        int minimalCapacity = keyCountHint * 4 / 3;
        int i;
        for (i = 2; (1 << i) < minimalCapacity; ++i) { }
        power = i;
        if (check && power < 2) Context.codeBug();
    }

    public boolean isEmpty() {
        return keyCount == 0;
    }

    public int size() {
        return keyCount;
    }

    public boolean has(Object key) {
        if (key == null) { key = NULL_VALUE; }
        return 0 <= findIndex(key);
    }

    /**
     * Get integer value assigned with key.
     * @return key integer value or defaultValue if key is absent
     */
    public int get(Object key, int defaultValue) {
        if (key == null) { key = NULL_VALUE; }
        int index = findIndex(key);
        if (0 <= index) {
            return values[index];
        }
        return defaultValue;
    }

    /**
     * Get integer value assigned with key.
     * @return key integer value
     * @throws RuntimeException if key does not exist
     */
    public int getExisting(Object key) {
        if (key == null) { key = NULL_VALUE; }
        int index = findIndex(key);
        if (0 <= index) {
            return values[index];
        }
        // Key must exist
        Context.codeBug();
        return 0;
    }

    public void put(Object key, int value) {
        if (key == null) { key = NULL_VALUE; }
        int index = ensureIndex(key);
        values[index] = value;
    }

    /**
     * If table already contains a key that equals to keyArg, return that key
     * while setting its value to zero, otherwise add keyArg with 0 value to
     * the table and return it.
     */
    public Object intern(Object keyArg) {
        boolean nullKey = false;
        if (keyArg == null) {
            nullKey = true;
            keyArg = NULL_VALUE;
        }
        int index = ensureIndex(keyArg);
        values[index] = 0;
        return (nullKey) ? null : keys[index];
    }

    public void remove(Object key) {
        if (key == null) { key = NULL_VALUE; }
        int index = findIndex(key);
        if (0 <= index) {
            keys[index] = DELETED;
            --keyCount;
        }
    }

    public void clear() {
        int i = keys.length;
        while (i != 0) {
            keys[--i] = null;
        }
        keyCount = 0;
        occupiedCount = 0;
    }

    public Iterator newIterator() {
        return new Iterator(this);
    }

    // The sole purpose of the method is to avoid accessing private fields
    // from the Iterator inner class to workaround JDK 1.1 compiler bug which
    // generates code triggering VerifierError on recent JVMs
    final void initIterator(Iterator i) {
        i.init(keys, values, keyCount);
    }

    /** Return array of present keys */
    public Object[] getKeys() {
        Object[] array = new Object[keyCount];
        getKeys(array, 0);
        return array;
    }

    public void getKeys(Object[] array, int offset) {
        int count = keyCount;
        for (int i = 0; count != 0; ++i) {
            Object key = keys[i];
            if (key != null && key != DELETED) {
                if (key == NULL_VALUE) { key = null; }
                array[offset] = key;
                ++offset;
                --count;
            }
        }
    }

    private static int tableLookupStep(int fraction, int mask, int power) {
        int shift = 32 - 2 * power;
        if (shift >= 0) {
            return ((fraction >>> shift) & mask) | 1;
        }
        else {
            return (fraction & (mask >>> -shift)) | 1;
        }
    }

    private int findIndex(Object key) {
        if (keys != null) {
            int hash = key.hashCode();
            int fraction = hash * A;
            int index = fraction >>> (32 - power);
               Object test = keys[index];
            if (test != null) {
                int N = 1 << power;
                if (test == key
                    || (values[N + index] == hash && test.equals(key)))
                {
                    return index;
                }
                // Search in table after first failed attempt
                int mask = N - 1;
                int step = tableLookupStep(fraction, mask, power);
                int n = 0;
                for (;;) {
                    if (check) {
                        if (n >= occupiedCount) Context.codeBug();
                        ++n;
                    }
                    index = (index + step) & mask;
                    test = keys[index];
                    if (test == null) {
                        break;
                    }
                    if (test == key
                        || (values[N + index] == hash && test.equals(key)))
                    {
                        return index;
                    }
                }
            }
        }
        return -1;
    }

// Insert key that is not present to table without deleted entries
// and enough free space
    private int insertNewKey(Object key, int hash) {
        if (check && occupiedCount != keyCount) Context.codeBug();
        if (check && keyCount == 1 << power) Context.codeBug();
        int fraction = hash * A;
        int index = fraction >>> (32 - power);
        int N = 1 << power;
        if (keys[index] != null) {
            int mask = N - 1;
            int step = tableLookupStep(fraction, mask, power);
            int firstIndex = index;
            do {
                if (check && keys[index] == DELETED) Context.codeBug();
                index = (index + step) & mask;
                if (check && firstIndex == index) Context.codeBug();
            } while (keys[index] != null);
        }
        keys[index] = key;
        values[N + index] = hash;
        ++occupiedCount;
        ++keyCount;

        return index;
    }

    private void rehashTable() {
        if (keys == null) {
            if (check && keyCount != 0) Context.codeBug();
            if (check && occupiedCount != 0) Context.codeBug();
            int N = 1 << power;
            keys = new Object[N];
            values = new int[2 * N];
        }
        else {
            // Check if removing deleted entries would free enough space
            if (keyCount * 2 >= occupiedCount) {
                // Need to grow: less then half of deleted entries
                ++power;
            }
            int N = 1 << power;
            Object[] oldKeys = keys;
            int[] oldValues = values;
            int oldN = oldKeys.length;
            keys = new Object[N];
            values = new int[2 * N];

            int remaining = keyCount;
            occupiedCount = keyCount = 0;
            for (int i = 0; remaining != 0; ++i) {
                Object key = oldKeys[i];
                if (key != null && key != DELETED) {
                    int keyHash = oldValues[oldN + i];
                    int index = insertNewKey(key, keyHash);
                    values[index] = oldValues[i];
                    --remaining;
                }
            }
        }
    }

// Ensure key index creating one if necessary
    private int ensureIndex(Object key) {
        int hash = key.hashCode();
        int index = -1;
        int firstDeleted = -1;
        if (keys != null) {
            int fraction = hash * A;
            index = fraction >>> (32 - power);
            Object test = keys[index];
            if (test != null) {
                int N = 1 << power;
                if (test == key
                    || (values[N + index] == hash && test.equals(key)))
                {
                    return index;
                }
                if (test == DELETED) {
                    firstDeleted = index;
                }

                // Search in table after first failed attempt
                int mask = N - 1;
                int step = tableLookupStep(fraction, mask, power);
                int n = 0;
                for (;;) {
                    if (check) {
                        if (n >= occupiedCount) Context.codeBug();
                        ++n;
                    }
                    index = (index + step) & mask;
                    test = keys[index];
                    if (test == null) {
                        break;
                    }
                    if (test == key
                        || (values[N + index] == hash && test.equals(key)))
                    {
                        return index;
                    }
                    if (test == DELETED && firstDeleted < 0) {
                        firstDeleted = index;
                    }
                }
            }
        }
        // Inserting of new key
        if (check && keys != null && keys[index] != null)
            Context.codeBug();
        if (firstDeleted >= 0) {
            index = firstDeleted;
        }
        else {
            // Need to consume empty entry: check occupation level
            if (keys == null || occupiedCount * 4 >= (1 << power) * 3) {
                // Too litle unused entries: rehash
                rehashTable();
                return insertNewKey(key, hash);
            }
            ++occupiedCount;
        }
        keys[index] = key;
        values[(1 << power) + index] = hash;
        ++keyCount;
        return index;
    }

    private void writeObject(ObjectOutputStream out)
        throws IOException
    {
        out.defaultWriteObject();

        int count = keyCount;
        for (int i = 0; count != 0; ++i) {
            Object key = keys[i];
            if (key != null && key != DELETED) {
                --count;
                out.writeObject(key);
                out.writeInt(values[i]);
            }
        }
    }

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();

        int writtenKeyCount = keyCount;
        if (writtenKeyCount != 0) {
            keyCount = 0;
            int N = 1 << power;
            keys = new Object[N];
            values = new int[2 * N];
            for (int i = 0; i != writtenKeyCount; ++i) {
                Object key = in.readObject();
                int hash = key.hashCode();
                int index = insertNewKey(key, hash);
                values[index] = in.readInt();
            }
        }
    }

    static final long serialVersionUID = 3396438333234169727L;

// A == golden_ratio * (1 << 32) = ((sqrt(5) - 1) / 2) * (1 << 32)
// See Knuth etc.
    private static final int A = 0x9e3779b9;

    private static final Object DELETED = new Object();

// Structure of kyes and values arrays (N == 1 << power):
// keys[0 <= i < N]: key value or null or DELETED mark
// values[0 <= i < N]: value of key at keys[i]
// values[N <= i < 2*N]: hash code of key at keys[i-N]

    private transient Object[] keys;
    private transient int[] values;

    private int power;
    private int keyCount;
    private transient int occupiedCount; // == keyCount + deleted_count

// If true, enables consitency checks
    private static final boolean check = false;

/* TEST START

    public static void main(String[] args) {
        if (!check) {
            System.err.println("Set check to true and re-run");
            throw new RuntimeException("Set check to true and re-run");
        }

        ObjToIntMap map;
        map = new ObjToIntMap(0);
        testHash(map, 3);
        map = new ObjToIntMap(0);
        testHash(map, 10 * 1000);
        map = new ObjToIntMap();
        testHash(map, 10 * 1000);
        map = new ObjToIntMap(30 * 1000);
        testHash(map, 10 * 100);
        map.clear();
        testHash(map, 4);
        map = new ObjToIntMap(0);
        testHash(map, 10 * 100);
    }

    private static void testHash(ObjToIntMap map, int N) {
        System.out.print("."); System.out.flush();
        for (int i = 0; i != N; ++i) {
            Object key = testKey(i);
            check(-1 == map.get(key, -1));
            map.put(key, i);
            check(i == map.get(key, -1));
        }

        System.out.print("."); System.out.flush();
        for (int i = 0; i != N; ++i) {
            Object key = testKey(i);
            map.put(key, i);
            check(i == map.get(key, -1));
        }

        check(map.size() == N);

        System.out.print("."); System.out.flush();
        Object[] keys = map.getKeys();
        check(keys.length == N);
        for (int i = 0; i != N; ++i) {
            Object key = keys[i];
            check(map.has(key));
        }


        System.out.print("."); System.out.flush();
        for (int i = 0; i != N; ++i) {
            Object key = testKey(i);
            check(i == map.get(key, -1));
        }

        int Nsqrt = -1;
        for (int i = 0; ; ++i) {
            if (i * i >= N) {
                Nsqrt = i;
                break;
            }
        }

        System.out.print("."); System.out.flush();
        for (int i = 0; i != N; ++i) {
            Object key = testKey(i * i);
            map.put(key, i);
            check(i == map.get(key, -1));
        }

        check(map.size() == 2 * N - Nsqrt);

        System.out.print("."); System.out.flush();
        for (int i = 0; i != N; ++i) {
            Object key = testKey(i * i);
            check(i == map.get(key, -1));
        }

        System.out.print("."); System.out.flush();
        for (int i = 0; i != N; ++i) {
            Object key = testKey(-1 - i * i);
            map.put(key, i);
            check(i == map.get(key, -1));
        }

        check(map.size() == 3 * N - Nsqrt);

        System.out.print("."); System.out.flush();
        for (int i = 0; i != N; ++i) {
            Object key = testKey(-1 - i * i);
            map.remove(key);
            check(!map.has(key));
        }

        check(map.size() == 2 * N - Nsqrt);

        System.out.print("."); System.out.flush();
        for (int i = 0; i != N; ++i) {
            Object key = testKey(i * i);
            check(i == map.get(key, -1));
        }

        System.out.print("."); System.out.flush();
        for (int i = 0; i != N; ++i) {
            Object key = testKey(i);
            int j = intSqrt(i);
            if (j * j == i) {
                check(j == map.get(key, -1));
            }else {
                check(i == map.get(key, -1));
            }
        }

        System.out.print("."); System.out.flush();
        for (int i = 0; i != N; ++i) {
            Object key = testKey(i * i);
            map.remove(key);
            check(-2 == map.get(key, -2));
        }

        System.out.print("."); System.out.flush();
        for (int i = 0; i != N; ++i) {
            Object key = testKey(i);
            map.put(key, i);
            check(i == map.get(key, -2));
        }

        check(map.size() == N);

        System.out.print("."); System.out.flush();
        for (int i = 0; i != N; ++i) {
            Object key = testKey(i);
            check(i == map.get(key, -1));
        }

        System.out.print("."); System.out.flush();
        ObjToIntMap copy = (ObjToIntMap)writeAndRead(map);
        check(copy.size() == N);

        for (int i = 0; i != N; ++i) {
            Object key = testKey(i);
            check(i == copy.get(key, -1));
        }

        System.out.print("."); System.out.flush();
        checkSameMaps(copy, map);

        System.out.println(); System.out.flush();
    }

    private static void checkSameMaps(ObjToIntMap map1, ObjToIntMap map2) {
        check(map1.size() == map2.size());
        Object[] keys = map1.getKeys();
        check(keys.length == map1.size());
        for (int i = 0; i != keys.length; ++i) {
            check(map1.get(keys[i], -1) == map2.get(keys[i], -1));
        }
    }

    private static void check(boolean condition) {
        if (!condition) Context.codeBug();
    }

    private static Object[] testPool;

    private static Object testKey(int i) {
        int MAX_POOL = 100;
        if (0 <= i && i < MAX_POOL) {
            if (testPool != null && testPool[i] != null) {
                return testPool[i];
            }
        }
        Object x = new Double(i + 0.5);
        if (0 <= i && i < MAX_POOL) {
            if (testPool == null) {
                testPool = new Object[MAX_POOL];
            }
            testPool[i] = x;
        }
        return x;
    }

    private static int intSqrt(int i) {
        int approx = (int)Math.sqrt(i) + 1;
        while (approx * approx > i) {
            --approx;
        }
        return approx;
    }

    private static Object writeAndRead(Object obj) {
        try {
            java.io.ByteArrayOutputStream
                bos = new java.io.ByteArrayOutputStream();
            java.io.ObjectOutputStream
                out = new java.io.ObjectOutputStream(bos);
            out.writeObject(obj);
            out.close();
            byte[] data = bos.toByteArray();
            java.io.ByteArrayInputStream
                bis = new java.io.ByteArrayInputStream(data);
            java.io.ObjectInputStream
                in = new java.io.ObjectInputStream(bis);
            Object result = in.readObject();
            in.close();
            return result;
        }catch (Exception ex) {
            throw new RuntimeException("Unexpected");
        }
    }

// TEST END */

}
