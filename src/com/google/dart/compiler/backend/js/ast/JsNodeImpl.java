package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.common.SourceInfo;

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
}