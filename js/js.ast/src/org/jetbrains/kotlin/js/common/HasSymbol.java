// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.common;

public interface HasSymbol {
  /**
   * @return Return the original user visible name for a Object represented
   * in a source map.
   */
  Symbol getSymbol();
}
