package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetToken;

import java.util.Arrays;

/**
 * @author Talanov Pavel
 */
public final class OperationTranslator extends AbstractTranslator {

    @NotNull
    static public OperationTranslator newInstance(@NotNull TranslationContext context) {
        return new OperationTranslator(context);
    }

    private OperationTranslator(@NotNull TranslationContext context) {
        super(context);
    }

    @NotNull
    JsExpression translatePostfixOperation(@NotNull JetPostfixExpression expression) {
        JsExpression baseExpression = translateBaseExpression(expression);
        JsNameRef operationReference = getOverloadedOperationReference(expression);
        if (operationReference != null) {
            return overloadedMethodInvocation(baseExpression, operationReference);
        }
        JsUnaryOperator operator = OperatorTable.getUnaryOperator(getOperationToken(expression));
        return new JsPostfixOperation(operator, baseExpression);
    }

    @NotNull
    JsExpression translatePrefixOperation(@NotNull JetPrefixExpression expression) {
        JsExpression baseExpression = translateBaseExpression(expression);
        JsNameRef operationReference = getOverloadedOperationReference(expression);
        if (operationReference != null) {
            return overloadedMethodInvocation(baseExpression, operationReference);
        }
        JsUnaryOperator operator = OperatorTable.getUnaryOperator(getOperationToken(expression));
        return new JsPrefixOperation(operator, baseExpression);
    }

    @NotNull
    private JsExpression translateBaseExpression(@NotNull JetUnaryExpression expression) {
        JetExpression baseExpression = expression.getBaseExpression();
        assert baseExpression != null : "Unary operation should have a base expression";
        return Translation.translateAsExpression(baseExpression, translationContext());
    }

    @NotNull
    private JsExpression overloadedMethodInvocation(@NotNull JsExpression base, @NotNull JsNameRef operationReference) {
        AstUtil.setQualifier(operationReference, base);
        return AstUtil.newInvocation(operationReference);
    }

    @NotNull
    private JetToken getOperationToken(@NotNull JetUnaryExpression expression) {
        JetSimpleNameExpression operationExpression = expression.getOperationSign();
        IElementType elementType = operationExpression.getReferencedNameElementType();
        assert elementType instanceof JetToken : "Unary operation should have IElementType of type JetToken";
        return (JetToken) elementType;
    }

    @NotNull
    JsExpression translate(@NotNull JetBinaryExpression expression) {
        JsInvocation setterCall = translateAsSetterCall(expression);
        if (setterCall != null) {
            return setterCall;
        }
        return translateAsBinaryOperation(expression);
    }

    //TODO: think about the ways to improve logic here
    @Nullable
    public JsInvocation translateAsSetterCall(@NotNull JetBinaryExpression expression) {
        JetToken jetOperationToken = getOperationToken(expression);
        if (!OperatorTable.isAssignment(jetOperationToken)) {
            return null;
        }
        JetExpression leftExpression = expression.getLeft();
        JsInvocation setterCall = Translation.propertyAccessTranslator(translationContext()).
                translateAsPropertySetterCall(leftExpression);
        if (setterCall == null) {
            return null;
        }
        JsExpression right = translateRightExpression(expression);
        setterCall.setArguments(Arrays.asList(right));
        return setterCall;
    }

    @NotNull
    private JsExpression translateAsBinaryOperation(@NotNull JetBinaryExpression expression) {

        JsExpression left = Translation.translateAsExpression(expression.getLeft(), translationContext());
        JsExpression right = translateRightExpression(expression);

        JsNameRef operationReference = getOverloadedOperationReference(expression);
        if (operationReference != null) {
            return overloadedMethodInvocation(left, right, operationReference);
        }
        JetToken token = getOperationToken(expression);
        if (OperatorTable.hasCorrespondingBinaryOperator(token)) {
            return new JsBinaryOperation(OperatorTable.getBinaryOperator(token), left, right);
        }
        if (OperatorTable.hasCorrespondingFunctionInvocation(token)) {
            JsInvocation functionInvocation = OperatorTable.getCorrespondingFunctionInvocation(token);
            functionInvocation.setArguments(Arrays.asList(left, right));
            return functionInvocation;
        }
        throw new AssertionError("Unsupported token encountered: " + token.toString());
    }

    private JsExpression overloadedMethodInvocation(JsExpression left, JsExpression right, JsNameRef operationReference) {
        AstUtil.setQualifier(operationReference, left);
        return AstUtil.newInvocation(operationReference, right);
    }

    @Nullable
    private JsNameRef getOverloadedOperationReference(@NotNull JetExpression expression) {
        DeclarationDescriptor operationDescriptor = getOperationDescriptor(expression);
        if (operationDescriptor == null) {
            return null;
        }
        if (!translationContext().isDeclared(operationDescriptor)) {
            return null;
        }
        return translationContext().getNameForDescriptor(operationDescriptor).makeRef();
    }

    @Nullable
    private DeclarationDescriptor getOperationDescriptor(@NotNull JetExpression expression) {
        JetSimpleNameExpression operationReference = null;
        if (expression instanceof JetBinaryExpression) {
            operationReference = ((JetBinaryExpression) expression).getOperationReference();
        }
        if (expression instanceof JetUnaryExpression) {
            operationReference = ((JetUnaryExpression) expression).getOperationSign();
        }
        assert operationReference != null : "should be applied only to unary or binary operations";
        return BindingUtils.getDescriptorForReferenceExpression
                (translationContext().bindingContext(), operationReference);
    }

    @NotNull
    private JsExpression translateRightExpression(@NotNull JetBinaryExpression expression) {
        JetExpression rightExpression = expression.getRight();
        assert rightExpression != null : "Binary expression should have a right expression";
        return Translation.translateAsExpression(rightExpression, translationContext());
    }

    @NotNull
    private JetToken getOperationToken(@NotNull JetBinaryExpression expression) {
        return (JetToken) expression.getOperationToken();
    }


}
