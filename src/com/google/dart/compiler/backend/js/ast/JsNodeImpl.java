package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.common.SourceInfo;

abstract class JsNodeImpl extends AbstractNode {
    private SourceInfo sourceInfo;

    protected JsNodeImpl() {
    }

    @Override
    public SourceInfo getSourceInfo() {
        return sourceInfo;
    }

    @Override
    public void setSourceInfo(SourceInfo info) {
        sourceInfo = info;
    }
}