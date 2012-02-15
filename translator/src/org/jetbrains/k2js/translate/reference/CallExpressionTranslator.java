package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsArrayLiteral;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.calls.*;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getDefaultArgument;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getResolvedCallForCallExpression;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getCallee;

/**
 * @author Pavel Talanov
 */
public final class CallExpressionTranslator extends AbstractTranslator {

    @NotNull
    public static JsExpression translate(@NotNull JetCallExpression expression,
                                         @Nullable JsExpression receiver,
                                         @NotNull CallType callType,
                                         @NotNull TranslationContext context) {
        return (new CallExpressionTranslator(expression, context)).translate(receiver, callType);
    }

    @NotNull
    private final JetCallExpression expression;

    private CallExpressionTranslator(@NotNull JetCallExpression expression,
                                     @NotNull TranslationContext context) {
        super(context);
        this.expression = expression;
    }

    @NotNull
    private JsExpression translate(@Nullable JsExpression receiver,
                                   @NotNull CallType callType) {
        ResolvedCall<?> resolvedCall = getResolvedCallForCallExpression(context().bindingContext(), expression);
        return CallBuilder.build(context())
                .receiver(receiver)
                .callee(getCalleeExpression(resolvedCall))
                .args(translateArguments())
                .resolvedCall(resolvedCall)
                .type(callType)
                .translate();
    }

    @Nullable
    private JsExpression getCalleeExpression(@NotNull ResolvedCall<?> resolvedCall) {
        if (resolvedCall.getCandidateDescriptor() instanceof ExpressionAsFunctionDescriptor) {
            return Translation.translateAsExpression(getCallee(expression), context());
        }
        return null;
    }

    @NotNull
    private List<JsExpression> translateArguments() {
        List<JsExpression> result = new ArrayList<JsExpression>();
        ResolvedCall<?> resolvedCall = getResolvedCallForCallExpression(context().bindingContext(), expression);
        for (ValueParameterDescriptor parameterDescriptor : resolvedCall.getResultingDescriptor().getValueParameters()) {
            ResolvedValueArgument actualArgument = resolvedCall.getValueArgumentsByIndex().get(parameterDescriptor.getIndex());
            result.add(translateSingleArgument(actualArgument, parameterDescriptor));
        }
        return result;
    }

    @NotNull
    private JsExpression translateSingleArgument(@NotNull ResolvedValueArgument actualArgument,
                                                 @NotNull ValueParameterDescriptor parameterDescriptor) {
        List<JetExpression> argumentExpressions = actualArgument.getArgumentExpressions();
        if (actualArgument instanceof VarargValueArgument) {
            return translateVarargArgument(argumentExpressions);
        }
        if (actualArgument instanceof DefaultValueArgument) {
            JetExpression defaultArgument = getDefaultArgument(context().bindingContext(), parameterDescriptor);
            return Translation.translateAsExpression(defaultArgument, context());
        }
        assert actualArgument instanceof ExpressionValueArgument;
        assert argumentExpressions.size() == 1;
        return Translation.translateAsExpression(argumentExpressions.get(0), context());
    }

    @NotNull
    private JsExpression translateVarargArgument(@NotNull List<JetExpression> arguments) {
        JsArrayLiteral varargArgument = new JsArrayLiteral();
        for (JetExpression argument : arguments) {
            varargArgument.getExpressions().add(Translation.translateAsExpression(argument, context()));
        }
        return varargArgument;
    }
}
