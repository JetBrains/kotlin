package org.jetbrains.k2js.translate.initializer;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsStatement;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.k2js.translate.Translation;
import org.jetbrains.k2js.translate.TranslationContext;
import org.jetbrains.k2js.translate.TranslationUtils;
import org.jetbrains.k2js.translate.TranslatorVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Talanov Pavel
 */
public final class InitializerVisitor extends TranslatorVisitor<List<JsStatement>> {


    protected InitializerVisitor() {
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

    @NotNull
    JsStatement assignmentToBackingField(@NotNull JetProperty property, @NotNull JsExpression initExpression,
                                         @NotNull TranslationContext context) {

        return AstUtil.newAssignmentStatement(
                TranslationUtils.backingFieldReference(context, property), initExpression);
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
        return new ArrayList<JsStatement>();
    }

    @NotNull
    public List<JsStatement> traverseClass(@NotNull JetClass expression, @NotNull TranslationContext context) {
        List<JsStatement> initializerStatements = new ArrayList<JsStatement>();
        for (JetDeclaration declaration : expression.getDeclarations()) {
            initializerStatements.addAll(declaration.accept(this, context));
        }
        return initializerStatements;
    }

    @NotNull
    public List<JsStatement> traverseNamespace(@NotNull JetNamespace expression, @NotNull TranslationContext context) {
        List<JsStatement> initializerStatements = new ArrayList<JsStatement>();
        for (JetDeclaration declaration : expression.getDeclarations()) {
            initializerStatements.addAll(declaration.accept(this, context));
        }
        return initializerStatements;
    }

    @NotNull
    public List<JsStatement> traverse(@NotNull JetDeclaration declaration, @NotNull TranslationContext context) {
        if (declaration instanceof JetNamespace) {
            return traverseNamespace((JetNamespace) declaration, context);
        }
        if (declaration instanceof JetClass) {
            return traverseClass((JetClass) declaration, context);
        }
        throw new AssertionError("Cannot traverse anything else.");
    }
}
