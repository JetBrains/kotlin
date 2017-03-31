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

import com.google.gwt.dev.js.ThrowExceptionOnErrorReporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.config.CommonConfigurationKeys;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.js.backend.ast.*;
import org.jetbrains.kotlin.js.inline.util.CollectUtilsKt;
import org.jetbrains.kotlin.js.inline.util.RewriteUtilsKt;
import org.jetbrains.kotlin.js.parser.ParserUtilsKt;
import org.jetbrains.kotlin.js.resolve.BindingContextSlicesJsKt;
import org.jetbrains.kotlin.js.resolve.diagnostics.JsCallChecker;
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtCallExpression;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.ValueArgument;
import org.jetbrains.kotlin.resolve.FunctionImportedFromObject;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.inline.InlineUtil;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;

import java.util.*;

import static org.jetbrains.kotlin.js.resolve.diagnostics.JsCallChecker.isJsCall;
import static org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator.getConstant;

public final class CallExpressionTranslator extends AbstractCallExpressionTranslator {

    @NotNull
    public static JsNode translate(
            @NotNull KtCallExpression expression,
            @Nullable JsExpression receiver,
            @NotNull TranslationContext context
    ) {
        ResolvedCall<? extends FunctionDescriptor> resolvedCall =
                CallUtilKt.getFunctionResolvedCallWithAssert(expression, context.bindingContext());

        if (isJsCall(resolvedCall)) {
            return (new CallExpressionTranslator(expression, receiver, context)).translateJsCode();
        }

        return (new CallExpressionTranslator(expression, receiver, context)).translate();
    }

    public static boolean shouldBeInlined(@NotNull CallableDescriptor descriptor, @NotNull TranslationContext context) {
        if (context.getConfig().getConfiguration().getBoolean(CommonConfigurationKeys.DISABLE_INLINE)) return false;

        return shouldBeInlined(descriptor);
    }

    public static boolean shouldBeInlined(@NotNull CallableDescriptor descriptor) {
        if (descriptor instanceof SimpleFunctionDescriptor ||
            descriptor instanceof PropertyAccessorDescriptor ||
            descriptor instanceof FunctionImportedFromObject
        ) {
            return InlineUtil.isInline(descriptor);
        }

        if (descriptor instanceof ValueParameterDescriptor) {
            return InlineUtil.isInline(descriptor.getContainingDeclaration()) &&
                   InlineUtil.isInlineLambdaParameter((ParameterDescriptor) descriptor) &&
                   !((ValueParameterDescriptor) descriptor).isCrossinline();
        }

        return false;
    }

    private CallExpressionTranslator(
            @NotNull KtCallExpression expression,
            @Nullable JsExpression receiver,
            @NotNull TranslationContext context
    ) {
        super(expression, receiver, context);
    }

    @NotNull
    private JsExpression translate() {
        return CallTranslator.translate(context(), resolvedCall, receiver);
    }

    @NotNull
    private JsNode translateJsCode() {
        List<? extends ValueArgument> arguments = expression.getValueArguments();
        KtExpression argumentExpression = arguments.get(0).getArgumentExpression();
        assert argumentExpression != null;

        List<JsStatement> statements = parseJsCode(argumentExpression);
        int size = statements.size();

        JsNode node;
        if (size == 0) {
            node = JsLiteral.NULL;
        }
        else if (size > 1) {
            node = new JsBlock(statements);
        }
        else {
            JsStatement resultStatement = statements.get(0);
            if (resultStatement instanceof JsExpressionStatement) {
                node = ((JsExpressionStatement) resultStatement).getExpression();
            }
            else {
                node = resultStatement;
            }
        }

        LexicalScope lexicalScope = context().bindingContext().get(BindingContextSlicesJsKt.LEXICAL_SCOPE_FOR_JS, resolvedCall);
        Map<JsName, JsExpression> replacements = new HashMap<>();
        if (lexicalScope != null) {
            Set<JsName> references = CollectUtilsKt.collectUsedNames(node);
            references.removeAll(CollectUtilsKt.collectDefinedNames(node));

            for (JsName name : references) {
                VariableDescriptor variable = getVariableByName(lexicalScope, Name.identifier(name.getIdent()));
                if (variable != null) {
                    replacements.put(name, ReferenceTranslator.translateAsValueReference(variable, context()));
                }
            }

            if (!replacements.isEmpty()) {
                node = RewriteUtilsKt.replaceNames(node, replacements);
            }
        }

        return node;
    }

    @Nullable
    private static VariableDescriptor getVariableByName(@NotNull LexicalScope scope, @NotNull Name name) {
        while (true) {
            Collection<VariableDescriptor> variables = scope.getContributedVariables(name, NoLookupLocation.FROM_BACKEND);
            if (!variables.isEmpty()) {
                return variables.size() == 1 ? variables.iterator().next() : null;
            }

            if (!(scope.getParent() instanceof LexicalScope)) break;
            LexicalScope parentScope = (LexicalScope) scope.getParent();
            if (scope.getOwnerDescriptor() != parentScope.getOwnerDescriptor()) break;
            scope = parentScope;
        }
        return null;
    }

    @NotNull
    private List<JsStatement> parseJsCode(@NotNull KtExpression jsCodeExpression) {
        String jsCode = JsCallChecker.extractStringValue(getConstant(jsCodeExpression, context().bindingContext()));

        assert jsCode != null : "jsCode must be compile time string " + jsCodeExpression.getText();

        // Parser can change local or global scope.
        // In case of js we want to keep new local names,
        // but no new global ones.
        JsScope currentScope = context().scope();
        assert currentScope instanceof JsFunctionScope : "Usage of js outside of function is unexpected";
        JsScope temporaryRootScope = new JsRootScope(new JsProgram());
        JsScope scope = new DelegatingJsFunctionScopeWithTemporaryParent((JsFunctionScope) currentScope, temporaryRootScope);
        return ParserUtilsKt.parse(jsCode, ThrowExceptionOnErrorReporter.INSTANCE, scope, jsCodeExpression.getContainingKtFile().getName());
    }
}
