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

package org.jetbrains.k2js.translate.expression;

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.k2js.translate.context.TranslationContext;

import java.util.List;

class InnerFunctionTranslator extends InnerDeclarationTranslator {
    private final FunctionDescriptor descriptor;

    public InnerFunctionTranslator(@NotNull JetElement element,
            @NotNull FunctionDescriptor descriptor,
            @NotNull TranslationContext context,
            @NotNull JsFunction fun) {
        super(element, descriptor, context, fun);
        this.descriptor = descriptor;
    }

    @Override
    protected List<ValueParameterDescriptor> getValueParameters() {
        return descriptor.getValueParameters();
    }

    @SuppressWarnings("MethodOverloadsMethodOfSuperclass")
    @NotNull
    public JsExpression translate(@NotNull JsNameRef nameRef, @NotNull TranslationContext outerContext) {
        return translate(nameRef, getThis(outerContext));
    }

    @Override
    protected JsExpression createExpression(@NotNull JsNameRef nameRef, @Nullable JsExpression self) {
        return nameRef;
    }

    @Override
    protected JsInvocation createInvocation(@NotNull JsNameRef nameRef, @Nullable JsExpression self) {
        JsInvocation bind = new JsInvocation(context.namer().kotlin(getBindMethodName()));
        bind.getArguments().add(nameRef);
        bind.getArguments().add(self);
        return bind;
    }

    @NotNull
    private JsExpression getThis(TranslationContext outerContext) {
        ClassDescriptor outerClassDescriptor = closureContext.outerClassDescriptor;
        if (outerClassDescriptor != null && descriptor.getReceiverParameter() == null) {
            return outerContext.getThisObject(outerClassDescriptor);
        }

        return JsLiteral.NULL;
    }

    @NotNull
    private String getBindMethodName() {
        if (closureContext.getDescriptors().isEmpty()) {
            return !hasArguments() ? "b3" : "b4";
        }
        else {
            return !hasArguments() ? (closureContext.getDescriptors().size() == 1 ? "b0" : "b1") : "b2";
        }
    }

    private boolean hasArguments() {
        return !getValueParameters().isEmpty() || descriptor.getReceiverParameter() != null;
    }

    @Override
    protected List<JsExpression> getCapturedValueParametersList(JsInvocation invocation) {
        if (closureContext.getDescriptors().size() > 1 || hasArguments()) {
            JsArrayLiteral values = new JsArrayLiteral();
            invocation.getArguments().add(values);
            return values.getExpressions();
        }

        return super.getCapturedValueParametersList(invocation);
    }
}
