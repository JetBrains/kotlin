// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

public interface JsOperator {
    int INFIX = 0x02;
    int LEFT = 0x01;
    int POSTFIX = 0x04;
    int PREFIX = 0x08;

    int getPrecedence();

    String getSymbol();

    boolean isKeyword();

    boolean isLeftAssociative();

    boolean isPrecedenceLessThan(JsOperator other);

    boolean isValidInfix();

    boolean isValidPostfix();

    boolean isValidPrefix();
}
