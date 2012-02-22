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
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetForExpression;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.intrinsic.string.LengthIntrinsic;
import org.jetbrains.k2js.translate.utils.BindingUtils;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.k2js.translate.utils.DescriptorUtils.getClassDescriptorForType;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getLoopBody;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getLoopRange;

/**
 * @author Pavel Talanov
 */
public final class ArrayForTranslator extends ForTranslator {

    @NotNull
    public static JsStatement translate(@NotNull JetForExpression expression,
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
    private final TemporaryVariable rangeExpression;

    private ArrayForTranslator(@NotNull JetForExpression forExpression, @NotNull TranslationContext context) {
        super(forExpression, context);
        rangeExpression = context.declareTemporary(Translation.translateAsExpression(getLoopRange(expression), context));
    }

    @NotNull
    private JsBlock translate() {
        JsExpression length = LengthIntrinsic.INSTANCE.apply(rangeExpression.reference(), Collections.<JsExpression>emptyList(), context());
        TemporaryVariable end = context().declareTemporary(length);
        List<JsStatement> blockStatements = temporariesInitialization(rangeExpression, end);
        blockStatements.add(generateForExpression(end));
        return AstUtil.newBlock(blockStatements);
    }

    @NotNull
    private JsFor generateForExpression(@NotNull TemporaryVariable end) {
        JsFor result = new JsFor();
        //TODO: make index instance variable
        TemporaryVariable indexVar = context().declareTemporary(program().getNumberLiteral(0));
        result.setInitVars(initExpression(indexVar));
        result.setCondition(getCondition(end, indexVar));
        result.setIncrExpr(getIncrExpression(indexVar));
        result.setBody(getBody(indexVar));
        return result;
    }

    @NotNull
    private JsStatement getBody(@NotNull TemporaryVariable index) {
        JsArrayAccess arrayAccess = new JsArrayAccess(rangeExpression.reference(), index.reference());
        JsStatement currentVar = AstUtil.newVar(parameterName, arrayAccess);
        JsStatement realBody = Translation.translateAsStatement(getLoopBody(expression), context());
        return AstUtil.newBlock(currentVar, realBody);
    }

    @NotNull
    private JsVars initExpression(TemporaryVariable start) {
        return AstUtil.newVar(start.name(), program().getNumberLiteral(0));
    }

    @NotNull
    private JsExpression getCondition(TemporaryVariable end, TemporaryVariable index) {
        return AstUtil.notEqual(index.reference(), end.reference());
    }

    @NotNull
    private JsExpression getIncrExpression(@NotNull TemporaryVariable index) {
        return new JsPrefixOperation(JsUnaryOperator.INC, index.reference());
    }

    //TODO: move somewhere util
    @NotNull
    private List<JsStatement> temporariesInitialization(TemporaryVariable... temporaries) {
        List<JsStatement> result = Lists.newArrayList();
        for (TemporaryVariable temporary : temporaries) {
            result.add(temporary.assignmentExpression().makeStmt());
        }
        return result;
    }
}
