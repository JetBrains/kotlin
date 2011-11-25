package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.psi.JetVisitor;

/**
 * @author Talanov Pavel
 *         <p/>
 *         This class is a base class for all visitors. It contains code that is shared among them.
 */
public class TranslatorVisitor<T> extends JetVisitor<T, TranslationContext> {

    @Override
    @NotNull
    public T visitJetElement(JetElement expression, TranslationContext context) {
        throw new RuntimeException("Unsupported expression encountered:" + expression.toString());
    }

    @NotNull
    protected JsNameRef backingFieldReference(@NotNull JetProperty expression, @NotNull TranslationContext context) {
        JsName backingFieldName = getBackingFieldName(getPropertyName(expression), context);
        if (BindingUtils.belongsToNamespace(context.bindingContext(), expression)) {
            return context.getNamespaceQualifiedReference(backingFieldName);
        }
        return AstUtil.thisQualifiedReference(backingFieldName);
    }

    @NotNull
    protected String getPropertyName(@NotNull JetProperty expression) {
        String propertyName = expression.getName();
        if (propertyName == null) {
            throw new AssertionError("Property with no name encountered!");
        }
        return propertyName;
    }

    @NotNull
    private JsName getBackingFieldName(@NotNull String propertyName, @NotNull TranslationContext context) {
        String backingFieldName = Namer.getKotlinBackingFieldName(propertyName);
        return context.enclosingScope().findExistingName(backingFieldName);
    }


    @Nullable
    protected JsExpression translateInitializerForProperty(@NotNull JetProperty declaration,
                                                           @NotNull TranslationContext context) {
        JsExpression jsInitExpression = null;
        JetExpression initializer = declaration.getInitializer();
        if (initializer != null) {
            jsInitExpression = Translation.translateAsExpression(initializer, context);
        }
        return jsInitExpression;
    }
}
