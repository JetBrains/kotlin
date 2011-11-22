package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.psi.JetWhenExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.k2js.declarations.Declarations;

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
    static public JsStatement translateNamespace(@NotNull JetNamespace namespace,
                                                 @NotNull TranslationContext context) {
        return NamespaceTranslator.translate(context, namespace);
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
    static public PatternTranslator patternTranslator(@NotNull TranslationContext context) {
        return PatternTranslator.newInstance(context);
    }

    @NotNull
    static public ReferenceTranslator referenceTranslator(@NotNull TranslationContext context) {
        return ReferenceTranslator.newInstance(context);
    }

    @NotNull
    static public JsNode translateExpression(@NotNull JetExpression expression, @NotNull TranslationContext context) {
        return expressionTranslator(context).translate(expression);
    }

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

    @NotNull
    static public JsNameRef generateCorrectReference(@NotNull TranslationContext context,
                                                     @NotNull JetSimpleNameExpression expression,
                                                     @NotNull JsName referencedName) {
        return (new ReferenceProvider(context, expression, referencedName)).generateCorrectReference();
    }

    public static void generateAst(@NotNull JsProgram result, @NotNull BindingContext bindingContext,
                                   @NotNull Declarations declarations, @NotNull JetNamespace namespace) {
        JsBlock block = result.getFragmentBlock(0);
        TranslationContext context = TranslationContext.rootContext(result, bindingContext, declarations);
        block.addStatement(Translation.translateNamespace(namespace, context));
    }


}
