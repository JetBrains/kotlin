package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsBlock;
import com.google.dart.compiler.backend.js.ast.JsFunction;
import com.google.dart.compiler.backend.js.ast.JsPropertyInitializer;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Talanov Pavel
 */
public class ClassBodyVisitor extends K2JsVisitor<List<JsPropertyInitializer>> {

    @Override
    @NotNull
    public List<JsPropertyInitializer> visitClass(JetClass expression, TranslationContext context) {
        List<JsPropertyInitializer> properties = new ArrayList<JsPropertyInitializer>();
        for (JetDeclaration declaration : expression.getDeclarations()) {
            properties.addAll(declaration.accept(this, context));
        }
        return properties;
    }

    @Override
    @NotNull
    public List<JsPropertyInitializer> visitProperty(JetProperty expression, TranslationContext context) {
        List<JsPropertyInitializer> methods = new ArrayList<JsPropertyInitializer>();
        if (expression.getSetter() != null) {
            methods.addAll(expression.getSetter().accept(this, context));
        }
        if (expression.getGetter() != null) {
            methods.addAll(expression.getGetter().accept(this, context));
        }
        return methods;
    }

    @Override
    @NotNull
    public List<JsPropertyInitializer> visitPropertyAccessor
            (JetPropertyAccessor expression, TranslationContext context) {
        List<JsPropertyInitializer> methods = new ArrayList<JsPropertyInitializer>();
        JsPropertyInitializer namedMethod = new JsPropertyInitializer();
        // TODO
        namedMethod.setLabelExpr(Namer.getNameForAccessor("nameForAccessor", expression).makeRef());
        JsFunction methodBody = new JsFunction(context.scope());
        ExpressionTranslator expressionTranslator =
                new ExpressionTranslator(context.newScope(methodBody.getScope()));
        JsBlock methodBlock = AstUtil.convertToBlock(expressionTranslator.translate(expression.getBodyExpression()));
        methodBody.setBody(methodBlock);
        namedMethod.setValueExpr(methodBody);
        methods.add(namedMethod);
        return methods;
    }


}
