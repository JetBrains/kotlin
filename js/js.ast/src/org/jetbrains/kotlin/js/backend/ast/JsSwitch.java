// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.kotlin.js.util.AstUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A JavaScript switch statement.
 */
public class JsSwitch extends SourceInfoAwareJsNode implements JsStatement {

    private final List<JsSwitchMember> cases;
    private JsExpression expression;

    public JsSwitch() {
        super();
        cases = new ArrayList<JsSwitchMember>();
    }

    public JsSwitch(JsExpression expression, List<JsSwitchMember> cases) {
        this.expression = expression;
        this.cases = cases;
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

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            expression = v.accept(expression);
            v.acceptList(cases);
        }
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsSwitch deepCopy() {
        JsExpression expressionCopy = AstUtil.deepCopy(expression);
        List<JsSwitchMember> casesCopy = AstUtil.deepCopy(cases);

        return new JsSwitch(expressionCopy, casesCopy).withMetadataFrom(this);
    }
}
