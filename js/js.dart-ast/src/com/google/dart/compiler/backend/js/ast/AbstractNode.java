package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.backend.js.JsToStringGenerationVisitor;
import com.google.dart.compiler.backend.js.ast.metadata.HasMetadata;
import com.google.dart.compiler.util.TextOutputImpl;

abstract class AbstractNode extends HasMetadata implements JsNode {
    @Override
    public String toString() {
        TextOutputImpl out = new TextOutputImpl();
        new JsToStringGenerationVisitor(out).accept(this);
        return out.toString();
    }

    protected <T extends HasMetadata> T withMetadataFrom(T other) {
        this.copyMetadataFrom(other);
        //noinspection unchecked
        return (T) this;
    }
}
