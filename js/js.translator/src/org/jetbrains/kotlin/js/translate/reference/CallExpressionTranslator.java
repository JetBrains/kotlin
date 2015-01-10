/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import com.google.gwt.dev.js.AbortParsingException;
import com.google.gwt.dev.js.JsParser;
import com.google.gwt.dev.js.JsParserException;
import com.google.gwt.dev.js.rhino.ErrorReporter;
import com.google.gwt.dev.js.rhino.EvaluatorException;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory2;
import org.jetbrains.jet.lang.diagnostics.ParametrizedDiagnostic;
import org.jetbrains.kotlin.psi.JetCallExpression;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.psi.JetStringTemplateExpression;
import org.jetbrains.kotlin.psi.ValueArgument;
import org.jetbrains.jet.lang.resolve.calls.callUtil.CallUtilPackage;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.VariableAsFunctionResolvedCall;
import org.jetbrains.kotlin.builtins.InlineStrategy;
import org.jetbrains.kotlin.builtins.InlineUtil;
import org.jetbrains.kotlin.js.resolve.diagnostics.ErrorsJs;
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.intrinsic.functions.patterns.DescriptorPredicate;
import org.jetbrains.kotlin.js.translate.intrinsic.functions.patterns.PatternBuilder;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.gwt.dev.js.rhino.Utils.isEndOfLine;
import static org.jetbrains.jet.lang.resolve.calls.callUtil.CallUtilPackage.getFunctionResolvedCallWithAssert;
import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getCompileTimeValue;

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

        if (!(argumentExpression instanceof JetStringTemplateExpression)) {
            context().getTrace().report(ErrorsJs.JSCODE_ARGUMENT_SHOULD_BE_LITERAL.on(expression));
            return program().getEmptyExpression();
        }

        List<JsStatement> statements = parseJsCode(argumentExpression);
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
        } catch (AbortParsingException e) {
            /** @see JsCodeErrorReporter#error */
            return Collections.emptyList();
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
            ParametrizedDiagnostic<JetExpression> diagnostic = getDiagnostic(ErrorsJs.JSCODE_ERROR, message, line, lineOffset);
            context().getTrace().report(diagnostic);
            throw new AbortParsingException();
        }

        @Override
        public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
            ParametrizedDiagnostic<JetExpression> diagnostic = getDiagnostic(ErrorsJs.JSCODE_WARNING, message, line, lineOffset);
            context().getTrace().report(diagnostic);
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

        private ParametrizedDiagnostic<JetExpression> getDiagnostic(
                @NotNull DiagnosticFactory2<JetExpression, String, List<TextRange>> diagnosticFactory,
                String message,
                int line,
                int lineOffset
        ) {
            String text = (String) getCompileTimeValue(bindingContext(), jsCodeExpression);
            int offset = jsCodeExpression.getTextOffset() + offsetFromStart(text, line, lineOffset);

            assert jsCodeExpression instanceof JetStringTemplateExpression: "js argument is expected to be compile-time string literal";
            int quotesLength = jsCodeExpression.getFirstChild().getTextLength();
            offset += quotesLength;

            TextRange textRange = new TextRange(offset, offset + 1);
            return diagnosticFactory.on(jsCodeExpression, message, Collections.singletonList(textRange));
        }

        /**
         * Calculates an offset from the start of a text for a position,
         * defined by line and offset in that line.
         */
        private int offsetFromStart(String text, int line, int offset) {
            int i = 0;
            int lineCount = 0;
            int offsetInLine = 0;

            while (i < text.length()) {
                char c = text.charAt(i);

                if (lineCount == line && offsetInLine == offset) {
                    return i;
                }

                if (isEndOfLine(c)) {
                    offsetInLine = 0;
                    lineCount++;
                    assert lineCount <= line;
                }

                i++;
                offsetInLine++;
            }

            return text.length();
        }
    }
}
