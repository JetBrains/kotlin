/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.translate.operation;

import org.jetbrains.kotlin.js.backend.ast.JsBinaryOperation;
import org.jetbrains.kotlin.js.backend.ast.JsBinaryOperator;
import org.jetbrains.kotlin.js.backend.ast.JsBlock;
import org.jetbrains.kotlin.js.backend.ast.JsExpression;
import org.jetbrains.kotlin.js.backend.ast.metadata.MetadataProperties;
import org.jetbrains.kotlin.js.util.AstUtil;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.js.translate.context.TemporaryVariable;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.js.translate.reference.AccessTranslator;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtUnaryExpression;
import org.jetbrains.kotlin.resolve.calls.tasks.DynamicCallsKt;
import org.jetbrains.kotlin.types.expressions.OperatorConventions;

import static org.jetbrains.kotlin.js.translate.reference.AccessTranslationUtils.getAccessTranslator;
import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getCallableDescriptorForOperationExpression;
import static org.jetbrains.kotlin.js.translate.utils.PsiUtils.getBaseExpression;
import static org.jetbrains.kotlin.js.translate.utils.PsiUtils.isPrefix;
import static org.jetbrains.kotlin.js.translate.utils.TranslationUtils.hasCorrespondingFunctionIntrinsic;

// TODO: provide better increment translator logic
public abstract class IncrementTranslator extends AbstractTranslator {

    public static boolean isIncrement(IElementType operationToken) {
        //noinspection SuspiciousMethodCalls
        return OperatorConventions.INCREMENT_OPERATIONS.contains(operationToken);
    }

    @NotNull
    public static JsExpression translate(@NotNull KtUnaryExpression expression,
                                         @NotNull TranslationContext context) {
        if (isDynamic(context, expression)) {
            return DynamicIncrementTranslator.doTranslate(expression, context);
        }
        if (hasCorrespondingFunctionIntrinsic(context, expression)) {
            return new IntrinsicIncrementTranslator(expression, context).translateIncrementExpression();
        }
        return (new OverloadedIncrementTranslator(expression, context)).translateIncrementExpression();
    }

    @NotNull
    protected final KtUnaryExpression expression;
    @NotNull
    protected final AccessTranslator accessTranslator;

    @NotNull
    private final JsBlock accessBlock = new JsBlock();

    protected IncrementTranslator(@NotNull KtUnaryExpression expression,
                                  @NotNull TranslationContext context) {
        super(context);
        this.expression = expression;
        KtExpression baseExpression = getBaseExpression(expression);
        this.accessTranslator = getAccessTranslator(baseExpression, context().innerBlock(accessBlock)).getCached();
    }

    @NotNull
    protected JsExpression translateIncrementExpression() {
        if (isPrefix(expression)) {
            return asPrefix();
        }
        return asPostfix();
    }

    //TODO: decide if this expression can be optimised in case of direct access (not property)
    @NotNull
    private JsExpression asPrefix() {
        // code fragment: expr(a++)
        // generate: expr(a = a.inc(), a)
        JsExpression getExpression = accessTranslator.translateAsGet();
        JsExpression reassignment = variableReassignment(context().innerBlock(accessBlock), getExpression);
        accessBlock.getStatements().add(JsAstUtils.asSyntheticStatement(reassignment));
        JsExpression getNewValue = accessTranslator.translateAsGet();

        JsExpression result;
        if (accessBlock.getStatements().size() == 1) {
            result = new JsBinaryOperation(JsBinaryOperator.COMMA, reassignment, getNewValue);
        }
        else {
            context().getCurrentBlock().getStatements().addAll(accessBlock.getStatements());
            result = getNewValue;
        }
        MetadataProperties.setSynthetic(result, true);
        return result;
    }

    //TODO: decide if this expression can be optimised in case of direct access (not property)
    @NotNull
    private JsExpression asPostfix() {
        // code fragment: expr(a++)
        // generate: expr( (t1 = a, t2 = t1, a = t1.inc(), t2) )
        TemporaryVariable t1 = context().declareTemporary(accessTranslator.translateAsGet());
        accessBlock.getStatements().add(t1.assignmentStatement());
        JsExpression variableReassignment = variableReassignment(context().innerBlock(accessBlock), t1.reference());
        accessBlock.getStatements().add(JsAstUtils.asSyntheticStatement(variableReassignment));

        JsExpression result;
        if (accessBlock.getStatements().size() == 2) {
            result = AstUtil.newSequence(t1.assignmentExpression(), variableReassignment, t1.reference());
        }
        else {
            context().getCurrentBlock().getStatements().addAll(accessBlock.getStatements());
            result = t1.reference();
        }

        MetadataProperties.setSynthetic(result, true);
        return result;
    }

    @NotNull
    private JsExpression variableReassignment(@NotNull TranslationContext context, @NotNull JsExpression toCallMethodUpon) {
        JsExpression overloadedMethodCallOnPropertyGetter = operationExpression(context, toCallMethodUpon);
        return accessTranslator.translateAsSet(overloadedMethodCallOnPropertyGetter);
    }

    @NotNull
    abstract JsExpression operationExpression(@NotNull TranslationContext context, @NotNull JsExpression receiver);

    private static boolean isDynamic(TranslationContext context, KtUnaryExpression expression) {
        CallableDescriptor operationDescriptor = getCallableDescriptorForOperationExpression(context.bindingContext(), expression);
        assert  operationDescriptor != null;
        return DynamicCallsKt.isDynamic(operationDescriptor);
    }
}
