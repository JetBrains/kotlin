package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsBlock;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.backend.js.ast.JsReturn;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lexer.JetToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Talanov Pavel
 */
public final class ExpressionVisitor extends TranslatorVisitor<JsNode> {

    public ExpressionVisitor(TranslationContext context) {
    }

    //TODO method too long
    @Override
    @NotNull
    public JsNode visitConstantExpression(@NotNull JetConstantExpression expression, @NotNull TranslationContext context) {
        JsExpression result = null;
        Object value;
        CompileTimeConstant<?> compileTimeValue =
                context.bindingContext().get(BindingContext.COMPILE_TIME_VALUE, expression);
        assert compileTimeValue != null;
        value = compileTimeValue.getValue();
        if (value instanceof Integer) {
            result = context.program().getNumberLiteral((Integer) value);
        }
        if (value instanceof Boolean) {
            return context.program().getBooleanLiteral((Boolean) value);
        }
        assert result != null;
        return result;
    }

    @Override
    @NotNull
    public JsNode visitBlockExpression(@NotNull JetBlockExpression jetBlock, @NotNull TranslationContext context) {
        List<JetElement> jetElements = jetBlock.getStatements();
        JsBlock jsBlock = new JsBlock();
        TranslationContext newContext = context.newBlock();
        for (JetElement jetElement : jetElements) {
            //TODO hack alert
            JetExpression jetExpression = (JetExpression) jetElement;
            JsNode jsNode = jetExpression.accept(this, newContext);
            jsBlock.addStatement(AstUtil.convertToStatement(jsNode));
        }
        return jsBlock;
    }

    @Override
    @NotNull
    public JsNode visitReturnExpression(@NotNull JetReturnExpression jetReturnExpression,
                                        @NotNull TranslationContext context) {
        JetExpression returnedExpression = jetReturnExpression.getReturnedExpression();
        if (returnedExpression != null) {
            JsExpression jsExpression = AstUtil.convertToExpression(returnedExpression.accept(this, context));
            return new JsReturn(jsExpression);
        }
        return new JsReturn();
    }

    @Override
    @NotNull
    public JsNode visitParenthesizedExpression(@NotNull JetParenthesizedExpression expression,
                                               @NotNull TranslationContext context) {
        JetExpression expressionInside = expression.getExpression();
        if (expressionInside != null) {
            return expressionInside.accept(this, context);
        }
        return context.program().getEmptyStmt();
    }

    @Override
    @NotNull
    public JsNode visitBinaryExpression(@NotNull JetBinaryExpression expression,
                                        @NotNull TranslationContext context) {
        JsInvocation setterCall = translateAsSetterCall(expression, context);
        if (setterCall != null) {
            return setterCall;
        }
        return translateAsBinaryOperation(expression, context);
    }

    @Nullable
    public JsInvocation translateAsSetterCall(@NotNull JetBinaryExpression expression,
                                              @NotNull TranslationContext context) {
        JetToken jetOperationToken = (JetToken)expression.getOperationToken();
        if (!OperationTranslator.isAssignment(jetOperationToken)) {
            return null;
        }
        PropertyAccessTranslator translator = new PropertyAccessTranslator(context);
        JetExpression leftExpression = expression.getLeft();
        JsInvocation setterCall = translator.resolveAsPropertySet(leftExpression);
        if (setterCall == null) {
            return null;
        }
        JetExpression rightExpression = expression.getRight();
        assert rightExpression != null : "Selector should not be null";
        JsExpression right = AstUtil.convertToExpression(rightExpression.accept(this, context));
        setterCall.setArguments(Arrays.asList(right));
        return setterCall;
    }

    @NotNull
    private JsNode translateAsBinaryOperation(@NotNull JetBinaryExpression expression, @NotNull TranslationContext context) {
        JsExpression left = AstUtil.convertToExpression(expression.getLeft().accept(this, context));
        JetExpression rightExpression = expression.getRight();
        assert rightExpression != null : "Selector should not be null";
        JsExpression right = AstUtil.convertToExpression(rightExpression.accept(this, context));
        JetToken jetOperationToken = (JetToken)expression.getOperationToken();
        return new JsBinaryOperation(OperationTranslator.getBinaryOperator(jetOperationToken), left, right);
    }

