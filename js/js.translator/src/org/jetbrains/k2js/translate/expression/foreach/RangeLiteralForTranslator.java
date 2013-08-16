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

import com.google.common.collect.Lists;
import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetForExpression;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;

import java.util.List;

import static org.jetbrains.k2js.translate.utils.JsAstUtils.*;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getLoopRange;
import static org.jetbrains.k2js.translate.utils.TemporariesUtils.temporariesInitialization;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.translateLeftExpression;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.translateRightExpression;


// TODO: implement reverse semantics
public final class RangeLiteralForTranslator extends ForTranslator {

    @NotNull
    public static JsStatement doTranslate(@NotNull JetForExpression expression,
                                          @NotNull TranslationContext context) {
        return (new RangeLiteralForTranslator(expression, context).translate());
    }

    public static boolean isApplicable(@NotNull JetForExpression expression,
                                       @NotNull TranslationContext context) {
        JetExpression loopRange = getLoopRange(expression);
        if (!(loopRange instanceof JetBinaryExpression)) {
            return false;
        }
        boolean isRangeToOperation = ((JetBinaryExpression) loopRange).getOperationToken() == JetTokens.RANGE;
        return isRangeToOperation && RangeForTranslator.isApplicable(expression, context);
    }

    @NotNull
    private final JsExpression rangeStart;

    @NotNull
    private final TemporaryVariable rangeEnd;

    private RangeLiteralForTranslator(@NotNull JetForExpression forExpression, @NotNull TranslationContext context) {
        super(forExpression, context);
        JetExpression loopRange = getLoopRange(expression);
        assert loopRange instanceof JetBinaryExpression;
        JetBinaryExpression loopRangeAsBinary = ((JetBinaryExpression) loopRange);
        rangeStart = translateLeftExpression(context, loopRangeAsBinary);
        rangeEnd = context.declareTemporary(getRangeEnd(loopRangeAsBinary));
    }

    @NotNull
    private JsExpression getRangeEnd(@NotNull JetBinaryExpression loopRangeAsBinary) {
        JsExpression rightExpression = translateRightExpression(context(), loopRangeAsBinary);
        return new JsBinaryOperation(JsBinaryOperator.ADD, rightExpression, program().getNumberLiteral(1));
    }

    @NotNull
    private JsBlock translate() {
        List<JsStatement> blockStatements = Lists.newArrayList();
        blockStatements.add(temporariesInitialization(rangeEnd).makeStmt());
        blockStatements.add(generateForExpression(initExpression(),
                                                  getCondition(),
                                                  getIncrExpression(),
                                                  translateBody(null)));
        return new JsBlock(blockStatements);
    }

    @NotNull
    private JsVars initExpression() {
        return newVar(parameterName, rangeStart);
    }

    @NotNull
    private JsExpression getCondition() {
        return inequality(parameterName.makeRef(), rangeEnd.reference());
    }

    @NotNull
    private JsExpression getIncrExpression() {
        return new JsPostfixOperation(JsUnaryOperator.INC, parameterName.makeRef());
    }
}
