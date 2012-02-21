// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js;

/**
 *  An unchecked wrapper exception to interop with Rhino.
 */
class UncheckedJsParserException extends RuntimeException {

  private final JsParserException parserException;

  public UncheckedJsParserException(JsParserException parserException) {
    this.parserException = parserException;
  }

  public JsParserException getParserException() {
    return parserException;
  }
}
