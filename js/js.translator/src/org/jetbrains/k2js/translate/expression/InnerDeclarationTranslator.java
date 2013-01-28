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
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.utils.closure.ClosureContext;
import org.jetbrains.k2js.translate.utils.closure.ClosureUtils;

import java.util.Collections;
import java.util.List;

abstract class InnerDeclarationTranslator {
    protected final ClosureContext closureContext;
    protected final TranslationContext context;
    protected final JsFunction fun;

    public InnerDeclarationTranslator(@NotNull JetElement declaration,
            @NotNull DeclarationDescriptor descriptor,
            @NotNull TranslationContext context,
            @NotNull JsFunction fun) {
        this.context = context;
        closureContext = ClosureUtils.captureClosure(context.bindingContext(), declaration, descriptor);
        this.fun = fun;
    }

    protected List<ValueParameterDescriptor> getValueParameters() {
        return Collections.emptyList();
    }

    @NotNull
    public abstract JsExpression translate(@NotNull JsNameRef nameRef);

    protected JsExpression translate(@NotNull JsNameRef nameRef, @Nullable JsExpression self) {
        if (closureContext.getDescriptors().isEmpty() && self == JsLiteral.NULL) {
            return createExpression(nameRef, self);
        }
        else {
            JsInvocation invocation = createInvocation(nameRef, self);
            addCapturedValueParameters(invocation);
            return invocation;
        }
    }

    protected abstract JsExpression createExpression(JsNameRef nameRef, JsExpression self);

    protected abstract JsInvocation createInvocation(JsNameRef nameRef, JsExpression self);

    private void addCapturedValueParameters(JsInvocation bind) {
        if (closureContext.getDescriptors().isEmpty()) {
            return;
        }

        List<JsExpression> expressions = getCapturedValueParametersList(bind);
        for (VariableDescriptor variableDescriptor : closureContext.getDescriptors()) {
            JsName name = context.getNameForDescriptor(variableDescriptor);
            fun.getParameters().add(new JsParameter(name));
            expressions.add(name.makeRef());
        }
    }

    protected List<JsExpression> getCapturedValueParametersList(JsInvocation invocation) {
        return invocation.getArguments();
    }
}
