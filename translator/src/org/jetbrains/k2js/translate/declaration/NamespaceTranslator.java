package org.jetbrains.k2js.translate.declaration;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.k2js.translate.context.Namer;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Pavel.Talanov
 */
//TODO: rework translator to translateAsLocalNameReference everything in the namespace not only in one file
// TEST IT
public final class NamespaceTranslator extends AbstractTranslator {

    @NotNull
    private final NamespaceDescriptor namespace;
    @NotNull
    private final JsName namespaceName;
    @NotNull
    private final ClassDeclarationTranslator classDeclarationTranslator;

    @NotNull
    public static JsStatement translateNamespace(@NotNull NamespaceDescriptor namespace, @NotNull TranslationContext context) {
        return (new NamespaceTranslator(namespace, context)).translateNamespace();
    }

    private NamespaceTranslator(@NotNull NamespaceDescriptor namespace, @NotNull TranslationContext context) {
        super(context.newNamespace(namespace));
        this.namespace = namespace;
        this.namespaceName = context.getNameForDescriptor(namespace);
        this.classDeclarationTranslator = new ClassDeclarationTranslator(context(), namespace);
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
        return AstUtil.newInvocation(context().namer().namespaceCreationMethodReference());
    }

    @NotNull
    private JsStatement namespaceOwnDeclarationStatement() {
        JsInvocation namespaceDeclaration = namespaceCreateMethodInvocation();
        addMemberDeclarations(namespaceDeclaration);
        addClassesDeclarations(namespaceDeclaration);
        return AstUtil.newVar(namespaceName, namespaceDeclaration);
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
        propertyList.add(Translation.generateNamespaceInitializerMethod(namespace, context()));
        propertyList.addAll(new DeclarationBodyVisitor().traverseNamespace(namespace, context()));
        return new JsObjectLiteral(propertyList);
    }
}
