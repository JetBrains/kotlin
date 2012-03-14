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

package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsInvocation;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.backend.js.ast.JsNew;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.calls.ExpressionAsFunctionDescriptor;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.intrinsic.Intrinsic;
import org.jetbrains.k2js.translate.utils.AnnotationsUtils;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.k2js.translate.utils.DescriptorUtils.*;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.*;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.getThisObject;

/**
 * @author Pavel Talanov
 */
//TODO: write tests on calling backing fields as functions
//TODO: think of the refactoring the class
public final class CallTranslator extends AbstractTranslator {

    private static class CallParameters {

        public CallParameters(@Nullable JsExpression receiver, @NotNull JsExpression functionReference) {
            this.receiver = receiver;
            this.functionReference = functionReference;
        }

        @Nullable
        public final JsExpression receiver;
        @NotNull
        public final JsExpression functionReference;
    }

    //NOTE: receiver may mean this object as well
    @Nullable
    private /*var*/ JsExpression receiver;

    @Nullable
    private final JsExpression callee;

    @NotNull
    private final List<JsExpression> arguments;

    @NotNull
    private final ResolvedCall<?> resolvedCall;

    @NotNull
    private final CallableDescriptor descriptor;

    @NotNull
    private final CallType callType;

    /*package*/ CallTranslator(@Nullable JsExpression receiver, @Nullable JsExpression callee,
                               @NotNull List<JsExpression> arguments,
                               @NotNull ResolvedCall<? extends CallableDescriptor> resolvedCall,
                               @NotNull CallableDescriptor descriptorToCall,
                               @NotNull CallType callType,
                               @NotNull TranslationContext context) {
        super(context);
        this.receiver = receiver;
        this.arguments = arguments;
        this.resolvedCall = resolvedCall;
        this.callType = callType;
        this.descriptor = descriptorToCall;
        this.callee = callee;
    }

    @NotNull
        /*package*/ JsExpression translate() {
        if (isIntrinsic()) {
            return intrinsicInvocation();
        }
        if (isConstructor()) {
            return constructorCall();
        }
        if (isNativeExtensionFunctionCall()) {
            return nativeExtensionCall();
        }
        if (isExtensionFunctionLiteral()) {
            return extensionFunctionLiteralCall();
        }
        if (isExtensionFunction()) {
            return extensionFunctionCall();
        }
        if (isExpressionAsFunction()) {
            return expressionAsFunctionCall();
        }
        return methodCall(callParameters(resolveThisObject(/*just get qualifier if null*/ true)));
    }

    private boolean isExpressionAsFunction() {
        return callee != null;
    }

    private JsExpression expressionAsFunctionCall() {
        assert callee != null;
        CallParameters expressionAsFunctionParameters = new CallParameters(null, callee);
        return methodCall(expressionAsFunctionParameters);
    }

    private boolean isIntrinsic() {
        return context().intrinsics().isIntrinsic(descriptor);
    }

    @NotNull
    private JsExpression intrinsicInvocation() {
        assert descriptor instanceof FunctionDescriptor;
        Intrinsic intrinsic =
                context().intrinsics().getFunctionIntrinsic((FunctionDescriptor) descriptor);
        JsExpression receiverExpression = resolveThisObject(/*do not get qualifier*/false);
        return intrinsic.apply(receiverExpression, arguments, context());
    }

    private boolean isConstructor() {
        return isConstructorDescriptor(descriptor);
    }

    @NotNull
    private JsExpression constructorCall() {
        JsExpression constructorReference = translateAsFunctionWithNoThisObject(descriptor);
        JsNew constructorCall = new JsNew(constructorReference);
        setArguments(constructorCall, arguments);
        return constructorCall;
    }

    @NotNull
    private JsExpression translateAsFunctionWithNoThisObject(@NotNull DeclarationDescriptor descriptor) {
        return ReferenceTranslator.translateAsFQReference(descriptor, context());
    }

    private boolean isNativeExtensionFunctionCall() {
        return AnnotationsUtils.isNativeObject(descriptor) && isExtensionFunction();
    }

    @NotNull
    private JsExpression nativeExtensionCall() {
        receiver = getExtensionFunctionCallReceiver();
        return methodCall(callParameters(resolveThisObject(/*just get qualifier if null = */ true)));
    }

    private boolean isExtensionFunctionLiteral() {
        boolean isLiteral = descriptor instanceof VariableAsFunctionDescriptor
                            || descriptor instanceof ExpressionAsFunctionDescriptor;
        return isExtensionFunction() && isLiteral;
    }

    @NotNull
    private JsExpression extensionFunctionLiteralCall() {
        JsExpression realReceiver = getExtensionFunctionCallReceiver();
        return callType.constructCall(realReceiver, new CallType.CallConstructor() {
            @NotNull
            @Override
            public JsExpression construct(@Nullable JsExpression receiver) {
                assert receiver != null : "Could not be null for extensions";
                return constructExtensionLiteralCall(receiver);
            }
        }, context());
    }

