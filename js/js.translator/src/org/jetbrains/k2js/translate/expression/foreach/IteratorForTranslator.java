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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetForExpression;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.reference.CallBuilder;

import static org.jetbrains.k2js.translate.utils.BindingUtils.*;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.convertToBlock;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.newVar;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getLoopBody;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getLoopRange;

/**
 * @author Pavel Talanov
 */
public final class IteratorForTranslator extends ForTranslator {

    @NotNull
    public static JsStatement doTranslate(@NotNull JetForExpression expression,
                                          @NotNull TranslationContext context) {
        return (new IteratorForTranslator(expression, context).translate());
    }

    @NotNull
    private final TemporaryVariable iterator;

    private IteratorForTranslator(@NotNull JetForExpression forExpression, @NotNull TranslationContext context) {
        super(forExpression, context);
        iterator = context().declareTemporary(iteratorMethodInvocation());
    }

    @NotNull
    private JsBlock translate() {
        JsBlock bodyBlock = generateCycleBody();
        JsWhile cycle = new JsWhile(hasNextMethodInvocation(), bodyBlock);
        return AstUtil.newBlock(iterator.assignmentExpression().makeStmt(), cycle);
    }

    //TODO: check whether complex logic with blocks is needed
    @NotNull
    private JsBlock generateCycleBody() {
        JsBlock cycleBody = new JsBlock();
        JsStatement parameterAssignment = newVar(parameterName, nextMethodInvocation());
        JsNode originalBody = Translation.translateExpression(getLoopBody(expression), context().innerBlock(cycleBody));
        cycleBody.getStatements().add(parameterAssignment);
        cycleBody.getStatements().add(convertToBlock(originalBody));
        return cycleBody;
    }

    @NotNull
    private JsExpression nextMethodInvocation() {
        FunctionDescriptor nextFunction = getNextFunction(bindingContext(), getLoopRange(expression));
        return translateMethodInvocation(iterator.reference(), nextFunction);
    }

    @NotNull
    private JsExpression hasNextMethodInvocation() {
        CallableDescriptor hasNextFunction = getHasNextCallable(bindingContext(), getLoopRange(expression));
        return translateMethodInvocation(iterator.reference(), hasNextFunction);
    }

    @NotNull
    private JsExpression iteratorMethodInvocation() {
        JetExpression rangeExpression = getLoopRange(expression);
        JsExpression range = Translation.translateAsExpression(rangeExpression, context());
        FunctionDescriptor iteratorFunction = getIteratorFunction(bindingContext(), rangeExpression);
        return translateMethodInvocation(range, iteratorFunction);
    }

    @NotNull
    private JsExpression translateMethodInvocation(@Nullable JsExpression receiver,
                                                   @NotNull CallableDescriptor descriptor) {
        return CallBuilder.build(context())
                .receiver(receiver)
                .descriptor(descriptor)
                .translate();
    }
}
