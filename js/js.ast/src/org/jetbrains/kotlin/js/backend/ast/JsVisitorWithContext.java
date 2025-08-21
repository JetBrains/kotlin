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

package org.jetbrains.kotlin.js.backend.ast;

/**
 * Taken from GWT project with modifications.
 * Original:
 *  repository: https://gwt.googlesource.com/gwt
 *  revision: e32bf0a95029165d9e6ab457c7ee7ca8b07b908c
 *  file: dev/core/src/com/google/gwt/dev/js/ast/JsVisitor.java
 */

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Implemented by nodes that will visit child nodes.
 */
@SuppressWarnings("UnusedParameters")
public abstract class JsVisitorWithContext {

    public final <T extends JsNode> T accept(T node) {
        if (node == null) return null;

        return doAccept(node);
    }

    public JsExpression acceptLvalue(JsExpression expr) {
        if (expr == null) return null;

        return doAcceptLvalue(expr);
    }

    public final <T extends JsNode> void acceptList(List<T> collection) {
        doAcceptList(collection);
    }

    @SuppressWarnings("unchecked")
    public final <T extends JsStatement> T acceptStatement(T statement) {
        if (statement == null) return null;

        return (T) doAcceptStatement(statement);
    }

    public final void acceptStatementList(List<JsStatement> statements) {
        doAcceptStatementList(statements);
    }

    public void endVisit(@NotNull JsExpression x, @NotNull JsContext ctx) {
    }

    public void endVisit(@NotNull JsArrayAccess x, @NotNull JsContext ctx) {
        endVisit((JsExpression) x, ctx);
    }

    public void endVisit(@NotNull JsArrayLiteral x, @NotNull JsContext ctx) {
        endVisit((JsExpression) x, ctx);
    }

    public void endVisit(@NotNull JsBinaryOperation x, @NotNull JsContext ctx) {
        endVisit((JsExpression) x, ctx);
    }

    public void endVisit(@NotNull JsBlock x, @NotNull JsContext ctx) {
    }

    public void endVisit(@NotNull JsBooleanLiteral x, @NotNull JsContext ctx) {
        endVisit((JsExpression) x, ctx);
    }

    public void endVisit(@NotNull JsBreak x, @NotNull JsContext ctx) {
    }

    public void endVisit(@NotNull JsCase x, @NotNull JsContext ctx) {
    }

    public void endVisit(@NotNull JsCatch x, @NotNull JsContext ctx) {
    }

    public void endVisit(@NotNull JsClass x, @NotNull JsContext ctx) {
        endVisit((JsExpression) x, ctx);
    }

    public void endVisit(@NotNull JsConditional x, @NotNull JsContext ctx) {
        endVisit((JsExpression) x, ctx);
    }

    public void endVisit(@NotNull JsContinue x, @NotNull JsContext ctx) {
    }

    public void endVisit(@NotNull JsYield x, @NotNull JsContext ctx) {
    }

    public void endVisit(@NotNull JsDebugger x, @NotNull JsContext ctx) {
    }

    public void endVisit(@NotNull JsDefault x, @NotNull JsContext ctx) {
    }

    public void endVisit(@NotNull JsDoWhile x, @NotNull JsContext ctx) {
        endVisit((JsLoop) x, ctx);
    }

    public void endVisit(@NotNull JsEmpty x, @NotNull JsContext ctx) {
    }

    public void endVisit(@NotNull JsExpressionStatement x, @NotNull JsContext ctx) {
    }

    public void endVisit(@NotNull JsFor x, @NotNull JsContext ctx) {
        endVisit((JsLoop) x, ctx);
    }

    public void endVisit(@NotNull JsForIn x, @NotNull JsContext ctx) {
        endVisit((JsLoop) x, ctx);
    }

    public void endVisit(@NotNull JsFunction x, @NotNull JsContext ctx) {
        endVisit((JsExpression) x, ctx);
    }

    public void endVisit(@NotNull JsIf x, @NotNull JsContext ctx) {
    }

    public void endVisit(@NotNull JsInvocation x, @NotNull JsContext ctx) {
        endVisit((JsExpression) x, ctx);
    }

    public void endVisit(@NotNull JsLabel x, @NotNull JsContext ctx) {
    }

    public void endVisit(@NotNull JsLoop x, @NotNull JsContext ctx) {
    }

    public void endVisit(@NotNull JsName x, @NotNull JsContext ctx) {
    }

    public void endVisit(@NotNull JsNameRef x, @NotNull JsContext ctx) {
        endVisit((JsExpression) x, ctx);
    }

    public void endVisit(@NotNull JsNew x, @NotNull JsContext ctx) {
    }

    public void endVisit(@NotNull JsNullLiteral x, @NotNull JsContext ctx) {
        endVisit((JsExpression) x, ctx);
    }

