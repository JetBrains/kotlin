package org.jetbrains.k2js.translate.operation;

import com.google.dart.compiler.backend.js.ast.JsBinaryOperation;
import com.google.dart.compiler.backend.js.ast.JsBinaryOperator;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.intrinsic.EqualsIntrinsic;
import org.jetbrains.k2js.translate.reference.CallTranslator;

import java.util.Arrays;

import static org.jetbrains.k2js.translate.operation.AssignmentTranslator.isAssignmentOperator;
import static org.jetbrains.k2js.translate.operation.CompareToTranslator.isCompareToCall;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getFunctionDescriptorForOperationExpression;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.isEquals;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getOperationToken;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.translateLeftExpression;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.translateRightExpression;


/**
 * @author Pavel Talanov
 */
public final class BinaryOperationTranslator extends AbstractTranslator {

    @NotNull
    public static JsExpression translate(@NotNull JetBinaryExpression expression,
                                         @NotNull TranslationContext context) {
        return (new BinaryOperationTranslator(expression, context).translate());
    }

    @NotNull
    private final JetBinaryExpression expression;

    @Nullable
    private final FunctionDescriptor operationDescriptor;

    private BinaryOperationTranslator(@NotNull JetBinaryExpression expression,
                                      @NotNull TranslationContext context) {
        super(context);
        this.expression = expression;
        this.operationDescriptor =
                getFunctionDescriptorForOperationExpression(context().bindingContext(), expression);
    }

    @NotNull
    private JsExpression translate() {
        if (isAssignmentOperator(expression)) {
            return AssignmentTranslator.translate(expression, context());
        }
        if (isNotOverloadable()) {
            return translateAsUnOverloadableBinaryOperation();
        }
        if (isCompareToCall(expression, context())) {
            return CompareToTranslator.translate(expression, context());
        }
        assert operationDescriptor != null :
                "Overloadable operations must have not null descriptor";
        if (isEquals(operationDescriptor)) {
            return translateAsEqualsCall();
        }
        return CallTranslator.translate(expression, context());
    }

    private boolean isNotOverloadable() {
        return operationDescriptor == null;
    }

    @NotNull
    private JsExpression translateAsEqualsCall() {
        assert operationDescriptor != null : "Equals operation must resolve to descriptor.";
        EqualsIntrinsic intrinsic = context().intrinsics().getEqualsIntrinsic(operationDescriptor);
        intrinsic.setNegated(expression.getOperationToken().equals(JetTokens.EXCLEQ));
        JsExpression left = translateLeftExpression(context(), expression);
        JsExpression right = translateRightExpression(context(), expression);
        return intrinsic.apply(left, Arrays.asList(right), context());
    }

    @NotNull
    private JsExpression translateAsUnOverloadableBinaryOperation() {
        JetToken token = getOperationToken(expression);
        JsBinaryOperator operator = OperatorTable.getBinaryOperator(token);
        assert OperatorConventions.NOT_OVERLOADABLE.contains(token);
        JsExpression left = translateLeftExpression(context(), expression);
        JsExpression right = translateRightExpression(context(), expression);
        return new JsBinaryOperation(operator, left, right);
    }

}
