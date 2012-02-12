package org.jetbrains.k2js.translate.reference;

import com.google.common.collect.Lists;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCallImpl;
import org.jetbrains.k2js.translate.context.TranslationContext;

import java.util.Arrays;
import java.util.List;

/**
 * @author Pavel Talanov
 */
public final class CallBuilder {

    public static CallBuilder build(@NotNull TranslationContext context) {
        return new CallBuilder(context);
    }

    @NotNull
    private final TranslationContext context;
    @Nullable
    private /*var*/ JsExpression receiver = null;
    @NotNull
    private final List<JsExpression> args = Lists.newArrayList();
    @NotNull
    private /*var*/ CallType callType = CallType.NORMAL;
    @Nullable
    private /*var*/ ResolvedCall<?> resolvedCall = null;
    @Nullable
    private  /*var*/ CallableDescriptor descriptor = null;
    @Nullable
    private /*var*/ JsExpression callee = null;


    private CallBuilder(@NotNull TranslationContext context) {
        this.context = context;
    }

    @NotNull
    public CallBuilder receiver(@Nullable JsExpression receiver) {
        this.receiver = receiver;
        return this;
    }

    @NotNull
    public CallBuilder args(@NotNull List<JsExpression> args) {
        assert this.args.isEmpty();
        this.args.addAll(args);
        return this;
    }

    @NotNull
    public CallBuilder args(@NotNull JsExpression... args) {
        return args(Arrays.asList(args));
    }

    @NotNull
    public CallBuilder descriptor(@NotNull CallableDescriptor descriptor) {
        this.descriptor = descriptor;
        return this;
    }

    @NotNull
    public CallBuilder callee(@Nullable JsExpression callee) {
        this.callee = callee;
        return this;
    }

    @NotNull
    public CallBuilder resolvedCall(@NotNull ResolvedCall<?> call) {
        this.resolvedCall = call;
        return this;
    }

    @NotNull
    public CallBuilder type(@NotNull CallType type) {
        this.callType = type;
        return this;
    }

    //TODO: must be private
    @NotNull
    public CallTranslator finish() {
        if (resolvedCall == null) {
            assert descriptor != null;
            resolvedCall = ResolvedCallImpl.create(descriptor);
        }
        if (descriptor == null) {
            descriptor = resolvedCall.getCandidateDescriptor().getOriginal();
        }
        assert resolvedCall != null;
        return new CallTranslator(receiver, callee, args, resolvedCall, descriptor, callType, context);
    }

    @NotNull
    public JsExpression translate() {
        return finish().translate();
    }
    /*
       @NotNull
        private CallTranslator buildFromUnary(@NotNull JetUnaryExpression unaryExpression) {
            JsExpression receiver = TranslationUtils.translateBaseExpression(context, unaryExpression);
            List<JsExpression> arguments = Collections.emptyList();
            ResolvedCall<?> resolvedCall =
                    getResolvedCall(context.bindingContext(), unaryExpression.getOperationReference());
            return new CallTranslator(receiver, null, arguments, resolvedCall, null, CallType.NORMAL, context);
        }

        //TODO: method too long
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

            ResolvedCall<?> resolvedCall =
                    getResolvedCall(context.bindingContext(), binaryExpression.getOperationReference());
            return new CallTranslator(receiver, null, arguments, resolvedCall, null, CallType.NORMAL, context);
        }

        @NotNull
        private CallTranslator buildFromCallExpression(@NotNull JetCallExpression callExpression,
                                                       @Nullable JsExpression receiver,
                                                       @NotNull CallType callType) {
            ResolvedCall<?> resolvedCall = getResolvedCallForCallExpression(context.bindingContext(), callExpression);
            List<JsExpression> arguments = translateArgumentsForCallExpression(callExpression, context);
            JsExpression callee = null;
            if (resolvedCall.getCandidateDescriptor() instanceof ExpressionAsFunctionDescriptor) {
                callee = Translation.translateAsExpression(getCallee(callExpression), context);
            }
            return new CallTranslator(receiver, callee, arguments, resolvedCall, null, callType, context);
        }

        @NotNull
        private List<JsExpression> translateArgumentsForCallExpression(@NotNull JetCallExpression callExpression,
                                                                       @NotNull TranslationContext context) {
            List<JsExpression> result = new ArrayList<JsExpression>();
            ResolvedCall<?> resolvedCall = getResolvedCallForCallExpression(context.bindingContext(), callExpression);
            Map<ValueParameterDescriptor, ResolvedValueArgument> formalToActualArguments = resolvedCall.getValueArguments();
            for (ValueParameterDescriptor parameterDescriptor : resolvedCall.getResultingDescriptor().getValueParameters()) {
                ResolvedValueArgument actualArgument = formalToActualArguments.get(parameterDescriptor);
                result.add(translateSingleArgument(actualArgument, parameterDescriptor));
            }
            return result;
        }


        //TODO: refactor
        @NotNull
        private JsExpression translateSingleArgument(@NotNull ResolvedValueArgument actualArgument,
                                                     @NotNull ValueParameterDescriptor parameterDescriptor) {
            List<JetExpression> argumentExpressions = actualArgument.getArgumentExpressions();
            if (actualArgument instanceof VarargValueArgument) {
                return translateVarargArgument(argumentExpressions);
            }
            if (actualArgument instanceof DefaultValueArgument) {
                JetExpression defaultArgument = getDefaultArgument(context.bindingContext(), parameterDescriptor);
                return Translation.translateAsExpression(defaultArgument, context);
            }
            assert actualArgument instanceof ExpressionValueArgument;
            assert argumentExpressions.size() == 1;
            return Translation.translateAsExpression(argumentExpressions.get(0), context);
        }

        @NotNull
        private JsExpression translateVarargArgument(@NotNull List<JetExpression> arguments) {
            JsArrayLiteral varargArgument = new JsArrayLiteral();
            for (JetExpression argument : arguments) {
                varargArgument.getExpressions().add(Translation.translateAsExpression(argument, context));
            }
            return varargArgument;
        }
      */


}
