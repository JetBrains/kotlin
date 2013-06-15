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

package org.jetbrains.k2js.translate.expression;

import com.google.dart.compiler.backend.js.ast.*;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;

import java.util.List;

import static org.jetbrains.k2js.translate.utils.JsAstUtils.convertToBlock;

//TODO: not tested at all
//TODO: not implemented catch logic
public final class TryTranslator extends AbstractTranslator {

    @NotNull
    public static JsTry translate(@NotNull JetTryExpression expression,
                                  @NotNull TranslationContext context) {
        return (new TryTranslator(expression, context)).translate();
    }

    @NotNull
    private final JetTryExpression expression;

    private TryTranslator(@NotNull JetTryExpression expression,
                          @NotNull TranslationContext context) {
        super(context);
        this.expression = expression;
    }

    private JsTry translate() {
        return new JsTry(translateTryBlock(), translateCatches(), translateFinallyBlock());
    }

    @Nullable
    private JsBlock translateFinallyBlock() {
        JetFinallySection finallyBlock = expression.getFinallyBlock();
        if (finallyBlock == null) return null;

        return convertToBlock(Translation.translateAsStatement(finallyBlock.getFinalExpression(), context()));
    }

    @NotNull
    private JsBlock translateTryBlock() {
        return convertToBlock(Translation.translateAsStatement(expression.getTryBlock(), context()));
    }

    @NotNull
    private List<JsCatch> translateCatches() {
        List<JsCatch> result = new SmartList<JsCatch>();
        for (JetCatchClause catchClause : expression.getCatchClauses()) {
            result.add(translateCatchClause(catchClause));
        }
        return result;
    }

    @NotNull
    private JsCatch translateCatchClause(@NotNull JetCatchClause catchClause) {
        JetParameter catchParameter = catchClause.getCatchParameter();
        assert catchParameter != null : "Valid catch must have a parameter.";

        JsName parameterName = context().getNameForElement(catchParameter);
        JsCatch result = new JsCatch(context().scope(), parameterName.getIdent());
        result.setBody(translateCatchBody(catchClause));
        return result;
    }

    @NotNull
    private JsBlock translateCatchBody(@NotNull JetCatchClause catchClause) {
        JetExpression catchBody = catchClause.getCatchBody();
        if (catchBody == null) {
            return convertToBlock(program().getEmptyStmt());
        }
        return convertToBlock(Translation.translateAsStatement(catchBody, context()));
    }

}
