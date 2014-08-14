/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.dart.compiler.backend.js.ast;

/**
 * Taken from GWT project with modifications.
 * Original:
 *  repository: https://gwt.googlesource.com/gwt
 *  revision: e32bf0a95029165d9e6ab457c7ee7ca8b07b908c
 *  file: dev/core/src/com/google/gwt/dev/js/ast/JsVisitor.java
 */

import java.util.List;

/**
 * Implemented by nodes that will visit child nodes.
 */
@SuppressWarnings("UnusedParameters")
public abstract class JsVisitorWithContext {

    public final <T extends JsNode> T accept(T node) {
        return doAccept(node);
    }

    public JsExpression acceptLvalue(JsExpression expr) {
        return doAcceptLvalue(expr);
    }

    public final <T extends JsNode> void acceptList(List<T> collection) {
        doAcceptList(collection);
    }

    public final <T extends JsStatement> T acceptStatement(T statement) {
        //noinspection unchecked
        return (T) doAcceptStatement(statement);
    }

    public final <T extends JsStatement> void acceptStatementList(List<T> statements) {
        doAcceptStatementList(statements);
    }

    public void endVisit(JsArrayAccess x, JsContext ctx) {
    }

    public void endVisit(JsArrayLiteral x, JsContext ctx) {
    }

    public void endVisit(JsBinaryOperation x, JsContext ctx) {
    }

    public void endVisit(JsBlock x, JsContext ctx) {
    }

    public void endVisit(JsLiteral.JsBooleanLiteral x, JsContext ctx) {
    }

    public void endVisit(JsBreak x, JsContext ctx) {
    }

    public void endVisit(JsCase x, JsContext ctx) {
    }

    public void endVisit(JsCatch x, JsContext ctx) {
    }

    public void endVisit(JsConditional x, JsContext ctx) {
    }

    public void endVisit(JsContinue x, JsContext ctx) {
    }

    public void endVisit(JsDebugger x, JsContext ctx) {
    }

    public void endVisit(JsDefault x, JsContext ctx) {
    }

    public void endVisit(JsDoWhile x, JsContext ctx) {
    }

    public void endVisit(JsEmpty x, JsContext ctx) {
    }

    public void endVisit(JsExpressionStatement x, JsContext ctx) {
    }

    public void endVisit(JsFor x, JsContext ctx) {
    }

    public void endVisit(JsForIn x, JsContext ctx) {
    }

    public void endVisit(JsFunction x, JsContext ctx) {
    }

    public void endVisit(JsIf x, JsContext ctx) {
    }

    public void endVisit(JsInvocation x, JsContext ctx) {
    }

    public void endVisit(JsLabel x, JsContext ctx) {
    }

    public void endVisit(JsName x, JsContext ctx) {
    }

    public void endVisit(JsNameRef x, JsContext ctx) {
    }

    public void endVisit(JsNew x, JsContext ctx) {
    }

    public void endVisit(JsNullLiteral x, JsContext ctx) {
    }

    public void endVisit(JsNumberLiteral x, JsContext ctx) {
    }

    public void endVisit(JsObjectLiteral x, JsContext ctx) {
    }

    public void endVisit(JsParameter x, JsContext ctx) {
    }

    public void endVisit(JsPostfixOperation x, JsContext ctx) {
    }

    public void endVisit(JsPrefixOperation x, JsContext ctx) {
    }

    public void endVisit(JsProgram x, JsContext ctx) {
    }

    public void endVisit(JsProgramFragment x, JsContext ctx) {
    }

    public void endVisit(JsPropertyInitializer x, JsContext ctx) {
    }

    public void endVisit(JsRegExp x, JsContext ctx) {
    }

    public void endVisit(JsReturn x, JsContext ctx) {
    }

    public void endVisit(JsStringLiteral x, JsContext ctx) {
    }

    public void endVisit(JsSwitch x, JsContext ctx) {
    }

    public void endVisit(JsLiteral.JsThisRef x, JsContext ctx) {
    }

    public void endVisit(JsThrow x, JsContext ctx) {
    }

    public void endVisit(JsTry x, JsContext ctx) {
    }

    public void endVisit(JsVars.JsVar x, JsContext ctx) {
    }

    public void endVisit(JsVars x, JsContext ctx) {
    }

    public void endVisit(JsWhile x, JsContext ctx) {
    }

    public boolean visit(JsArrayAccess x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsArrayLiteral x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsBinaryOperation x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsBlock x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsLiteral.JsBooleanLiteral x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsBreak x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsCase x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsCatch x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsConditional x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsContinue x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsDebugger x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsDefault x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsDoWhile x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsEmpty x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsExpressionStatement x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsFor x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsForIn x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsFunction x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsIf x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsInvocation x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsLabel x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsName x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsNameRef x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsNew x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsNullLiteral x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsNumberLiteral x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsObjectLiteral x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsParameter x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsPostfixOperation x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsPrefixOperation x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsProgram x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsProgramFragment x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsPropertyInitializer x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsRegExp x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsReturn x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsStringLiteral x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsSwitch x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsLiteral.JsThisRef x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsThrow x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsTry x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsVars.JsVar x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsVars x, JsContext ctx) {
        return true;
    }

    public boolean visit(JsWhile x, JsContext ctx) {
        return true;
    }

    protected abstract  <T extends JsNode> T doAccept(T node);

    protected abstract JsExpression doAcceptLvalue(JsExpression expr);

    protected abstract <T extends JsNode> void doAcceptList(List<T> collection);

    protected abstract <T extends JsStatement> JsStatement doAcceptStatement(T statement);

    protected abstract <T extends JsStatement> void doAcceptStatementList(List<T> statements);

    protected abstract <T extends JsNode> void doTraverse(T node, JsContext ctx) ;
}
