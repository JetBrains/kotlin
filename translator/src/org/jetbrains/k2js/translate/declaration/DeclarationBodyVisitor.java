package org.jetbrains.k2js.translate.declaration;

import com.google.dart.compiler.backend.js.ast.JsPropertyInitializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.general.TranslationContext;
import org.jetbrains.k2js.translate.general.TranslatorVisitor;
import org.jetbrains.k2js.translate.utils.BindingUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Talanov Pavel
 */
public final class DeclarationBodyVisitor extends TranslatorVisitor<List<JsPropertyInitializer>> {


    @NotNull
    public List<JsPropertyInitializer> traverseClass(@NotNull JetClass expression,
                                                     @NotNull TranslationContext context) {
        List<JsPropertyInitializer> properties = new ArrayList<JsPropertyInitializer>();
        for (JetDeclaration declaration : expression.getDeclarations()) {
            properties.addAll(declaration.accept(this, context));
        }
        return properties;
    }

    @NotNull
    public List<JsPropertyInitializer> traverseNamespace(@NotNull JetNamespace expression,
                                                         @NotNull TranslationContext context) {
        List<JsPropertyInitializer> properties = new ArrayList<JsPropertyInitializer>();
        for (JetDeclaration declaration : expression.getDeclarations()) {
            properties.addAll(declaration.accept(this, context));
        }
        return properties;
    }

    @Override
    @NotNull
    public List<JsPropertyInitializer> visitClass(@NotNull JetClass expression, @NotNull TranslationContext context) {
        return Collections.emptyList();
    }

    @Override
    @NotNull
    public List<JsPropertyInitializer> visitNamedFunction(@NotNull JetNamedFunction expression,
                                                          @NotNull TranslationContext context) {
        return Arrays.asList(Translation.functionTranslator(expression, context).translateAsMethod());
    }

    @Override
    @NotNull
    public List<JsPropertyInitializer> visitProperty(@NotNull JetProperty expression,
                                                     @NotNull TranslationContext context) {
        PropertyDescriptor propertyDescriptor =
                BindingUtils.getPropertyDescriptor(context.bindingContext(), expression);
        return PropertyTranslator.translateAccessors(propertyDescriptor, context);
    }


    @Override
    @NotNull
    public List<JsPropertyInitializer> visitAnonymousInitializer(@NotNull JetClassInitializer expression,
                                                                 @NotNull TranslationContext context) {
        // parsed it in initializer visitor => no additional actions are needed
        return Collections.emptyList();
    }
}
