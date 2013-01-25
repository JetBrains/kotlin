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

package org.jetbrains.k2js.translate.expression;

import com.google.dart.compiler.backend.js.ast.*;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.k2js.translate.context.TranslationContext;

class InnerFunctionTranslator extends InnerDeclarationTranslator {
    private final FunctionDescriptor descriptor;

    public InnerFunctionTranslator(
            @NotNull FunctionDescriptor descriptor,
            @NotNull TranslationContext context,
            @NotNull JsFunction fun
    ) {
        super(context, fun);
        this.descriptor = descriptor;
    }

    @SuppressWarnings("MethodOverloadsMethodOfSuperclass")
    public JsExpression translate(@NotNull JsNameRef nameRef, @NotNull TranslationContext outerContext) {
        return translate(nameRef, getThis(outerContext));
    }

    @Override
    protected JsExpression createExpression(@NotNull JsNameRef nameRef, JsExpression self) {
        return nameRef;
    }

    @Override
    protected JsInvocation createInvocation(@NotNull JsNameRef nameRef, @Nullable JsExpression self) {
        return new JsInvocation(new JsNameRef("bind", nameRef), new SmartList<JsExpression>(self));
    }

    @NotNull
    private JsExpression getThis(TranslationContext outerContext) {
        //noinspection ConstantConditions
        ClassDescriptor outerClassDescriptor = context.usageTracker().getOuterClassDescriptor();
        if (outerClassDescriptor != null && descriptor.getReceiverParameter() == null) {
            return outerContext.getThisObject(outerClassDescriptor);
        }

        return JsLiteral.NULL;
    }
}
