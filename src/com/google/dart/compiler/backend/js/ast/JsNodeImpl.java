package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.backend.js.JsSourceGenerationVisitor;
import com.google.dart.compiler.backend.js.JsToStringGenerationVisitor;
import com.google.dart.compiler.common.AbstractNode;
import com.google.dart.compiler.common.SourceInfo;
import com.google.dart.compiler.util.TextOutputImpl;

abstract class JsNodeImpl extends AbstractNode implements JsNode {
    protected JsNodeImpl() {
    }

    // Causes source generation to delegate to the one visitor
    public final String toSource() {
        TextOutputImpl out = new TextOutputImpl();
        JsSourceGenerationVisitor v = new JsSourceGenerationVisitor(out);
        v.accept(this);
        return out.toString();
    }

    // Causes source generation to delegate to the one visitor
    @Override
    public String toString() {
        TextOutputImpl out = new TextOutputImpl();
        JsToStringGenerationVisitor v = new JsToStringGenerationVisitor(out);
        v.accept(this);
        return out.toString();
    }

    @Override
    public SourceInfo getSourceInfo() {
        return this;
    }

    public JsNode setSourceRef(SourceInfo info) {
        if (info != null) {
            this.setSourceInfo(info);
        }
        return this;
    }
}
