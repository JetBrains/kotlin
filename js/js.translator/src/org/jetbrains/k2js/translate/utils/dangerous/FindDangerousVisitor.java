/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.k2js.translate.utils.dangerous;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.reference.InlinedCallExpressionTranslator;

import static org.jetbrains.k2js.translate.utils.BindingUtils.isStatement;

/**
 * @author Pavel Talanov
 */
public final class FindDangerousVisitor extends JetTreeVisitor<DangerousData> {

    @NotNull
    private final TranslationContext context;

    public FindDangerousVisitor(@NotNull TranslationContext context) {
        this.context = context;
    }

    @Override
    public Void visitDeclaration(JetDeclaration dcl, DangerousData data) {
        return null;
    }

    @Override
    public Void visitJetElement(JetElement element, DangerousData data) {
        if (data.exists()) {
            return null;
        }
        return super.visitJetElement(element, data);
    }

    @Override
    public Void visitWhenExpression(JetWhenExpression expression, DangerousData data) {
        if (expressionFound(expression, data)) {
            return null;
        }
        return super.visitWhenExpression(expression, data);
    }

    @Override
    public Void visitIfExpression(JetIfExpression expression, DangerousData data) {
        if (expressionFound(expression, data)) {
            return null;
        }
        return super.visitIfExpression(expression, data);
    }

    @Override
    public Void visitBlockExpression(JetBlockExpression expression, DangerousData data) {
        if (isStatement(context.bindingContext(), expression)) {
            return null;
        }
        else {
            return super.visitBlockExpression(expression, data);
        }
    }

    @Override
    public Void visitCallExpression(JetCallExpression expression, DangerousData data) {
        if (InlinedCallExpressionTranslator.shouldBeInlined(expression, context)) {
            if (expressionFound(expression, data)) {
                return null;
            }
        }
        return super.visitCallExpression(expression, data);
    }

    private boolean expressionFound(@NotNull JetExpression expression, @NotNull DangerousData data) {
        if (data.exists()) {
            return true;
        }
        if (!isStatement(context.bindingContext(), expression)) {
            data.setDangerousNode(expression);
            return true;
        }
        return false;
    }
}
