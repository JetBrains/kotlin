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

import static org.jetbrains.k2js.inline.InlinePackage.aliasArgumentsIfNeeded;
import static org.jetbrains.k2js.inline.InlinePackage.renameLocals;

import java.util.*;

class FunctionInlineMutator {
    private static final String BREAK_LABEL = "break_inlined";
    private static final String RESULT_LABEL = "result_inlined";

    private JsBlock body;
    private final boolean isResultNeeded;
    private final JsFunction invokedFunction;
    private final List<JsExpression> arguments;
    private final List<JsParameter> parameters;
    private final RenamingContext<JsBlock> renamingContext;
    private final InsertionPoint<JsStatement> insertionPoint;
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
        FunctionContext functionContext = inliningContext.getFunctionContext();
        invokedFunction = functionContext.getFunctionDefinition(call);
        body = invokedFunction.getBody().deepCopy();
        isResultNeeded = inliningContext.isResultNeeded(call);
        arguments = call.getArguments();
        parameters = invokedFunction.getParameters();
        renamingContext = inliningContext.getRenamingContext();
        insertionPoint = inliningContext.getStatementContext().getInsertionPoint();
    }

    private void process() {
        aliasArgumentsIfNeeded(renamingContext, arguments, parameters);
        renameLocals(renamingContext, invokedFunction);
        applyRenaming();
        replaceReturns();
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
}
