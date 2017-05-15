/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.translate.utils;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.js.backend.ast.*;
import org.jetbrains.kotlin.js.backend.ast.metadata.MetadataProperties;
import org.jetbrains.kotlin.js.naming.NameSuggestion;
import org.jetbrains.kotlin.js.translate.context.Namer;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.expression.LocalFunctionCollector;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.js.translate.general.Translation;
import org.jetbrains.kotlin.js.translate.reference.ReferenceTranslator;
import org.jetbrains.kotlin.psi.KtDeclarationWithBody;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.resolve.source.KotlinSourceElementKt;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getDefaultArgument;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.*;
import static org.jetbrains.kotlin.js.translate.utils.mutator.LastExpressionMutator.mutateLastExpression;

public final class FunctionBodyTranslator extends AbstractTranslator {

    @NotNull
    public static JsBlock translateFunctionBody(
            @NotNull FunctionDescriptor descriptor,
            @NotNull KtDeclarationWithBody declarationWithBody,
            @NotNull TranslationContext functionBodyContext
    ) {
        Map<DeclarationDescriptor, JsExpression> aliases = new HashMap<>();
        LocalFunctionCollector functionCollector = new LocalFunctionCollector(functionBodyContext.bindingContext());
        declarationWithBody.acceptChildren(functionCollector, null);

        for (FunctionDescriptor localFunction : functionCollector.getFunctions()) {
            String localIdent = localFunction.getName().isSpecial() ? "lambda" : localFunction.getName().asString();
            JsName localName = JsScope.declareTemporaryName(NameSuggestion.sanitizeName(localIdent));
            MetadataProperties.setDescriptor(localName, localFunction);
            JsExpression alias = JsAstUtils.pureFqn(localName, null);
            aliases.put(localFunction, alias);
        }

        if (!aliases.isEmpty()) {
            functionBodyContext = functionBodyContext.innerContextWithDescriptorsAliased(aliases);
        }

        return (new FunctionBodyTranslator(descriptor, declarationWithBody, functionBodyContext)).translate();
    }

    @NotNull
    public static List<JsStatement> setDefaultValueForArguments(@NotNull FunctionDescriptor descriptor,
            @NotNull TranslationContext functionBodyContext) {
        List<ValueParameterDescriptor> valueParameters = descriptor.getValueParameters();

        List<JsStatement> result = new ArrayList<>(valueParameters.size());
        for (ValueParameterDescriptor valueParameter : valueParameters) {
            if (!valueParameter.declaresDefaultValue()) continue;

            JsExpression jsNameRef = ReferenceTranslator.translateAsValueReference(valueParameter, functionBodyContext);
            KtExpression defaultArgument = getDefaultArgument(valueParameter);
            JsBlock defaultArgBlock = new JsBlock();
            JsExpression defaultValue = Translation.translateAsExpression(defaultArgument, functionBodyContext, defaultArgBlock);
            PsiElement psi = KotlinSourceElementKt.getPsi(valueParameter.getSource());
            JsStatement assignStatement = assignment(jsNameRef, defaultValue).source(psi).makeStmt();
            JsStatement thenStatement = JsAstUtils.mergeStatementInBlockIfNeeded(assignStatement, defaultArgBlock);
            JsBinaryOperation checkArgIsUndefined = equality(jsNameRef, Namer.getUndefinedExpression());
            checkArgIsUndefined.source(KotlinSourceElementKt.getPsi(valueParameter.getSource()));
            JsIf jsIf = JsAstUtils.newJsIf(checkArgIsUndefined, thenStatement);
            result.add(jsIf);
        }

        return result;
    }

    @NotNull
    private final FunctionDescriptor descriptor;
    @NotNull
    private final KtDeclarationWithBody declaration;

    private FunctionBodyTranslator(@NotNull FunctionDescriptor descriptor,
                                   @NotNull KtDeclarationWithBody declaration,
                                   @NotNull TranslationContext context) {
        super(context);
        this.descriptor = descriptor;
        this.declaration = declaration;
    }

    @NotNull
    private JsBlock translate() {
        KtExpression jetBodyExpression = declaration.getBodyExpression();
        assert jetBodyExpression != null : "Cannot translate a body of an abstract function.";
        JsBlock jsBlock = new JsBlock();


        JsNode jsBody = Translation.translateExpression(jetBodyExpression, context(), jsBlock);
        jsBlock.getStatements().addAll(mayBeWrapWithReturn(jsBody).getStatements());
        return jsBlock;
    }

    @NotNull
    private JsBlock mayBeWrapWithReturn(@NotNull JsNode body) {
        if (!mustAddReturnToGeneratedFunctionBody()) {
            return convertToBlock(body);
        }
        return convertToBlock(lastExpressionReturned(body));
    }

    private boolean mustAddReturnToGeneratedFunctionBody() {
        KotlinType functionReturnType = descriptor.getReturnType();
        assert functionReturnType != null : "Function return typed type must be resolved.";
        return (!declaration.hasBlockBody()) && !(KotlinBuiltIns.isUnit(functionReturnType) && !descriptor.isSuspend());
    }

    @NotNull
    private JsNode lastExpressionReturned(@NotNull JsNode body) {
        return mutateLastExpression(body, node -> {
            if (!(node instanceof JsExpression)) {
                return node;
            }

            assert declaration.getBodyExpression() != null;
            assert descriptor.getReturnType() != null;
            KotlinType bodyType = context().bindingContext().getType(declaration.getBodyExpression());
            if (bodyType == null && KotlinBuiltIns.isCharOrNullableChar(descriptor.getReturnType()) ||
                bodyType != null && KotlinBuiltIns.isCharOrNullableChar(bodyType) && TranslationUtils.shouldBoxReturnValue(descriptor)) {
                node = JsAstUtils.charToBoxedChar((JsExpression) node);
            }

            JsReturn jsReturn = new JsReturn((JsExpression) node);
            jsReturn.setSource(declaration.getBodyExpression());
            MetadataProperties.setReturnTarget(jsReturn, descriptor);
            return jsReturn;
        });
    }
}
