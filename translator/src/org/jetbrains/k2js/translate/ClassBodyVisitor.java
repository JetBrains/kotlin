package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.psi.JetPropertyAccessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Talanov Pavel
 */
public final class ClassBodyVisitor extends TranslatorVisitor<List<JsPropertyInitializer>> {

    @Override
    @NotNull
    public List<JsPropertyInitializer> visitClass(@NotNull JetClass expression, @NotNull TranslationContext context) {
        List<JsPropertyInitializer> properties = new ArrayList<JsPropertyInitializer>();
        for (JetDeclaration declaration : expression.getDeclarations()) {
            properties.addAll(declaration.accept(this, context));
        }
        return properties;
    }

    @Override
    @NotNull
    public List<JsPropertyInitializer> visitProperty(@NotNull JetProperty expression, @NotNull TranslationContext context) {
        List<JsPropertyInitializer> methods = new ArrayList<JsPropertyInitializer>();
        addSetterToMethods(expression, context, methods);
        addGetterToMethods(expression, context, methods);
        return methods;
    }

    private void addGetterToMethods(@NotNull JetProperty expression, @NotNull TranslationContext context,
                                    @NotNull List<JsPropertyInitializer> methods) {
        JetPropertyAccessor getter = expression.getGetter();
        if (getter != null) {
            methods.addAll(getter.accept(this, context));
        } else {
            methods.add(generateDefaultGetter(expression, context));
        }
    }

    private void addSetterToMethods(@NotNull JetProperty expression, @NotNull TranslationContext context,
                                    @NotNull List<JsPropertyInitializer> methods) {
        JetPropertyAccessor setter = expression.getSetter();
        if (setter != null) {
            methods.addAll(setter.accept(this, context));
        } else {
            if (expression.isVar()) {
                methods.add(generateDefaultSetter(expression, context));
            }
        }
    }

    @NotNull
    private JsPropertyInitializer generateDefaultGetter(@NotNull JetProperty expression, @NotNull TranslationContext context) {
        JsName getterName = context.classScope().declareName(Namer.getNameForGetter(expression.getName()));
        return AstUtil.newNamedMethod(getterName, generateDefaultGetterFunction(expression, context));
    }

    @NotNull
    private JsFunction generateDefaultGetterFunction(@NotNull JetProperty expression,
                                                     @NotNull TranslationContext context) {
        JsNameRef backingFieldRef = declareOrGetBackingFieldName(getPropertyName(expression), context).makeRef();
        JsReturn returnExpression = new JsReturn(backingFieldRef);
        return AstUtil.newFunction
            (context.enclosingScope(), null, new ArrayList<JsParameter>(), AstUtil.convertToBlock(returnExpression));
    }

    @NotNull
    private JsName declareOrGetBackingFieldName(@NotNull String propertyName, @NotNull TranslationContext context) {
        String backingFieldName = Namer.getBackingFieldNameForProperty(propertyName);
        JsName jsBackingFieldName = context.classScope().findExistingName(backingFieldName);
        if (jsBackingFieldName == null) {
            jsBackingFieldName = context.classScope().declareName(backingFieldName);
        }
        return jsBackingFieldName;
    }


    @NotNull
    private JsPropertyInitializer generateDefaultSetter(@NotNull JetProperty expression,
                                                        @NotNull TranslationContext context) {
        JsName setterName = context.classScope().declareName(Namer.getNameForSetter(expression.getName()));
        return AstUtil.newNamedMethod(setterName, generateDefaultSetterFunction(expression, context));
    }

    @NotNull
    private JsFunction generateDefaultSetterFunction(@NotNull JetProperty expression,
                                                     @NotNull TranslationContext context) {
        JsFunction result = new JsFunction(context.enclosingScope());
        JsParameter defaultParameter = new JsParameter(context.enclosingScope().declareTemporary());
        JsBinaryOperation assignment = assignmentToBackingFieldFromDefaultParameter
                                        (expression, context, defaultParameter);
        result.setParameters(Arrays.asList(defaultParameter));
        result.setBody(AstUtil.convertToBlock(assignment));
        return result;
    }

    @NotNull
    private JsBinaryOperation assignmentToBackingFieldFromDefaultParameter
        (@NotNull JetProperty expression, @NotNull TranslationContext context, @NotNull JsParameter defaultParameter) {
        JsNameRef backingFieldRef = declareOrGetBackingFieldName(getPropertyName(expression), context).makeRef();
        JsBinaryOperation assignExpression = new JsBinaryOperation(JsBinaryOperator.ASG);
        assignExpression.setArg1(backingFieldRef);
        assignExpression.setArg2(defaultParameter.getName().makeRef());
        return assignExpression;
    }

//    @Override
//    @NotNull
//    public List<JsPropertyInitializer> visitPropertyAccessor
//            (JetPropertyAccessor expression, TranslationContext context) {
//        List<JsPropertyInitializer> methods = new ArrayList<JsPropertyInitializer>();
//        JsPropertyInitializer namedMethod = generateMethodForAccessor(expression, context);
//        methods.add(namedMethod);
//        return methods;
//    }
//
//    private JsPropertyInitializer generateMethodForAccessor(JetPropertyAccessor expression, TranslationContext context) {
//        JsFunction methodBody = new JsFunction(context.enclosingScope());
//        ExpressionTranslator expressionTranslator =
//                new ExpressionTranslator(context.newFunction(methodBody));
//        JsBlock methodBodyBlock = AstUtil.convertToBlock(expressionTranslator.translate(expression.getBodyExpression()));
//        methodBody.setBody(methodBodyBlock);
//        //TODO figure out naming pattern
//        return AstUtil.newNamedMethod(Namer.getNameForAccessor("nameForAccessor", expression), methodBody);
//    }


}
