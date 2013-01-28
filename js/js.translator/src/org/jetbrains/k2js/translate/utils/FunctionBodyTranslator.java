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

import com.google.dart.compiler.backend.js.ast.JsBlock;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsNode;
import com.google.dart.compiler.backend.js.ast.JsReturn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetDeclarationWithBody;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.utils.mutator.Mutator;

import static org.jetbrains.k2js.translate.utils.JsAstUtils.convertToBlock;
import static org.jetbrains.k2js.translate.utils.mutator.LastExpressionMutator.mutateLastExpression;

public final class FunctionBodyTranslator extends AbstractTranslator {

    @NotNull
    public static JsBlock translateFunctionBody(@NotNull FunctionDescriptor descriptor,
                                                @NotNull JetDeclarationWithBody declarationWithBody,
                                                @NotNull TranslationContext functionBodyContext) {
        return (new FunctionBodyTranslator(descriptor, declarationWithBody, functionBodyContext)).translate();
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
        return mayBeWrapWithReturn(Translation.translateExpression(jetBodyExpression, context()));
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
