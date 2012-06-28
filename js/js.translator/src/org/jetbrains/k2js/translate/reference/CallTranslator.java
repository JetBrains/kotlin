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

import com.google.common.collect.Lists;
import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.calls.ExpressionAsFunctionDescriptor;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.VariableAsFunctionResolvedCall;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.intrinsic.Intrinsic;
import org.jetbrains.k2js.translate.utils.AnnotationsUtils;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.k2js.translate.reference.CallParametersResolver.resolveCallParameters;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.*;
import static org.jetbrains.k2js.translate.utils.JsDescriptorUtils.isConstructorDescriptor;

/**
 * @author Pavel Talanov
 */
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
        if (isInvoke()) {
            return invokeCall();
        }
        return methodCall(getThisObjectOrQualifier());
    }

    //TODO:
    private boolean isInvoke() {
        return descriptor.getName().getName().equals("invoke");
    }

    @NotNull
    private JsExpression invokeCall() {
        JsInvocation callMethodInvocation = generateCallMethodInvocation();
        List<JsExpression> parameters = Lists.<JsExpression>newArrayList(context().program().getNullLiteral());
        parameters.addAll(arguments);
        setArguments(callMethodInvocation, parameters);
        return callMethodInvocation;
    }

    private boolean isExpressionAsFunction() {
        return descriptor instanceof ExpressionAsFunctionDescriptor ||
               resolvedCall instanceof VariableAsFunctionResolvedCall;
    }

    @NotNull
    private JsExpression expressionAsFunctionCall() {
        return methodCall(null);
    }

    private boolean isIntrinsic() {
        return context().intrinsics().isIntrinsic(descriptor);
    }

    @NotNull
    private JsExpression intrinsicInvocation() {
        assert descriptor instanceof FunctionDescriptor;
        try {
            Intrinsic intrinsic = context().intrinsics().getFunctionIntrinsic((FunctionDescriptor) descriptor);
            return intrinsic.apply(callParameters.getThisOrReceiverOrNull(), arguments, context());
        } catch (RuntimeException e) {
            PsiElement element = BindingContextUtils.descriptorToDeclaration(bindingContext(), descriptor);
            if (element == null) {
                element = BindingContextUtils.descriptorToDeclaration(bindingContext(), descriptor.getOriginal());
            }
            if (element == null && descriptor instanceof ASTNode) {
                element = DiagnosticUtils.getClosestPsiElement((ASTNode) descriptor);
            }
            if (element != null) {
                String location = DiagnosticUtils.atLocation(element);
                throw new IllegalStateException(e.getMessage() + " at " + location, e);
            } else {
                throw e;
            }
        }
    }

    private boolean isConstructor() {
        return isConstructorDescriptor(descriptor);
    }

    @NotNull
    private JsExpression constructorCall() {
        JsExpression constructorReference = translateAsFunctionWithNoThisObject(descriptor);
        JsExpression constructorCall = createConstructorCallExpression(constructorReference);
        assert constructorCall instanceof HasArguments : "Constructor call should be expression with arguments.";
        ((HasArguments) constructorCall).getArguments().addAll(arguments);
        return constructorCall;
    }

    @NotNull
    private JsExpression createConstructorCallExpression(@NotNull JsExpression constructorReference) {
        if (context().isEcma5() && !AnnotationsUtils.isNativeObject(resolvedCall.getCandidateDescriptor())) {
            return AstUtil.newInvocation(constructorReference);
        }
        else {
            return new JsNew(constructorReference);
        }
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
        return methodCall(callParameters.getReceiver());
    }

    private boolean isExtensionFunctionLiteral() {
        boolean isLiteral = isInvoke()
                            || descriptor instanceof ExpressionAsFunctionDescriptor;
        return isExtensionFunction() && isLiteral;
    }

    @NotNull
    private JsExpression extensionFunctionLiteralCall() {
        return callType.constructCall(callParameters.getReceiver(), new CallType.CallConstructor() {
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
        JsNameRef callMethodNameRef = new JsNameRef("call");
        JsInvocation callMethodInvocation = new JsInvocation();
        callMethodInvocation.setQualifier(callMethodNameRef);
        setQualifier(callMethodInvocation, callParameters.getFunctionReference());
        return callMethodInvocation;
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    private boolean isExtensionFunction() {
        boolean hasReceiver = resolvedCall.getReceiverArgument().exists();
        return hasReceiver;
    }

    @NotNull
    private JsExpression extensionFunctionCall() {
        return callType.constructCall(callParameters.getReceiver(), new CallType.CallConstructor() {
            @NotNull
            @Override
            public JsExpression construct(@Nullable JsExpression receiver) {
                assert receiver != null : "Could not be null for extensions";
                return constructExtensionFunctionCall(receiver);
            }
        }, context());
    }

    @NotNull
    private JsExpression constructExtensionFunctionCall(@NotNull JsExpression receiver) {
        List<JsExpression> argumentList = generateExtensionCallArgumentList(receiver);
        JsExpression functionReference = callParameters.getFunctionReference();
        setQualifier(functionReference, getThisObjectOrQualifier());
        return newInvocation(functionReference, argumentList);
    }

    @NotNull
    private List<JsExpression> generateExtensionCallArgumentList(@NotNull JsExpression receiver) {
        List<JsExpression> argumentList = new ArrayList<JsExpression>();
        argumentList.add(receiver);
        argumentList.addAll(arguments);
        return argumentList;
    }

    @NotNull
    private JsExpression methodCall(@Nullable JsExpression receiver) {
        return callType.constructCall(receiver, new CallType.CallConstructor() {
            @NotNull
            @Override
            public JsExpression construct(@Nullable JsExpression receiver) {
                JsExpression qualifiedCallee = getQualifiedCallee(receiver);

                if (isEcma5PropertyAccess()) {
                    return ecma5PropertyAccess(qualifiedCallee);
                }

                return newInvocation(qualifiedCallee, arguments);
            }
        }, context());
    }

    @NotNull
    private JsExpression ecma5PropertyAccess(@NotNull JsExpression callee) {
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

    private boolean isEcma5PropertyAccess() {
        return context().isEcma5() && descriptor instanceof PropertyAccessorDescriptor;
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
}
