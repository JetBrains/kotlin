package com.google.dart.compiler.backend.js.ast;

abstract class JsNodeImpl extends AbstractNode {
    private Object sourceInfo;

    protected JsNodeImpl() {
    }

    @Override
    public Object getSourceInfo() {
        return sourceInfo;
    }

    @Override
    public void setSourceInfo(Object info) {
        sourceInfo = info;
    }

    @Override
    public void acceptChildren(JsVisitor visitor, JsContext context) {
    }
}