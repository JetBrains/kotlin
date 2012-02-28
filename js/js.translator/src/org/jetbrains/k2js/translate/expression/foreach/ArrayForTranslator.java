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

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetForExpression;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.intrinsic.Intrinsic;
import org.jetbrains.k2js.translate.utils.BindingUtils;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.k2js.translate.expression.foreach.ForTranslatorUtils.temporariesInitialization;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.getClassDescriptorForType;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.*;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getLoopRange;

/**
 * @author Pavel Talanov
 */
public final class ArrayForTranslator extends ForTranslator {

    @NotNull
    public static JsStatement doTranslate(@NotNull JetForExpression expression,
                                          @NotNull TranslationContext context) {
        return (new ArrayForTranslator(expression, context).translate());
    }

    public static boolean isApplicable(@NotNull JetForExpression expression,
                                       @NotNull TranslationContext context) {
        JetExpression loopRange = getLoopRange(expression);
        JetType rangeType = BindingUtils.getTypeForExpression(context.bindingContext(), loopRange);
        //TODO: better check
        //TODO: IMPORTANT!
        return getClassDescriptorForType(rangeType).getName().equals("Array")
               || getClassDescriptorForType(rangeType).getName().equals("IntArray");
    }

    @NotNull
    private final TemporaryVariable loopRange;

    @NotNull
    private final TemporaryVariable end;

    @NotNull
    private final TemporaryVariable index;

    private ArrayForTranslator(@NotNull JetForExpression forExpression, @NotNull TranslationContext context) {
        super(forExpression, context);
        loopRange = context.declareTemporary(Translation.translateAsExpression(getLoopRange(expression), context));
        Intrinsic lengthPropertyIntrinsic = context().intrinsics().getLengthPropertyIntrinsic();
        JsExpression length = lengthPropertyIntrinsic.apply(loopRange.reference(),
                                                            Collections.<JsExpression>emptyList(),
                                                            context());
        end = context().declareTemporary(length);
        index = context().declareTemporary(program().getNumberLiteral(0));
    }

    @NotNull
    private JsBlock translate() {
        List<JsStatement> blockStatements = temporariesInitialization(loopRange, end);
        blockStatements.add(generateForExpression(getInitExpression(), getCondition(), getIncrExpression(), getBody()));
        return newBlock(blockStatements);
    }


    @NotNull
    private JsStatement getBody() {
        JsArrayAccess arrayAccess = new JsArrayAccess(loopRange.reference(), index.reference());
        JsStatement currentVar = newVar(parameterName, arrayAccess);
        JsStatement realBody = translateOriginalBodyExpression();
        return AstUtil.newBlock(currentVar, realBody);
    }

    @NotNull
    private JsVars getInitExpression() {
        return newVar(index.name(), program().getNumberLiteral(0));
    }

    @NotNull
    private JsExpression getCondition() {
        return notEqual(index.reference(), end.reference());
    }

    @NotNull
    private JsExpression getIncrExpression() {
        return new JsPrefixOperation(JsUnaryOperator.INC, index.reference());
    }
}
