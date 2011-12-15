package org.jetbrains.k2js.translate.operation;

import com.google.dart.compiler.backend.js.ast.JsBinaryOperation;
import com.google.dart.compiler.backend.js.ast.JsBinaryOperator;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.intrinsic.CompareToIntrinsic;
import org.jetbrains.k2js.translate.reference.CallTranslator;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import java.util.Arrays;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getFunctionDescriptorForOperationExpression;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.isCompareTo;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getOperationToken;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.*;

/**
 * @author Talanov Pavel
 */
public final class CompareToTranslator extends AbstractTranslator {

    public static boolean isCompareToCall(@NotNull JetBinaryExpression expression,
                                          @NotNull TranslationContext context) {
        FunctionDescriptor operationDescriptor =
                getFunctionDescriptorForOperationExpression(context.bindingContext(), expression);

        if (operationDescriptor == null) return false;

        return (isCompareTo(operationDescriptor));
    }

    @NotNull
    public static JsExpression translate(@NotNull JetBinaryExpression expression,
                                         @NotNull TranslationContext context) {
        return (new CompareToTranslator(expression, context)).translate();
    }

    @NotNull
    private final JetBinaryExpression expression;

    @NotNull
    private final FunctionDescriptor descriptor;

    private CompareToTranslator(@NotNull JetBinaryExpression expression,
                                @NotNull TranslationContext context) {
        super(context);
        this.expression = expression;
        FunctionDescriptor functionDescriptor =
                getFunctionDescriptorForOperationExpression(context.bindingContext(), expression);
        assert functionDescriptor != null : "CompareTo should always have a descriptor";
        this.descriptor = functionDescriptor;
        assert (OperatorConventions.COMPARISON_OPERATIONS.contains(getOperationToken(expression)));
    }

    @NotNull
    private JsExpression translate() {
        if (isIntrinsicOperation(context(), expression)) {
            return intrinsicCompareTo();
        }
        return overloadedCompareTo();
    }

    @NotNull
    private JsExpression overloadedCompareTo() {
        JsBinaryOperator correspondingOperator = OperatorTable.getBinaryOperator(getOperationToken(expression));
        JsExpression methodCall = CallTranslator.translate(expression, context());
        return new JsBinaryOperation(correspondingOperator, methodCall, TranslationUtils.zeroLiteral(context()));
    }

    @NotNull
    private JsExpression intrinsicCompareTo() {
        CompareToIntrinsic intrinsic = context().intrinsics().getCompareToIntrinsic(descriptor);
        intrinsic.setComparisonToken((JetToken) expression.getOperationToken());
        JsExpression left = translateLeftExpression(context(), expression);
        JsExpression right = translateRightExpression(context(), expression);
        return intrinsic.apply(left, Arrays.asList(right), context());
    }
}
