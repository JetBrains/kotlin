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
Implementation of resizable array with focus on minimizing memory usage by storing few initial array elements in object fields. Can also be used as a stack.
*/

public class ObjArray implements Serializable {

    public ObjArray() { }

    public ObjArray(int capacityHint) {
        if (capacityHint < 0) throw new IllegalArgumentException();
        if (capacityHint > FIELDS_STORE_SIZE) {
            data = new Object[capacityHint - FIELDS_STORE_SIZE];
        }
    }

    public final boolean isEmpty() {
        return size == 0;
    }

    public final int size() {
        return size;
    }

    public final void setSize(int newSize) {
        if (newSize < 0) throw new IllegalArgumentException();
        int N = size;
        if (newSize < N) {
            for (int i = newSize; i != N; ++i) {
                setImpl(i, null);
            }
        }else if (newSize > N) {
            if (newSize > FIELDS_STORE_SIZE) {
                ensureCapacity(newSize);
            }
        }
        size = newSize;
    }

    public final Object get(int index) {
        if (!(0 <= index && index < size)) throw invalidIndex(index, size);
        return getImpl(index);
    }

    public final void set(int index, Object value) {
        if (!(0 <= index && index < size)) throw invalidIndex(index, size);
        setImpl(index, value);
    }

    private Object getImpl(int index) {
        switch (index) {
            case 0: return f0;
            case 1: return f1;
            case 2: return f2;
            case 3: return f3;
            case 4: return f4;
            case 5: return f5;
        }
        return data[index - FIELDS_STORE_SIZE];
    }

    private void setImpl(int index, Object value) {
        switch (index) {
            case 0: f0 = value; break;
            case 1: f1 = value; break;
            case 2: f2 = value; break;
            case 3: f3 = value; break;
            case 4: f4 = value; break;
            case 5: f5 = value; break;
            default: data[index - FIELDS_STORE_SIZE] = value;
        }

    }

    public int indexOf(Object obj) {
        int N = size;
        for (int i = 0; i != N; ++i) {
            Object current = getImpl(i);
            if (current == obj || (current != null && current.equals(obj))) {
                return i;
            }
        }
        return -1;
    }

    public int lastIndexOf(Object obj) {
        for (int i = size; i != 0;) {
            --i;
            Object current = getImpl(i);
            if (current == obj || (current != null && current.equals(obj))) {
                return i;
            }
        }
        return -1;
    }

    public final Object peek() {
        int N = size;
        if (N == 0) throw invalidEmptyStackAccess();
        return getImpl(N - 1);
    }

    public final Object pop() {
        int N = size;
        --N;
        Object top;
        switch (N) {
            case -1: throw invalidEmptyStackAccess();
            case 0: top = f0; f0 = null; break;
            case 1: top = f1; f1 = null; break;
            case 2: top = f2; f2 = null; break;
            case 3: top = f3; f3 = null; break;
            case 4: top = f4; f4 = null; break;
            case 5: top = f5; f5 = null; break;
            default:
                top = data[N - FIELDS_STORE_SIZE];
                data[N - FIELDS_STORE_SIZE] = null;
        }
        size = N;
        return top;
    }

    public final void push(Object value) {
        add(value);
    }

    public final void add(Object value) {
        int N = size;
        if (N >= FIELDS_STORE_SIZE) {
            ensureCapacity(N + 1);
        }
        size = N + 1;
        setImpl(N, value);
    }

    public final void add(int index, Object value) {
        Object tmp;
        int N = size;
        if (!(0 <= index && index <= N)) throw invalidIndex(index, N + 1);
        switch (index) {
            case 0:
                if (N == 0) { f0 = value; break; }
                tmp = f0; f0 = value; value = tmp;
            case 1:
                if (N == 1) { f1 = value; break; }
                tmp = f1; f1 = value; value = tmp;
            case 2:
                if (N == 2) { f2 = value; break; }
                tmp = f2; f2 = value; value = tmp;
            case 3:
                if (N == 3) { f3 = value; break; }
                tmp = f3; f3 = value; value = tmp;
            case 4:
                if (N == 4) { f4 = value; break; }
                tmp = f4; f4 = value; value = tmp;
            case 5:
                if (N == 5) { f5 = value; break; }
                tmp = f5; f5 = value; value = tmp;

                index = FIELDS_STORE_SIZE;
            default:
                ensureCapacity(N + 1);
                if (index != N) {
                    System.arraycopy(data, index - FIELDS_STORE_SIZE,
                                     data, index - FIELDS_STORE_SIZE + 1,
                                     N - index);
                }
                data[index - FIELDS_STORE_SIZE] = value;
        }
        size = N + 1;
    }

