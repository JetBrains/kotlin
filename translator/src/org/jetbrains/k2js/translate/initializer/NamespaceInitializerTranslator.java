package org.jetbrains.k2js.translate.initializer;

import com.google.dart.compiler.backend.js.ast.JsFunction;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.k2js.translate.context.TranslationContext;

import static org.jetbrains.k2js.translate.utils.TranslationUtils.functionWithScope;

/**
 * @author Pavel Talanov
 */
public final class NamespaceInitializerTranslator extends AbstractInitializerTranslator {

    @NotNull
    private final NamespaceDescriptor namespace;

    public NamespaceInitializerTranslator(@NotNull NamespaceDescriptor namespace, @NotNull TranslationContext context) {
        super(context.getScopeForDescriptor(namespace).innerScope
                ("initializer " + namespace.getName()), context);
        this.namespace = namespace;
    }

    @Override
    @NotNull
    protected JsFunction generateInitializerFunction() {
        JsFunction result = functionWithScope(initializerMethodScope);
        result.setBody(AstUtil.newBlock(translateNamespaceInitializers(namespace)));
        return result;
    }


}
