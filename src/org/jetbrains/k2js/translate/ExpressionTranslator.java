package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import groovyjarjarantlr.collections.AST;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lexer.JetToken;

import java.util.List;

/**
 * Talanov Pavel
 */
class ExpressionTranslator extends AbstractTranslator {

    public ExpressionTranslator(TranslationContext context) {
        super(context);
    }

    @NotNull
    public JsNode translate(JetExpression jetExpression) {
        assert jetExpression != null;
        if (jetExpression instanceof JetConstantExpression) {
            return translateConstantExpression((JetConstantExpression) jetExpression);
        }
        if (jetExpression instanceof JetReturnExpression) {
            return translateReturn((JetReturnExpression) jetExpression);
        }
        if (jetExpression instanceof JetBlockExpression) {
            return translateBlock((JetBlockExpression) jetExpression);
        }
        if (jetExpression instanceof JetBinaryExpression) {
            return translateBinaryExpression((JetBinaryExpression)jetExpression);
        }
        if (jetExpression instanceof JetParenthesizedExpression) {
            return translate(((JetParenthesizedExpression)(jetExpression)).getExpression());
        }
        if (jetExpression instanceof JetProperty) {
            DeclarationTranslator declarationTranslator = new DeclarationTranslator(translationContext());
            return declarationTranslator.translateProperty((JetProperty)jetExpression);
        }
        if (jetExpression instanceof JetSimpleNameExpression) {
            return translateSimpleNameExpression((JetSimpleNameExpression)jetExpression);
        }
        throw new RuntimeException("Unexpected expression encountered:" + jetExpression.toString());
    }

    private JsNameRef translateSimpleNameExpression(JetSimpleNameExpression jetSimpleNameExpression) {
        String referencedName = jetSimpleNameExpression.getReferencedName();
        return new JsNameRef(getJSName(referencedName));
    }

    @NotNull
    private JsExpression translateConstantExpression(JetConstantExpression expression) {
        JsExpression result = null;
        Object value;
        CompileTimeConstant<?> compileTimeValue = bindingContext().get(BindingContext.COMPILE_TIME_VALUE, expression);
        assert compileTimeValue != null;
        value = compileTimeValue.getValue();
        if (value instanceof Integer) {
            result = program().getNumberLiteral((Integer) value);
        }
        assert result != null;
        return result;
    }

    @NotNull
    private JsReturn translateReturn(JetReturnExpression jetReturnExpression) {
        JsExpression jsExpression = (JsExpression) translate(jetReturnExpression.getReturnedExpression());
        JsReturn jsReturnExpression = new JsReturn(jsExpression);
        return jsReturnExpression;
    }

    @NotNull
    private JsBlock translateBlock(JetBlockExpression jetBlock) {
        List<JetElement> jetElements = jetBlock.getStatements();
        JsBlock jsBlock = new JsBlock();
        ExpressionTranslator expressionTranslator =
                new ExpressionTranslator(translationContext().newBlock(jsBlock));
        for (JetElement jetElement : jetElements) {
            //TODO hack alert
            JetExpression jetExpression = (JetExpression) jetElement;
            JsNode jsNode = expressionTranslator.translate(jetExpression);
            jsBlock.addStatement(AstUtil.convertToStatement(jsNode));
        }
        return jsBlock;
    }

    private JsBinaryOperation translateBinaryExpression(JetBinaryExpression jetBinaryExpression) {
        JsExpression left = AstUtil.convertToExpression(translate(jetBinaryExpression.getLeft()));
        JsExpression right = AstUtil.convertToExpression(translate(jetBinaryExpression.getRight()));
        //TODO cast dangerous?
        JetToken jetOperationToken = (JetToken)jetBinaryExpression.getOperationToken();
        JsBinaryOperation jsBinaryOperation =
                new JsBinaryOperation(OperationTranslator.getBinaryOperator(jetOperationToken), left, right);
        return jsBinaryOperation;
    }

}
