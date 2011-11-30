package org.jetbrains.k2js.translate.expression;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.constants.NullValue;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.general.TranslationContext;
import org.jetbrains.k2js.translate.general.TranslatorVisitor;
import org.jetbrains.k2js.translate.operation.BinaryOperationTranslator;
import org.jetbrains.k2js.translate.operation.UnaryOperationTranslator;
import org.jetbrains.k2js.translate.reference.PropertyAccessTranslator;
import org.jetbrains.k2js.translate.reference.ReferenceTranslator;
import org.jetbrains.k2js.translate.utils.BindingUtils;

import java.util.List;

import static org.jetbrains.k2js.translate.utils.TranslationUtils.*;

/**
 * @author Talanov Pavel
 */
public final class ExpressionVisitor extends TranslatorVisitor<JsNode> {

    @NotNull
    private JsExpression translateAsExpression(@NotNull JetExpression expression,
                                               @NotNull TranslationContext context) {
        return AstUtil.convertToExpression(expression.accept(this, context));
    }

    @Override
    @NotNull
    public JsNode visitConstantExpression(@NotNull JetConstantExpression expression,
                                          @NotNull TranslationContext context) {
        CompileTimeConstant<?> compileTimeValue =
                context.bindingContext().get(BindingContext.COMPILE_TIME_VALUE, expression);
        assert compileTimeValue != null;
        if (compileTimeValue instanceof NullValue) {
            return context.program().getNullLiteral();
        }
        Object value = compileTimeValue.getValue();
        if (value instanceof Integer) {
            return context.program().getNumberLiteral((Integer) value);
        }
        if (value instanceof Boolean) {
            return context.program().getBooleanLiteral((Boolean) value);
        }
        throw new AssertionError("Unsupported constant expression" + expression.toString());
    }

