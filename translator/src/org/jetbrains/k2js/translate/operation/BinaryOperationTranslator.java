package org.jetbrains.k2js.translate.operation;

import com.google.dart.compiler.backend.js.ast.JsBinaryOperation;
import com.google.dart.compiler.backend.js.ast.JsBinaryOperator;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.lang.psi.JetOperationExpression;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.intrinsic.CompareToIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.EqualsIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.Intrinsic;
import org.jetbrains.k2js.translate.reference.CallTranslator;

import java.util.Arrays;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getDescriptorForReferenceExpression;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getFunctionDescriptorForOperationExpression;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getOperationToken;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.translateLeftExpression;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.translateRightExpression;


/**
 * @author Talanov Pavel
 */
public final class BinaryOperationTranslator extends AbstractTranslator {


    //TODO: move to utils
    @NotNull
    private static DeclarationDescriptor getOperationDescriptor(@NotNull JetOperationExpression expression,
                                                                @NotNull TranslationContext context) {
        DeclarationDescriptor descriptorForReferenceExpression = getDescriptorForReferenceExpression
                (context.bindingContext(), expression.getOperation());
        assert descriptorForReferenceExpression != null;
        return descriptorForReferenceExpression;
    }

    @NotNull
    public static JsExpression translate(@NotNull JetBinaryExpression expression,
                                         @NotNull TranslationContext context) {
        return (new BinaryOperationTranslator(expression, context).translate());
    }

    private BinaryOperationTranslator(@NotNull JetBinaryExpression expression,
                                      @NotNull TranslationContext context) {
        super(context);
        this.expression = expression;
        this.operationDescriptor =
                getFunctionDescriptorForOperationExpression(context().bindingContext(), expression);
    }

    @NotNull
    private final JetBinaryExpression expression;

    @Nullable
    private final FunctionDescriptor operationDescriptor;

    @NotNull
    private JsExpression translate() {
        if (AssignmentTranslator.isAssignmentOperator(expression)) {
            return AssignmentTranslator.translate(expression, context());
        }
        if (operationDescriptor == null) {
            return translateAsUnOverloadableBinaryOperation();
        }
        if (isEqualsCall()) {
            return translateAsEqualsCall();
        }
        if (isCompareToCall()) {
            return translateAsCompareToCall();
        }
        return CallTranslator.translate(expression, context());
    }

    private boolean isEqualsCall() {
        //TODO: add descriptor is equals utils
        assert operationDescriptor != null;
        boolean isEquals = operationDescriptor.getName().equals("equals");
        boolean isIntrinsic = context().intrinsics().hasDescriptor(operationDescriptor);
        return isEquals && isIntrinsic;
    }

    @NotNull
    private JsExpression translateAsEqualsCall() {
        Intrinsic intrinsic = context().intrinsics().
                getIntrinsic(getOperationDescriptor(expression, context()));
        //TODO
        ((EqualsIntrinsic) intrinsic).setNegated(expression.getOperationToken().equals(JetTokens.EXCLEQ));
        JsExpression left = translateLeftExpression(context(), expression);
        JsExpression right = translateRightExpression(context(), expression);
        return intrinsic.apply(left, Arrays.asList(right), context());
    }

    private boolean isCompareToCall() {
        DeclarationDescriptor operationDescriptor = getOperationDescriptor(expression, context());
        return (operationDescriptor.getName().equals("compareTo")
                && (context().intrinsics().hasDescriptor(operationDescriptor)));
    }

    @NotNull
    private JsExpression translateAsCompareToCall() {
        Intrinsic intrinsic = context().intrinsics().
                getIntrinsic(getOperationDescriptor(expression, context()));
        ((CompareToIntrinsic) intrinsic).setComparisonToken((JetToken) expression.getOperationToken());
        return intrinsic.apply(translateLeftExpression(context(), expression),
                Arrays.asList(translateRightExpression(context(), expression)), context());
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
