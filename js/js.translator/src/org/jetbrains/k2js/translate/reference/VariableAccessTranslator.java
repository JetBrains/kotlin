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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.resolve.bindingContextUtil.BindingContextUtilPackage;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.VariableAsFunctionResolvedCall;
import org.jetbrains.k2js.translate.callTranslator.CallTranslator;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;

import java.util.Collections;
import java.util.List;

public class VariableAccessTranslator extends AbstractTranslator implements AccessTranslator {
    public static VariableAccessTranslator newInstance(
            @NotNull TranslationContext context,
            @NotNull JetReferenceExpression referenceExpression,
            @Nullable JsExpression receiver
    ) {
        ResolvedCall<?> resolvedCall = BindingContextUtilPackage.getResolvedCallWithAssert(referenceExpression, context.bindingContext());
        if (resolvedCall instanceof VariableAsFunctionResolvedCall) {
            resolvedCall = ((VariableAsFunctionResolvedCall) resolvedCall).getVariableCall();
        }
        assert resolvedCall.getResultingDescriptor() instanceof VariableDescriptor;
        return new VariableAccessTranslator(context, (ResolvedCall<? extends VariableDescriptor>) resolvedCall, receiver);
    }


    private final ResolvedCall<? extends VariableDescriptor> resolvedCall;
    private final JsExpression receiver;

    private VariableAccessTranslator(
            @NotNull TranslationContext context,
            @NotNull ResolvedCall<? extends VariableDescriptor> resolvedCall,
            @Nullable JsExpression receiver
    ) {
        super(context);
        this.receiver = receiver;
        this.resolvedCall = resolvedCall;
    }

    @NotNull
    @Override
    public JsExpression translateAsGet() {
        return CallTranslator.instance$.translateGet(context(), resolvedCall, receiver);
    }

    @NotNull
    @Override
    public JsExpression translateAsSet(@NotNull JsExpression setTo) {
        return CallTranslator.instance$.translateSet(context(), resolvedCall, setTo, receiver);
    }

    @NotNull
    @Override
    public CachedAccessTranslator getCached() {
        TemporaryVariable temporaryVariable = receiver == null ? null : context().declareTemporary(receiver);
        return new CachedVariableAccessTranslator(context(), resolvedCall, temporaryVariable);
    }

    private static class CachedVariableAccessTranslator extends VariableAccessTranslator implements CachedAccessTranslator {
        @Nullable
        private final TemporaryVariable cachedReceiver;

        public CachedVariableAccessTranslator(
                @NotNull TranslationContext context,
                @NotNull  ResolvedCall<? extends VariableDescriptor> resolvedCall,
                @Nullable TemporaryVariable cachedReceiver
        ) {
            super(context, resolvedCall, cachedReceiver == null ? null : cachedReceiver.reference());
            this.cachedReceiver = cachedReceiver;
        }

        @NotNull
        @Override
        public List<TemporaryVariable> declaredTemporaries() {
            return cachedReceiver == null ? Collections.<TemporaryVariable>emptyList() : Collections.singletonList(cachedReceiver);
        }

        @NotNull
        @Override
        public CachedAccessTranslator getCached() {
            return this;
        }
    }

}
