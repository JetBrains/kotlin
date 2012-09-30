// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Utility methods for operating on memory-efficient lists. All lists of size 0
 * or 1 are assumed to be immutable. All lists of size greater than 1 are
 * assumed to be mutable.
 */
public class Lists {
    private Lists() {
    }

    public static <T> List<T> add(List<T> list, int index, T toAdd) {
    switch (list.size()) {
      case 0:
        // Empty -> Singleton
        if (index != 0) {
          throw newIndexOutOfBounds(list, index);
        }
        return Collections.singletonList(toAdd);
      case 1: {
        // Singleton -> ArrayList
        List<T> result = new ArrayList<T>(2);
        switch (index) {
          case 0:
            result.add(toAdd);
            result.add(list.get(0));
            return result;
          case 1:
            result.add(list.get(0));
            result.add(toAdd);
            return result;
          default:
            throw newIndexOutOfBounds(list, index);
        }
      }
      default:
        // ArrayList
        list.add(index, toAdd);
        return list;
    }
  }

  public static <T> List<T> add(List<T> list, T toAdd) {
    switch (list.size()) {
      case 0:
        // Empty -> Singleton
        return Collections.singletonList(toAdd);
      case 1: {
        // Singleton -> ArrayList
        List<T> result = new ArrayList<T>(2);
        result.add(list.get(0));
        result.add(toAdd);
        return result;
      }
      default:
        // ArrayList
        list.add(toAdd);
        return list;
    }
  }

  public static <T> List<T> addAll(List<T> list, int index, List<T> toAdd) {
    switch (toAdd.size()) {
      case 0:
        // No-op.
        return list;
      case 1:
        // Add one element.
        return add(list, index, toAdd.get(0));
      default:
        // True list merge, result >= 2.
        switch (list.size()) {
          case 0:
            if (index != 0) {
              throw newIndexOutOfBounds(list, index);
            }
            return new ArrayList<T>(toAdd);
          case 1: {
            List<T> result = new ArrayList<T>(1 + toAdd.size());
            switch (index) {
              case 0:
                result.addAll(toAdd);
                result.add(list.get(0));
                return result;
              case 1:
                result.add(list.get(0));
                result.addAll(toAdd);
                return result;
              default:
                throw newIndexOutOfBounds(list, index);
            }
          }
          default:
            list.addAll(index, toAdd);
            return list;
        }
    }
  }

  public static <T> List<T> addAll(List<T> list, List<T> toAdd) {
    switch (toAdd.size()) {
      case 0:
        // No-op.
        return list;
      case 1:
        // Add one element.
        return add(list, toAdd.get(0));
      default:
        // True list merge, result >= 2.
        switch (list.size()) {
          case 0:
            return new ArrayList<T>(toAdd);
          case 1: {
            List<T> result = new ArrayList<T>(1 + toAdd.size());
            result.add(list.get(0));
            result.addAll(toAdd);
            return result;
          }
          default:
            list.addAll(toAdd);
            return list;
        }
    }
  }

  public static <T> List<T> addAll(List<T> list, T... toAdd) {
    switch (toAdd.length) {
      case 0:
        // No-op.
        return list;
      case 1:
        // Add one element.
        return add(list, toAdd[0]);
      default:
        // True list merge, result >= 2.
        switch (list.size()) {
          case 0:
            return new ArrayList<T>(Arrays.asList(toAdd));
          case 1: {
            List<T> result = new ArrayList<T>(1 + toAdd.length);
            result.add(list.get(0));
            result.addAll(Arrays.asList(toAdd));
            return result;
          }
          default:
            list.addAll(Arrays.asList(toAdd));
            return list;
        }
    }
  }

  public static <T> List<T> create() {
    return Collections.emptyList();
  }

  public static <T> List<T> create(Collection<T> collection) {
    switch (collection.size()) {
      case 0:
        return create();
      default:
        return new ArrayList<T>(collection);
    }
  }

  public static <T> List<T> create(List<T> list) {
    switch (list.size()) {
      case 0:
        return create();
      case 1:
        return create(list.get(0));
      default:
        return new ArrayList<T>(list);
    }
  }

  public static <T> List<T> create(T item) {
    return Collections.singletonList(item);
  }

  public static <T> List<T> create(T... items) {
    switch (items.length) {
      case 0:
        return create();
      case 1:
        return create(items[0]);
      default:
        return new ArrayList<T>(Arrays.asList(items));
    }
  }

  public static <T> List<T> remove(List<T> list, int toRemove) {
    switch (list.size()) {
      case 0:
        // Empty
        throw newIndexOutOfBounds(list, toRemove);
      case 1:
        // Singleton -> Empty
        if (toRemove == 0) {
          return Collections.emptyList();
        } else {
          throw newIndexOutOfBounds(list, toRemove);
        }
      case 2:
        // ArrayList -> Singleton
        switch (toRemove) {
          case 0:
            return Collections.singletonList(list.get(1));
          case 1:
            return Collections.singletonList(list.get(0));
          default:
            throw newIndexOutOfBounds(list, toRemove);
        }
      default:
        // ArrayList
        list.remove(toRemove);
        return list;
    }
  }

  public static <T> List<T> set(List<T> list, int index, T e) {
    switch (list.size()) {
      case 0:
        // Empty
        throw newIndexOutOfBounds(list, index);
      case 1:
        // Singleton
        if (index == 0) {
          return Collections.singletonList(e);
        } else {
          throw newIndexOutOfBounds(list, index);
        }
      default:
        // ArrayList
        list.set(index, e);
        return list;
    }
  }

  public static <T extends Comparable<? super T>> List<T> sort(List<T> list) {
    if (list.size() > 1) {
      Collections.sort(list);
    }
    return list;
  }

  public static <T> List<T> sort(List<T> list, Comparator<? super T> sort) {
    if (list.size() > 1) {
      Collections.sort(list, sort);
    }
    return list;
  }

  private static <T> IndexOutOfBoundsException newIndexOutOfBounds(List<T> list, int index) {
    return new IndexOutOfBoundsException("Index: " + index + ", Size: " + list.size());
  }
}
