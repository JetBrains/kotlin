// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * A JavaScript switch statement.
 */
public class JsSwitch extends SourceInfoAwareJsNode implements JsStatement {

    private final List<JsSwitchMember> cases = new ArrayList<JsSwitchMember>();
    private JsExpression expression;

    public JsSwitch() {
        super();
    }

    public List<JsSwitchMember> getCases() {
        return cases;
    }

    public JsExpression getExpression() {
        return expression;
    }

    public void setExpression(JsExpression expression) {
        this.expression = expression;
    }

    @Override
    public void accept(JsVisitor v) {
        v.visit(this);
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {
        visitor.accept(expression);
        visitor.acceptWithInsertRemove(cases);
    }
}