    @Override
    @NotNull
    public JsNode visitBlockExpression(@NotNull JetBlockExpression jetBlock, @NotNull TranslationContext context) {
        List<JetElement> statements = jetBlock.getStatements();
        JsBlock jsBlock = new JsBlock();
        ;
        for (JetElement statement : statements) {
            assert statement instanceof JetExpression : "Elements in JetBlockExpression " +
                    "should be of type JetExpression";
            JsNode jsNode = statement.accept(this, context);
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
            JsExpression jsExpression = translateAsExpression(returnedExpression, context);
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
        return BinaryOperationTranslator.translate(expression, context);
    }

    @Override
    @NotNull
    // assume it is a local variable declaration
    public JsNode visitProperty(@NotNull JetProperty expression, @NotNull TranslationContext context) {
        JsName jsPropertyName = context.enclosingScope().declareName(getPropertyName(expression));
        JsExpression jsInitExpression = translateInitializerForProperty(expression, context);
        return AstUtil.newVar(jsPropertyName, jsInitExpression);
    }

    @Override
    @NotNull
    public JsNode visitCallExpression(JetCallExpression expression, TranslationContext context) {
        JsExpression callee = translateCallee(expression, context);
        List<JsExpression> arguments = translateArgumentList(expression.getValueArguments(), context);
        if (isConstructorInvocation(context, expression)) {
            JsNew constructorCall = new JsNew(callee);
            constructorCall.setArguments(arguments);
            return constructorCall;
        }
        return AstUtil.newInvocation(callee, arguments);
    }

    @NotNull
    private JsExpression translateCallee(@NotNull JetCallExpression expression, @NotNull TranslationContext context) {
        JetExpression callee = expression.getCalleeExpression();
        if (callee == null) {
            throw new AssertionError("Call expression with no callee encountered!");
        }
        return translateAsExpression(callee, context);
    }

    @Override
    @NotNull
    public JsNode visitIfExpression(@NotNull JetIfExpression expression, @NotNull TranslationContext context) {
        boolean isStatement = BindingUtils.isStatement(context.bindingContext(), expression);
        if (isStatement) {
            return translateAsIfStatement(expression, context);
        } else {
            return translateAsConditionalExpression(expression, context);
        }
    }

    @Override
    @NotNull
    public JsNode visitSimpleNameExpression(@NotNull JetSimpleNameExpression expression,
                                            @NotNull TranslationContext context) {
        return ReferenceTranslator.translateSimpleName(expression, context);
    }


    @NotNull
    private JsIf translateAsIfStatement(@NotNull JetIfExpression expression,
                                        @NotNull TranslationContext context) {
        JsIf result = new JsIf();
        result.setIfExpr(translateConditionExpression(expression.getCondition(), context));
        result.setThenStmt(translateNullableExpressionAsNotNullStatement(expression.getThen(), context));
        result.setElseStmt(translateElseAsStatement(expression, context));
        return result;
    }

    @Nullable
    private JsStatement translateElseAsStatement(@NotNull JetIfExpression expression,
                                                 @NotNull TranslationContext context) {
        JetExpression jetElse = expression.getElse();
        if (jetElse == null) {
            return null;
        }
        return AstUtil.convertToStatement(jetElse.accept(this, context));
    }

    @NotNull
    private JsConditional translateAsConditionalExpression(@NotNull JetIfExpression expression,
                                                           @NotNull TranslationContext context) {
        JsConditional result = new JsConditional();
        result.setTestExpression(translateConditionExpression(expression.getCondition(), context));
        result.setThenExpression(translateNullableExpression(expression.getThen(), context));
        result.setElseExpression(translateNullableExpression(expression.getElse(), context));
        return result;
    }

    @NotNull
    private JsStatement translateNullableExpressionAsNotNullStatement(@Nullable JetExpression nullableExpression,
                                                                      @NotNull TranslationContext context) {
        if (nullableExpression == null) {
            return context.program().getEmptyStmt();
        }
        return AstUtil.convertToStatement(nullableExpression.accept(this, context));
    }

    @NotNull
    private JsExpression translateConditionExpression(@Nullable JetExpression expression,
                                                      @NotNull TranslationContext context) {
        JsExpression jsCondition = translateNullableExpression(expression, context);
        if (jsCondition == null) {
            throw new AssertionError("Empty condition!");
        }
        return AstUtil.convertToExpression(jsCondition);
    }

    @Nullable
    private JsExpression translateNullableExpression(@Nullable JetExpression expression,
                                                     @NotNull TranslationContext context) {
        if (expression == null) {
            return null;
        }
        return translateAsExpression(expression, context);
    }

    @Override
    @NotNull
    public JsNode visitWhileExpression(@NotNull JetWhileExpression expression, @NotNull TranslationContext context) {
        JsWhile result = new JsWhile();
        result.setCondition(translateConditionExpression(expression.getCondition(), context));
        result.setBody(translateNullableExpressionAsNotNullStatement(expression.getBody(), context));
        return result;
    }

    @Override
    @NotNull
    public JsNode visitDoWhileExpression(@NotNull JetDoWhileExpression expression, @NotNull TranslationContext context) {
        JsDoWhile result = new JsDoWhile();
        result.setCondition(translateConditionExpression(expression.getCondition(), context));
        result.setBody(translateNullableExpressionAsNotNullStatement(expression.getBody(), context));
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
            String constantString = (String) value;
            return context.program().getStringLiteral(constantString);
        }
        return null;
    }

    @Override
    @NotNull
    public JsNode visitDotQualifiedExpression(@NotNull JetDotQualifiedExpression expression,
                                              @NotNull TranslationContext context) {
        return translateQualifiedExpression(expression, context);
    }

    @NotNull
    private JsExpression translateQualifiedExpression(@NotNull JetQualifiedExpression expression,
                                                      @NotNull TranslationContext context) {
        if (PropertyAccessTranslator.canBePropertyGetterCall(expression, context)) {
            return PropertyAccessTranslator.translateAsPropertyGetterCall(expression, context);
        }
        return translateAsQualifiedAccess(expression, context);
    }

