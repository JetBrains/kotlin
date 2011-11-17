package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.psi.JetClass;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Talanov Pavel
 */
//TODO ClassTranslator needs heavy improvement
// did some improvement
public final class ClassTranslator extends AbstractTranslator {

    @NotNull
    private final ClassBodyVisitor classBodyVisitor = new ClassBodyVisitor();

    @NotNull
    public static ClassTranslator newInstance(@NotNull TranslationContext context) {
        return new ClassTranslator(context);
    }

    private ClassTranslator(TranslationContext context) {
        super(context);
    }

    @NotNull
    public JsStatement translateClass(@NotNull JetClass jetClassDeclaration) {
        JsInvocation jsClassDeclaration = classCreateMethodInvocation(jetClassDeclaration);
        addSuperclassReferences(jetClassDeclaration, jsClassDeclaration);
        addClassOwnDeclarations(jetClassDeclaration, jsClassDeclaration);
        return classDeclarationStatement(jetClassDeclaration, jsClassDeclaration);
    }

    @NotNull
    private JsInvocation classCreateMethodInvocation(@NotNull JetClass jetClassDeclaration) {
        if (jetClassDeclaration.isTrait()) {
            return AstUtil.newInvocation(Namer.traitCreationMethodReference());
        } else {
            return AstUtil.newInvocation(Namer.classCreationMethodReference());
        }
    }

    @NotNull
    private JsStatement classDeclarationStatement(@NotNull JetClass classDeclaration,
                                                  @NotNull JsInvocation jsClassDeclaration) {
        return AstUtil.newAssignmentStatement
                (namespaceQualifiedClassNameReference(classDeclaration), jsClassDeclaration);
    }

    @NotNull
    private JsNameRef namespaceQualifiedClassNameReference(@NotNull JetClass classDeclaration) {
        return translationContext().getNamespaceQualifiedReference
                (translationContext().getNameForElement(classDeclaration));
    }

    private void addClassOwnDeclarations(@NotNull JetClass classDeclaration,
                                         @NotNull JsInvocation jsClassDeclaration) {
        JsObjectLiteral jsClassDescription = translateClassDeclarations(classDeclaration);
        jsClassDeclaration.getArguments().add(jsClassDescription);
    }

    private void addSuperclassReferences(@NotNull JetClass classDeclaration,
                                         @NotNull JsInvocation jsClassDeclaration) {
        for (JsNameRef superClassReference : getSuperclassNameReferences(classDeclaration)) {
            jsClassDeclaration.getArguments().add(superClassReference);
        }
    }

    @NotNull
    private List<JsNameRef> getSuperclassNameReferences(@NotNull JetClass classDeclaration) {
        List<JsNameRef> superclassReferences = new ArrayList<JsNameRef>();
        List<ClassDescriptor> superclassDescriptors =
                BindingUtils.getSuperclassDescriptors(translationContext().bindingContext(), classDeclaration);
        addAncestorClass(superclassReferences, superclassDescriptors);
        addTraits(superclassReferences, superclassDescriptors);
        return superclassReferences;
    }

    private void addTraits(@NotNull List<JsNameRef> superclassReferences,
                           @NotNull List<ClassDescriptor> superclassDescriptors) {
        for (ClassDescriptor superClassDescriptor :
                superclassDescriptors) {
            assert (superClassDescriptor.getKind() == ClassKind.TRAIT) : "Only traits are expected here";
            superclassReferences.add(getClassReference(superClassDescriptor));
        }
    }

    private void addAncestorClass(@NotNull List<JsNameRef> superclassReferences,
                                  @NotNull List<ClassDescriptor> superclassDescriptors) {
        //here we remove ancestor class from the list
        ClassDescriptor ancestorClass = findAndRemoveAncestorClass(superclassDescriptors);
        if (ancestorClass != null) {
            superclassReferences.add(getClassReference(ancestorClass));
        }
    }

    @NotNull
    private JsNameRef getClassReference(@NotNull ClassDescriptor superClassDescriptor) {
        //TODO should get a full class name here
        return translationContext().getNamespaceQualifiedReference
                (translationContext().getNameForDescriptor(superClassDescriptor));
    }

    @Nullable
    private ClassDescriptor findAndRemoveAncestorClass(@NotNull List<ClassDescriptor> superclassDescriptors) {
        ClassDescriptor ancestorClass = BindingUtils.findAncestorClass(superclassDescriptors);
        superclassDescriptors.remove(ancestorClass);
        return ancestorClass;
    }

    @NotNull
    private JsObjectLiteral translateClassDeclarations(@NotNull JetClass classDeclaration) {
        List<JsPropertyInitializer> propertyList = new ArrayList<JsPropertyInitializer>();
        if (!classDeclaration.isTrait()) {
            propertyList.add(generateInitializeMethod(classDeclaration));
        }
        propertyList.addAll(classDeclaration.accept(classBodyVisitor,
                translationContext().newClass(classDeclaration)));
        return new JsObjectLiteral(propertyList);
    }

    // TODO: names are inconsistent
    @NotNull
    private JsPropertyInitializer generateInitializeMethod(@NotNull JetClass classDeclaration) {
        JsPropertyInitializer initializer = new JsPropertyInitializer();
        initializer.setLabelExpr(program().getStringLiteral(Namer.INITIALIZE_METHOD_NAME));
        initializer.setValueExpr(generateInitializeMethodBody(classDeclaration));
        return initializer;
    }

    @NotNull
    private JsFunction generateInitializeMethodBody(@NotNull JetClass classDeclaration) {
        InitializerVisitor initializerVisitor = new InitializerVisitor(classDeclaration,
                translationContext().newClass(classDeclaration));
        return initializerVisitor.generateInitializeMethod();
    }

}
