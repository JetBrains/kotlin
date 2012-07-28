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

package org.jetbrains.k2js.translate.operation;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetUnaryExpression;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.reference.CallBuilder;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getFunctionDescriptorForOperationExpression;

/**
 * @author Pavel Talanov
 */
public final class OverloadedIncrementTranslator extends IncrementTranslator {

    @NotNull
    private final FunctionDescriptor operationDescriptor;

    /*package*/ OverloadedIncrementTranslator(@NotNull JetUnaryExpression expression,
                                              @NotNull TranslationContext context) {
        super(expression, context);
        FunctionDescriptor functionDescriptor = getFunctionDescriptorForOperationExpression(context.bindingContext(), expression);
        assert functionDescriptor != null : "Descriptor should not be null for overloaded increment expression.";
        this.operationDescriptor = functionDescriptor;
    }


    @Override
    @NotNull
    protected JsExpression operationExpression(@NotNull JsExpression receiver) {
        return CallBuilder.build(context())
                .receiver(receiver)
                .descriptor(operationDescriptor)
                .translate();
    }

}
