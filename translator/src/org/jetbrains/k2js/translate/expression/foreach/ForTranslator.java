/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetForExpression;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;

import static org.jetbrains.k2js.translate.utils.PsiUtils.getLoopBody;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getLoopParameter;

/**
 * @author Pavel Talanov
 */

//TODO: create util class for managing stuff like binary operations
public abstract class ForTranslator extends AbstractTranslator {

    @NotNull
    public static JsStatement translate(@NotNull JetForExpression expression,
                                        @NotNull TranslationContext context) {
        if (RangeLiteralForTranslator.isApplicable(expression, context)) {
            return RangeLiteralForTranslator.translate(expression, context);
        }
        if (ArrayForTranslator.isApplicable(expression, context)) {
            return ArrayForTranslator.translate(expression, context);
        }
        if (RangeForTranslator.isApplicable(expression, context)) {
            return RangeForTranslator.translate(expression, context);
        }
        return IteratorForTranslator.translate(expression, context);
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
        JetParameter loopParameter = getLoopParameter(expression);
        return context().getNameForElement(loopParameter);
    }

    //TODO: look for should-be-usages
    @NotNull
    protected JsStatement translateOriginalBodyExpression() {
        return Translation.translateAsStatement(getLoopBody(expression), context());
    }


}
