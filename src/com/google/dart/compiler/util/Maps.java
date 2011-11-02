// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Utility methods for operating on memory-efficient maps. All maps of size 0 or
 * 1 are assumed to be immutable. All maps of size greater than 1 are assumed to
 * be mutable.
 */
public class Maps {

  private static final Class<?> MULTI_MAP_CLASS = HashMap.class;
  private static final Class<?> SINGLETON_MAP_CLASS =
      Collections.singletonMap(null, null).getClass();

  public static <K, V> Map<K, V> create() {
    return Collections.emptyMap();
  }

  public static <K, V> Map<K, V> create(K key, V value) {
    return Collections.singletonMap(key, value);
  }

  public static <K, V> Map<K, V> normalize(Map<K, V> map) {
    switch (map.size()) {
      case 0:
        return create();
      case 1: {
        if (map.getClass() == SINGLETON_MAP_CLASS) {
          return map;
        }
        K key = map.keySet().iterator().next();
        return create(key, map.get(key));
      }
      default:
        if (map.getClass() == MULTI_MAP_CLASS) {
          return map;
        }
        return new HashMap<K, V>(map);
    }
  }

  public static <K, V> Map<K, V> normalizeUnmodifiable(Map<K, V> map) {
    if (map.size() < 2) {
      return normalize(map);
    } else {
      // TODO: implement an UnmodifiableHashMap?
      return Collections.unmodifiableMap(normalize(map));
    }
  }

  public static <K, V> Map<K, V> put(Map<K, V> map, K key, V value) {
    switch (map.size()) {
      case 0:
        // Empty -> Singleton
        return Collections.singletonMap(key, value);
      case 1: {
        if (map.containsKey(key)) {
          return create(key, value);
        }
        // Singleton -> HashMap
        Map<K, V> result = new HashMap<K, V>();
        result.put(map.keySet().iterator().next(), map.values().iterator().next());
        result.put(key, value);
        return result;
      }
      default:
        // HashMap
        map.put(key, value);
        return map;
    }
  }

  public static <K, V> Map<K, V> putAll(Map<K, V> map, Map<K, V> toAdd) {
    switch (toAdd.size()) {
      case 0:
        // No-op.
        return map;
      case 1: {
        // Add one element.
        K key = toAdd.keySet().iterator().next();
        return put(map, key, toAdd.get(key));
      }
      default:
        // True list merge, result >= 2.
        switch (map.size()) {
          case 0:
            return new HashMap<K, V>(toAdd);
          case 1: {
            HashMap<K, V> result = new HashMap<K, V>();
            K key = map.keySet().iterator().next();
            result.put(key, map.get(key));
            result.putAll(toAdd);
            return result;
          }
          default:
            map.putAll(toAdd);
            return map;
        }
    }
  }

  /**
   * A variation of the put method which uses a LinkedHashMap.
   */
  public static <K, V> Map<K, V> putOrdered(Map<K, V> map, K key, V value) {
    switch (map.size()) {
      case 0:
        // Empty -> Singleton
        return Collections.singletonMap(key, value);
      case 1: {
        if (map.containsKey(key)) {
          return create(key, value);
        }
        // Singleton -> LinkedHashMap
        Map<K, V> result = new LinkedHashMap<K, V>();
        result.put(map.keySet().iterator().next(), map.values().iterator().next());
        result.put(key, value);
        return result;
      }
      default:
        // LinkedHashMap
        map.put(key, value);
        return map;
    }
  }

  public static <K, V> Map<K, V> remove(Map<K, V> map, K key) {
    switch (map.size()) {
      case 0:
        // Empty
        return map;
      case 1:
        // Singleton -> Empty
        if (map.containsKey(key)) {
          return create();
        }
        return map;
      case 2:
        // HashMap -> Singleton
        if (map.containsKey(key)) {
          map.remove(key);
          key = map.keySet().iterator().next();
          return create(key, map.get(key));
        }
        return map;
      default:
        // IdentityHashMap
        map.remove(key);
        return map;
    }
  }
}
