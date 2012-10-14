// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.common;

public interface HasSourceInfo {
  /**
   * Return the source info associated with this object.
   */
  Object getSourceInfo();

  /**
   * Set the source info associated with this object.
   * @param info
   */
  void setSourceInfo(Object info);
}
