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

    @NotNull
    public static NamespaceTranslator newInstance(@NotNull TranslationContext context) {
        return new NamespaceTranslator(context);
    }

    private NamespaceTranslator(@NotNull TranslationContext context) {
        super(context);
    }

    public JsProgram generateAst(@NotNull JetNamespace namespace) {
        translate(namespace);
        return program();
    }

    //TODO logic unclear
    @NotNull
    public JsBlock translate(@NotNull JetNamespace namespace) {
        // TODO support multiple namespaces
        JsBlock block = program().getFragmentBlock(0);
        JsName namespaceName = translationContext().enclosingScope().declareName(Namer.getNameForNamespace(namespace.getName()));
        block.addStatement(namespaceInitStatement(namespaceName));
        TranslationContext newContext = translationContext().newNamespace(namespace);
        JsFunction dummyFunction = JsFunction.getAnonymousFunctionWithScope
                (translationContext().getScopeForElement(namespace));
        JsBlock namespaceDeclarations = translateDeclarations(namespace, newContext);
        JsInvocation namespaceExpression = newNamespace(namespaceName, namespaceDeclarations, dummyFunction);
        block.addStatement(AstUtil.convertToStatement(namespaceExpression));
        return block;
    }

    @NotNull
    private JsBlock translateDeclarations(@NotNull JetNamespace namespace, @NotNull TranslationContext newContext) {
        NamespaceDeclarationTranslator namespaceDeclarationTranslator = Translation.declarationTranslator(newContext);
        JsBlock namespaceDeclarations = new JsBlock();
        for (JetDeclaration declaration : namespace.getDeclarations()) {
            namespaceDeclarations.addStatement(namespaceDeclarationTranslator.translateDeclaration(declaration));
        }
        return namespaceDeclarations;
    }

    @NotNull
    private JsStatement namespaceInitStatement(@NotNull JsName namespaceName) {
        return AstUtil.newAssignmentStatement(namespaceName.makeRef(), new JsObjectLiteral());
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
