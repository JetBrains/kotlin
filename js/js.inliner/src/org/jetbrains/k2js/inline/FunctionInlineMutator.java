/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.jetbrains.k2js.inline;

import com.google.dart.compiler.backend.js.ast.*;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import static org.jetbrains.k2js.inline.util.UtilPackage.replaceReturns;
import static org.jetbrains.k2js.inline.util.UtilPackage.replaceThisReference;

import static org.jetbrains.k2js.inline.InlinePackage.*;

import java.util.Collection;
import java.util.List;

class FunctionInlineMutator {

    private final JsInvocation call;
    private final InliningContext inliningContext;
    private final JsFunction invokedFunction;
    private final boolean isResultNeeded;
    private final RenamingContext<JsBlock> renamingContext;
    private final InsertionPoint<JsStatement> insertionPoint;
    private JsBlock body;
    private JsExpression resultExpr = null;
    private JsLabel breakLabel = null;

    public static InlineableResult getInlineableCallReplacement(
            @NotNull JsInvocation call,
            @NotNull InliningContext inliningContext
    ) {
        FunctionInlineMutator mutator = new FunctionInlineMutator(call, inliningContext);
        mutator.process();

        JsStatement inlineableBody = mutator.body;
        if (mutator.breakLabel != null) {
            mutator.breakLabel.setStatement(inlineableBody);
            inlineableBody = mutator.breakLabel;
        }

        JsExpression resultExpression = null;
        if (mutator.isResultNeeded) {
            resultExpression = mutator.resultExpr;
        }

        return new InlineableResult(inlineableBody, resultExpression);
    }

    private FunctionInlineMutator(@NotNull JsInvocation call, @NotNull InliningContext inliningContext) {
        this.inliningContext = inliningContext;
        this.call = call;

        FunctionContext functionContext = inliningContext.getFunctionContext();
        invokedFunction = functionContext.getFunctionDefinition(call);
        body = invokedFunction.getBody().deepCopy();
        isResultNeeded = inliningContext.isResultNeeded(call);
        renamingContext = inliningContext.getRenamingContext();
        insertionPoint = inliningContext.getStatementContext().getInsertionPoint();
    }

    private void process() {
        List<JsExpression> arguments = getArguments();
        List<JsParameter> parameters = getParameters();

        replaceThis();
        removeRedundantDefaultInitializers(arguments, parameters, body);
        aliasArgumentsIfNeeded(renamingContext, arguments, parameters);
        renameLocalNames(renamingContext, invokedFunction);

        if (canBeExpression(body)) {
            applyRenaming();
            doDirectInline();
        } else {
            processReturns();
            applyRenaming();
        }
    }

    private void replaceThis() {
        if (!hasThisReference(body)) return;

        JsExpression thisReplacement = getThisReplacement(call);
        if (thisReplacement == null) return;

        if (needToAlias(thisReplacement)) {
            JsName thisName = renamingContext.getFreshName(getThisAlias());
            renamingContext.newVar(thisName, thisReplacement);
            thisReplacement = thisName.makeRef();
        }

        replaceThisReference(body, thisReplacement);
    }

    private void applyRenaming() {
        RenamingResult<JsBlock> renamingResult = renamingContext.applyRename(body);
        body = renamingResult.getRenamed();
        Collection<JsVars> declarations = renamingResult.getDeclarations();
        insertionPoint.insertAllBefore(declarations);
    }

    private void replaceReturns() {
        int returnCount = ReturnCounter.countReturns(body);
        if (returnCount == 0) {
            // TODO return Unit (KT-5647)
            resultExpr = JsLiteral.UNDEFINED;
        } else {
            doReplaceReturns(returnCount);
        }
    }

    private void doDirectInline() {
        resultExpr = asExpression(body);
        body.getStatements().clear();
    }

    private void doReplaceReturns(int returnCount) {
        JsReturn returnOnTop = ContainerUtil.findInstance(body.getStatements(), JsReturn.class);
        boolean hasReturnOnTopLevel = returnOnTop != null;

        if (isResultNeeded) {
            JsName resultName = renamingContext.getFreshName(getResultLabel());
            renamingContext.newVar(resultName, null);
            resultExpr = resultName.makeRef();
        }

        boolean needBreakLabel = !(returnCount == 1 && hasReturnOnTopLevel);
        JsNameRef breakLabelRef = null;

        if (needBreakLabel) {
            JsName breakName = renamingContext.getFreshName(getBreakLabel());
            breakLabelRef = breakName.makeRef();
            breakLabel = new JsLabel(breakName);
        }

        assert resultExpr == null || resultExpr instanceof JsNameRef;
        replaceReturns(body, (JsNameRef) resultExpr, breakLabelRef);
    }

    @NotNull
    private List<JsExpression> getArguments() {
        return inliningContext.getArguments(call);
    }

    @NotNull
    private List<JsParameter> getParameters() {
        return invokedFunction.getParameters();
    }

    @NotNull
    private String getResultLabel() {
        return getLabelPrefix() + "result";
    }

    @NotNull
    private String getBreakLabel() {
        return getLabelPrefix() + "break";
    }

    @SuppressWarnings("MethodMayBeStatic")
    @NotNull
    private String getThisAlias() {
        return "$this";
    }

    @NotNull
    String getLabelPrefix() {
        String ident = getSimpleIdent(call);
        String labelPrefix = ident != null ? ident : "inline$";

        if (labelPrefix.endsWith("$")) {
            return labelPrefix;
        }

        return labelPrefix + "$";
    }

    @Nullable
    private static JsExpression getThisReplacement(JsInvocation call) {
        if (isCallInvocation(call)) {
            return call.getArguments().get(0);
        }

        if (hasCallerQualifier(call)) {
            return getCallerQualifier(call);
        }

        return null;
    }

    private static boolean hasThisReference(JsBlock body) {
        List<JsLiteral.JsThisRef> thisRefs = collectInstances(JsLiteral.JsThisRef.class, body);
        return !thisRefs.isEmpty();
    }

    private static boolean canBeExpression(JsBlock body) {
        List<JsStatement> statements = body.getStatements();
        return statements.size() == 1 && statements.get(0) instanceof JsReturn;
    }

    private static JsExpression asExpression(JsBlock body) {
        assert canBeExpression(body);

        List<JsStatement> statements = body.getStatements();
        JsReturn returnStatement = (JsReturn) statements.get(0);
        return returnStatement.getExpression();
    }
}
