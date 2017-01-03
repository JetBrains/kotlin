// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility methods for operating on memory-efficient maps. All maps of size 0 or
 * 1 are assumed to be immutable. All maps of size greater than 1 are assumed to
 * be mutable.
 */
public class Maps {
    private Maps() {
    }

    public static <K, V> Map<K, V> put(Map<K, V> map, K key, V value) {
        switch (map.size()) {
            case 0:
                // Empty -> Singleton
                return Collections.singletonMap(key, value);
            case 1: {
                if (map.containsKey(key)) {
                    return Collections.singletonMap(key, value);
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
}
