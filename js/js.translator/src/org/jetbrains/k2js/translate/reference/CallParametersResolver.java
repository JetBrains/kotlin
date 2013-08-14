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
import com.google.dart.compiler.backend.js.ast.JsLiteral;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallWithTrace;
import org.jetbrains.jet.lang.resolve.calls.model.VariableAsFunctionResolvedCall;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ClassReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExtensionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ThisReceiver;
import org.jetbrains.k2js.translate.context.TranslationContext;

import static org.jetbrains.k2js.translate.utils.JsDescriptorUtils.getDeclarationDescriptorForReceiver;

public final class CallParametersResolver {
    public static CallParameters resolveCallParameters(@Nullable JsExpression qualifier,
            @Nullable JsExpression callee,
            @NotNull CallableDescriptor descriptor,
            @NotNull ResolvedCall<? extends CallableDescriptor> call,
            @NotNull TranslationContext context) {
        return (new CallParametersResolver(qualifier, callee, descriptor, call, context)).resolve();
    }

    // the actual qualifier for the call at the call site
    @Nullable
    private final JsExpression qualifier;
    @Nullable
    private final JsExpression callee;
    @NotNull
    private final CallableDescriptor descriptor;
    @NotNull
    private final TranslationContext context;
    @NotNull
    private final ResolvedCall<? extends CallableDescriptor> resolvedCall;
    private final boolean isExtensionCall;

    private CallParametersResolver(@Nullable JsExpression qualifier,
            @Nullable JsExpression callee,
            @NotNull CallableDescriptor descriptor,
            @NotNull ResolvedCall<? extends CallableDescriptor> call,
            @NotNull TranslationContext context) {
        this.qualifier = qualifier;
        this.callee = callee;
        this.descriptor = descriptor;
        this.context = context;
        this.resolvedCall = call;
        this.isExtensionCall = resolvedCall.getReceiverArgument().exists();
    }

    @NotNull
    private CallParameters resolve() {
        JsExpression receiver = isExtensionCall ? getExtensionFunctionCallReceiver() : null;
        JsExpression functionReference = getFunctionReference();
        JsExpression thisObject = getThisObject();
        return new CallParameters(receiver, functionReference, thisObject);
    }

    @NotNull
    private JsExpression getFunctionReference() {
        if (callee != null) {
            return callee;
        }
        if (!(resolvedCall instanceof VariableAsFunctionResolvedCall)) {
            return ReferenceTranslator.translateAsLocalNameReference(descriptor, context);
        }
        ResolvedCallWithTrace<FunctionDescriptor> call = ((VariableAsFunctionResolvedCall) resolvedCall).getFunctionCall();
        return CallBuilder.build(context).resolvedCall(call).translate();
    }

    @Nullable
    private JsExpression getThisObject() {
        if (qualifier != null && !isExtensionCall) {
            return qualifier;
        }

        ReceiverValue thisObject = resolvedCall.getThisObject();
        if (!thisObject.exists()) {
            return null;
        }

        if (thisObject instanceof ClassReceiver) {
            JsExpression ref = context.getAliasForDescriptor(((ClassReceiver) thisObject).getDeclarationDescriptor());
            return ref == null ? JsLiteral.THIS : ref;
        }
        else if (thisObject instanceof ExtensionReceiver) {
            return context.getAliasForDescriptor(getDeclarationDescriptorForReceiver(thisObject));
        }

        return resolvedCall.getReceiverArgument().exists() && resolvedCall.getExplicitReceiverKind().isThisObject() ? JsLiteral.THIS : null;
    }

    @NotNull
    private JsExpression getExtensionFunctionCallReceiver() {
        if (qualifier != null) {
            return qualifier;
        }
        return context.getThisObject(((ThisReceiver) resolvedCall.getReceiverArgument()).getDeclarationDescriptor());
    }
}
