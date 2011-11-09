// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package compiler.common;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.MapMaker;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.concurrent.ConcurrentMap;

/**
 * Manages the life cycle, uniqueness, and object identity invariants of
 * {@link Name}.
 */
final class NameFactory {
  /*
   * TODO: does this actually save memory in practice? For each interned Name,
   * we use:
   * 
   * 1) The data array (n bytes).
   * 
   * 2) The Name object (2 fields)
   * 
   * 3) The RealKey (1 field + 4 fields from Reference).
   * 
   * 4) ConcurrentMap.Entry (4 fields)
   * 
   * Of course, a Map is really much heavier than we need, all we really need is
   * a Set where you can retrieve an object already in the Set. A bare linear
   * probe hash table of RealKey would let us get rid of the Entry object.
   */

  /**
   * The whole point of this class is to cheaply create a light-weight key that
   * doesn't need to make its own copy of the data. This object is constructed
   * with equality semantics to {com.google.dart.compiler.backend.common.NameFactory.RealKey}, for doing cheap map lookups.
   */
  private static final class FakeKey {
    private final char[] data;
    private final int hashCode;
    private final int length;
    private final int offset;

    public FakeKey(char[] data, int hashCode) {
      this(data, 0, data.length, hashCode);
    }

    public FakeKey(char[] data, int offset, int length, int hashCode) {
      this.data = data;
      this.offset = offset;
      this.length = length;
      this.hashCode = hashCode;
    }

    @Override
    public boolean equals(Object obj) {
      /*
       * NOTE: this ONLY WORKS for comparisons to RealKey. But that's all we
       * need since we only store real keys in the map.
       */
      Name name = ((RealKey) obj).get();
      if (name == null) {
        return false;
      }
      return equalsName(name);
    }

    public boolean equalsName(Name name) {
      if (this.length != name.data.length) {
        return false;
      }
      int itMine = this.offset;
      int itTheirs = 0;
      int endMine = this.offset + this.length;
      while (itMine < endMine) {
        if (this.data[itMine++] != name.data[itTheirs++]) {
          return false;
        }
      }
      return true;
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

    @Override
    public String toString() {
      return "FakeKey(" + String.valueOf(data, offset, length) + ")";
    }
  }

  /**
   * Adapted from {@link com.google.common.collect.Interners#newWeakInterner()}.
   */
  private static final class RealKey extends WeakReference<Name> {
    /**
     * Must store the hashCode locally so we can be removed from
     * {com.google.dart.compiler.backend.common.NameFactory#map} after our referent is cleared.
     */
    private final int hashCode;

    public RealKey(Name name, ReferenceQueue<Name> queue) {
      super(name, queue);
      hashCode = name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof FakeKey) {
        Name referent = get();
        if (referent == null) {
          return false;
        }
        return ((FakeKey) obj).equalsName(referent);
      }
      if (obj instanceof RealKey) {
        Name referent = get();
        if (referent == null) {
          return false;
        }
        Name otherReferent = ((RealKey) obj).get();
        if (otherReferent == null) {
          return false;
        }
        return Arrays.equals(referent.data, otherReferent.data);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

    @Override
    public String toString() {
      Name referent = get();
      return "RealKey(" + referent + ")";
    }
  }

  private final ConcurrentMap<Object, RealKey> map = new MapMaker().makeMap();
  private final ReferenceQueue<Name> queue = new ReferenceQueue<Name>();

  /**
   * Return the Name corresponding to the data. An internal reference to
   * <code>data</code> may be kept.
   */
  public Name of(char[] data) {
    cleanUp();
    int hashCode = Name.computeHashCode(data, 0, data.length);
    FakeKey fakeKey = new FakeKey(data, hashCode);
    Name result = get(fakeKey);
    if (result == null) {
      result = put(new Name(data, hashCode));
    }
    return result;
  }

  /**
   * Return the Name corresponding to the data. An internal copy of the data is
   * made.
   */
  public Name of(char[] data, int offset, int length) {
    cleanUp();
    int hashCode = Name.computeHashCode(data, offset, length);
    FakeKey fakeKey = new FakeKey(data, offset, length, hashCode);
    Name result = get(fakeKey);
    if (result == null) {
      char[] copy = new char[length];
      System.arraycopy(data, offset, copy, 0, length);
      result = put(new Name(copy, hashCode));
    }
    return result;
  }

  @VisibleForTesting
  void cleanUp() {
    RealKey item;
    while ((item = (RealKey) queue.poll()) != null) {
      map.remove(item);
    }
  }

  @VisibleForTesting
  WeakReference<Name> getRefFor(Name name) {
    return map.get(new FakeKey(name.data, name.hashCode()));
  }

  @VisibleForTesting
  int numEntries() {
    return map.size();
  }

  private Name get(FakeKey fakeKey) {
    RealKey realKey = map.get(fakeKey);
    if (realKey != null) {
      return realKey.get();
    }
    return null;
  }

  private Name put(Name name) {
    RealKey realKey = new RealKey(name, queue);
    while (true) {
      RealKey sneakyRef = map.putIfAbsent(realKey, realKey);
      if (sneakyRef == null) {
        return name;
      } else {
        Name canonical = sneakyRef.get();
        if (canonical != null) {
          return canonical;
        }
      }
    }
  }
}
