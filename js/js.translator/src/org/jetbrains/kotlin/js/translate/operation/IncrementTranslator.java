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

import com.google.common.collect.Lists;
import com.google.dart.compiler.backend.js.ast.JsBinaryOperation;
import com.google.dart.compiler.backend.js.ast.JsBinaryOperator;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.util.AstUtil;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.js.translate.context.TemporaryVariable;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.js.translate.reference.CachedAccessTranslator;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.psi.JetUnaryExpression;
import org.jetbrains.kotlin.resolve.calls.tasks.TasksPackage;
import org.jetbrains.kotlin.types.expressions.OperatorConventions;

import java.util.List;

import static org.jetbrains.kotlin.js.translate.reference.AccessTranslationUtils.getCachedAccessTranslator;
import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getCallableDescriptorForOperationExpression;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.newSequence;
import static org.jetbrains.kotlin.js.translate.utils.PsiUtils.getBaseExpression;
import static org.jetbrains.kotlin.js.translate.utils.PsiUtils.isPrefix;
import static org.jetbrains.kotlin.js.translate.utils.TemporariesUtils.temporariesInitialization;
import static org.jetbrains.kotlin.js.translate.utils.TranslationUtils.hasCorrespondingFunctionIntrinsic;

// TODO: provide better increment translator logic
public abstract class IncrementTranslator extends AbstractTranslator {

    public static boolean isIncrement(IElementType operationToken) {
        //noinspection SuspiciousMethodCalls
        return OperatorConventions.INCREMENT_OPERATIONS.contains(operationToken);
    }

    @NotNull
    public static JsExpression translate(@NotNull JetUnaryExpression expression,
                                         @NotNull TranslationContext context) {
        if (hasCorrespondingFunctionIntrinsic(context, expression) || isDynamic(context, expression)) {
            return IntrinsicIncrementTranslator.doTranslate(expression, context);
        }
        return (new OverloadedIncrementTranslator(expression, context)).translateIncrementExpression();
    }

    @NotNull
    protected final JetUnaryExpression expression;
    @NotNull
    protected final CachedAccessTranslator accessTranslator;

    protected IncrementTranslator(@NotNull JetUnaryExpression expression,
                                  @NotNull TranslationContext context) {
        super(context);
        this.expression = expression;
        JetExpression baseExpression = getBaseExpression(expression);
        this.accessTranslator = getCachedAccessTranslator(baseExpression, context());
    }

    @NotNull
    protected JsExpression translateIncrementExpression() {
        return withTemporariesInitialized(doTranslateIncrementExpression());
    }

    @NotNull
    private JsExpression doTranslateIncrementExpression() {
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
        JsExpression reassignment = variableReassignment(getExpression);
        JsExpression getNewValue = accessTranslator.translateAsGet();
        return new JsBinaryOperation(JsBinaryOperator.COMMA, reassignment, getNewValue);
    }

    //TODO: decide if this expression can be optimised in case of direct access (not property)
    @NotNull
    private JsExpression asPostfix() {
        // code fragment: expr(a++)
        // generate: expr( (t1 = a, t2 = t1, a = t1.inc(), t2) )
        TemporaryVariable t1 = context().declareTemporary(accessTranslator.translateAsGet());
        TemporaryVariable t2 = context().declareTemporary(t1.reference());
        JsExpression variableReassignment = variableReassignment(t1.reference());
        return AstUtil.newSequence(t1.assignmentExpression(), t2.assignmentExpression(),
                                   variableReassignment, t2.reference());
    }

    @NotNull
    private JsExpression variableReassignment(@NotNull JsExpression toCallMethodUpon) {
        JsExpression overloadedMethodCallOnPropertyGetter = operationExpression(toCallMethodUpon);
        return accessTranslator.translateAsSet(overloadedMethodCallOnPropertyGetter);
    }

    @NotNull
    private JsExpression withTemporariesInitialized(@NotNull JsExpression expression) {
        List<TemporaryVariable> temporaries = accessTranslator.declaredTemporaries();
        List<JsExpression> expressions = Lists.newArrayList(temporariesInitialization(temporaries));
        expressions.add(expression);
        return newSequence(expressions);
    }

    @NotNull
    abstract JsExpression operationExpression(@NotNull JsExpression receiver);

    private static boolean isDynamic(TranslationContext context, JetUnaryExpression expression) {
        CallableDescriptor operationDescriptor = getCallableDescriptorForOperationExpression(context.bindingContext(), expression);
        return TasksPackage.isDynamic(operationDescriptor);
    }
}
