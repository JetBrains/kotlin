package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lexer.JetToken;

import java.util.List;

/**
 * @author Talanov Pavel
 */
public class ExpressionVisitor extends K2JsVisitor<JsNode>  {

    @Override
    @NotNull
    public JsNode visitConstantExpression(JetConstantExpression expression, TranslationContext context) {
        JsExpression result = null;
        Object value;
        CompileTimeConstant<?> compileTimeValue = context.bindingContext().get(BindingContext.COMPILE_TIME_VALUE, expression);
        assert compileTimeValue != null;
        value = compileTimeValue.getValue();
        if (value instanceof Integer) {
            result = context.program().getNumberLiteral((Integer) value);
        }
        assert result != null;
        return result;
    }

    @Override
    @NotNull
    public JsNode visitBlockExpression(JetBlockExpression jetBlock, TranslationContext context) {
        List<JetElement> jetElements = jetBlock.getStatements();
        JsBlock jsBlock = new JsBlock();
        ExpressionTranslator expressionTranslator =
                new ExpressionTranslator(context.newBlock(jsBlock));
        for (JetElement jetElement : jetElements) {
            //TODO hack alert
            JetExpression jetExpression = (JetExpression) jetElement;
            JsNode jsNode = jetExpression.accept(this, context.newBlock(jsBlock));
            jsBlock.addStatement(AstUtil.convertToStatement(jsNode));
        }
        assert jsBlock != null;
        return jsBlock;
    }

    @Override
    @NotNull
    public JsNode visitReturnExpression(JetReturnExpression jetReturnExpression, TranslationContext context) {
        //TODO hack alert
        JsExpression jsExpression = (JsExpression) (jetReturnExpression.getReturnedExpression().accept(this, context));
        JsReturn jsReturnExpression = new JsReturn(jsExpression);
        return jsReturnExpression;
    }

    @Override
    @NotNull
    public JsNode visitParenthesizedExpression(JetParenthesizedExpression expression, TranslationContext context) {
        return (expression.getExpression()).accept(this, context);
    }

    @Override
    @NotNull
    public JsNode visitBinaryExpression(JetBinaryExpression expression, TranslationContext context) {
        JsExpression left = AstUtil.convertToExpression(expression.getLeft().accept(this, context));
        JsExpression right = AstUtil.convertToExpression(expression.getRight().accept(this, context));
        //TODO cast dangerous?
        JetToken jetOperationToken = (JetToken)expression.getOperationToken();
        JsBinaryOperation jsBinaryOperation =
                new JsBinaryOperation(OperationTranslator.getBinaryOperator(jetOperationToken), left, right);
        return jsBinaryOperation;
    }

    @Override
    @NotNull
    public JsNode visitSimpleNameExpression(JetSimpleNameExpression expression, TranslationContext context) {
        String referencedName = expression.getReferencedName();
        return new JsNameRef(context.getJSName(referencedName));
    }

    @Override
    @NotNull
    public JsNode visitProperty(JetProperty expression, TranslationContext context) {
        DeclarationTranslator translator = new DeclarationTranslator(context);
        return translator.translateProperty(expression);
    }

}
