package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsFunction;
import com.google.dart.compiler.backend.js.ast.JsScope;
import com.google.dart.compiler.backend.js.ast.JsStatement;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetNamespace;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Talanov Pavel
 */
public final class NamespaceInitializerVisitor extends AbstractInitializerVisitor {

    @NotNull
    private final JetNamespace namespace;

    public NamespaceInitializerVisitor(@NotNull JetNamespace namespace, @NotNull TranslationContext context) {
        super(context, new JsScope(context.getScopeForElement(namespace),
                "initializer " + namespace.getName()));
        this.namespace = namespace;
    }

    @Override
    @NotNull
    public JsFunction generate() {
        JsFunction result = JsFunction.getAnonymousFunctionWithScope(initializerMethodScope);
        result.setBody(AstUtil.newBlock(namespace.accept(this, initializerMethodContext)));
        return result;
    }

    @Override
    @NotNull
    public List<JsStatement> visitNamespace(@NotNull JetNamespace expression, @NotNull TranslationContext context) {
        List<JsStatement> initializerStatements = new ArrayList<JsStatement>();
        for (JetDeclaration declaration : expression.getDeclarations()) {
            initializerStatements.addAll(declaration.accept(this, context));
        }
        return initializerStatements;
    }

}
