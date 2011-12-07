package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsInvocation;
import com.google.dart.compiler.backend.js.ast.JsNew;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.intrinsic.FunctionIntrinsic;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getDescriptorForReferenceExpression;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getFunctionDescriptorForCallExpression;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.isConstructorDescriptor;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.*;

/**
 * @author Talanov Pavel
 */
//TODO: write tests on calling backing fields as functions
public final class CallTranslator extends AbstractTranslator {

    public static JsExpression translate(@NotNull JetUnaryExpression unaryExpression,
                                         @NotNull TranslationContext context) {
        JsExpression receiver = TranslationUtils.translateBaseExpression(context, unaryExpression);
        List<JsExpression> arguments = Collections.emptyList();
        DeclarationDescriptor descriptor = getDescriptorForReferenceExpression
                (context.bindingContext(), unaryExpression.getOperation());
        assert descriptor instanceof FunctionDescriptor;
        return (new CallTranslator(receiver, arguments, (FunctionDescriptor) descriptor, context)).translate();
    }

    //TODO: method too long
    public static JsExpression translate(@NotNull JetDotQualifiedExpression dotExpression,
                                         @NotNull TranslationContext context) {
        //TODO: look for duplication
        JsExpression receiver = translateReceiver(context, dotExpression);
        JetExpression selectorExpression = dotExpression.getSelectorExpression();
        assert selectorExpression instanceof JetCallExpression;
        JetCallExpression callExpression = (JetCallExpression) selectorExpression;
        List<JsExpression> arguments =
                translateArgumentList(context, callExpression.getValueArguments());
        FunctionDescriptor descriptor =
                getFunctionDescriptorForCallExpression(context.bindingContext(), callExpression);
        return (new CallTranslator(receiver, arguments, descriptor, context)).translate();
    }

    public static JsExpression translate(@NotNull JetCallExpression callExpression,
                                         @NotNull TranslationContext context) {
        DeclarationDescriptor descriptor =
                getFunctionDescriptorForCallExpression(context.bindingContext(), callExpression);
        JsExpression receiver = getImplicitReceiver(context, descriptor);
        List<JsExpression> arguments = translateArgumentList(context, callExpression.getValueArguments());
        return (new CallTranslator(receiver, arguments, (FunctionDescriptor) descriptor, context)).translate();
    }

    public static JsExpression translate(@NotNull JetBinaryExpression binaryExpression,
                                         @NotNull TranslationContext context) {
        JsExpression receiver = translateLeftExpression(context, binaryExpression);
        List<JsExpression> arguments = Arrays.asList(translateRightExpression(context, binaryExpression));
        //TODO: use PSI util method to get operation reference
        DeclarationDescriptor descriptor = getDescriptorForReferenceExpression
                (context.bindingContext(), binaryExpression.getOperation());
        assert descriptor instanceof FunctionDescriptor;
        return (new CallTranslator(receiver, arguments, (FunctionDescriptor) descriptor, context)).translate();
    }

    @Nullable
    private final JsExpression receiver;

    @NotNull
    private final List<JsExpression> arguments;

    @NotNull
    private final FunctionDescriptor functionDescriptor;

    private CallTranslator(@Nullable JsExpression receiver, @NotNull List<JsExpression> arguments,
                           @NotNull FunctionDescriptor descriptor, @NotNull TranslationContext context) {
        super(context);
        this.receiver = receiver;
        this.arguments = arguments;
        this.functionDescriptor = descriptor;
    }

    @NotNull
    private JsExpression translate() {
        if (isIntrinsic()) {
            return intrinsicInvocation();
        }
        if (isConstructor()) {
            return constructorCall();
        }
        return methodCall();
    }

    @NotNull
    private JsExpression intrinsicInvocation() {
        FunctionIntrinsic functionIntrinsic = context().intrinsics().getFunctionIntrinsic(functionDescriptor);
        assert receiver != null : "Functions that have functionIntrinsic implementation should have a receiver.";
        return functionIntrinsic.apply(receiver, arguments, context());
    }

    private JsInvocation methodCall() {
        return AstUtil.newInvocation(calleeReference(), arguments);
    }

    private boolean isConstructor() {
        return isConstructorDescriptor(functionDescriptor);
    }

    private boolean isIntrinsic() {
        return context().intrinsics().isIntrinsic(functionDescriptor);
    }

    @NotNull
    private JsExpression calleeReference() {
        //TODO: refactor
        //TODO: write tests on this cases
        if (functionDescriptor instanceof VariableAsFunctionDescriptor) {
            VariableDescriptor variableDescriptor = ((VariableAsFunctionDescriptor) functionDescriptor).getVariableDescriptor();
            if (variableDescriptor instanceof PropertyDescriptor) {
                PropertyDescriptor propertyDescriptor = (PropertyDescriptor) variableDescriptor;
                return PropertyAccessTranslator.translateAsPropertyGetterCall(propertyDescriptor, context());
            }
            return qualifiedMethodReference(variableDescriptor);
        }
        return qualifiedMethodReference(functionDescriptor);
    }

    @NotNull
    private JsExpression qualifiedMethodReference(@NotNull DeclarationDescriptor descriptor) {
        JsExpression methodReference = ReferenceTranslator.translateReference(descriptor, context());
        if (receiver != null) {
            AstUtil.setQualifier(methodReference, receiver);
        }
        return methodReference;
    }

    @NotNull
    private JsExpression constructorCall() {
        JsNew constructorCall = new JsNew(calleeReference());
        constructorCall.setArguments(arguments);
        return constructorCall;
    }
}
