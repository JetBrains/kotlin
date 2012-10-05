package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.Source;
import com.google.dart.compiler.common.SourceInfo;

public class ChameleonJsExpression implements JsExpression {
  private JsExpression expression;

  public void resolve(JsExpression expression) {
    this.expression = expression;
  }

  @Override
  public boolean hasSideEffects() {
    return expression.hasSideEffects();
  }

  @Override
  public boolean isDefinitelyNotNull() {
    return expression.isDefinitelyNotNull();
  }

  @Override
  public boolean isDefinitelyNull() {
    return expression.isDefinitelyNull();
  }

  @Override
  public boolean isLeaf() {
    return expression.isLeaf();
  }

  @Override
  public JsStatement makeStmt() {
    return expression.makeStmt();
  }

  @Override
  public NodeKind getKind() {
    return expression.getKind();
  }

  @Override
  public void traverse(JsVisitor visitor, JsContext context) {
    expression.traverse(visitor, context);
  }

  @Override
  public SourceInfo getSourceInfo() {
    return expression.getSourceInfo();
  }

  @Override
  public void setSourceInfo(SourceInfo info) {
    expression.setSourceInfo(info);
  }

  @Override
  public void setSourceLocation(Source source, int line, int column, int startPosition, int length) {
    expression.setSourceLocation(source, line, column, startPosition, length);
  }

  @Override
  public Source getSource() {
    return expression.getSource();
  }

  @Override
  public int getLine() {
    return 0;
  }

  @Override
  public int getColumn() {
    return expression.getColumn();
  }

  @Override
  public int getStart() {
    return expression.getStart();
  }

  @Override
  public int getLength() {
    return expression.getLength();
  }
}
