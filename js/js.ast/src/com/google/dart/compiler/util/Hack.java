// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.util;

/**
 * Utility to hack around untypable (or yet to be) code.
 */
public class Hack {
  public static <T> T cast(Object o) {
    @SuppressWarnings("unchecked")
    T t = (T) o;
    return t;
  }
}
