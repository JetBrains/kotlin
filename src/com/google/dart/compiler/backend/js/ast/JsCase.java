// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

/**
 * Represents the JavaScript case statement.
 */
public final class JsCase extends JsSwitchMember {
    private JsExpression caseExpression;

    public JsCase() {
        super();
    }

    public JsExpression getCaseExpression() {
        return caseExpression;
    }

    public void setCaseExpression(JsExpression caseExpression) {
        this.caseExpression = caseExpression;
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitCase(this);
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {
        visitor.accept(caseExpression);
        super.acceptChildren(visitor);
    }
}
