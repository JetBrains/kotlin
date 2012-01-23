package org.jetbrains.k2js.translate.expression;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.constants.NullValue;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.general.TranslatorVisitor;
import org.jetbrains.k2js.translate.operation.BinaryOperationTranslator;
import org.jetbrains.k2js.translate.operation.IncrementTranslator;
import org.jetbrains.k2js.translate.operation.UnaryOperationTranslator;
import org.jetbrains.k2js.translate.reference.AccessTranslator;
import org.jetbrains.k2js.translate.reference.CallTranslator;
import org.jetbrains.k2js.translate.reference.ReferenceTranslator;
import org.jetbrains.k2js.translate.utils.BindingUtils;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import java.util.List;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getCompileTimeValue;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getDescriptorForReferenceExpression;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.translateInitializerForProperty;

/**
 * @author Pavel Talanov
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
        TranslationContext blockContext = context.innerBlock(jsBlock);
        for (JetElement statement : statements) {
            assert statement instanceof JetExpression : "Elements in JetBlockExpression " +
                    "should be of type JetExpression";
            JsNode jsNode = statement.accept(this, blockContext);
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
        JsName jsPropertyName = context.declareLocalVariable(expression);
        JsExpression jsInitExpression = translateInitializerForProperty(expression, context);
        return AstUtil.newVar(jsPropertyName, jsInitExpression);
    }

    @Override
    @NotNull
    public JsNode visitCallExpression(@NotNull JetCallExpression expression,
                                      @NotNull TranslationContext context) {
        return CallTranslator.translate(expression, null, context);
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
        assert (jsCondition != null) : "Condition should not be empty";
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
        return resolveAsTemplate(expression, context);
    }

    @NotNull
    private JsNode resolveAsTemplate(@NotNull JetStringTemplateExpression expression,
                                     @NotNull TranslationContext context) {
        return StringTemplateTranslator.translate(expression, context);
    }

    @Nullable
    private JsStringLiteral resolveAsStringConstant(@NotNull JetExpression expression,
                                                    @NotNull TranslationContext context) {
        Object value = getCompileTimeValue(context.bindingContext(), expression);
        if (value == null) {
            return null;
        }
        assert value instanceof String : "Compile time constant template should be a String constant.";
        String constantString = (String) value;
        return context.program().getStringLiteral(constantString);
    }

    @Override
    @NotNull
    public JsNode visitDotQualifiedExpression(@NotNull JetDotQualifiedExpression expression,
                                              @NotNull TranslationContext context) {
        return QualifiedExpressionTranslator.translateDotQualifiedExpression(expression, context);
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
        return IncrementTranslator.translate(expression, context);
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
        return QualifiedExpressionTranslator.translateSafeQualifiedExpression(expression, context);
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
        return Translation.functionTranslator(expression, context).translateAsLiteral();
    }

    @Override
    @NotNull
    public JsNode visitThisExpression(@NotNull JetThisExpression expression,
                                      @NotNull TranslationContext context) {
        DeclarationDescriptor descriptor =
                getDescriptorForReferenceExpression(context.bindingContext(), expression.getInstanceReference());
        return TranslationUtils.getThisObject(context, descriptor);
    }

    @Override
    @NotNull
    public JsNode visitArrayAccessExpression(@NotNull JetArrayAccessExpression expression,
                                             @NotNull TranslationContext context) {
        return AccessTranslator.translateAsGet(expression, context);
    }

    @Override
    @NotNull
    public JsNode visitForExpression(@NotNull JetForExpression expression,
                                     @NotNull TranslationContext context) {
        return ForTranslator.translate(expression, context);
    }

    @Override
    @NotNull
    public JsNode visitTryExpression(@NotNull JetTryExpression expression,
                                     @NotNull TranslationContext context) {
        return TryTranslator.translate(expression, context);
    }
}