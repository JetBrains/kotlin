package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsFunction;
import com.google.dart.compiler.backend.js.ast.JsPropertyInitializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetNamedDeclaration;
import org.jetbrains.jet.lang.psi.JetNamespace;


//TODO: thing about redesigning this class
public class InitializerGenerator {

    @NotNull
    private final TranslationContext context;
    @NotNull
    private final JetNamedDeclaration declaration;

    static public JsPropertyInitializer generateInitializeMethod(@NotNull JetNamedDeclaration declaration,
                                                                 @NotNull TranslationContext context) {
        return (new InitializerGenerator(context, declaration)).generateInitializeMethod();
    }

    private InitializerGenerator(@NotNull TranslationContext context, @NotNull JetNamedDeclaration declaration) {
        this.context = context;
        this.declaration = declaration;
        assert (declaration instanceof JetClass) || (declaration instanceof JetNamespace) :
                "Can create initializers for classes or namespaces only";
    }

    @NotNull
    public JsPropertyInitializer generateInitializeMethod() {
        JsPropertyInitializer initializer = new JsPropertyInitializer();
        initializer.setLabelExpr(context.program().getStringLiteral(Namer.INITIALIZE_METHOD_NAME));
        initializer.setValueExpr(generateInitializeFunction());
        return initializer;
    }

    @NotNull
    private JsFunction generateInitializeFunction() {
        AbstractInitializerVisitor visitor;
        if (declaration instanceof JetNamespace) {
            JetNamespace namespaceDeclaration = (JetNamespace) declaration;
            visitor = new NamespaceInitializerVisitor(namespaceDeclaration,
                    context.newNamespace(namespaceDeclaration));
        } else {
            assert declaration instanceof JetClass;
            JetClass classDeclaration = (JetClass) declaration;
            visitor = new ClassInitializerVisitor(classDeclaration,
                    context.newClass(classDeclaration));
        }
        return visitor.generate();
    }
}