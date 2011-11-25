package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.k2js.declarations.Declarations;
import org.jetbrains.k2js.translate.initializer.ClassInitializerTranslator;
import org.jetbrains.k2js.translate.initializer.NamespaceInitializerTranslator;

/**
 * @author Talanov Pavel
 *         <p/>
 *         This class provides a interface which all translators use to interact with each other.
 *         Goal is to simlify interaction between translators.
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
        return NamespaceTranslator.translateNamespace(namespace, context);
    }

    @NotNull
    static public JsInvocation translateClassDeclaration(@NotNull JetClass classDeclaration,
                                                         @NotNull TranslationContext context) {
        return ClassTranslator.translateClass(classDeclaration, context);
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
    static public JsPropertyInitializer generateClassInitializerMethod(@NotNull JetClass classDeclaration,
                                                                       @NotNull TranslationContext context) {
        return (new ClassInitializerTranslator(classDeclaration, context)).generateInitializeMethod();
    }

    @NotNull
    static public JsPropertyInitializer generateNamespaceInitializerMethod(@NotNull JetNamespace namespace,
                                                                           @NotNull TranslationContext context) {
        return (new NamespaceInitializerTranslator(namespace, context)).generateInitializeMethod();
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
        TranslationContext context = TranslationContext.rootContext(result, bindingContext, declarations, block);
        block.addStatement(Translation.translateNamespace(namespace, context));
    }


}
