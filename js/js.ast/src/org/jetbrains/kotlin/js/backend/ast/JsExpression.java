// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class JsExpression extends SourceInfoAwareJsNode {
    /**
     * Determines whether or not this expression is a leaf, such as a
     * {@link JsNameRef}, {@link JsLiteral.JsBooleanLiteral}, and so on. Leaf expressions
     * never need to be parenthesized.
     */
    public boolean isLeaf() {
        // Conservatively say that it isn't a leaf.
        // Individual subclasses can speak for themselves if they are a leaf.
        return false;
    }

    @NotNull
    public JsStatement makeStmt() {
        return new JsExpressionStatement(this);
    }

    public abstract static class JsExpressionHasArguments extends JsExpression implements HasArguments {
        protected final List<JsExpression> arguments;

        protected JsExpressionHasArguments(List<JsExpression> arguments) {
            this.arguments = arguments;
        }

        @Override
        public List<JsExpression> getArguments() {
            return arguments;
        }
    }

    @Override
    public JsExpression source(Object info) {
        setSource(info);
        return this;
    }

    @NotNull
    @Override
    public abstract JsExpression deepCopy();
}
