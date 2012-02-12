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
import org.jetbrains.k2js.translate.intrinsic.FunctionIntrinsic;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.k2js.translate.utils.DescriptorUtils.*;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.getThisObject;

/**
 * @author Pavel Talanov
 */
//TODO: write tests on calling backing fields as functions
public final class CallTranslator extends AbstractTranslator {

    private static class CallParameters {

        public CallParameters(@Nullable JsExpression receiver, @NotNull JsExpression functionReference) {
            this.receiver = receiver;
            this.functionReference = functionReference;
        }

        @Nullable
        public /*var*/ JsExpression receiver;
        @NotNull
        public /*var*/ JsExpression functionReference;
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
        if (isExtensionFunctionLiteral()) {
            return extensionFunctionLiteralCall();
        }
        if (isExtensionFunction()) {
            return extensionFunctionCall();
        }
        return methodCall();
    }


    private boolean isIntrinsic() {
        return context().intrinsics().isIntrinsic(descriptor);
    }

    @NotNull
    private JsExpression intrinsicInvocation() {
        assert descriptor instanceof FunctionDescriptor;
        FunctionIntrinsic functionIntrinsic =
                context().intrinsics().getFunctionIntrinsic((FunctionDescriptor) descriptor);
        JsExpression receiverExpression = resolveThisObject(/*do not get qualifier*/false);
        return functionIntrinsic.apply(receiverExpression, arguments, context());
    }

    private boolean isConstructor() {
        return isConstructorDescriptor(descriptor);
    }

    @NotNull
    private JsExpression constructorCall() {
        JsExpression constructorReference = translateAsFunctionWithNoThisObject(descriptor);
        JsNew constructorCall = new JsNew(constructorReference);
        constructorCall.setArguments(arguments);
        return constructorCall;
    }

    @NotNull
    private JsExpression translateAsFunctionWithNoThisObject(@NotNull DeclarationDescriptor descriptor) {
        return ReferenceTranslator.translateAsFQReference(descriptor, context());
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
        callMethodInvocation.setArguments(callArguments);
        return callMethodInvocation;
    }

    @NotNull
    private JsInvocation generateCallMethodInvocation() {
        JsNameRef callMethodNameRef = AstUtil.newQualifiedNameRef("call");
        JsInvocation callMethodInvocation = new JsInvocation();
        callMethodInvocation.setQualifier(callMethodNameRef);
        AstUtil.setQualifier(callMethodInvocation, callParameters().functionReference);
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
        JsExpression functionReference = callParameters().functionReference;
        AstUtil.setQualifier(functionReference, callParameters().receiver);
        return AstUtil.newInvocation(functionReference, argumentList);
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
    private JsExpression methodCall() {
        final CallParameters callParameters = callParameters();
        return callType.constructCall(callParameters.receiver, new CallType.CallConstructor() {
            @NotNull
            @Override
            public JsExpression construct(@Nullable JsExpression receiver) {
                JsExpression functionReference = callParameters.functionReference;
                if (receiver != null) {
                    AstUtil.setQualifier(functionReference, receiver);
                }
                return AstUtil.newInvocation(functionReference, arguments);
            }
        }, context());
    }

    @NotNull
    private CallParameters callParameters() {
        if (callee != null) {
            return new CallParameters(null, callee);
        }
        JsExpression thisObject = resolveThisObject(/*just get qualifier if null*/ true);
        JsExpression functionReference = functionReference();
        return new CallParameters(thisObject, functionReference);
    }

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
