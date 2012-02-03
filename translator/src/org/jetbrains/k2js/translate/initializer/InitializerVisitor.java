package org.jetbrains.k2js.translate.initializer;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsInvocation;
import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.declaration.ClassTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.general.TranslatorVisitor;
import org.jetbrains.k2js.translate.utils.BindingUtils;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getDeclarationsForNamespace;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getPropertyDescriptorForObjectDeclaration;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getObjectDeclarationForName;

/**
 * @author Pavel Talanov
 */
public final class InitializerVisitor extends TranslatorVisitor<List<JsStatement>> {

    /*package*/ InitializerVisitor() {
    }

    @Override
    @NotNull
    public List<JsStatement> visitProperty(@NotNull JetProperty expression, @NotNull TranslationContext context) {
        JetExpression initializer = expression.getInitializer();
        if (initializer == null) {
            return new ArrayList<JsStatement>();
        }
        return Arrays.asList(translateInitializer(expression, context, initializer));
    }

    @NotNull
    private JsStatement translateInitializer(@NotNull JetProperty property, @NotNull TranslationContext context,
                                             @NotNull JetExpression initializer) {
        JsExpression initExpression = Translation.translateAsExpression(initializer, context);
        return assignmentToBackingField(property, initExpression, context);
    }

    //TODO:
    @NotNull
    JsStatement assignmentToBackingField(@NotNull JetProperty property, @NotNull JsExpression initExpression,
                                         @NotNull TranslationContext context) {

        PropertyDescriptor propertyDescriptor = BindingUtils.getPropertyDescriptor(context.bindingContext(), property);
        return TranslationUtils.assignmentToBackingField(context, propertyDescriptor, initExpression);
    }

    @Override
    @NotNull
    public List<JsStatement> visitAnonymousInitializer(@NotNull JetClassInitializer initializer,
                                                       @NotNull TranslationContext context) {
        return Arrays.asList(Translation.translateAsStatement(initializer.getBody(), context));
    }

    @Override
    @NotNull
    // Not interested in other types of declarations, they do not contain initializers.
    public List<JsStatement> visitDeclaration(@NotNull JetDeclaration expression, @NotNull TranslationContext context) {
        return Collections.emptyList();
    }

    @Override
    @NotNull
    public List<JsStatement> visitObjectDeclarationName(@NotNull JetObjectDeclarationName objectName,
                                                        @NotNull TranslationContext context) {
        PropertyDescriptor propertyDescriptorForObjectDeclaration
                = getPropertyDescriptorForObjectDeclaration(context.bindingContext(), objectName);
        JsName objectPropertyName = context.getNameForDescriptor(propertyDescriptorForObjectDeclaration);
        JetObjectDeclaration objectDeclaration = getObjectDeclarationForName(objectName);
        JsInvocation objectValue = ClassTranslator.generateClassCreationExpression(objectDeclaration, context);
        return singletonList(TranslationUtils.assignmentToBackingField(context,
                propertyDescriptorForObjectDeclaration, objectValue));
    }

    @NotNull
    public List<JsStatement> traverseClass(@NotNull JetClassOrObject expression, @NotNull TranslationContext context) {
        List<JsStatement> initializerStatements = new ArrayList<JsStatement>();
        for (JetDeclaration declaration : expression.getDeclarations()) {
            initializerStatements.addAll(declaration.accept(this, context));
        }
        return initializerStatements;
    }

    @NotNull
    public List<JsStatement> traverseNamespace(@NotNull NamespaceDescriptor namespace, @NotNull TranslationContext context) {
        List<JsStatement> initializerStatements = new ArrayList<JsStatement>();
        for (JetDeclaration declaration : getDeclarationsForNamespace(context.bindingContext(), namespace)) {
            initializerStatements.addAll(declaration.accept(this, context));
        }
        return initializerStatements;
    }
}
