package com.google.dart.compiler.backend.js.ast;

public class ChameleonJsExpression implements JsExpression {
    private JsExpression expression;

    public ChameleonJsExpression(JsExpression initialExpression) {
        expression = initialExpression;
    }

    public ChameleonJsExpression() {
    }

    public void resolve(JsExpression expression) {
        this.expression = expression;
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
    public void accept(JsVisitor visitor) {
        expression.accept(visitor);
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {
        expression.acceptChildren(visitor);
    }

    @Override
    public Object getSource() {
        return expression.getSource();
    }

    @Override
    public void setSource(Object info) {
        expression.setSource(info);
    }

    @Override
    public JsExpression source(Object info) {
        return expression.source(info);
    }
}
