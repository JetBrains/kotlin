package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetToken;

import java.util.Arrays;

/**
 * @author Talanov Pavel
 */
public final class UnaryOperationTranslator extends OperationTranslator {


    @NotNull
    static public JsExpression translate(@NotNull JetPrefixExpression expression,
                                         @NotNull TranslationContext context) {
        return (new UnaryOperationTranslator(context, expression, true)).translate();
    }

    @NotNull
    static public JsExpression translate(@NotNull JetPostfixExpression expression,
                                         @NotNull TranslationContext context) {
        return (new UnaryOperationTranslator(context, expression, false)).translate();
    }

    @NotNull
    private final JetUnaryExpression expression;
    @NotNull
    private final JsExpression baseExpression;
    private final boolean isPrefix;
    private final boolean isVariableReassignment;
    private final boolean isStatement;
    private final boolean isPropertyAccess;
    @Nullable
    private final JsNameRef operationReference;

    private UnaryOperationTranslator(@NotNull TranslationContext context, @NotNull JetUnaryExpression expression,
                                     boolean isPrefix) {
        super(context);
        this.expression = expression;
        this.isPrefix = isPrefix;
        this.isVariableReassignment = isVariableReassignment(expression);
        this.isStatement = BindingUtils.isStatement(translationContext().bindingContext(), expression);
        this.baseExpression = translateBaseExpression();
        this.isPropertyAccess = isPropertyAccess();
        this.operationReference = getOverloadedOperationReference(expression);
    }

    private boolean isPropertyAccess() {
        return Translation.propertyAccessTranslator(translationContext()).canBePropertyAccess(getBaseExpression());

    }

    private boolean isVariableReassignment(JetUnaryExpression expression) {
        return BindingUtils.isVariableReassignment
                (translationContext().bindingContext(), expression);
    }

    @NotNull
    JsExpression translate() {
        if ((operationReference != null) || isPropertyAccess) {
            return translateOverload();
        }
        return jsUnaryExpression();
    }

    @NotNull
    private JsExpression translateOverload() {
        if (isStatement || isPrefix) {
            return asPrefix();
        }
        if (isVariableReassignment) {
            return asPostfixWithReassignment();
        } else {
            return asPostfixWithNoReassignment();
        }
    }

    @NotNull
    private JsExpression asPrefix() {
        if (isVariableReassignment) {
            return variableReassignment(baseExpression);
        }
        return operationExpression(baseExpression);
    }

    //TODO: decide if this expression can be optimised in case of direct access (not property)
    @NotNull
    private JsExpression asPostfixWithReassignment() {
        // code fragment: expr(a++)
        // generate: expr( (t1 = a, t2 = t1, a = t1.inc(), t2) )
        TemporaryVariable t1 = declareTemporary(baseExpression);
        TemporaryVariable t2 = declareTemporary(t1.nameReference());
        JsExpression variableReassignment = variableReassignment(t1.nameReference());
        return AstUtil.newSequence(t1.assignmentExpression(), t2.assignmentExpression(),
                variableReassignment, t2.nameReference());
    }

    @NotNull
    private JsExpression asPostfixWithNoReassignment() {
        // code fragment: expr(a++)
        // generate: expr( (t1 = a, t2 = t1, t2.inc(), t1) )
        assert operationReference != null;
        TemporaryVariable t1 = declareTemporary(baseExpression);
        TemporaryVariable t2 = declareTemporary(t1.nameReference());
        JsExpression methodCall = operationExpression(t2.nameReference());
        JsExpression returnedValue = t1.nameReference();
        return AstUtil.newSequence(t1.assignmentExpression(), t2.assignmentExpression(), methodCall, returnedValue);
    }


    //TODO: should modify this for properties
    @NotNull
    private JsExpression variableReassignment(@NotNull JsExpression toCallMethodUpon) {
        if (isPropertyAccess) {
            return propertyReassignment(toCallMethodUpon);
        }
        return localVariableReassignment(toCallMethodUpon);
    }

    private JsExpression localVariableReassignment(@NotNull JsExpression toCallMethodUpon) {
        assert baseExpression instanceof JsNameRef : "Base expression should be an l-value";
        return AstUtil.newAssignment((JsNameRef) baseExpression, operationExpression(toCallMethodUpon));
    }

    @NotNull
    private JsExpression propertyReassignment(@NotNull JsExpression toCallMethodUpon) {
        JetExpression jetBaseExpression = getBaseExpression();
        PropertyAccessTranslator propertyAccessTranslator = Translation.propertyAccessTranslator(translationContext());
        JsInvocation setterCall = propertyAccessTranslator.translateAsPropertySetterCall(jetBaseExpression);
        assert propertyAccessTranslator.canBePropertyGetterCall(jetBaseExpression) : "Should be a getter call";
        JsExpression overloadedMethodCallOnPropertyGetter = operationExpression(toCallMethodUpon);
        setterCall.setArguments(Arrays.asList(overloadedMethodCallOnPropertyGetter));
        return setterCall;
    }

    @NotNull
    private JsExpression operationExpression(@NotNull JsExpression receiver) {
        if (operationReference != null) {
            AstUtil.setQualifier(operationReference, receiver);
            return AstUtil.newInvocation(operationReference);
        }
        return new JsPrefixOperation(OperatorTable.getUnaryOperator(getOperationToken()), receiver);
    }

    @NotNull
    private JsExpression jsUnaryExpression() {
        JsUnaryOperator operator = OperatorTable.getUnaryOperator(getOperationToken());
        if (isPrefix) {
            return new JsPrefixOperation(operator, baseExpression);
        } else {
            return new JsPostfixOperation(operator, baseExpression);
        }
    }

    @NotNull
    private JsExpression translateBaseExpression() {
        JetExpression baseExpression = getBaseExpression();
        return Translation.translateAsExpression(baseExpression, translationContext());
    }

    private JetExpression getBaseExpression() {
        JetExpression baseExpression = expression.getBaseExpression();
        assert baseExpression != null : "Unary expression should have a base expression";
        return baseExpression;
    }

    @NotNull
    private JetToken getOperationToken() {
        JetSimpleNameExpression operationExpression = expression.getOperationSign();
        IElementType elementType = operationExpression.getReferencedNameElementType();
        assert elementType instanceof JetToken : "Unary expression should have IElementType of type JetToken";
        return (JetToken) elementType;
    }
}
