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

package org.jetbrains.kotlin.js.translate.reference;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.gwt.dev.js.ThrowExceptionOnErrorReporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.js.parser.ParserPackage;
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.psi.JetCallExpression;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.psi.JetStringTemplateExpression;
import org.jetbrains.kotlin.psi.ValueArgument;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.kotlin.resolve.inline.InlineUtil;
import org.jetbrains.kotlin.types.JetType;

import java.util.List;

import static org.jetbrains.kotlin.js.resolve.diagnostics.JsCallChecker.isJsCall;
import static org.jetbrains.kotlin.js.translate.utils.PsiUtils.getFunctionDescriptor;
import static org.jetbrains.kotlin.js.translate.utils.UtilsPackage.setInlineCallMetadata;
import static org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilPackage.getFunctionResolvedCallWithAssert;

public final class CallExpressionTranslator extends AbstractCallExpressionTranslator {

    @NotNull
    public static JsNode translate(
            @NotNull JetCallExpression expression,
            @Nullable JsExpression receiver,
            @NotNull TranslationContext context
    ) {
        ResolvedCall<? extends FunctionDescriptor> resolvedCall = getFunctionResolvedCallWithAssert(expression, context.bindingContext());

        if (isJsCall(resolvedCall)) {
            return (new CallExpressionTranslator(expression, receiver, context)).translateJsCode();
        }
        
        JsExpression callExpression = (new CallExpressionTranslator(expression, receiver, context)).translate();

        if (!resolvedCall.isSafeCall() && shouldBeInlined(expression, context)) {
            setInlineCallMetadata(callExpression, expression, context);
        }

        return callExpression;
    }

    public static boolean shouldBeInlined(@NotNull JetCallExpression expression, @NotNull TranslationContext context) {
        if (!context.getConfig().isInlineEnabled()) return false;

        CallableDescriptor descriptor = getFunctionDescriptor(expression, context);
        return shouldBeInlined(descriptor);
    }

    public static boolean shouldBeInlined(@NotNull CallableDescriptor descriptor) {
        if (descriptor instanceof SimpleFunctionDescriptor) {
            return InlineUtil.isInline(descriptor);
        }

        if (descriptor instanceof ValueParameterDescriptor) {
            return InlineUtil.isInline(descriptor.getContainingDeclaration()) && InlineUtil.isInlineLambdaParameter(descriptor);
        }

        return false;
    }

    private CallExpressionTranslator(
            @NotNull JetCallExpression expression,
            @Nullable JsExpression receiver,
            @NotNull TranslationContext context
    ) {
        super(expression, receiver, context);
    }

    @NotNull
    private JsExpression translate() {
        return CallTranslator.INSTANCE$.translate(context(), resolvedCall, receiver);
    }

    @NotNull
    private JsNode translateJsCode() {
        List<? extends ValueArgument> arguments = expression.getValueArguments();
        JetExpression argumentExpression = arguments.get(0).getArgumentExpression();
        assert argumentExpression instanceof JetStringTemplateExpression;

        List<JsStatement> statements = parseJsCode((JetStringTemplateExpression) argumentExpression);
        int size = statements.size();

        if (size == 0) {
            return program().getEmptyExpression();
        } else if (size > 1) {
            return new JsBlock(statements);
        } else {
            JsStatement resultStatement = statements.get(0);
            if (resultStatement instanceof JsExpressionStatement) {
                return ((JsExpressionStatement) resultStatement).getExpression();
            }

            return resultStatement;
        }
    }

    @NotNull
    private List<JsStatement> parseJsCode(@NotNull JetStringTemplateExpression jsCodeExpression) {
        BindingTrace bindingTrace = TemporaryBindingTrace.create(context().bindingTrace(), "parseJsCode");
        JetType stringType = KotlinBuiltIns.getInstance().getStringType();
        CompileTimeConstant<?> constant = ConstantExpressionEvaluator.evaluate(jsCodeExpression, bindingTrace, stringType);

        assert constant != null: "jsCode must be compile time string " + jsCodeExpression;
        String jsCode = (String) constant.getValue();
        assert jsCode != null: jsCodeExpression.toString();

        // Parser can change local or global scope.
        // In case of js we want to keep new local names,
        // but no new global ones.
        JsScope currentScope = context().scope();
        assert currentScope instanceof JsFunctionScope: "Usage of js outside of function is unexpected";
        JsScope temporaryRootScope = new JsRootScope(new JsProgram("<js code>"));
        JsScope scope = new DelegatingJsFunctionScopeWithTemporaryParent((JsFunctionScope) currentScope, temporaryRootScope);
        return ParserPackage.parse(jsCode, ThrowExceptionOnErrorReporter.INSTANCE$, scope);
    }
}
