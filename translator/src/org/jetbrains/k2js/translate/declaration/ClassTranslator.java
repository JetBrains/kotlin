package org.jetbrains.k2js.translate.declaration;

import com.google.dart.compiler.backend.js.ast.JsInvocation;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.backend.js.ast.JsObjectLiteral;
import com.google.dart.compiler.backend.js.ast.JsPropertyInitializer;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.utils.BindingUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Talanov Pavel
 */
public final class ClassTranslator extends AbstractTranslator {

    @NotNull
    private final DeclarationBodyVisitor declarationBodyVisitor = new DeclarationBodyVisitor();

    @NotNull
    private final JetClass classDeclaration;

    @NotNull
    static public JsInvocation translateClass(@NotNull JetClass classDeclaration, @NotNull TranslationContext context) {
        return (new ClassTranslator(classDeclaration, context)).translateClass();
    }

    private ClassTranslator(@NotNull JetClass classDeclaration, @NotNull TranslationContext context) {
        super(context.newClass(classDeclaration));
        this.classDeclaration = classDeclaration;
    }

    @NotNull
    private JsInvocation translateClass() {
        JsInvocation jsClassDeclaration = classCreateMethodInvocation();
        addSuperclassReferences(jsClassDeclaration);
        addClassOwnDeclarations(jsClassDeclaration);
        return jsClassDeclaration;
    }

    @NotNull
    private JsInvocation classCreateMethodInvocation() {
        if (classDeclaration.isTrait()) {
            return AstUtil.newInvocation(context().namer().traitCreationMethodReference());
        } else {
            return AstUtil.newInvocation(context().namer().classCreationMethodReference());
        }
    }

    private void addClassOwnDeclarations(@NotNull JsInvocation jsClassDeclaration) {
        JsObjectLiteral jsClassDescription = translateClassDeclarations();
        jsClassDeclaration.getArguments().add(jsClassDescription);
    }

    private void addSuperclassReferences(@NotNull JsInvocation jsClassDeclaration) {
        for (JsNameRef superClassReference : getSuperclassNameReferences()) {
            jsClassDeclaration.getArguments().add(superClassReference);
        }
    }

    @NotNull
    private List<JsNameRef> getSuperclassNameReferences() {
        List<JsNameRef> superclassReferences = new ArrayList<JsNameRef>();
        List<ClassDescriptor> superclassDescriptors =
                BindingUtils.getSuperclassDescriptors(context().bindingContext(), classDeclaration);
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
        //TODO we actually know that in current implementation superclass must have an alias but
        // in future it might change
        return context().aliaser().getAliasForDeclaration(superClassDescriptor);
    }

    @Nullable
    private ClassDescriptor findAndRemoveAncestorClass(@NotNull List<ClassDescriptor> superclassDescriptors) {
        ClassDescriptor ancestorClass = BindingUtils.findAncestorClass(superclassDescriptors);
        superclassDescriptors.remove(ancestorClass);
        return ancestorClass;
    }

    @NotNull
    private JsObjectLiteral translateClassDeclarations() {
        List<JsPropertyInitializer> propertyList = new ArrayList<JsPropertyInitializer>();
        if (!classDeclaration.isTrait()) {
            propertyList.add(Translation.generateClassInitializerMethod(classDeclaration, context()));
        }
        propertyList.addAll(translatePropertiesAsConstructorParameters());
        propertyList.addAll(declarationBodyVisitor.traverseClass(classDeclaration, context()));
        return new JsObjectLiteral(propertyList);
    }

    @NotNull
    private List<JsPropertyInitializer> translatePropertiesAsConstructorParameters() {
        List<JsPropertyInitializer> result = new ArrayList<JsPropertyInitializer>();
        for (JetParameter parameter : classDeclaration.getPrimaryConstructorParameters()) {
            PropertyDescriptor descriptor =
                    BindingUtils.getPropertyDescriptorForConstructorParameter(context().bindingContext(), parameter);
            if (descriptor != null) {
                result.addAll(PropertyTranslator.translateAccessors(descriptor, context()));
            }
        }
        return result;
    }
}
