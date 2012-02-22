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
import org.jetbrains.k2js.translate.utils.BindingUtils;

import java.util.List;

import static org.jetbrains.k2js.translate.utils.DescriptorUtils.getClassDescriptorForType;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getLoopBody;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getLoopRange;

/**
 * @author Pavel Talanov
 */
public final class RangeForTranslator extends ForTranslator {

    @NotNull
    public static JsStatement translate(@NotNull JetForExpression expression,
                                        @NotNull TranslationContext context) {
        return (new RangeForTranslator(expression, context).translate());
    }

    public static boolean isApplicable(@NotNull JetForExpression expression,
                                       @NotNull TranslationContext context) {
        JetExpression loopRange = getLoopRange(expression);
        JetType rangeType = BindingUtils.getTypeForExpression(context.bindingContext(), loopRange);
        //TODO: better check
        return getClassDescriptorForType(rangeType).getName().equals("IntRange");
    }

    @NotNull
    private final TemporaryVariable rangeExpression;

    private RangeForTranslator(@NotNull JetForExpression forExpression, @NotNull TranslationContext context) {
        super(forExpression, context);
        rangeExpression = context.declareTemporary(Translation.translateAsExpression(getLoopRange(expression), context));
    }

    @NotNull
    private JsBlock translate() {
        //TODO: make fields
        JsExpression isReversed = callFunction("get_reversed");
        JsConditional incrVarValue = new JsConditional(isReversed,
                program().getNumberLiteral(-1),
                program().getNumberLiteral(1));
        TemporaryVariable incrVar = context().declareTemporary(incrVarValue);
        TemporaryVariable start = context().declareTemporary(callFunction("get_start"));
        TemporaryVariable end = context().declareTemporary(new JsBinaryOperation(JsBinaryOperator.ADD, callFunction("get_end"), incrVar.reference()));
        List<JsStatement> blockStatements = temporariesInitialization(rangeExpression, incrVar, start, end);
        blockStatements.add(generateForExpression(incrVar, start, end));
        return AstUtil.newBlock(blockStatements);
    }

    @NotNull
    private JsFor generateForExpression(@NotNull TemporaryVariable incrVar,
                                        @NotNull TemporaryVariable start,
                                        @NotNull TemporaryVariable end) {
        JsFor result = new JsFor();
        result.setInitVars(initExpression(start, parameterName));
        result.setCondition(getCondition(end, parameterName));
        result.setIncrExpr(getIncrExpression(incrVar, parameterName));
        result.setBody(Translation.translateAsStatement(getLoopBody(expression), context()));
        return result;
    }

    @NotNull
    private JsVars initExpression(TemporaryVariable start, JsName parameterName) {
        return AstUtil.newVar(parameterName, start.reference());
    }

    @NotNull
    private JsExpression getCondition(TemporaryVariable end, JsName parameterName) {
        return AstUtil.notEqual(parameterName.makeRef(), end.reference());
    }

    @NotNull
    private JsExpression getIncrExpression(@NotNull TemporaryVariable incrVar, @NotNull JsName parameterName) {
        return new JsBinaryOperation(JsBinaryOperator.ASG_ADD, parameterName.makeRef(), incrVar.reference());
    }

    @NotNull
    private JsExpression getField(@NotNull String fieldName) {
        JsNameRef nameRef = AstUtil.newQualifiedNameRef(fieldName);
        AstUtil.setQualifier(nameRef, rangeExpression.reference());
        return nameRef;
    }

    @NotNull
    private JsExpression callFunction(@NotNull String funName) {
        return AstUtil.newInvocation(getField(funName));
    }

    @NotNull
    private List<JsStatement> temporariesInitialization(TemporaryVariable... temporaries) {
        List<JsStatement> result = Lists.newArrayList();
        for (TemporaryVariable temporary : temporaries) {
            result.add(temporary.assignmentExpression().makeStmt());
        }
        return result;
    }
}
