// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

/**
 * Represents the operator in a JavaScript binary operation.
 */
public enum JsBinaryOperator implements JsOperator {
  /*
   * Precedence indices from "JavaScript - The Definitive Guide" 4th Edition
   * (page 57)
   *
   *
   * Precedence 15 is for really important things that have their own AST
   * classes.
   *
   * Precedence 14 is for unary operators.
   */

  MUL("*", 13, LEFT | INFIX), DIV("/", 13, LEFT | INFIX), MOD("%", 13, LEFT
      | INFIX),

  ADD("+", 12, LEFT | INFIX), SUB("-", 12, LEFT | INFIX),

  SHL("<<", 11, LEFT | INFIX), SHR(">>", 11, LEFT | INFIX), SHRU(">>>", 11,
      LEFT | INFIX),

  LT("<", 10, LEFT | INFIX), LTE("<=", 10, LEFT | INFIX), GT(">", 10, LEFT
      | INFIX), GTE(">=", 10, LEFT | INFIX), INSTANCEOF("instanceof", 10, LEFT
      | INFIX), INOP("in", 10, LEFT | INFIX),

  EQ("==", 9, LEFT | INFIX), NEQ("!=", 9, LEFT | INFIX), REF_EQ("===", 9, LEFT
      | INFIX), REF_NEQ("!==", 9, LEFT | INFIX),

  BIT_AND("&", 8, LEFT | INFIX),

  BIT_XOR("^", 7, LEFT | INFIX),

  BIT_OR("|", 6, LEFT | INFIX),

  AND("&&", 5, LEFT | INFIX),

  OR("||", 4, LEFT | INFIX),

  // Precedence 3 is for the condition operator.

  // These assignment operators are right-associative.
  ASG("=", 2, INFIX), ASG_ADD("+=", 2, INFIX), ASG_SUB("-=", 2, INFIX), ASG_MUL(
      "*=", 2, INFIX), ASG_DIV("/=", 2, INFIX), ASG_MOD("%=", 2, INFIX), ASG_SHL(
      "<<=", 2, INFIX), ASG_SHR(">>=", 2, INFIX), ASG_SHRU(">>>=", 2, INFIX), ASG_BIT_AND(
      "&=", 2, INFIX), ASG_BIT_OR("|=", 2, INFIX), ASG_BIT_XOR("^=", 2, INFIX),

  COMMA(",", 1, LEFT | INFIX);

  private final int mask;
  private final int precedence;
  private final String symbol;

  private JsBinaryOperator(String symbol, int precedence, int mask) {
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

  public boolean isAssignment() {
    return getPrecedence() == ASG.getPrecedence();
  }

  @Override
  public boolean isKeyword() {
    return this == INSTANCEOF || this == INOP;
  }

  @Override
  public boolean isLeftAssociative() {
    return (mask & LEFT) != 0;
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
