// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import java.util.List;

abstract class JsExpressionImpl extends SourceInfoAwareJsNode implements JsExpression {
    /**
     * Determines whether or not this expression is a leaf, such as a
     * {@link JsNameRef}, {@link JsLiteral.JsBooleanLiteral}, and so on. Leaf expressions
     * never need to be parenthesized.
     */
    @Override
    public boolean isLeaf() {
        // Conservatively say that it isn't a leaf.
        // Individual subclasses can speak for themselves if they are a leaf.
        return false;
    }

    @Override
    public JsStatement makeStmt() {
        return new JsExpressionStatement(this);
    }

    protected abstract static class JsExpressionHasArguments extends JsExpressionImpl implements HasArguments {
        protected final List<JsExpression> arguments;

        public JsExpressionHasArguments(List<JsExpression> arguments) {
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
}