    public final void remove(int index) {
        int N = size;
        if (!(0 <= index && index < N)) throw invalidIndex(index, N);
        --N;
        switch (index) {
            case 0:
                if (N == 0) { f0 = null; break; }
                f0 = f1;
            case 1:
                if (N == 1) { f1 = null; break; }
                f1 = f2;
            case 2:
                if (N == 2) { f2 = null; break; }
                f2 = f3;
            case 3:
                if (N == 3) { f3 = null; break; }
                f3 = f4;
            case 4:
                if (N == 4) { f4 = null; break; }
                f4 = f5;
            case 5:
                if (N == 5) { f5 = null; break; }
                f5 = data[0];

                index = FIELDS_STORE_SIZE;
            default:
                if (index != N) {
                    System.arraycopy(data, index - FIELDS_STORE_SIZE + 1,
                                     data, index - FIELDS_STORE_SIZE,
                                     N - index);
                }
                data[N - FIELDS_STORE_SIZE] = null;
        }
        size = N;
    }

    public final void clear() {
        int N = size;
        for (int i = 0; i != N; ++i) {
            setImpl(i, null);
        }
        size = 0;
    }

    public final Object[] toArray() {
        Object[] array = new Object[size];
        toArray(array, 0);
        return array;
    }

    public final void toArray(Object[] array) {
        toArray(array, 0);
    }

    public final void toArray(Object[] array, int offset) {
        int N = size;
        switch (N) {
            default:
                System.arraycopy(data, 0, array, offset + FIELDS_STORE_SIZE,
                                 N - FIELDS_STORE_SIZE);
            case 6: array[offset + 5] = f5;
            case 5: array[offset + 4] = f4;
            case 4: array[offset + 3] = f3;
            case 3: array[offset + 2] = f2;
            case 2: array[offset + 1] = f1;
            case 1: array[offset + 0] = f0;
            case 0: break;
        }
    }

    private void ensureCapacity(int minimalCapacity) {
        int required = minimalCapacity - FIELDS_STORE_SIZE;
        if (required <= 0) throw new IllegalArgumentException();
        if (data == null) {
            int alloc = FIELDS_STORE_SIZE * 2;
            if (alloc < required) {
                alloc = required;
            }
            data = new Object[alloc];
        } else {
            int alloc = data.length;
            if (alloc < required) {
                   if (alloc <= FIELDS_STORE_SIZE) {
                    alloc = FIELDS_STORE_SIZE * 2;
                } else {
                    alloc *= 2;
                }
                if (alloc < required) {
                    alloc = required;
                }
                Object[] tmp = new Object[alloc];
                if (size > FIELDS_STORE_SIZE) {
                    System.arraycopy(data, 0, tmp, 0,
                                     size - FIELDS_STORE_SIZE);
                }
                data = tmp;
            }
        }
    }

    private static RuntimeException invalidIndex(int index, int upperBound) {
        // \u2209 is "NOT ELEMENT OF"
        String msg = index+" \u2209 [0, "+upperBound+')';
        return new IndexOutOfBoundsException(msg);
    }

    private static RuntimeException invalidEmptyStackAccess() {
        throw new RuntimeException("Empty stack");
    }

    private void writeObject(ObjectOutputStream os) throws IOException {
        os.defaultWriteObject();
        int N = size;
        for (int i = 0; i != N; ++i) {
            Object obj = getImpl(i);
            os.writeObject(obj);
        }
    }

    private void readObject(ObjectInputStream is)
        throws IOException, ClassNotFoundException
    {
        is.defaultReadObject(); // It reads size
        int N = size;
        if (N > FIELDS_STORE_SIZE) {
            data = new Object[N - FIELDS_STORE_SIZE];
        }
        for (int i = 0; i != N; ++i) {
            Object obj = is.readObject();
            setImpl(i, obj);
        }
    }

    static final long serialVersionUID = 7448768847663119705L;

// Number of data elements
    private int size;

    private static final int FIELDS_STORE_SIZE = 6;
    private transient Object f0, f1, f2, f3, f4, f5;
    private transient Object[] data;
}
