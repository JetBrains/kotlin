package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.psi.JetPropertyAccessor;

import java.util.ArrayList;
import java.util.Collections;
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
        } else {
            if (expression.isVar()) {
                methods.add(generateDefaultSetter(expression, context));
            }
        }
        if (expression.getGetter() != null) {
            methods.addAll(expression.getGetter().accept(this, context));
        } else {
            methods.add(generateDefaultGetter(expression, context));
        }
        return methods;
    }

    @NotNull
    private JsPropertyInitializer generateDefaultGetter(JetProperty expression, TranslationContext context) {
        return AstUtil.newNamedMethod
                (Namer.getNameForGetter(expression.getName()), generateDefaultGetterFunction(expression, context));
    }

    @NotNull
    private JsFunction generateDefaultGetterFunction(JetProperty expression, TranslationContext context) {
        JsNameRef backingFieldRef = Namer.getBackingFieldNameForProperty(expression.getName()).makeRef();
        JsReturn returnExpression = new JsReturn(backingFieldRef);
        return AstUtil.newFunction
                (context.enclosingScope(), null, Collections.EMPTY_LIST, AstUtil.convertToBlock(returnExpression));
    }


    @NotNull
    private JsPropertyInitializer generateDefaultSetter(JetProperty expression, TranslationContext context) {
        return AstUtil.newNamedMethod(
                Namer.getNameForAccessor(expression.getName(), null), generateDefaultSetterFunction(expression, context));
    }

    @NotNull
    private JsFunction generateDefaultSetterFunction(JetProperty expression, TranslationContext context) {
        JsFunction result = new JsFunction(context.enclosingScope());
        JsParameter defaultParameter = new JsParameter(context.enclosingScope().declareFreshName(Namer.DEFAULT_SETTER_PARAM_NAME));
        JsBinaryOperation assignExpression = new JsBinaryOperation(JsBinaryOperator.ASG);
        assignExpression.setArg1(Namer.getBackingFieldNameForProperty(expression).makeRef());
        assignExpression.setArg2(defaultParameter.getName().makeRef());
        return result;
    }

    @Override
    @NotNull
    public List<JsPropertyInitializer> visitPropertyAccessor
            (JetPropertyAccessor expression, TranslationContext context) {
        List<JsPropertyInitializer> methods = new ArrayList<JsPropertyInitializer>();
        JsPropertyInitializer namedMethod = generateMethodForAccessor(expression, context);
        methods.add(namedMethod);
        return methods;
    }

    private JsPropertyInitializer generateMethodForAccessor(JetPropertyAccessor expression, TranslationContext context) {
        JsFunction methodBody = new JsFunction(context.enclosingScope());
        ExpressionTranslator expressionTranslator =
                new ExpressionTranslator(context.newFunction(methodBody));
        JsBlock methodBodyBlock = AstUtil.convertToBlock(expressionTranslator.translate(expression.getBodyExpression()));
        methodBody.setBody(methodBodyBlock);
        //TODO figure out naming pattern
        return AstUtil.newNamedMethod(Namer.getNameForAccessor("nameForAccessor", expression), methodBody);
    }


}
