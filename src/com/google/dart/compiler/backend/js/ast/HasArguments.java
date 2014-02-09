// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import java.util.List;

/**
 * Implemented by JavaScript objects that accept arguments.
 */
public interface HasArguments extends JsExpression {

  List<JsExpression> getArguments();
}
