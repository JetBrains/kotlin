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
import com.google.dart.compiler.backend.js.ast.metadata.MetadataPackage;
import com.google.dart.compiler.common.SourceInfoImpl;
import com.google.gwt.dev.js.JsParser;
import com.google.gwt.dev.js.ThrowExceptionOnErrorReporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.InlineStrategy;
import org.jetbrains.kotlin.builtins.InlineUtil;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.psi.JetCallExpression;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.psi.JetStringTemplateExpression;
import org.jetbrains.kotlin.psi.ValueArgument;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilPackage;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.kotlin.types.JetType;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilPackage.getFunctionResolvedCallWithAssert;
import static org.jetbrains.kotlin.js.resolve.diagnostics.JsCallChecker.isJsCall;

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

        if (shouldBeInlined(expression, context)
            && callExpression instanceof JsInvocation) {

            MetadataPackage.setInlineStrategy((JsInvocation) callExpression, InlineStrategy.IN_PLACE);
        }

        return callExpression;
    }

    public static boolean shouldBeInlined(@NotNull JetCallExpression expression, @NotNull TranslationContext context) {
        if (!context.getConfig().isInlineEnabled()) return false;

        ResolvedCall<?> resolvedCall = CallUtilPackage.getResolvedCall(expression, context.bindingContext());
        assert resolvedCall != null;

        CallableDescriptor descriptor;

        if (resolvedCall instanceof VariableAsFunctionResolvedCall) {
            descriptor = ((VariableAsFunctionResolvedCall) resolvedCall).getVariableCall().getCandidateDescriptor();
        } else {
            descriptor = resolvedCall.getCandidateDescriptor();
        }

        if (descriptor instanceof SimpleFunctionDescriptor) {
            return ((SimpleFunctionDescriptor) descriptor).getInlineStrategy().isInline();
        }

        if (descriptor instanceof ValueParameterDescriptor) {
            DeclarationDescriptor containingDescriptor = descriptor.getContainingDeclaration();
            return InlineUtil.getInlineType(containingDescriptor).isInline()
                   && !InlineUtil.hasNoinlineAnnotation(descriptor);
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

        List<JsStatement> statements = new ArrayList<JsStatement>();

        try {
            SourceInfoImpl info = new SourceInfoImpl(null, 0, 0, 0, 0);
            JsScope scope = context().scope();
            StringReader reader = new StringReader(jsCode);
            statements.addAll(JsParser.parse(info, scope, reader, ThrowExceptionOnErrorReporter.INSTANCE$, /* insideFunction= */ true));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return statements;
    }
}