    //TODO correct look-up logic
    @Override
    @NotNull
    public JsNode visitSimpleNameExpression(JetSimpleNameExpression expression, TranslationContext context) {
        String referencedName = expression.getReferencedName();
        JsName jsName = context.enclosingScope().findExistingName(referencedName);
        if (jsName == null) {
            throw new AssertionError("Unindentified name " + expression.getReferencedName());
        }
        if (context.namespaceScope().ownsName(jsName)) {
            return context.getNamespaceQualifiedReference(jsName);
        }
        return jsName.makeRef();
    }

    @Override
    @NotNull
    // assume it is a local variable declaration
    public JsNode visitProperty(JetProperty expression, TranslationContext context) {
            JsName jsPropertyName = context.declareLocalName(getPropertyName(expression));
            JsExpression jsInitExpression = translateInitializer(expression, context);
            return AstUtil.newVar(jsPropertyName, jsInitExpression);
    }

    //TODO duplicate code translateInitializer 2
    @Nullable
    private JsExpression translateInitializer(JetProperty declaration, TranslationContext context) {
        JsExpression jsInitExpression = null;
        JetExpression initializer = declaration.getInitializer();
        if (initializer != null) {
            jsInitExpression = AstUtil.convertToExpression(
                (new ExpressionTranslator(context)).translate(initializer));
        }
        return jsInitExpression;
    }


    @Override
    @NotNull
    public JsNode visitCallExpression(JetCallExpression expression, TranslationContext context) {
        JsExpression callee = getCallee(expression, context);
        List<JsExpression> arguments = generateArgumentList(expression.getValueArguments(), context);

        if (isConstructorInvocation(expression, context)) {
            JsNew constructorCall = new JsNew(callee);
            constructorCall.setArguments(arguments);
            return constructorCall;
        }
        return AstUtil.newInvocation(callee, arguments);
    }

    private boolean isConstructorInvocation(@NotNull JetCallExpression expression,
                                            @NotNull TranslationContext context) {
        ResolvedCall<?> resolvedCall =
                (context.bindingContext().get(BindingContext.RESOLVED_CALL, expression.getCalleeExpression()));
        if (resolvedCall == null) {
            return false;
        }
        CallableDescriptor descriptor = resolvedCall.getCandidateDescriptor();
        return (descriptor instanceof ConstructorDescriptor);
    }

    @NotNull
    private JsExpression getCallee(@NotNull JetCallExpression expression, @NotNull TranslationContext context) {
        JetExpression jetCallee = expression.getCalleeExpression();
        if (jetCallee == null) {
            throw new AssertionError("Call expression with no callee encountered!");
        }
        JsNode jsCallee = jetCallee.accept(this, context);
        return AstUtil.convertToExpression(jsCallee);
    }

    @NotNull
    private List<JsExpression> generateArgumentList(@NotNull List<? extends ValueArgument> jetArguments,
                                                    @NotNull TranslationContext context) {
        List<JsExpression> jsArguments = new ArrayList<JsExpression>();
        for (ValueArgument argument : jetArguments) {
            jsArguments.add(translateArgument(context, argument));
        }
        return jsArguments;
    }

    @NotNull
    private JsExpression translateArgument(@NotNull TranslationContext context, @NotNull ValueArgument argument) {
        JetExpression jetExpression = argument.getArgumentExpression();
        if (jetExpression == null) {
            throw new AssertionError("Argument with no expression encountered!");
        }
        return AstUtil.convertToExpression(jetExpression.accept(this, context));
    }

    @Override
    @NotNull
    public JsNode visitIfExpression(@NotNull JetIfExpression expression, @NotNull TranslationContext context) {
        JsIf result = new JsIf();
        result.setIfExpr(translateConditionExpression(expression.getCondition(), context));
        result.setThenStmt(translateNullableExpressionToNotNullStatement(expression.getThen(), context));
        result.setElseStmt(translateElseStatement(expression, context));
        return result;
    }

    @NotNull
    private JsExpression translateConditionExpression(@Nullable JetExpression expression,
                                                      @NotNull TranslationContext context) {
        JsNode jsCondition = translateNullableExpression(expression, context);
        if (jsCondition == context.program().getEmptyStmt()) {
            throw new AssertionError("Empty condition clause!");
        }
        return AstUtil.convertToExpression(jsCondition);
    }

    @NotNull
    private JsStatement translateNullableExpressionToNotNullStatement(@Nullable JetExpression expression,
                                                                      @NotNull TranslationContext context) {
        return AstUtil.convertToStatement(translateNullableExpression(expression, context));
    }

