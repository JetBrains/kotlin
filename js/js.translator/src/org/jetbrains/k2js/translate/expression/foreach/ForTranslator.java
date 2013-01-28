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

package org.jetbrains.k2js.translate.expression.foreach;

import com.google.dart.compiler.backend.js.ast.JsBlock;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetForExpression;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;

import static org.jetbrains.k2js.translate.utils.JsAstUtils.newVar;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getLoopBody;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getLoopParameter;

public abstract class ForTranslator extends AbstractTranslator {

    @NotNull
    public static JsStatement translate(@NotNull JetForExpression expression,
                                        @NotNull TranslationContext context) {
        if (RangeLiteralForTranslator.isApplicable(expression, context)) {
            return RangeLiteralForTranslator.doTranslate(expression, context);
        }
        if (RangeForTranslator.isApplicable(expression, context)) {
            return RangeForTranslator.doTranslate(expression, context);
        }
        if (ArrayForTranslator.isApplicable(expression, context)) {
            return ArrayForTranslator.doTranslate(expression, context);
        }
        return IteratorForTranslator.doTranslate(expression, context);
    }

    @NotNull
    protected final JetForExpression expression;
    @NotNull
    protected final JsName parameterName;

    protected ForTranslator(@NotNull JetForExpression forExpression, @NotNull TranslationContext context) {
        super(context);
        this.expression = forExpression;
        this.parameterName = declareParameter();
    }

    @NotNull
    private JsName declareParameter() {
        return context().getNameForElement(getLoopParameter(expression));
    }

    @NotNull
    protected JsStatement translateOriginalBodyExpression() {
        return Translation.translateAsStatement(getLoopBody(expression), context());
    }

    @NotNull
    protected JsStatement translateBody(JsExpression itemValue) {
        JsStatement currentVar = newVar(parameterName, itemValue);
        JsStatement realBody = translateOriginalBodyExpression();
        if (realBody instanceof JsBlock) {
            JsBlock block = (JsBlock) realBody;
            block.getStatements().add(0, currentVar);
            return block;
        }
        else {
            return new JsBlock(currentVar, realBody);
        }
    }
}
