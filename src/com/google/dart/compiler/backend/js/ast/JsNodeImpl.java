package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.backend.js.JsToStringGenerationVisitor;
import com.google.dart.compiler.common.AbstractNode;
import com.google.dart.compiler.common.SourceInfo;
import com.google.dart.compiler.util.TextOutputImpl;

abstract class JsNodeImpl extends AbstractNode implements JsNode {
    protected JsNodeImpl() {
    }

    @Override
    public String toString() {
        TextOutputImpl out = new TextOutputImpl();
        new JsToStringGenerationVisitor(out).accept(this);
        return out.toString();
    }

    @Override
    public SourceInfo getSourceInfo() {
        return this;
    }

    public JsNode setSourceRef(SourceInfo info) {
        if (info != null) {
            setSourceInfo(info);
        }
        return this;
    }
}
