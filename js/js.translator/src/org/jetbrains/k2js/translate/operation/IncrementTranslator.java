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

package org.jetbrains.k2js.translate.operation;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetUnaryExpression;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.reference.AccessTranslator;

import static org.jetbrains.k2js.translate.utils.BindingUtils.isStatement;
import static org.jetbrains.k2js.translate.utils.BindingUtils.isVariableReassignment;
import static org.jetbrains.k2js.translate.utils.PsiUtils.*;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.isIntrinsicOperation;

/**
 * @author Pavel Talanov
 */
// TODO: provide better increment translator logic
public abstract class IncrementTranslator extends AbstractTranslator {

    public static boolean isIncrement(@NotNull JetUnaryExpression expression) {
        return OperatorConventions.INCREMENT_OPERATIONS.contains(getOperationToken(expression));
    }

    @NotNull
    public static JsExpression translate(@NotNull JetUnaryExpression expression,
                                         @NotNull TranslationContext context) {
        if (isIntrinsicOperation(context, expression)) {
            return IntrinsicIncrementTranslator.doTranslate(expression, context);
        }
        return OverloadedIncrementTranslator.doTranslate(expression, context);
    }

    @NotNull
    protected final JetUnaryExpression expression;
    @NotNull
    protected final AccessTranslator accessTranslator;
    private final boolean isVariableReassignment;

    protected IncrementTranslator(@NotNull JetUnaryExpression expression,
                                  @NotNull TranslationContext context) {
        super(context);
        this.expression = expression;
        this.isVariableReassignment = isVariableReassignment(context.bindingContext(), expression);
        JetExpression baseExpression = getBaseExpression(expression);
        this.accessTranslator = AccessTranslator.getAccessTranslator(baseExpression, context());
    }

    @NotNull
    protected JsExpression translateAsMethodCall() {
        if (returnValueIgnored() || isPrefix(expression)) {
            return asPrefix();
        }
        if (isVariableReassignment) {
            return asPostfixWithReassignment();
        }
        else {
            return asPostfixWithNoReassignment();
        }
    }

    private boolean returnValueIgnored() {
        return isStatement(bindingContext(), expression);
    }

    @NotNull
    private JsExpression asPrefix() {
        JsExpression getExpression = accessTranslator.translateAsGet();
        if (isVariableReassignment) {
            return variableReassignment(getExpression);
        }
        return operationExpression(getExpression);
    }

    //TODO: decide if this expression can be optimised in case of direct access (not property)
    @NotNull
    private JsExpression asPostfixWithReassignment() {
        // code fragment: expr(a++)
        // generate: expr( (t1 = a, t2 = t1, a = t1.inc(), t2) )
        TemporaryVariable t1 = context().declareTemporary(accessTranslator.translateAsGet());
        TemporaryVariable t2 = context().declareTemporary(t1.reference());
        JsExpression variableReassignment = variableReassignment(t1.reference());
        return AstUtil.newSequence(t1.assignmentExpression(), t2.assignmentExpression(),
                                   variableReassignment, t2.reference());
    }

    //TODO: TEST
    @NotNull
    private JsExpression asPostfixWithNoReassignment() {
        // code fragment: expr(a++)
        // generate: expr( (t1 = a, t2 = t1, t2.inc(), t1) )
        TemporaryVariable t1 = context().declareTemporary(accessTranslator.translateAsGet());
        TemporaryVariable t2 = context().declareTemporary(t1.reference());
        JsExpression methodCall = operationExpression(t2.reference());
        JsExpression returnedValue = t1.reference();
        return AstUtil.newSequence(t1.assignmentExpression(), t2.assignmentExpression(), methodCall, returnedValue);
    }

    @NotNull
    private JsExpression variableReassignment(@NotNull JsExpression toCallMethodUpon) {
        JsExpression overloadedMethodCallOnPropertyGetter = operationExpression(toCallMethodUpon);
        return accessTranslator.translateAsSet(overloadedMethodCallOnPropertyGetter);
    }

    @NotNull
    abstract JsExpression operationExpression(@NotNull JsExpression receiver);
}
