package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetExpression;

/**
 * @author Talanov Pavel
 *         <p/>
 *         This class is a factory for obtaining instances of translators.
 */
public final class Translation {

    @NotNull
    static public ExpressionTranslator expressionTranslator(@NotNull TranslationContext context) {
        return ExpressionTranslator.newInstance(context);
    }

    @NotNull
    static public FunctionTranslator functionTranslator(@NotNull TranslationContext context) {
        return FunctionTranslator.newInstance(context);
    }

    @NotNull
    static public PropertyAccessTranslator propertyAccessTranslator(@NotNull TranslationContext context) {
        return PropertyAccessTranslator.newInstance(context);
    }

    @NotNull
    static public NamespaceTranslator namespaceTranslator(@NotNull TranslationContext context) {
        return NamespaceTranslator.newInstance(context);
    }

    @NotNull
    static public ClassTranslator classTranslator(@NotNull TranslationContext context) {
        return ClassTranslator.newInstance(context);
    }

    @NotNull
    static public OperationTranslator operationTranslator(@NotNull TranslationContext context) {
        return OperationTranslator.newInstance(context);
    }

    @NotNull
    static public DeclarationTranslator declarationTranslator(@NotNull TranslationContext context) {
        return DeclarationTranslator.newInstance(context);
    }

    @NotNull
    static public JsNode translateExpression(@NotNull JetExpression expression, @NotNull TranslationContext context) {
        return expressionTranslator(context).translate(expression);
    }
}
