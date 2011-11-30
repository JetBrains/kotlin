package org.jetbrains.k2js.translate.general;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.k2js.declarations.Declarations;
import org.jetbrains.k2js.translate.declaration.ClassTranslator;
import org.jetbrains.k2js.translate.declaration.NamespaceTranslator;
import org.jetbrains.k2js.translate.expression.ExpressionVisitor;
import org.jetbrains.k2js.translate.expression.FunctionTranslator;
import org.jetbrains.k2js.translate.expression.PatternTranslator;
import org.jetbrains.k2js.translate.expression.WhenTranslator;
import org.jetbrains.k2js.translate.initializer.ClassInitializerTranslator;
import org.jetbrains.k2js.translate.initializer.NamespaceInitializerTranslator;
import org.jetbrains.k2js.translate.reference.PropertyAccessTranslator;
import org.jetbrains.k2js.translate.utils.BindingUtils;

/**
 * @author Talanov Pavel
 *         <p/>
 *         This class provides a interface which all translators use to interact with each other.
 *         Goal is to simlify interaction between translators.
 */
public final class Translation {

    @NotNull
    static public FunctionTranslator functionTranslator(@NotNull JetDeclarationWithBody function,
                                                        @NotNull TranslationContext context) {
        return FunctionTranslator.newInstance(function, context);
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

//    @NotNull
//    static public ReferenceTranslator referenceTranslator(@NotNull TranslationContext context) {
//        return ReferenceTranslator.translateSimpleName(context);
//    }

    @NotNull
    static public JsNode translateExpression(@NotNull JetExpression expression, @NotNull TranslationContext context) {
        return expression.accept(new ExpressionVisitor(), context);
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

    public static JsProgram generateAst(@NotNull BindingContext bindingContext,
                                        @NotNull JetNamespace namespace, @NotNull Project project) {
        //TODO hardcoded
        JsProgram result = new JsProgram("main");
        NamespaceDescriptor descriptor = BindingUtils.getNamespaceDescriptor(bindingContext, namespace);
        Declarations declarations = Declarations.newInstance(result.getRootScope());
        declarations.extractStandardLibrary(JetStandardLibrary.getJetStandardLibrary(project));
        declarations.extractDeclarations(descriptor);
        JsBlock block = result.getFragmentBlock(0);
        TranslationContext context = TranslationContext.rootContext(result, bindingContext, declarations, project);
        block.addStatement(Translation.translateNamespace(namespace, context));
        return result;
    }


}
