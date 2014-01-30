package com.google.dart.compiler.backend.js.ast;

abstract class SourceInfoAwareJsNode extends AbstractNode {
    private Object source;

    @Override
    public Object getSource() {
        return source;
    }

    @Override
    public void setSource(Object info) {
        source = info;
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {
    }

    @Override
    public JsNode source(Object info) {
        source = info;
        return this;
    }
}