    @NotNull
    private JsExpression constructExtensionLiteralCall(@NotNull JsExpression realReceiver) {
        List<JsExpression> callArguments = generateExtensionCallArgumentList(realReceiver);
        JsInvocation callMethodInvocation = generateCallMethodInvocation();
        setArguments(callMethodInvocation, callArguments);
        return callMethodInvocation;
    }

    @NotNull
    private JsInvocation generateCallMethodInvocation() {
        JsNameRef callMethodNameRef = AstUtil.newQualifiedNameRef("call");
        JsInvocation callMethodInvocation = new JsInvocation();
        callMethodInvocation.setQualifier(callMethodNameRef);
        setQualifier(callMethodInvocation, callParameters(resolveThisObject(/*just get qualifier if null*/ true)).functionReference);
        return callMethodInvocation;
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    private boolean isExtensionFunction() {
        boolean hasReceiver = resolvedCall.getReceiverArgument().exists();
        return hasReceiver;
    }

    @NotNull
    private JsExpression extensionFunctionCall() {
        JsExpression realReceiver = getExtensionFunctionCallReceiver();
        return callType.constructCall(realReceiver, new CallType.CallConstructor() {
            @NotNull
            @Override
            public JsExpression construct(@Nullable JsExpression receiver) {
                assert receiver != null : "Could not be null for extensions";
                return constructExtensionFunctionCall(receiver);
            }
        }, context());
    }

    @NotNull
    private JsExpression getExtensionFunctionCallReceiver() {
        if (receiver != null) {
            JsExpression result = receiver;
            //Now the rest of the code can work as if it was simple method invocation
            receiver = null;
            return result;
        }
        DeclarationDescriptor expectedReceiverDescriptor = getExpectedReceiverDescriptor(descriptor);
        assert expectedReceiverDescriptor != null;
        return getThisObject(context(), expectedReceiverDescriptor);
    }

    @NotNull
    private JsExpression constructExtensionFunctionCall(@NotNull JsExpression receiver) {
        List<JsExpression> argumentList = generateExtensionCallArgumentList(receiver);
        CallParameters callParameters = callParameters(resolveThisObject(/*just get qualifier if null*/ true));
        JsExpression functionReference = callParameters.functionReference;
        setQualifier(functionReference, callParameters.receiver);
        return newInvocation(functionReference, argumentList);
    }

    @NotNull
    private List<JsExpression> generateExtensionCallArgumentList(@NotNull JsExpression receiver) {
        List<JsExpression> argumentList = new ArrayList<JsExpression>();
        assert this.receiver == null : "Should be null at that point";
        argumentList.add(receiver);
        argumentList.addAll(arguments);
        return argumentList;
    }

    @NotNull
    private JsExpression methodCall(@NotNull final CallParameters callParameters) {
        return callType.constructCall(callParameters.receiver, new CallType.CallConstructor() {
            @NotNull
            @Override
            public JsExpression construct(@Nullable JsExpression receiver) {
                JsExpression functionReference = callParameters.functionReference;
                if (receiver != null) {
                    setQualifier(functionReference, receiver);
                }
                return newInvocation(functionReference, arguments);
            }
        }, context());
    }

    @NotNull
    private CallParameters callParameters(@Nullable JsExpression receiver) {
        JsExpression functionReference = functionReference();
        return new CallParameters(receiver, functionReference);
    }

    //TODO: inspect
    @NotNull
    private JsExpression functionReference() {
        if (!isVariableAsFunction(descriptor)) {
            return ReferenceTranslator.translateAsLocalNameReference(descriptor, context());
        }
        VariableDescriptor variableDescriptor =
                getVariableDescriptorForVariableAsFunction((VariableAsFunctionDescriptor) descriptor);
        if (variableDescriptor instanceof PropertyDescriptor) {
            return getterCall((PropertyDescriptor) variableDescriptor);
        }
        return ReferenceTranslator.translateAsLocalNameReference(variableDescriptor, context());
    }

    @NotNull
    private JsExpression getterCall(@NotNull PropertyDescriptor variableDescriptor) {
        //TODO: call type?
        return PropertyAccessTranslator.translateAsPropertyGetterCall(variableDescriptor, resolvedCall, context());
    }

    //TODO: refactor
    @Nullable
    private JsExpression resolveThisObject(boolean getQualifierIfNull) {
        if (receiver != null) {
            return receiver;
        }
        ReceiverDescriptor thisObject = resolvedCall.getThisObject();
        if (thisObject.exists()) {
            DeclarationDescriptor expectedThisDescriptor = getDeclarationDescriptorForReceiver(thisObject);
            return TranslationUtils.getThisObject(context(), expectedThisDescriptor);
        }
        if (getQualifierIfNull) {
            return context().getQualifierForDescriptor(descriptor);
        }
        return null;
    }
}
