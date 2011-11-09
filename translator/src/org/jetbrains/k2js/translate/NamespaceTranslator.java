package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetNamespace;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Pavel.Talanov
 */
public final class NamespaceTranslator extends AbstractTranslator {

    public NamespaceTranslator(@NotNull TranslationContext context) {
        super(context);
    }

    public JsProgram generateAst(@NotNull JetNamespace namespace) {
        translate(namespace);
        return program();
    }

    @NotNull
    public JsBlock translate(@NotNull JetNamespace namespace) {
        // TODO support multiple namespaces
        JsBlock block = program().getFragmentBlock(0);
        JsName namespaceName = scope().declareName(Namer.getNameForNamespace(namespace.getName()));
        block.addStatement(namespaceInitStatement(namespaceName));
        JsFunction dummyFunction = new JsFunction(scope());
        TranslationContext newContext = translationContext().newNamespace(namespaceName, dummyFunction);
        JsBlock namespaceDeclarations = translateDeclarations(namespace, newContext);
        block.addStatement(AstUtil.convertToStatement(newNamespace(namespaceName, namespaceDeclarations, dummyFunction)));
        return block;
    }

    @NotNull
    private JsBlock translateDeclarations(@NotNull JetNamespace namespace, @NotNull TranslationContext newContext) {
        DeclarationTranslator declarationTranslator = new DeclarationTranslator(newContext);
        JsBlock namespaceDeclarations = new JsBlock();
        for (JetDeclaration declaration : namespace.getDeclarations()) {
            namespaceDeclarations.addStatement(declarationTranslator.translateDeclaration(declaration));
        }
        return namespaceDeclarations;
    }

    @NotNull
    private JsStatement namespaceInitStatement(@NotNull JsName namespaceName) {
        return AstUtil.convertToStatement(AstUtil.newAssignment(namespaceName.makeRef(), new JsObjectLiteral()));
    }

    @NotNull
    private JsInvocation newNamespace(@NotNull JsName name, @NotNull JsBlock namespaceDeclarations,
                                     @NotNull JsFunction dummyFunction) {
        List<JsParameter> params = new ArrayList<JsParameter>();
        params.add(new JsParameter(name));
        dummyFunction.setParameters(params);
        JsExpression invocationParam = name.makeRef();
        dummyFunction.setBody(namespaceDeclarations);
        return AstUtil.newInvocation(dummyFunction, invocationParam);
    }
}
