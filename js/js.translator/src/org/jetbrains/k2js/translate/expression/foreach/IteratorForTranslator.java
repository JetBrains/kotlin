/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetForExpression;
import org.jetbrains.k2js.translate.context.Namer;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.reference.CallBuilder;

import static org.jetbrains.k2js.translate.utils.BindingUtils.*;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getLoopRange;

/**
 * @author Pavel Talanov
 */
public final class IteratorForTranslator extends ForTranslator {
    @NotNull
    private final Pair<JsVars.JsVar, JsNameRef> iterator;

    @NotNull
    public static JsStatement doTranslate(@NotNull JetForExpression expression,
                                          @NotNull TranslationContext context) {
        return (new IteratorForTranslator(expression, context).translate());
    }

    private IteratorForTranslator(@NotNull JetForExpression forExpression, @NotNull TranslationContext context) {
        super(forExpression, context);
        iterator = context.dynamicContext().createTemporary(iteratorMethodInvocation());
    }

    @NotNull
    private JsBlock translate() {
        return new JsBlock(new JsVars(iterator.first), new JsWhile(hasNextMethodInvocation(), translateBody(nextMethodInvocation())));
    }

    @NotNull
    private JsExpression nextMethodInvocation() {
        return translateMethodInvocation(iterator.second, getNextFunction(bindingContext(), getLoopRange(expression)));
    }

    @NotNull
    private JsExpression hasNextMethodInvocation() {
        CallableDescriptor hasNextFunction = getHasNextCallable(bindingContext(), getLoopRange(expression));
        if (hasNextFunction instanceof FunctionDescriptor && !isJavaUtilIterator(hasNextFunction)) {
            return translateMethodInvocation(iterator.second, hasNextFunction);
        }

        // develar: I don't know, why hasNext called as function for PropertyDescriptor, our JS side define it as property and all other code translate it as property
        JsNameRef hasNext = new JsNameRef(Namer.getNameForAccessor("hasNext", true, context().isEcma5()));
        hasNext.setQualifier(iterator.second);
        if (context().isEcma5()) {
            return hasNext;
        }
        else {
            return new JsInvocation(hasNext);
        }
    }

    // kotlin iterator define hasNext as property, but java util as function, our js side expects as property
    private static boolean isJavaUtilIterator(CallableDescriptor descriptor) {
        DeclarationDescriptor declaration = descriptor.getContainingDeclaration();
        return declaration.getName().getName().equals("Iterator");
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
