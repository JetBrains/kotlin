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

import com.google.dart.compiler.backend.js.ast.HasArguments;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsInvocation;
import com.google.dart.compiler.backend.js.ast.JsNew;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.VariableAsFunctionResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.util.ExpressionAsFunctionDescriptor;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.intrinsic.functions.basic.FunctionIntrinsic;
import org.jetbrains.k2js.translate.utils.AnnotationsUtils;
import org.jetbrains.k2js.translate.utils.ErrorReportingUtils;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import java.util.List;

import static org.jetbrains.k2js.translate.reference.CallParametersResolver.resolveCallParameters;
import static org.jetbrains.k2js.translate.utils.BindingUtils.isObjectDeclaration;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.assignment;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.setQualifier;
import static org.jetbrains.k2js.translate.utils.JsDescriptorUtils.isConstructorDescriptor;

//TODO: write tests on calling backing fields as functions
public final class CallTranslator extends AbstractTranslator {
    @NotNull
    private final List<JsExpression> arguments;
    @NotNull
    private final ResolvedCall<?> resolvedCall;
    @NotNull
    private final CallableDescriptor descriptor;
    @NotNull
    private final CallType callType;
    @NotNull
    private final CallParameters callParameters;

    /*package*/ CallTranslator(@Nullable JsExpression receiver, @Nullable JsExpression callee,
            @NotNull List<JsExpression> arguments,
            @NotNull ResolvedCall<? extends CallableDescriptor> resolvedCall,
            @NotNull CallableDescriptor descriptorToCall,
            @NotNull CallType callType,
            @NotNull TranslationContext context) {
        super(context);
        this.arguments = arguments;
        this.resolvedCall = resolvedCall;
        this.callType = callType;
        this.descriptor = descriptorToCall;
        this.callParameters = resolveCallParameters(receiver, callee, descriptor, resolvedCall, context);
    }

    @NotNull
    public ResolvedCall<? extends CallableDescriptor> getResolvedCall() {
        return resolvedCall;
    }

    @NotNull
    public CallParameters getCallParameters() {
        return callParameters;
    }

    @NotNull
        /*package*/ JsExpression translate() {
        JsExpression result = intrinsicInvocation();
        if (result != null) {
            return result;
        }
        if (isConstructor()) {
            return createConstructorCallExpression(translateAsFunctionWithNoThisObject(descriptor));
        }
        if (resolvedCall.getReceiverArgument().exists()) {
            if (AnnotationsUtils.isNativeObject(descriptor)) {
                return nativeExtensionCall();
            }
            return extensionFunctionCall(!(descriptor instanceof ExpressionAsFunctionDescriptor));
        }
        if (isExpressionAsFunction()) {
            return expressionAsFunctionCall();
        }
        return methodCall(getThisObjectOrQualifier());
    }

    private boolean isExpressionAsFunction() {
        return descriptor instanceof ExpressionAsFunctionDescriptor ||
               resolvedCall instanceof VariableAsFunctionResolvedCall;
    }

    @NotNull
    private JsExpression expressionAsFunctionCall() {
        return methodCall(null);
    }

    @Nullable
    private JsExpression intrinsicInvocation() {
        if (descriptor instanceof FunctionDescriptor) {
            try {
                FunctionIntrinsic intrinsic = context().intrinsics().getFunctionIntrinsics().getIntrinsic((FunctionDescriptor) descriptor);
                if (intrinsic.exists()) {
                    return intrinsic.apply(this, arguments, context());
                }
            }
            catch (RuntimeException e) {
                throw ErrorReportingUtils.reportErrorWithLocation(e, descriptor, bindingContext());
            }
        }
        return null;
    }

    private boolean isConstructor() {
        return isConstructorDescriptor(descriptor);
    }

    @NotNull
    public HasArguments createConstructorCallExpression(@NotNull JsExpression constructorReference) {
        if (context().isEcma5() && !AnnotationsUtils.isNativeObject(resolvedCall.getCandidateDescriptor())) {
            return new JsInvocation(constructorReference, arguments);
        }
        else {
            return new JsNew(constructorReference, arguments);
        }
    }

    @NotNull
    private JsExpression translateAsFunctionWithNoThisObject(@NotNull DeclarationDescriptor descriptor) {
        return ReferenceTranslator.translateAsFQReference(descriptor, context());
    }

    @NotNull
    private JsExpression nativeExtensionCall() {
        return methodCall(callParameters.getReceiver());
    }

    @NotNull
    public JsExpression extensionFunctionCall(boolean useThis) {
        return callType.constructCall(callParameters.getReceiver(), new ExtensionCallConstructor(useThis), context());
    }

    @NotNull
    private List<JsExpression> generateCallArgumentList(@NotNull JsExpression receiver) {
        return TranslationUtils.generateInvocationArguments(receiver, arguments);
    }

    @NotNull
    private JsExpression methodCall(@Nullable JsExpression receiver) {
        return callType.constructCall(receiver, new CallType.CallConstructor() {
            @NotNull
            @Override
            public JsExpression construct(@Nullable JsExpression receiver) {
                JsExpression qualifiedCallee = getQualifiedCallee(receiver);
                if (isDirectPropertyAccess()) {
                    return directPropertyAccess(qualifiedCallee);
                }

                return new JsInvocation(qualifiedCallee, arguments);
            }
        }, context());
    }

    @NotNull
    private JsExpression directPropertyAccess(@NotNull JsExpression callee) {
        if (descriptor instanceof PropertyGetterDescriptor) {
            assert arguments.isEmpty();
            return callee;
        }
        else {
            assert descriptor instanceof PropertySetterDescriptor;
            assert arguments.size() == 1;
            return assignment(callee, arguments.get(0));
        }
    }

    private boolean isDirectPropertyAccess() {
        return descriptor instanceof PropertyAccessorDescriptor &&
               (context().isEcma5() || isObjectAccessor((PropertyAccessorDescriptor) descriptor));
    }

    private boolean isObjectAccessor(@NotNull PropertyAccessorDescriptor propertyAccessorDescriptor) {
        PropertyDescriptor correspondingProperty = propertyAccessorDescriptor.getCorrespondingProperty();
        return isObjectDeclaration(bindingContext(), correspondingProperty);
    }

    @NotNull
    private JsExpression getQualifiedCallee(@Nullable JsExpression receiver) {
        JsExpression callee = callParameters.getFunctionReference();
        if (receiver != null) {
            setQualifier(callee, receiver);
        }
        return callee;
    }

    @Nullable
    private JsExpression getThisObjectOrQualifier() {
        JsExpression thisObject = callParameters.getThisObject();
        if (thisObject != null) {
            return thisObject;
        }
        return context().getQualifierForDescriptor(descriptor);
    }

    private class ExtensionCallConstructor implements CallType.CallConstructor {
        private final boolean useThis;

        private ExtensionCallConstructor(boolean useThis) {
            this.useThis = useThis;
        }

        @NotNull
        @Override
        public JsExpression construct(@Nullable JsExpression receiver) {
            assert receiver != null : "Could not be null for extensions";
            JsExpression functionReference = callParameters.getFunctionReference();
            if (useThis) {
                setQualifier(functionReference, getThisObjectOrQualifier());
            }
            return new JsInvocation(callParameters.getFunctionReference(), generateCallArgumentList(receiver));
        }
    }
}
