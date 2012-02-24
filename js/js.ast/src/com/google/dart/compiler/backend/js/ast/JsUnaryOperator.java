// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

/**
 * A JavaScript unary operator.
 */
public enum JsUnaryOperator implements JsOperator {

  /*
   * Precedence indices from "JavaScript - The Definitive Guide" 4th Edition
   * (page 57)
   */
  BIT_NOT("~", 14, PREFIX), DEC("--", 14, POSTFIX | PREFIX), DELETE("delete", 14, PREFIX), INC(
      "++", 14, POSTFIX | PREFIX), NEG("-", 14, PREFIX), POS("+", 14, PREFIX),
  NOT("!", 14, PREFIX), TYPEOF("typeof", 14, PREFIX), VOID("void", 14, PREFIX);

  private final int mask;
  private final int precedence;
  private final String symbol;

  private JsUnaryOperator(String symbol, int precedence, int mask) {
    this.symbol = symbol;
    this.precedence = precedence;
    this.mask = mask;
  }

  @Override
  public int getPrecedence() {
    return precedence;
  }

  @Override
  public String getSymbol() {
    return symbol;
  }

  @Override
  public boolean isKeyword() {
    return this == DELETE || this == TYPEOF || this == VOID;
  }

  @Override
  public boolean isLeftAssociative() {
    return (mask & LEFT) != 0;
  }

  public boolean isModifying() {
    return this == DEC || this == INC || this == DELETE;
  }

  @Override
  public boolean isPrecedenceLessThan(JsOperator other) {
    return precedence < other.getPrecedence();
  }

  @Override
  public boolean isValidInfix() {
    return (mask & INFIX) != 0;
  }

  @Override
  public boolean isValidPostfix() {
    return (mask & POSTFIX) != 0;
  }

  @Override
  public boolean isValidPrefix() {
    return (mask & PREFIX) != 0;
  }

  @Override
  public String toString() {
    return symbol;
  }
}
