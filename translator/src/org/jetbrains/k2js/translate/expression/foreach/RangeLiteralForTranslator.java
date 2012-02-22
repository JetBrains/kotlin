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

import com.google.common.collect.Lists;
import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetForExpression;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;

import java.util.List;

import static org.jetbrains.k2js.translate.utils.PsiUtils.getLoopRange;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.translateLeftExpression;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.translateRightExpression;

/**
 * @author Pavel Talanov
 */

// TODO: respect reverse semantics
public final class RangeLiteralForTranslator extends ForTranslator {

    @NotNull
    public static JsStatement translate(@NotNull JetForExpression expression,
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
    private final TemporaryVariable rangeStart;

    @NotNull
    private final TemporaryVariable rangeEnd;

    private RangeLiteralForTranslator(@NotNull JetForExpression forExpression, @NotNull TranslationContext context) {
        super(forExpression, context);
        JetExpression loopRange = getLoopRange(expression);
        assert loopRange instanceof JetBinaryExpression;
        JetBinaryExpression loopRangeAsBinary = ((JetBinaryExpression) loopRange);
        rangeStart = context.declareTemporary(translateLeftExpression(context, loopRangeAsBinary));
        rangeEnd = context.declareTemporary(getRangeEnd(loopRangeAsBinary));
    }

    @NotNull
    private JsExpression getRangeEnd(@NotNull JetBinaryExpression loopRangeAsBinary) {
        JsExpression rightExpression = translateRightExpression(context(), loopRangeAsBinary);
        return new JsBinaryOperation(JsBinaryOperator.ADD, rightExpression, program().getNumberLiteral(1));
    }

    @NotNull
    private JsBlock translate() {
        List<JsStatement> blockStatements = temporariesInitialization(rangeEnd, rangeStart);
        blockStatements.add(generateForExpression());
        return AstUtil.newBlock(blockStatements);
    }

    @NotNull
    private JsFor generateForExpression() {
        JsFor result = new JsFor();
        result.setInitVars(initExpression());
        result.setCondition(getCondition());
        result.setIncrExpr(getIncrExpression());
        result.setBody(translateOriginalBodyExpression());
        return result;
    }

    @NotNull
    private JsVars initExpression() {
        return AstUtil.newVar(parameterName, rangeStart.reference());
    }

    @NotNull
    private JsExpression getCondition() {
        return AstUtil.notEqual(parameterName.makeRef(), rangeEnd.reference());
    }

    @NotNull
    private JsExpression getIncrExpression() {
        return new JsPrefixOperation(JsUnaryOperator.INC, parameterName.makeRef());
    }

    //TODO : UTIL!!
    @NotNull
    private List<JsStatement> temporariesInitialization(@NotNull TemporaryVariable... temporaries) {
        List<JsStatement> result = Lists.newArrayList();
        for (TemporaryVariable temporary : temporaries) {
            result.add(temporary.assignmentExpression().makeStmt());
        }
        return result;
    }
}
