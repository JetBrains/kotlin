package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.ExpressionValueArgument;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lexer.JetToken;

import java.util.ArrayList;
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
        if (value instanceof Boolean) {
            return context.program().getBooleanLiteral((Boolean) value);
        }
        assert result != null;
        return result;
    }

    @Override
    @NotNull
    public JsNode visitBlockExpression(JetBlockExpression jetBlock, TranslationContext context) {
        List<JetElement> jetElements = jetBlock.getStatements();
        JsBlock jsBlock = new JsBlock();
        TranslationContext newContext = context.newBlock();
        for (JetElement jetElement : jetElements) {
            //TODO hack alert
            JetExpression jetExpression = (JetExpression) jetElement;
            JsNode jsNode = jetExpression.accept(this, newContext);
            jsBlock.addStatement(AstUtil.convertToStatement(jsNode));
        }
        assert jsBlock != null;
        return jsBlock;
    }

    @Override
    @NotNull
    public JsNode visitReturnExpression(JetReturnExpression jetReturnExpression, TranslationContext context) {
        //TODO hack alert
        if (jetReturnExpression.getReturnedExpression() != null) {
            JsExpression jsExpression = (JsExpression) (jetReturnExpression.getReturnedExpression().accept(this, context));
            JsReturn jsReturnExpression = new JsReturn(jsExpression);
            return jsReturnExpression;
        }
        return new JsReturn();
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

    //TODO think about recursive/non-recursive name look-up
    @Override
    @NotNull
    public JsNode visitSimpleNameExpression(JetSimpleNameExpression expression, TranslationContext context) {
        String referencedName = expression.getReferencedName();
        JsName jsName = context.enclosingScope().findExistingNameNoRecurse(referencedName);
        if (jsName != null) {
            return jsName.makeRef();
        }
        jsName = context.functionScope().findExistingNameNoRecurse(referencedName);
        if (jsName != null) {
            return context.getNamespaceQualifiedReference(jsName);
        }
        jsName = context.namespaceScope().findExistingNameNoRecurse(referencedName);
        if (jsName != null) {
            return context.getNamespaceQualifiedReference(jsName);
        }
        throw new AssertionError("Unindentified name " + expression.getReferencedName());
    }

    @Override
    @NotNull
    public JsNode visitProperty(JetProperty expression, TranslationContext context) {
        DeclarationTranslator translator = new DeclarationTranslator(context);
        return translator.translateProperty(expression);
    }

    @Override
    @NotNull
    public JsNode visitCallExpression(JetCallExpression expression, TranslationContext context) {
        JsExpression callee = getCallee(expression, context);
        List<JsExpression> arguments = generateArgumentList(expression.getValueArguments(), context);
        return AstUtil.newInvocation(callee, arguments);
    }

    @NotNull
    private JsExpression getCallee(JetCallExpression expression, TranslationContext context) {
        JsNode jsCallee = expression.getCalleeExpression().accept(this, context);
        return AstUtil.convertToExpression(jsCallee);
    }

    @NotNull
    private List<JsExpression> generateArgumentList(List<? extends ValueArgument> jetArguments, TranslationContext context) {
        List<JsExpression> jsArguments = new ArrayList<JsExpression>();
        for (ValueArgument argument : jetArguments) {
            JetExpression jetExpression = argument.getArgumentExpression();
            jsArguments.add(AstUtil.convertToExpression(jetExpression.accept(this, context)));
        }
        return jsArguments;
    }

}
