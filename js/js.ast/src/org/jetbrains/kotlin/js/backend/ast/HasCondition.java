// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

/**
 * Implemented by JavaScript objects with conditional execution.
 */
public interface HasCondition {

  JsExpression getCondition();

  void setCondition(JsExpression condition);
}
