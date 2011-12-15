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
import org.jetbrains.k2js.translate.utils.DescriptorUtils;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.google.dart.compiler.util.AstUtil.not;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getDescriptorForReferenceExpression;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getFunctionDescriptorForCallExpression;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.getVariableDescriptorForVariableAsFunction;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.isConstructorDescriptor;
import static org.jetbrains.k2js.translate.utils.PsiUtils.isInOrNotInOperation;
import static org.jetbrains.k2js.translate.utils.PsiUtils.isNotInOperation;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.*;

/**
 * @author Pavel Talanov
 */
//TODO: move translate() static methods into builder (consider!)
//TODO: write tests on calling backing fields as functions
public final class CallTranslator extends AbstractTranslator {

    public static JsExpression translate(@NotNull JetUnaryExpression unaryExpression,
                                         @NotNull TranslationContext context) {
        JsExpression receiver = TranslationUtils.translateBaseExpression(context, unaryExpression);
        List<JsExpression> arguments = Collections.emptyList();
        DeclarationDescriptor descriptor = getDescriptorForReferenceExpression
                (context.bindingContext(), unaryExpression.getOperationReference());
        assert descriptor instanceof FunctionDescriptor;
        return translate(context, receiver, arguments, (FunctionDescriptor) descriptor);
    }

    //TODO: method too long
    public static JsExpression translate(@NotNull JetDotQualifiedExpression dotExpression,
                                         @NotNull TranslationContext context) {
        JsExpression receiver = translateReceiver(context, dotExpression);
        //TODO: util?
        JetExpression selectorExpression = dotExpression.getSelectorExpression();
        assert selectorExpression instanceof JetCallExpression;
        JetCallExpression callExpression = (JetCallExpression) selectorExpression;
        List<JsExpression> arguments =
                translateArgumentList(context, callExpression.getValueArguments());
        FunctionDescriptor descriptor =
                getFunctionDescriptorForCallExpression(context.bindingContext(), callExpression);
        return translate(context, receiver, arguments, descriptor);
    }

    public static JsExpression translate(@NotNull JetCallExpression callExpression,
                                         @NotNull TranslationContext context) {
        DeclarationDescriptor descriptor =
                getFunctionDescriptorForCallExpression(context.bindingContext(), callExpression);
        JsExpression receiver = getImplicitReceiver(context, descriptor);
        List<JsExpression> arguments = translateArgumentList(context, callExpression.getValueArguments());
        return translate(context, receiver, arguments, (FunctionDescriptor) descriptor);
    }

    //TODO: refactor
    public static JsExpression translate(@NotNull JetBinaryExpression binaryExpression,
                                         @NotNull TranslationContext context) {
        JsExpression receiver = translateLeftExpression(context, binaryExpression);
        List<JsExpression> arguments = Arrays.asList(translateRightExpression(context, binaryExpression));
        DeclarationDescriptor descriptor = getDescriptorForReferenceExpression
                (context.bindingContext(), binaryExpression.getOperationReference());
        assert descriptor instanceof FunctionDescriptor;
        FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
        if (isInOrNotInOperation(binaryExpression)) {
            JsExpression inRangeCheck = translateAsInOperation(context, receiver, arguments, functionDescriptor);
            return mayBeWrapWithNegation(inRangeCheck, isNotInOperation(binaryExpression));
        }
        return translate(context, receiver, arguments, functionDescriptor);
    }

    public static JsExpression mayBeWrapWithNegation(@NotNull JsExpression expression, boolean shouldWrap) {
        if (shouldWrap) {
            return not(expression);
        } else {
            return expression;
        }
    }

    private static JsExpression translateAsInOperation(@NotNull TranslationContext context,
                                                       @NotNull JsExpression receiver,
                                                       @NotNull List<JsExpression> arguments,
                                                       @NotNull FunctionDescriptor functionDescriptor) {
        return translateWithReceiverAndArgumentSwapped(context, receiver, arguments, functionDescriptor);
    }

    @NotNull
    private static JsExpression translateWithReceiverAndArgumentSwapped(@NotNull TranslationContext context,
                                                                        @NotNull JsExpression receiver,
                                                                        @NotNull List<JsExpression> arguments,
                                                                        @NotNull FunctionDescriptor functionDescriptor) {
        assert arguments.size() == 1 : "Must have one argument.";
        return translate(context, arguments.get(0), Arrays.asList(receiver), functionDescriptor);
    }

    @NotNull
    private static JsExpression translate(@NotNull TranslationContext context,
                                          @Nullable JsExpression receiver,
                                          @NotNull List<JsExpression> arguments,
                                          @NotNull FunctionDescriptor functionDescriptor) {
        return (new CallTranslator(receiver, arguments, functionDescriptor, context)).translate();
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
        if (isExtensionFunction()) {
            return extensionFunctionCall();
        }
        return methodCall();
    }

    @NotNull
    private JsExpression extensionFunctionCall() {
        List<JsExpression> argumentList = new ArrayList<JsExpression>();
        argumentList.add(receiver);
        argumentList.addAll(arguments);
        return AstUtil.newInvocation(calleeReference(), argumentList);
    }

    private boolean isExtensionFunction() {
        return DescriptorUtils.isExtensionFunction(functionDescriptor);
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
        if (DescriptorUtils.isVariableDescriptor(functionDescriptor)) {
            //TODO: write tests on this cases
            VariableDescriptor variableDescriptor =
                    getVariableDescriptorForVariableAsFunction((VariableAsFunctionDescriptor) functionDescriptor);
            if (variableDescriptor instanceof PropertyDescriptor) {
                return getterCall((PropertyDescriptor) variableDescriptor);
            }
            return qualifiedMethodReference(variableDescriptor);
        }
        return qualifiedMethodReference(functionDescriptor);
    }

    @NotNull
    private JsExpression getterCall(PropertyDescriptor variableDescriptor) {
        return PropertyAccessTranslator.translateAsPropertyGetterCall(variableDescriptor, context());
    }

    @NotNull
    private JsExpression qualifiedMethodReference(@NotNull DeclarationDescriptor descriptor) {
        JsExpression methodReference = ReferenceTranslator.translateReference(descriptor, context());
        if (isExtensionFunction()) {
            return extensionFunctionReference(methodReference);
        } else if (receiver != null) {
            AstUtil.setQualifier(methodReference, receiver);
        }
        return methodReference;
    }

    private JsExpression extensionFunctionReference(@NotNull JsExpression methodReference) {
        JsExpression qualifier = TranslationUtils.getExtensionFunctionImplicitReceiver(context(), functionDescriptor);
        if (qualifier != null) {
            AstUtil.setQualifier(methodReference, qualifier);
        }
        return methodReference;
    }

    @NotNull
    private JsExpression constructorCall() {
        JsNew constructorCall = new JsNew(qualifiedMethodReference(functionDescriptor));
        constructorCall.setArguments(arguments);
        return constructorCall;
    }
}