    public void endVisit(@NotNull JsNumberLiteral x, @NotNull JsContext ctx) {
        endVisit((JsExpression) x, ctx);
    }

    public void endVisit(@NotNull JsObjectLiteral x, @NotNull JsContext ctx) {
        endVisit((JsExpression) x, ctx);
    }

    public void endVisit(@NotNull JsParameter x, @NotNull JsContext ctx) {
    }

    public void endVisit(@NotNull JsPostfixOperation x, @NotNull JsContext ctx) {
        endVisit((JsExpression) x, ctx);
    }

    public void endVisit(@NotNull JsPrefixOperation x, @NotNull JsContext ctx) {
        endVisit((JsExpression) x, ctx);
    }

    public void endVisit(@NotNull JsProgram x, @NotNull JsContext ctx) {
    }

    public void endVisit(@NotNull JsPropertyInitializer x, @NotNull JsContext ctx) {
    }

    public void endVisit(@NotNull JsRegExp x, @NotNull JsContext ctx) {
        endVisit((JsExpression) x, ctx);
    }

    public void endVisit(@NotNull JsReturn x, @NotNull JsContext ctx) {
    }

    public void endVisit(@NotNull JsStringLiteral x, @NotNull JsContext ctx) {
        endVisit((JsExpression) x, ctx);
    }

    public void endVisit(@NotNull JsSwitch x, @NotNull JsContext ctx) {
    }

    public void endVisit(@NotNull JsThisRef x, @NotNull JsContext ctx) {
        endVisit((JsExpression) x, ctx);
    }

    public void endVisit(@NotNull JsSuperRef x, @NotNull JsContext ctx) {
        endVisit((JsExpression) x, ctx);
    }
    public void endVisit(@NotNull JsThrow x, @NotNull JsContext ctx) {
    }

    public void endVisit(@NotNull JsTry x, @NotNull JsContext ctx) {
    }

    public void endVisit(@NotNull JsVars.JsVar x, @NotNull JsContext ctx) {
    }

    public void endVisit(@NotNull JsVars x, @NotNull JsContext ctx) {
    }

    public void endVisit(@NotNull JsSingleLineComment x, @NotNull JsContext ctx) {
    }

    public void endVisit(@NotNull JsMultiLineComment x, @NotNull JsContext ctx) {
    }

    public void endVisit(@NotNull JsExport x, @NotNull JsContext ctx) {
    }

    public void endVisit(@NotNull JsImport x, @NotNull JsContext ctx) {
    }

    public void endVisit(@NotNull JsWhile x, @NotNull JsContext ctx) {
        endVisit((JsLoop) x, ctx);
    }

    public boolean visit(@NotNull JsArrayAccess x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsArrayLiteral x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsBinaryOperation x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsBlock x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsBooleanLiteral x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsBreak x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsCase x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsCatch x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsClass x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsConditional x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsContinue x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsYield x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsDebugger x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsDefault x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsDoWhile x, @NotNull JsContext ctx) {
        return visit((JsLoop) x, ctx);
    }

    public boolean visit(@NotNull JsEmpty x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsExpressionStatement x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsFor x, @NotNull JsContext ctx) {
        return visit((JsLoop) x, ctx);
    }

    public boolean visit(@NotNull JsForIn x, @NotNull JsContext ctx) {
        return visit((JsLoop) x, ctx);
    }

    public boolean visit(@NotNull JsFunction x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsIf x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsInvocation x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsLabel x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsLoop x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsName x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsNameRef x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsNew x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsNullLiteral x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsNumberLiteral x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsBigIntLiteral x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsObjectLiteral x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsParameter x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsPostfixOperation x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsPrefixOperation x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsProgram x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsPropertyInitializer x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsRegExp x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsReturn x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsStringLiteral x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsSwitch x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsThisRef x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsSuperRef x, @NotNull JsContext ctx) {
        return true;
    }
    public boolean visit(@NotNull JsThrow x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsTry x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsVars.JsVar x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsVars x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsWhile x, @NotNull JsContext ctx) {
        return visit((JsLoop) x, ctx);
    }

    public boolean visit(@NotNull JsSingleLineComment x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsMultiLineComment x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsExport x, @NotNull JsContext ctx) {
        return true;
    }

    public boolean visit(@NotNull JsImport x, @NotNull JsContext ctx) {
        return true;
    }

    protected abstract  <T extends JsNode> T doAccept(T node);

    protected abstract JsExpression doAcceptLvalue(JsExpression expr);

    protected abstract <T extends JsNode> void doAcceptList(List<T> collection);

    protected abstract <T extends JsStatement> JsStatement doAcceptStatement(T statement);

    protected abstract void doAcceptStatementList(List<JsStatement> statements);

    protected abstract <T extends JsNode> void doTraverse(T node, JsContext ctx) ;
}