    @Nullable
    private JsStatement translateElseStatement(@NotNull JetIfExpression expression,
                                               @NotNull TranslationContext context) {
        if (expression.getElse() == null) {
            return null;
        }
        return AstUtil.convertToStatement(translateNullableExpression(expression.getElse(), context));
    }

    @NotNull
    private JsNode translateNullableExpression(@Nullable JetExpression expression,
                                               @NotNull TranslationContext context) {
        if (expression == null) {
            return context.program().getEmptyStmt();
        }
        return expression.accept(this, context);
    }

    @Override
    @NotNull
    public JsNode visitWhileExpression(@NotNull JetWhileExpression expression, @NotNull TranslationContext context) {
        JsWhile result = new JsWhile();
        result.setCondition(translateConditionExpression(expression.getCondition(), context));
        result.setBody(translateNullableExpressionToNotNullStatement(expression.getBody(), context));
        return result;
    }

    @Override
    @NotNull
    public JsNode visitDoWhileExpression(@NotNull JetDoWhileExpression expression, @NotNull TranslationContext context) {
        JsDoWhile result = new JsDoWhile();
        result.setCondition(translateConditionExpression(expression.getCondition(), context));
        result.setBody(translateNullableExpressionToNotNullStatement(expression.getBody(), context));
        return result;
    }

    @Override
    @NotNull
    public JsNode visitStringTemplateExpression(@NotNull JetStringTemplateExpression expression,
                                      @NotNull TranslationContext context) {
        JsStringLiteral stringLiteral = resolveAsStringConstant(expression, context);
        if (stringLiteral != null) {
            return stringLiteral;
        }
        throw new AssertionError("String templates not supported!");
    }

    @Nullable
    private JsStringLiteral resolveAsStringConstant(@NotNull JetStringTemplateExpression expression,
                                           @NotNull TranslationContext context) {
        CompileTimeConstant<?> compileTimeValue =
                context.bindingContext().get(BindingContext.COMPILE_TIME_VALUE, expression);
        if (compileTimeValue != null) {
            Object value = compileTimeValue.getValue();
            assert value instanceof String : "Compile time constant template should be a String constant.";
            String constantString = (String)value;
            return context.program().getStringLiteral(constantString);
        }
        return null;
    }

    //TODO method too long
    @Override
    @NotNull
    public JsNode visitDotQualifiedExpression(@NotNull JetDotQualifiedExpression expression,
                                      @NotNull TranslationContext context) {
        JsInvocation getterCall = translateAsGetterCall(expression, context);
        if (getterCall != null) {
            return getterCall;
        }
        return translateAsQualifiedExpression(expression, context);
    }

    @Nullable
    private JsInvocation translateAsGetterCall(@NotNull JetDotQualifiedExpression expression,
                                               @NotNull TranslationContext context) {
        JsInvocation result;
        PropertyAccessTranslator translator = new PropertyAccessTranslator(context);
        result = translator.resolveAsPropertyGet(expression);
        return result;
    }

    @NotNull
    private JsNode translateAsQualifiedExpression(@NotNull JetDotQualifiedExpression expression,
                                                  @NotNull TranslationContext context) {
        JsExpression receiver = AstUtil.convertToExpression(expression.getReceiverExpression().accept(this, context));
        JetExpression jetSelector = expression.getSelectorExpression();
        assert jetSelector != null : "Selector should not be null in dot qualified expression.";
        JsExpression selector = AstUtil.convertToExpression(jetSelector.accept(this, context));
        assert (selector instanceof JsNameRef || selector instanceof JsInvocation)
                : "Selector should be a name reference or a method invocation in dot qualified expression.";
        if (selector instanceof JsInvocation) {
            return translateAsQualifiedInvocation(receiver, (JsInvocation) selector);
        } else {
            return translateAsQualifiedNameReference(receiver, (JsNameRef) selector);
        }
    }

    @NotNull
    private JsNode translateAsQualifiedNameReference(@NotNull JsExpression receiver, @NotNull JsNameRef selector) {
        JsNameRef result = (JsNameRef)selector;
        result.setQualifier(receiver);
        return result;
    }

    @NotNull
    private JsNode translateAsQualifiedInvocation(@NotNull JsExpression receiver, @NotNull JsInvocation selector) {
        JsInvocation result = (JsInvocation)selector;
        JsExpression qualifier = result.getQualifier();
        JsNameRef nameRef = (JsNameRef)qualifier;
        nameRef.setQualifier(receiver);
        return result;
    }


}
