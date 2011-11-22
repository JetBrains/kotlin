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
    private final JetNamespace namespace;
    @NotNull
    private final JsName namespaceName;

    @NotNull
    public static JsStatement translate(@NotNull TranslationContext context,
                                        @NotNull JetNamespace namespace) {
        return (new NamespaceTranslator(context, namespace)).translateNamespace();
    }

    private NamespaceTranslator(@NotNull TranslationContext context, @NotNull JetNamespace namespace) {
        super(context);
        this.namespace = namespace;
        this.namespaceName = context.getNameForElement(namespace);
    }

    @NotNull
    public JsStatement translateNamespace() {
        ClassDeclarationTranslator translator = new ClassDeclarationTranslator(translationContext(), namespace);
        translator.generateDeclarations();
        JsName declarationsObjectName = translator.getDeclarationsObjectName();
        JsBlock result = new JsBlock();
        JsInvocation namespaceDeclaration = namespaceCreateMethodInvocation();
        namespaceDeclaration.getArguments().add(declarationsObjectName.makeRef());
        addMemberDeclarations(namespaceDeclaration);
        result.addStatement(translator.getDeclarationsStatement());
        result.addStatement(namespaceDeclarationStatement(namespaceDeclaration));
        result.addStatement(namespaceInitializeStatement());
        return result;
    }

    private JsStatement namespaceInitializeStatement() {
        JsNameRef initializeMethodReference = Namer.initializeMethodReference();
        AstUtil.setQualifier(initializeMethodReference, namespaceName.makeRef());
        return AstUtil.newInvocation(initializeMethodReference).makeStmt();
    }

    @NotNull
    private JsInvocation namespaceCreateMethodInvocation() {
        return AstUtil.newInvocation(Namer.namespaceCreationMethodReference());
    }

    @NotNull
    private JsStatement namespaceDeclarationStatement(@NotNull JsInvocation namespaceDeclaration) {
        return AstUtil.newAssignmentStatement
                (translationContext().getNameForElement(namespace).makeRef(), namespaceDeclaration);
    }

    private void addMemberDeclarations(@NotNull JsInvocation jsNamespace) {
        JsObjectLiteral jsClassDescription = translateNamespaceMemberDeclarations();
        jsNamespace.getArguments().add(jsClassDescription);
    }

    @NotNull
    private JsObjectLiteral translateNamespaceMemberDeclarations() {
        List<JsPropertyInitializer> propertyList = new ArrayList<JsPropertyInitializer>();
        propertyList.add(InitializerGenerator.generateInitializeMethod(namespace, translationContext()));
        propertyList.addAll(new DeclarationBodyVisitor().traverseNamespace(namespace,
                translationContext().newNamespace(namespace)));
        return new JsObjectLiteral(propertyList);
    }
}
