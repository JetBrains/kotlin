/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.translate.reference;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.js.backend.ast.JsBinaryOperation;
import org.jetbrains.kotlin.js.backend.ast.JsExpression;
import org.jetbrains.kotlin.js.backend.ast.JsNameRef;
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.psi.KtReferenceExpression;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall;
import org.jetbrains.kotlin.resolve.inline.InlineUtil;

import static org.jetbrains.kotlin.js.translate.utils.InlineUtils.setInlineCallMetadata;

public class VariableAccessTranslator extends AbstractTranslator implements AccessTranslator {
    public static VariableAccessTranslator newInstance(
            @NotNull TranslationContext context,
            @NotNull KtReferenceExpression referenceExpression,
            @Nullable JsExpression receiver
    ) {
        ResolvedCall<? extends VariableDescriptor> resolvedCall =
                CallUtilKt.getVariableResolvedCallWithAssert(referenceExpression, context.bindingContext());
        if (resolvedCall instanceof VariableAsFunctionResolvedCall) {
            resolvedCall = ((VariableAsFunctionResolvedCall) resolvedCall).getVariableCall();
        }
        return new VariableAccessTranslator(context, referenceExpression, resolvedCall, receiver);
    }


    private final ResolvedCall<? extends VariableDescriptor> resolvedCall;
    private final KtReferenceExpression referenceExpression;
    private final JsExpression receiver;

    private VariableAccessTranslator(
            @NotNull TranslationContext context,
            @NotNull KtReferenceExpression referenceExpression,
            @NotNull ResolvedCall<? extends VariableDescriptor> resolvedCall,
            @Nullable JsExpression receiver
    ) {
        super(context);
        this.referenceExpression = referenceExpression;
        this.receiver = receiver;
        this.resolvedCall = resolvedCall;
    }

    @NotNull
    @Override
    public JsExpression translateAsGet() {
        JsExpression e = CallTranslator.INSTANCE.translateGet(context(), resolvedCall, receiver);
        CallableDescriptor original = resolvedCall.getResultingDescriptor().getOriginal();
        if (original instanceof PropertyDescriptor) {
            PropertyGetterDescriptor getter = ((PropertyDescriptor) original).getGetter();
            if (InlineUtil.isInline(getter)) {
                if (e instanceof JsNameRef) {
                    // Get was translated as a name reference
                    setInlineCallMetadata((JsNameRef) e, referenceExpression, getter, context());
                } else {
                    setInlineCallMetadata(e, referenceExpression, getter, context());
                }
            }
        }
        return e;
    }

    @NotNull
    @Override
    public JsExpression translateAsSet(@NotNull JsExpression setTo) {
        JsExpression e = CallTranslator.INSTANCE.translateSet(context(), resolvedCall, setTo, receiver);
        CallableDescriptor original = resolvedCall.getResultingDescriptor().getOriginal();
        if (original instanceof PropertyDescriptor) {
            PropertySetterDescriptor setter = ((PropertyDescriptor)original).getSetter();
            if (InlineUtil.isInline(setter)) {
                if (e instanceof JsBinaryOperation && ((JsBinaryOperation) e).getOperator().isAssignment()) {
                    // Set was translated as an assignment
                    setInlineCallMetadata((JsNameRef) (((JsBinaryOperation) e).getArg1()), referenceExpression, setter, context());
                } else {
                    setInlineCallMetadata(e, referenceExpression, setter, context());
                }
            }
        }
        return e;
    }

    @NotNull
    @Override
    public AccessTranslator getCached() {
        JsExpression cachedReceiver = receiver != null ? context().cacheExpressionIfNeeded(receiver) : null;
        return new CachedVariableAccessTranslator(context(), referenceExpression, resolvedCall, cachedReceiver);
    }

    private static class CachedVariableAccessTranslator extends VariableAccessTranslator implements AccessTranslator {
        public CachedVariableAccessTranslator(
                @NotNull TranslationContext context,
                @NotNull KtReferenceExpression referenceExpression,
                @NotNull  ResolvedCall<? extends VariableDescriptor> resolvedCall,
                @Nullable JsExpression cachedReceiver
        ) {
            super(context, referenceExpression, resolvedCall, cachedReceiver);
        }

        @NotNull
        @Override
        public AccessTranslator getCached() {
            return this;
        }
    }
}
