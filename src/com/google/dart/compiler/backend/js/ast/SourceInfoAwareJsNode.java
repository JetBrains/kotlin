package com.google.dart.compiler.backend.js.ast;

abstract class SourceInfoAwareJsNode extends AbstractNode {
    private Object sourceInfo;

    @Override
    public Object getSourceInfo() {
        return sourceInfo;
    }

    @Override
    public void setSourceInfo(Object info) {
        sourceInfo = info;
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {
    }
}