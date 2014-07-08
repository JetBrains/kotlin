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

package org.jetbrains.k2js.translate.utils;

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.JetDeclarationWithBody;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.utils.mutator.Mutator;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getDefaultArgument;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.assignment;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.convertToBlock;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.equality;
import static org.jetbrains.k2js.translate.utils.mutator.LastExpressionMutator.mutateLastExpression;

public final class FunctionBodyTranslator extends AbstractTranslator {

    @NotNull
    public static JsBlock translateFunctionBody(@NotNull FunctionDescriptor descriptor,
                                                @NotNull JetDeclarationWithBody declarationWithBody,
                                                @NotNull TranslationContext functionBodyContext) {
        return (new FunctionBodyTranslator(descriptor, declarationWithBody, functionBodyContext)).translate();
    }

    @NotNull
    public static List<JsStatement> setDefaultValueForArguments(@NotNull FunctionDescriptor descriptor,
            @NotNull TranslationContext functionBodyContext) {
        List<JsStatement> result = new ArrayList<JsStatement>();
        for (ValueParameterDescriptor valueParameter : descriptor.getValueParameters()) {
            if (valueParameter.hasDefaultValue()) {
                JsNameRef jsNameRef = functionBodyContext.getNameForDescriptor(valueParameter).makeRef();
                JetExpression defaultArgument = getDefaultArgument(valueParameter);
                JsExpression defaultValue = Translation.translateAsExpression(defaultArgument, functionBodyContext);

                JsBinaryOperation checkArgIsUndefined = equality(jsNameRef, functionBodyContext.namer().getUndefinedExpression());
                JsIf jsIf = new JsIf(checkArgIsUndefined, assignment(jsNameRef, defaultValue).makeStmt());
                result.add(jsIf);
            }
        }
        return result;
    }

    @NotNull
    private final FunctionDescriptor descriptor;
    @NotNull
    private final JetDeclarationWithBody declaration;

    private FunctionBodyTranslator(@NotNull FunctionDescriptor descriptor,
                                   @NotNull JetDeclarationWithBody declaration,
                                   @NotNull TranslationContext context) {
        super(context);
        this.descriptor = descriptor;
        this.declaration = declaration;
    }

    @NotNull
    private JsBlock translate() {
        JetExpression jetBodyExpression = declaration.getBodyExpression();
        assert jetBodyExpression != null : "Cannot translate a body of an abstract function.";
        JsBlock jsBlock = new JsBlock(setDefaultValueForArguments(descriptor, context()));
        jsBlock.getStatements().addAll(mayBeWrapWithReturn(Translation.translateExpression(jetBodyExpression, context())).getStatements());
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
        JetType functionReturnType = descriptor.getReturnType();
        assert functionReturnType != null : "Function return typed type must be resolved.";
        return (!declaration.hasBlockBody()) && (!KotlinBuiltIns.getInstance().isUnit(functionReturnType));
    }

    @NotNull
    private static JsNode lastExpressionReturned(@NotNull JsNode body) {
        return mutateLastExpression(body, new Mutator() {
            @Override
            @NotNull
            public JsNode mutate(@NotNull JsNode node) {
                if (!(node instanceof JsExpression)) {
                    return node;
                }
                return new JsReturn((JsExpression)node);
            }
        });
    }
}
