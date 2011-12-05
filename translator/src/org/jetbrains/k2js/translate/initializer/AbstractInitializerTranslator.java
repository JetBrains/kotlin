package org.jetbrains.k2js.translate.initializer;

import com.google.dart.compiler.backend.js.ast.JsFunction;
import com.google.dart.compiler.backend.js.ast.JsPropertyInitializer;
import com.google.dart.compiler.backend.js.ast.JsStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.k2js.translate.context.Namer;
import org.jetbrains.k2js.translate.context.NamingScope;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;

import java.util.List;

/**
 * @author Talanov Pavel
 */
public abstract class AbstractInitializerTranslator extends AbstractTranslator {


    @NotNull
    protected final InitializerVisitor visitor;
    @NotNull
    protected final NamingScope initializerMethodScope;

    protected AbstractInitializerTranslator(@NotNull NamingScope scope, @NotNull TranslationContext context) {
        super(context.contextWithScope(scope));
        this.visitor = new InitializerVisitor();
        this.initializerMethodScope = scope;
    }

    abstract protected JsFunction generateInitializerFunction();


    @NotNull
    public JsPropertyInitializer generateInitializeMethod() {
        JsPropertyInitializer initializer = new JsPropertyInitializer();
        initializer.setLabelExpr(Namer.initializeMethodReference());
        initializer.setValueExpr(generateInitializerFunction());
        return initializer;
    }

    @NotNull
    protected List<JsStatement> translatePropertyAndAnonymousInitializers(@NotNull JetDeclaration declaration) {
        return visitor.traverse(declaration, context());
    }
}
