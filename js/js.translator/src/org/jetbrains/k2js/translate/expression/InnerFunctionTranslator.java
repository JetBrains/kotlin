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
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.k2js.translate.context.TranslationContext;

import java.util.List;

class InnerFunctionTranslator extends InnerDeclarationTranslator {
    private final FunctionDescriptor descriptor;

    public InnerFunctionTranslator(@NotNull JetElement declaration,
            @NotNull FunctionDescriptor descriptor,
            @NotNull TranslationContext context,
            @NotNull JsFunction fun) {
        super(declaration, descriptor, context, fun);
        this.descriptor = descriptor;
    }

    public boolean isLocalVariablesAffected() {
        return closureContext.isLocalVariablesAffected();
    }

    @Override
    protected List<ValueParameterDescriptor> getValueParameters() {
        return descriptor.getValueParameters();
    }

    @Override
    @NotNull
    public JsExpression translate(@NotNull JsNameRef nameRef) {
        JsExpression result = translate(nameRef, getThis());
        FunctionTranslator.addParameters(fun.getParameters(), descriptor, context);
        return result;
    }

    @Override
    protected JsExpression createExpression(JsNameRef nameRef, JsExpression self) {
        return nameRef;
    }

    @Override
    protected JsInvocation createInvocation(JsNameRef nameRef, JsExpression self) {
        JsInvocation bind = new JsInvocation(context.namer().kotlin(getBindMethodName()));
        bind.getArguments().add(nameRef);
        bind.getArguments().add(self);
        return bind;
    }

    @NotNull
    private JsExpression getThis() {
        ClassDescriptor outerClassDescriptor = closureContext.outerClassDescriptor;
        if (outerClassDescriptor != null && descriptor.getReceiverParameter() == null) {
            return JsLiteral.THIS;
        }

        return JsLiteral.NULL;
    }

    @NotNull
    private String getBindMethodName() {
        if (closureContext.getDescriptors().isEmpty()) {
            return getValueParameters().isEmpty() ? "b3" : "b4";
        }
        else {
            return getValueParameters().isEmpty() ? (closureContext.getDescriptors().size() == 1 ? "b0" : "b1") : "b2";
        }
    }

    @Override
    protected List<JsExpression> getCapturedValueParametersList(JsInvocation invocation) {
        if (closureContext.getDescriptors().size() > 1 || !getValueParameters().isEmpty()) {
            JsArrayLiteral values = new JsArrayLiteral();
            invocation.getArguments().add(values);
            return values.getExpressions();
        }

        return super.getCapturedValueParametersList(invocation);
    }
}
