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
    private final ClassDeclarationTranslator classDeclarationTranslator;

    @NotNull
    public static JsStatement translateNamespace(@NotNull JetNamespace namespace, @NotNull TranslationContext context) {
        return (new NamespaceTranslator(namespace, context)).translateNamespace();
    }

    private NamespaceTranslator(@NotNull JetNamespace namespace, @NotNull TranslationContext context) {
        super(context.newNamespace(namespace));
        this.namespace = namespace;
        this.namespaceName = context.getNameForElement(namespace);
        this.classDeclarationTranslator = new ClassDeclarationTranslator(context, namespace);
    }

    @NotNull
    public JsStatement translateNamespace() {
        classDeclarationTranslator.generateDeclarations();
        return AstUtil.newBlock(classDeclarationsStatement(),
                namespaceOwnDeclarationStatement(),
                namespaceInitializeStatement());
    }

    private JsStatement classDeclarationsStatement() {
        return classDeclarationTranslator.getDeclarationsStatement();
    }

    @NotNull
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
    private JsStatement namespaceOwnDeclarationStatement() {
        JsInvocation namespaceDeclaration = namespaceCreateMethodInvocation();
        addMemberDeclarations(namespaceDeclaration);
        addClassesDeclarations(namespaceDeclaration);
        return AstUtil.newAssignmentStatement
                (translationContext().getNameForElement(namespace).makeRef(), namespaceDeclaration);
    }

    private void addClassesDeclarations(@NotNull JsInvocation namespaceDeclaration) {
        namespaceDeclaration.getArguments().add(classDeclarationTranslator.getDeclarationsObjectName().makeRef());
    }

    private void addMemberDeclarations(@NotNull JsInvocation jsNamespace) {
        JsObjectLiteral jsClassDescription = translateNamespaceMemberDeclarations();
        jsNamespace.getArguments().add(jsClassDescription);
    }

    @NotNull
    private JsObjectLiteral translateNamespaceMemberDeclarations() {
        List<JsPropertyInitializer> propertyList = new ArrayList<JsPropertyInitializer>();
        propertyList.add(Translation.generateNamespaceInitializerMethod(namespace, translationContext()));
        propertyList.addAll(new DeclarationBodyVisitor().traverseNamespace(namespace, translationContext()));
        return new JsObjectLiteral(propertyList);
    }
}
