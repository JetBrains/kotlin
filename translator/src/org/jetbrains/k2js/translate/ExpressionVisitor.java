package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsBlock;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.backend.js.ast.JsReturn;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lexer.JetToken;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Talanov Pavel
 */
public final class ExpressionVisitor extends TranslatorVisitor<JsNode> {


    //TODO method too long
    @Override
    @NotNull
    public JsNode visitConstantExpression(@NotNull JetConstantExpression expression, @NotNull TranslationContext context) {
        JsExpression result = null;
        Object value;
        CompileTimeConstant<?> compileTimeValue = context.bindingContext().get(BindingContext.COMPILE_TIME_VALUE, expression);
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
        JsExpression left = AstUtil.convertToExpression(expression.getLeft().accept(this, context));
        JetExpression rightExpression = expression.getRight();
        if (rightExpression == null) {
            throw new AssertionError("BinaryExpression with no right parameter");
        }
        JsExpression right = AstUtil.convertToExpression(rightExpression.accept(this, context));
        //TODO cast dangerous?
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
        ConstructorDescriptor constructorDescriptor = (context.bindingContext().get(BindingContext.CONSTRUCTOR, expression.getCalleeExpression()));
        JetExpression calleeExpression = expression.getCalleeExpression();
        if (constructorDescriptor == null) {
            JsExpression callee = getCallee(expression, context);
            List<JsExpression> arguments = generateArgumentList(expression.getValueArguments(), context);
            return AstUtil.newInvocation(callee, arguments);
        }
        throw new AssertionError("Should generate constructor invocation");
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
        result.setIfExpr(translateIfExpression(expression, context));
        result.setThenStmt(translateThenStatement(expression, context));
        result.setElseStmt(translateElseStatement(expression, context));
        return result;
    }

    @NotNull
    private JsExpression translateIfExpression(@NotNull JetIfExpression expression,
                                               @NotNull TranslationContext context) {
        JsNode jsCondition = translateNullableExpression(expression.getCondition(), context);
        if (jsCondition == context.program().getEmptyStmt()) {
            throw new AssertionError("Empty condition in if clause!");
        }
        return AstUtil.convertToExpression(jsCondition);
    }

    @NotNull
    private JsStatement translateThenStatement(@NotNull JetIfExpression expression,
                                               @NotNull TranslationContext context) {
        return AstUtil.convertToStatement(translateNullableExpression(expression.getThen(), context));
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

}
