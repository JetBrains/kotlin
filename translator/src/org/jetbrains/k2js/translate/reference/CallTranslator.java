package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsInvocation;
import com.google.dart.compiler.backend.js.ast.JsNew;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.calls.DefaultValueArgument;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.ResolvedValueArgument;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.intrinsic.FunctionIntrinsic;
import org.jetbrains.k2js.translate.utils.DescriptorUtils;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import java.util.*;

import static com.google.dart.compiler.util.AstUtil.not;
import static org.jetbrains.k2js.translate.utils.BindingUtils.*;
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

    private static final class Builder {

        @NotNull
        private final TranslationContext context;

        private Builder(@NotNull TranslationContext context) {
            this.context = context;
        }

        @NotNull
        private CallTranslator buildFromUnary(@NotNull JetUnaryExpression unaryExpression) {
            JsExpression receiver = TranslationUtils.translateBaseExpression(context, unaryExpression);
            List<JsExpression> arguments = Collections.emptyList();
            DeclarationDescriptor descriptor = getDescriptorForReferenceExpression
                    (context.bindingContext(), unaryExpression.getOperationReference());
            assert descriptor instanceof FunctionDescriptor;
            return new CallTranslator(receiver, arguments, (FunctionDescriptor) descriptor, context);
        }

        @NotNull
        private CallTranslator buildFromBinary(@NotNull JetBinaryExpression binaryExpression,
                                               boolean swapReceiverAndArgument) {

            JsExpression leftExpression = translateLeftExpression(context, binaryExpression);
            JsExpression rightExpression = translateRightExpression(context, binaryExpression);

            JsExpression receiver;
            List<JsExpression> arguments;
            if (swapReceiverAndArgument) {
                receiver = rightExpression;
                arguments = Arrays.asList(leftExpression);
            } else {
                receiver = leftExpression;
                arguments = Arrays.asList(rightExpression);
            }

            DeclarationDescriptor descriptor = getDescriptorForReferenceExpression
                    (context.bindingContext(), binaryExpression.getOperationReference());
            assert descriptor instanceof FunctionDescriptor;
            FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
            return new CallTranslator(receiver, arguments, functionDescriptor, context);
        }

        @NotNull
        private CallTranslator buildFromDotQualified(@NotNull JetDotQualifiedExpression dotExpression) {
            JsExpression receiver = translateReceiver(context, dotExpression);
            //TODO: util?
            JetExpression selectorExpression = dotExpression.getSelectorExpression();
            assert selectorExpression instanceof JetCallExpression;
            JetCallExpression callExpression = (JetCallExpression) selectorExpression;
            FunctionDescriptor descriptor =
                    getFunctionDescriptorForCallExpression(context.bindingContext(), callExpression);
            List<JsExpression> arguments = translateArgumentsForCallExpression(callExpression, context);
            return new CallTranslator(receiver, arguments, descriptor, context);
        }

        @NotNull
        private CallTranslator buildFromCallExpression(@NotNull JetCallExpression callExpression) {
            FunctionDescriptor descriptor =
                    getFunctionDescriptorForCallExpression(context.bindingContext(), callExpression);
            JsExpression receiver = getImplicitReceiver(context, descriptor);
            List<JsExpression> arguments = translateArgumentsForCallExpression(callExpression, context);
            return new CallTranslator(receiver, arguments, descriptor, context);
        }

        @NotNull
        private static List<JsExpression> translateArgumentsForCallExpression(@NotNull JetCallExpression callExpression,
                                                                              @NotNull TranslationContext context) {
            List<JsExpression> result = new ArrayList<JsExpression>();
            ResolvedCall<?> resolvedCall = getResolvedCallForCallExpression(context.bindingContext(), callExpression);
            Map<ValueParameterDescriptor, ResolvedValueArgument> formalToActualArguments = resolvedCall.getValueArguments();
            for (ValueParameterDescriptor parameterDescriptor : resolvedCall.getResultingDescriptor().getValueParameters()) {
                JetExpression argument = getActualArgument(formalToActualArguments, parameterDescriptor, context);
                result.add(Translation.translateAsExpression(argument, context));
            }
            return result;
        }

        @NotNull
        private static JetExpression getActualArgument(
                @NotNull Map<ValueParameterDescriptor, ResolvedValueArgument> formalToActualArguments,
                @NotNull ValueParameterDescriptor parameterDescriptor, @NotNull TranslationContext context) {
            ResolvedValueArgument actualArgument = formalToActualArguments.get(parameterDescriptor);
            if (actualArgument instanceof DefaultValueArgument) {
                assert parameterDescriptor.hasDefaultValue() : "Unsupplied parameter must have default value.";
                JetParameter psiParameter = getParameterForDescriptor(context.bindingContext(), parameterDescriptor);
                JetExpression defaultValue = psiParameter.getDefaultValue();
                assert defaultValue != null : "No default value found in PSI.";
                return defaultValue;
            }
            List<JetExpression> argumentExpressions = actualArgument.getArgumentExpressions();
            assert !argumentExpressions.isEmpty() : "Actual arguments must be supplied.";
            assert argumentExpressions.size() == 1 : "Varargs not supported.";
            return argumentExpressions.get(0);
        }
    }

    @NotNull
    public static JsExpression translate(@NotNull JetUnaryExpression unaryExpression,
                                         @NotNull TranslationContext context) {
        return (new Builder(context).buildFromUnary(unaryExpression)).translate();
    }

    @NotNull
    public static JsExpression translate(@NotNull JetDotQualifiedExpression dotExpression,
                                         @NotNull TranslationContext context) {
        return (new Builder(context).buildFromDotQualified(dotExpression)).translate();
    }


    @NotNull
    public static JsExpression translate(@NotNull JetCallExpression callExpression,
                                         @NotNull TranslationContext context) {
        return (new Builder(context).buildFromCallExpression(callExpression)).translate();
    }

    @NotNull
    public static JsExpression translate(@NotNull JetBinaryExpression binaryExpression,
                                         @NotNull TranslationContext context) {
        boolean shouldSwapReceiverAndArgument = isInOrNotInOperation(binaryExpression);
        JsExpression result = (new Builder(context))
                .buildFromBinary(binaryExpression, shouldSwapReceiverAndArgument).translate();
        if (isInOrNotInOperation(binaryExpression)) {
            return mayBeWrapWithNegation(result, isNotInOperation(binaryExpression));
        }
        return result;
    }

    @NotNull
    private static JsExpression mayBeWrapWithNegation(@NotNull JsExpression expression, boolean shouldWrap) {
        if (shouldWrap) {
            return not(expression);
        } else {
            return expression;
        }
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

    @NotNull
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

    @NotNull
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
