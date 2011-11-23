package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsInvocation;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.backend.js.ast.JsObjectLiteral;
import com.google.dart.compiler.backend.js.ast.JsPropertyInitializer;
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
    private final DeclarationBodyVisitor declarationBodyVisitor = new DeclarationBodyVisitor();

    @NotNull
    public static ClassTranslator newInstance(@NotNull TranslationContext context) {
        return new ClassTranslator(context);
    }

    private ClassTranslator(TranslationContext context) {
        super(context);
    }

    @NotNull
    public JsInvocation translateClass(@NotNull JetClass jetClassDeclaration) {
        JsInvocation jsClassDeclaration = classCreateMethodInvocation(jetClassDeclaration);
        addSuperclassReferences(jetClassDeclaration, jsClassDeclaration);
        addClassOwnDeclarations(jetClassDeclaration, jsClassDeclaration);
        return jsClassDeclaration;
    }

    @NotNull
    private JsInvocation classCreateMethodInvocation(@NotNull JetClass jetClassDeclaration) {
        if (jetClassDeclaration.isTrait()) {
            return AstUtil.newInvocation(Namer.traitCreationMethodReference());
        } else {
            return AstUtil.newInvocation(Namer.classCreationMethodReference());
        }
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
            propertyList.add(InitializerGenerator.generateInitializeMethod(classDeclaration, translationContext()));
        }
        propertyList.addAll(declarationBodyVisitor.traverseClass(classDeclaration,
                translationContext().newClass(classDeclaration)));
        return new JsObjectLiteral(propertyList);
    }
}
