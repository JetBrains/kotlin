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

package org.jetbrains.k2js.translate.expression.foreach;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.backend.js.ast.JsVars.JsVar;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetForExpression;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.intrinsic.functions.factories.ArrayFIF;
import org.jetbrains.k2js.translate.utils.BindingUtils;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import java.util.Collections;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getClassDescriptorForType;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.generateForExpression;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.inequality;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getLoopRange;

/**
 * @author Pavel Talanov
 */
public final class ArrayForTranslator extends ForTranslator {
    @NotNull
    private final Pair<JsVar, JsNameRef> loopRange;
    @NotNull
    private final Pair<JsVar, JsNameRef> end;
    @NotNull
    private final Pair<JsVar, JsNameRef> index;

    private ArrayForTranslator(@NotNull JetForExpression forExpression, @NotNull TranslationContext context) {
        super(forExpression, context);

        loopRange = TranslationUtils.createTemporaryIfNeed(Translation.translateAsExpression(getLoopRange(expression), context), context);

        JsExpression length = ArrayFIF.ARRAY_LENGTH_INTRINSIC.apply(loopRange.second, Collections.<JsExpression>emptyList(), context());
        end = context.dynamicContext().createTemporary(length);
        index = context.dynamicContext().createTemporary(program().getNumberLiteral(0));
    }

    @NotNull
    public static JsStatement doTranslate(@NotNull JetForExpression expression,
                                          @NotNull TranslationContext context) {
        return new ArrayForTranslator(expression, context).translate();
    }

    public static boolean isApplicable(@NotNull JetForExpression expression,
            @NotNull TranslationContext context) {
        JetExpression loopRange = getLoopRange(expression);
        JetType rangeType = BindingUtils.getTypeForExpression(context.bindingContext(), loopRange);
        //TODO: better check
        //TODO: IMPORTANT!
        return getClassDescriptorForType(rangeType).getName().getName().equals("Array")
               || getClassDescriptorForType(rangeType).getName().getName().equals("IntArray");
    }

    @NotNull
    private JsFor translate() {
        return generateForExpression(getInitExpression(), getCondition(), getIncrementExpression(), getBody());
    }

    @NotNull
    private JsStatement getBody() {
        return translateBody(new JsArrayAccess(loopRange.second, index.second));
    }

    @NotNull
    private JsVars getInitExpression() {
        JsVars vars = new JsVars();
        if (loopRange.first != null) {
            vars.add(loopRange.first);
        }
        vars.add(index.first);
        vars.add(end.first);
        return vars;
    }

    @NotNull
    private JsExpression getCondition() {
        return inequality(index.second, end.second);
    }

    @NotNull
    private JsExpression getIncrementExpression() {
        return new JsPostfixOperation(JsUnaryOperator.INC, index.second);
    }
}
