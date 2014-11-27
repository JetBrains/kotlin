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

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.backend.js.ast.metadata.MetadataPackage;
import com.google.dart.compiler.common.SourceInfoImpl;
import com.google.gwt.dev.js.JsParser;
import com.google.gwt.dev.js.JsParserException;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.calls.callUtil.CallUtilPackage;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.VariableAsFunctionResolvedCall;
import org.jetbrains.jet.lang.types.lang.InlineStrategy;
import org.jetbrains.jet.lang.types.lang.InlineUtil;
import org.jetbrains.jet.lang.psi.ValueArgument;
import org.jetbrains.k2js.translate.callTranslator.CallTranslator;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.DescriptorPredicate;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.PatternBuilder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.k2js.translate.utils.BindingUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.calls.callUtil.CallUtilPackage.getFunctionResolvedCallWithAssert;

public final class CallExpressionTranslator extends AbstractCallExpressionTranslator {

    @NotNull
    private final static DescriptorPredicate JSCODE_PATTERN = PatternBuilder.pattern("kotlin.js.js(String)");

    @NotNull
    public static JsNode translate(
            @NotNull JetCallExpression expression,
            @Nullable JsExpression receiver,
            @NotNull TranslationContext context
    ) {
        if (matchesJsCode(expression, context)) {
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

    private static boolean matchesJsCode(
            @NotNull JetCallExpression expression,
            @NotNull TranslationContext context
    ) {
        FunctionDescriptor descriptor = getFunctionResolvedCallWithAssert(expression, context.bindingContext())
                                            .getResultingDescriptor();

        return JSCODE_PATTERN.apply(descriptor) && expression.getValueArguments().size() == 1;
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
        assert argumentExpression != null;

        List<JsStatement> statements = parseJsCode(argumentExpression);
        int size = statements.size();

        if (size == 0) {
            return program().getEmptyStatement();
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
    private List<JsStatement> parseJsCode(@NotNull JetExpression jsCodeExpression) {
        Object jsCode = getCompileTimeValue(bindingContext(), jsCodeExpression);
        assert jsCode instanceof String: "jsCode must be compile time string";

        List<JsStatement> statements = new ArrayList<JsStatement>();
        ErrorReporter errorReporter = new JsCodeErrorReporter(jsCodeExpression);

        try {
            SourceInfoImpl info = new SourceInfoImpl(null, 0, 0, 0, 0);
            JsScope scope = context().scope();
            StringReader reader = new StringReader((String) jsCode);
            statements.addAll(JsParser.parse(info, scope, reader, errorReporter, /* insideFunction= */ true));
        } catch (JsParserException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return statements;
    }

    private class JsCodeErrorReporter implements ErrorReporter {
        @NotNull
        private final JetExpression jsCodeExpression;
        private JsCodeErrorReporter(@NotNull JetExpression expression) {
            jsCodeExpression = expression;
        }

        @Override
        public void error(String message, String sourceName, int line, String lineSource, int lineOffset) {
        }

        /**
         * TODO: seems not called anywhere, so remove
         */
        @Override
        public EvaluatorException runtimeError(
                String message, String sourceName, int line, String lineSource, int lineOffset
        ) {
            throw new RuntimeException(message);
        }

        /**
         * Do not report warnings
         */
        @Override
        public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
        }
    }
}
