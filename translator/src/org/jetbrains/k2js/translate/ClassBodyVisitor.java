package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.PropertyGetterDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertySetterDescriptor;
import org.jetbrains.jet.lang.psi.*;

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
    // method declaration
    public List<JsPropertyInitializer> visitNamedFunction(@NotNull JetNamedFunction expression, @NotNull TranslationContext context) {
        List<JsPropertyInitializer> properties = new ArrayList<JsPropertyInitializer>();
        properties.add((new FunctionTranslator(context)).translateAsMethod(expression));
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
    private JsPropertyInitializer generateDefaultGetter
            (@NotNull JetProperty expression, @NotNull TranslationContext context) {
        PropertyGetterDescriptor getterDescriptor =
                BindingUtils.getPropertyGetterDescriptorForProperty(context.bindingContext(), expression);
        return AstUtil.newNamedMethod(context.getNameForDescriptor(getterDescriptor),
                generateDefaultGetterFunction(expression, context.newPropertyAccess(getterDescriptor)));
    }

    @NotNull
    private JsFunction generateDefaultGetterFunction(@NotNull JetProperty expression,
                                                     @NotNull TranslationContext context) {
        JsNameRef backingFieldRef = getBackingFieldName(getPropertyName(expression), context).makeRef();
        backingFieldRef.setQualifier(new JsThisRef());
        JsReturn returnExpression = new JsReturn(backingFieldRef);
        return AstUtil.newFunction
            (context.enclosingScope(), null, new ArrayList<JsParameter>(), AstUtil.convertToBlock(returnExpression));
    }

    @NotNull
    private JsPropertyInitializer generateDefaultSetter(@NotNull JetProperty expression,
                                                        @NotNull TranslationContext context) {
        PropertySetterDescriptor setterDescriptor =
                BindingUtils.getPropertySetterDescriptorForProperty(context.bindingContext(), expression);
        return AstUtil.newNamedMethod(context.getNameForDescriptor(setterDescriptor),
                 generateDefaultSetterFunction(expression, context.newPropertyAccess(setterDescriptor)));
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
        JsNameRef backingFieldRef = getBackingFieldName(getPropertyName(expression), context).makeRef();
        backingFieldRef.setQualifier(new JsThisRef());
        JsBinaryOperation assignExpression = new JsBinaryOperation(JsBinaryOperator.ASG);
        assignExpression.setArg1(backingFieldRef);
        assignExpression.setArg2(defaultParameter.getName().makeRef());
        return assignExpression;
    }

    @Override
    @NotNull
    public List<JsPropertyInitializer> visitPropertyAccessor(@NotNull JetPropertyAccessor expression,
                                                             @NotNull TranslationContext context) {
        List<JsPropertyInitializer> methods = new ArrayList<JsPropertyInitializer>();
        methods.add(generateMethodForAccessor(expression, context));
        return methods;
    }

    @NotNull
    private JsPropertyInitializer generateMethodForAccessor(@NotNull JetPropertyAccessor expression,
                                                            @NotNull TranslationContext context) {
        JsFunction methodBody = translateAccessorBody(expression, context);
        // we know that custom getters and setters always have their descriptors
        return AstUtil.newNamedMethod(context.getNameForElement(expression), methodBody);
    }

    @NotNull
    private JsFunction translateAccessorBody(@NotNull JetPropertyAccessor expression,
                                             @NotNull TranslationContext context) {
        JsFunction methodBody = JsFunction.getAnonymousFunctionWithScope(context.getScopeForElement(expression));
        ExpressionTranslator expressionTranslator =
                new ExpressionTranslator(context.newPropertyAccess(expression));
        JetExpression bodyExpression = expression.getBodyExpression();
        assert bodyExpression != null : "Custom accessor should have a body.";
        JsBlock methodBodyBlock = AstUtil.convertToBlock(expressionTranslator.translate(bodyExpression));
        methodBody.setBody(methodBodyBlock);
        return methodBody;
    }

//    @NotNull
//    JsName getNameForGetter(@NotNull String propertyName, @NotNull TranslationContext context) {
//        return getNameForAccessor(propertyName, true, context);
//    }
//
//    @NotNull
//    JsName getNameForSetter(@NotNull String propertyName, @NotNull TranslationContext context) {
//        return getNameForAccessor(propertyName, false, context);
//    }
//
//    @NotNull
//    JsName getNameForAccessor(@NotNull String propertyName, boolean isGetter,
//                              @NotNull TranslationContext context) {
//        return context.classScope().findExistingName(Namer.getNameForAccessor(propertyName, isGetter));
//    }


}
