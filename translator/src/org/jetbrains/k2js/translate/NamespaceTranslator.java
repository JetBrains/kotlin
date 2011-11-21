package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
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

    @NotNull
    public JsStatement translateNamespace(@NotNull JetNamespace namespace) {
        ClassDeclarationTranslator translator = new ClassDeclarationTranslator(translationContext(), namespace);
        translator.generateDeclarations();
        JsName declarationsObjectName = translator.getDeclarationsObjectName();
        JsBlock result = new JsBlock();
        JsInvocation namespaceDeclaration = namespaceCreateMethodInvocation(namespace);
        namespaceDeclaration.getArguments().add(declarationsObjectName.makeRef());
        addMemberDeclarations(namespace, namespaceDeclaration);
        result.addStatement(translator.getDeclarationsStatement());
        result.addStatement(namespaceDeclarationStatement(namespace, namespaceDeclaration));
        return result;
    }

    @NotNull
    private JsInvocation namespaceCreateMethodInvocation(@NotNull JetNamespace namespace) {
        return AstUtil.newInvocation(Namer.namespaceCreationMethodReference());
    }

    @NotNull
    private JsStatement namespaceDeclarationStatement(@NotNull JetNamespace namespace,
                                                      @NotNull JsInvocation namespaceDeclaration) {
        return AstUtil.newAssignmentStatement
                (translationContext().getNameForElement(namespace).makeRef(), namespaceDeclaration);
    }

    private void addMemberDeclarations(@NotNull JetNamespace namespace,
                                       @NotNull JsInvocation jsNamespace) {
        JsObjectLiteral jsClassDescription = translateNamespaceMemberDeclarations(namespace);
        jsNamespace.getArguments().add(jsClassDescription);
    }

    @NotNull
    private JsObjectLiteral translateNamespaceMemberDeclarations(@NotNull JetNamespace namespace) {
        List<JsPropertyInitializer> propertyList = new ArrayList<JsPropertyInitializer>();
        propertyList.add(InitializerGenerator.generateInitializeMethod(namespace, translationContext()));
        propertyList.addAll(new DeclarationBodyVisitor().traverseNamespace(namespace,
                translationContext().newNamespace(namespace)));
        return new JsObjectLiteral(propertyList);
    }
}
