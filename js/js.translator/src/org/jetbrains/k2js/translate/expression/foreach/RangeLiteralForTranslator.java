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

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetForExpression;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import static org.jetbrains.k2js.translate.utils.JsAstUtils.lessThanEq;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.newVar;
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
        JsBlock startBlock = new JsBlock();
        JsExpression rangeStartExpression = translateLeftExpression(context, loopRangeAsBinary, startBlock);
        JsBlock endBlock = new JsBlock();
        JsExpression rightExpression = translateRightExpression(context(), loopRangeAsBinary, endBlock);
        if (TranslationUtils.isCacheNeeded(rangeStartExpression)) {
            TemporaryVariable startVar = context.declareTemporary(rangeStartExpression);
            rangeStartExpression = startVar.reference();
            context.addStatementToCurrentBlock(startVar.assignmentExpression().makeStmt());
        }
        rangeStart = rangeStartExpression;
        context.addStatementsToCurrentBlockFrom(startBlock);
        context.addStatementsToCurrentBlockFrom(endBlock);
        rangeEnd = context.declareTemporary(rightExpression);
    }

    @NotNull
    private JsStatement translate() {
        context().addStatementToCurrentBlock(temporariesInitialization(rangeEnd).makeStmt());
        return new JsFor(initExpression(), getCondition(), getIncrExpression(), translateBody(null));
    }

    @NotNull
    private JsVars initExpression() {
        return newVar(parameterName, rangeStart);
    }

    @NotNull
    private JsExpression getCondition() {
        return lessThanEq(parameterName.makeRef(), rangeEnd.reference());
    }

    @NotNull
    private JsExpression getIncrExpression() {
        return new JsPostfixOperation(JsUnaryOperator.INC, parameterName.makeRef());
    }
}
