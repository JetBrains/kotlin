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

import static org.jetbrains.k2js.inline.InlinePackage.needToAlias;
import static org.jetbrains.k2js.inline.InlinePackage.aliasArgumentsIfNeeded;
import static org.jetbrains.k2js.inline.InlinePackage.renameLocals;
import static org.jetbrains.k2js.inline.InlinePackage.replaceThisReference;
import static org.jetbrains.k2js.inline.InlinePackage.hasThisReference;

import java.util.*;

class FunctionInlineMutator {
    private static final String BREAK_LABEL = "break_inlined";
    private static final String RESULT_LABEL = "result_inlined";
    private static final String THIS_ALIAS = "self";

    private final JsInvocation call;
    private final InliningContext inliningContext;
    private final JsFunction invokedFunction;
    private final boolean isResultNeeded;
    private final RenamingContext<JsBlock> renamingContext;
    private final InsertionPoint<JsStatement> insertionPoint;
    private JsBlock body;
    private JsNameRef resultExpr = null;
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
        replaceThis();
        aliasArgumentsIfNeeded(renamingContext, getArguments(), getParameters());
        renameLocals(renamingContext, invokedFunction);
        applyRenaming();
        replaceReturns();
    }

    private void replaceThis() {
        if (!hasThisReference(body)) {
            return;
        }

        JsExpression thisReplacement = inliningContext.getThisReplacement(call);
        if (thisReplacement == JsLiteral.THIS) {
            return;
        }

        if (needToAlias(thisReplacement)) {
            JsName thisName = renamingContext.getFreshName(THIS_ALIAS);
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
        ReplaceReturnVisitor.replaceReturn(body, (JsNameRef) resultExpr, breakLabelRef);
    }

    @NotNull
    private List<JsExpression> getArguments() {
        return inliningContext.getArguments(call);
    }

    @NotNull
    private List<JsParameter> getParameters() {
        return invokedFunction.getParameters();
    }
}
