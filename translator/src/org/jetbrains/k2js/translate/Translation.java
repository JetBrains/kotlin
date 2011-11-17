package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsNode;
import com.google.dart.compiler.backend.js.ast.JsStatement;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetWhenExpression;

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
    static public PatternTranslator typeOperationTranslator(@NotNull TranslationContext context) {
        return PatternTranslator.newInstance(context);
    }

    @NotNull
    static public JsNode translateExpression(@NotNull JetExpression expression, @NotNull TranslationContext context) {
        return expressionTranslator(context).translate(expression);
    }

    //TODO: clean out similar code fragments
    @NotNull
    static public JsExpression translateAsExpression(@NotNull JetExpression expression, @NotNull TranslationContext context) {
        return AstUtil.convertToExpression(translateExpression(expression, context));
    }

    @NotNull
    static public JsStatement translateAsStatement(@NotNull JetExpression expression, @NotNull TranslationContext context) {
        return AstUtil.convertToStatement(translateExpression(expression, context));
    }

    @NotNull
    static public JsNode translateWhenExpression(@NotNull JetWhenExpression expression, @NotNull TranslationContext context) {
        return WhenTranslator.translateWhenExpression(expression, context);
    }
}