    @NotNull
    private JsExpression translateAsQualifiedAccess(@NotNull JetQualifiedExpression expression,
                                                    @NotNull TranslationContext context) {
        JsExpression receiver = translateReceiver(expression, context);
        JsExpression selector = translateSelector(expression, context);
        assert (selector instanceof JsNameRef || selector instanceof JsInvocation)
                : "Selector should be a name reference or a method invocation in dot qualified expression.";
        if (selector instanceof JsInvocation) {
            return translateAsQualifiedInvocation(receiver, (JsInvocation) selector);
        } else {
            return translateAsQualifiedNameReference(receiver, (JsNameRef) selector);
        }
    }

    @NotNull
    private JsExpression translateSelector(@NotNull JetQualifiedExpression expression,
                                           @NotNull TranslationContext context) {
        JetExpression jetSelector = expression.getSelectorExpression();
        assert jetSelector != null : "Selector should not be null in dot qualified expression.";
        return translateAsExpression(jetSelector, context);
    }

    @NotNull
    private JsExpression translateReceiver(@NotNull JetQualifiedExpression expression,
                                           @NotNull TranslationContext context) {
        return translateAsExpression(expression.getReceiverExpression(), context);
    }

    @NotNull
    private JsExpression translateAsQualifiedNameReference(@NotNull JsExpression receiver, @NotNull JsNameRef selector) {
        selector.setQualifier(receiver);
        return selector;
    }

    @NotNull
    private JsExpression translateAsQualifiedInvocation(@NotNull JsExpression receiver, @NotNull JsInvocation selector) {
        JsExpression qualifier = selector.getQualifier();
        JsNameRef nameRef = (JsNameRef) qualifier;
        nameRef.setQualifier(receiver);
        return selector;
    }

    @Override
    @NotNull
    public JsNode visitPrefixExpression(@NotNull JetPrefixExpression expression,
                                        @NotNull TranslationContext context) {
        return UnaryOperationTranslator.translate(expression, context);

    }

    @Override
    @NotNull
    public JsNode visitPostfixExpression(@NotNull JetPostfixExpression expression,
                                         @NotNull TranslationContext context) {
        return UnaryOperationTranslator.translate(expression, context);
    }

    @Override
    @NotNull
    public JsNode visitIsExpression(@NotNull JetIsExpression expression,
                                    @NotNull TranslationContext context) {
        return Translation.patternTranslator(context).translateIsExpression(expression);
    }

    @Override
    @NotNull
    public JsNode visitSafeQualifiedExpression(@NotNull JetSafeQualifiedExpression expression,
                                               @NotNull TranslationContext context) {
        JsExpression receiver = translateReceiver(expression, context);
        JsNullLiteral nullLiteral = context.program().getNullLiteral();
        JsExpression thenExpression = translateQualifiedExpression(expression, context);
        return new JsConditional(notNullCheck(context, receiver),
                thenExpression, nullLiteral);
    }

    @Override
    @NotNull
    public JsNode visitWhenExpression(@NotNull JetWhenExpression expression,
                                      @NotNull TranslationContext context) {
        return Translation.translateWhenExpression(expression, context);
    }


    @Override
    @NotNull
    public JsNode visitBinaryWithTypeRHSExpression(@NotNull JetBinaryExpressionWithTypeRHS expression,
                                                   @NotNull TranslationContext context) {
        // we actually do not care for types in js
        return Translation.translateExpression(expression.getLeft(), context);
    }

    @Override
    @NotNull
    public JsNode visitBreakExpression(@NotNull JetBreakExpression expression,
                                       @NotNull TranslationContext context) {
        return new JsBreak();
    }

    @Override
    @NotNull
    public JsNode visitContinueExpression(@NotNull JetContinueExpression expression,
                                          @NotNull TranslationContext context) {
        return new JsContinue();
    }

    @Override
    @NotNull
    public JsNode visitFunctionLiteralExpression(@NotNull JetFunctionLiteralExpression expression,
                                                 @NotNull TranslationContext context) {
        return Translation.functionTranslator(expression.getFunctionLiteral(), context).translateAsLiteral();
    }

    @Override
    @NotNull
    public JsNode visitThisExpression(@NotNull JetThisExpression expression,
                                      @NotNull TranslationContext context) {
        if (context.aliaser().hasAliasForThis()) {
            return context.aliaser().getAliasForThis();
        }
        return new JsThisRef();
    }


}
