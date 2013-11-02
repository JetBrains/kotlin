/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.VariableAsFunctionResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.util.ExpressionAsFunctionDescriptor;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.utils.AnnotationsUtils;
import org.jetbrains.k2js.translate.utils.PsiUtils;

import static org.jetbrains.k2js.translate.utils.PsiUtils.getCallee;

public final class CallExpressionTranslator extends AbstractCallExpressionTranslator {

    @NotNull
    public static JsExpression translate(@NotNull JetCallExpression expression,
            @Nullable JsExpression receiver,
            @NotNull CallType callType,
            @NotNull TranslationContext context) {
        if (InlinedCallExpressionTranslator.shouldBeInlined(expression, context)) {
            return InlinedCallExpressionTranslator.translate(expression, receiver, callType, context);
        }
        return (new CallExpressionTranslator(expression, receiver, callType, context)).translate();
    }

    private final boolean isNativeFunctionCall;
    private CallArgumentTranslator.ArgumentsInfo argumentsInfo = null;
    private JsExpression translatedReceiver = null;
    private JsExpression translatedCallee = null;

    private CallExpressionTranslator(@NotNull JetCallExpression expression,
            @Nullable JsExpression receiver,
            @NotNull CallType callType, @NotNull TranslationContext context) {
        super(expression, receiver, callType, context);
        this.isNativeFunctionCall = AnnotationsUtils.isNativeObject(resolvedCall.getCandidateDescriptor());
    }

    @NotNull
    private JsExpression translate() {
        prepareToBuildCall();

        return CallBuilder.build(context())
                .receiver(translatedReceiver)
                .callee(translatedCallee)
                .args(argumentsInfo.getTranslateArguments())
                .resolvedCall(getResolvedCall())
                .type(callType)
                .translate();
    }

    private void prepareToBuildCall() {
        argumentsInfo = CallArgumentTranslator.translate(resolvedCall, receiver, context());
        translatedReceiver = getReceiver();
        translatedCallee = getCalleeExpression();
    }

    @NotNull
    private ResolvedCall<?> getResolvedCall() {
        if (resolvedCall instanceof VariableAsFunctionResolvedCall) {
            return ((VariableAsFunctionResolvedCall) resolvedCall).getFunctionCall();
        }
        return resolvedCall;
    }

    @Nullable
    private JsExpression getReceiver() {
        assert argumentsInfo != null : "the results of this function depends on the argumentsInfo";
        if (receiver == null) {
            return null;
        }
        if (argumentsInfo.getCachedReceiver() != null) {
            return argumentsInfo.getCachedReceiver().assignmentExpression();
        }
        return receiver;
    }

    @Nullable
    private JsExpression getCalleeExpression() {
        assert argumentsInfo != null : "the results of this function depends on the argumentsInfo";
        if (isNativeFunctionCall && argumentsInfo.isHasSpreadOperator()) {
            JsName functionName = context().getNameForDescriptor(resolvedCall.getCandidateDescriptor());
            return new JsNameRef("apply", functionName.makeRef());
        }
        CallableDescriptor candidateDescriptor = resolvedCall.getCandidateDescriptor();
        if (candidateDescriptor instanceof ExpressionAsFunctionDescriptor) {
            return translateExpressionAsFunction();
        }
        if (resolvedCall instanceof VariableAsFunctionResolvedCall) {
            return translateVariableForVariableAsFunctionResolvedCall();
        }
        return null;
    }

    @NotNull
    //TODO: looks hacky and should be modified soon
    private JsExpression translateVariableForVariableAsFunctionResolvedCall() {
        JetExpression callee = PsiUtils.getCallee(expression);
        if (callee instanceof JetSimpleNameExpression) {
            return ReferenceTranslator.getAccessTranslator((JetSimpleNameExpression) callee, receiver, context()).translateAsGet();
        }
        assert receiver != null;
        return Translation.translateAsExpression(callee, context());
    }

    @NotNull
    private JsExpression translateExpressionAsFunction() {
        return Translation.translateAsExpression(getCallee(expression), context());
    }

}
