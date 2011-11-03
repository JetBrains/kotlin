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
public class NamespaceTranslator extends AbstractTranslator {

    private final JetNamespace namespace;

    NamespaceTranslator(TranslationContext context, JetNamespace namespace) {
        super(context);
        this.namespace = namespace;
    }

    JsProgram generateAst() {
        translate(namespace);
        return program();
    }

    public JsBlock translate(JetNamespace namespace) {
        // TODO support multiple namespaces
        JsBlock block = program().getFragmentBlock(0);
        JsName namespaceName = Namer.getNameForNamespace(namespace.getName());
        block.addStatement(namespaceInitStatement(namespaceName));
        JsFunction dummyFunction = new JsFunction(scope());
        TranslationContext newContext = translationContext().newNamespace(namespaceName, dummyFunction);
        JsBlock namespaceDeclarations = translateDeclaration(namespace, newContext);
        block.addStatement(AstUtil.convertToStatement(newNamespace(namespaceName, namespaceDeclarations, dummyFunction)));
        return block;
    }

    @NotNull
    private JsBlock translateDeclaration(JetNamespace namespace, TranslationContext newContext) {
        DeclarationTranslator declarationTranslator = new DeclarationTranslator(newContext);
        JsBlock namespaceDeclarations = new JsBlock();
        for (JetDeclaration declaration : namespace.getDeclarations()) {
            namespaceDeclarations.addStatement(declarationTranslator.translateDeclaration(declaration));
        }
        return namespaceDeclarations;
    }

    private JsStatement namespaceInitStatement(JsName namespaceName) {
        return AstUtil.convertToStatement(AstUtil.newAssignment(namespaceName.makeRef(), new JsObjectLiteral()));
    }

    public JsInvocation newNamespace(JsName name, JsBlock namespaceDeclarations, JsFunction dummyFunction) {
        List<JsParameter> params = new ArrayList<JsParameter>();
        params.add(new JsParameter(name));
        dummyFunction.setParameters(params);
        JsExpression invocationParam = name.makeRef();
        dummyFunction.setBody(namespaceDeclarations);
        return AstUtil.newInvocation(dummyFunction, invocationParam);
    }
}
