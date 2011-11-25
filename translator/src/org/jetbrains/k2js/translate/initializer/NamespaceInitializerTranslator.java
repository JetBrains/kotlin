package org.jetbrains.k2js.translate.initializer;

import com.google.dart.compiler.backend.js.ast.JsFunction;
import com.google.dart.compiler.backend.js.ast.JsScope;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.k2js.translate.general.TranslationContext;

/**
 * @author Talanov Pavel
 */
public final class NamespaceInitializerTranslator extends AbstractInitializerTranslator {

    @NotNull
    private final JetNamespace namespace;

    public NamespaceInitializerTranslator(@NotNull JetNamespace namespace, @NotNull TranslationContext context) {
        super(new JsScope(context.getScopeForElement(namespace),
                "initializer " + namespace.getName()), context);
        this.namespace = namespace;
    }

    @Override
    @NotNull
    protected JsFunction generateInitializerFunction() {
        JsFunction result = JsFunction.getAnonymousFunctionWithScope(initializerMethodScope);
        result.setBody(AstUtil.newBlock(translatePropertyAndAnonymousInitializers(namespace)));
        return result;
    }


}
