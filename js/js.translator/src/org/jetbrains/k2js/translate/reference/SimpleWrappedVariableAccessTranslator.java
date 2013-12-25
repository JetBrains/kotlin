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

package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.utils.BindingUtils;

import java.util.Collections;
import java.util.List;

public class SimpleWrappedVariableAccessTranslator implements CachedAccessTranslator {
    private final TranslationContext context;
    private final ResolvedCall<? extends VariableDescriptor> resolvedCall;
    private final JsExpression receiver;

    public SimpleWrappedVariableAccessTranslator(
            @NotNull TranslationContext context,
            @NotNull JetReferenceExpression referenceExpression,
            JsExpression receiver
    ) {
        this.context = context;
        this.receiver = receiver;
        ResolvedCall<?> resolvedCall = BindingUtils.getResolvedCallForProperty(context.bindingContext(), referenceExpression);
        assert resolvedCall.getResultingDescriptor() instanceof VariableDescriptor;
        this.resolvedCall = (ResolvedCall<? extends VariableDescriptor>) resolvedCall;
    }

    @NotNull
    @Override
    public JsExpression translateAsGet() {
        return ReferencePackage.buildGet(context, resolvedCall, receiver);
    }

    @NotNull
    @Override
    public JsExpression translateAsSet(@NotNull JsExpression setTo) {
        return ReferencePackage.buildSet(context, resolvedCall, setTo, receiver);
    }

    @NotNull
    @Override
    public CachedAccessTranslator getCached() {
        return this;
    }

    @NotNull
    @Override
    public List<TemporaryVariable> declaredTemporaries() {       // TODO : fix this
        return Collections.emptyList();
    }
}
