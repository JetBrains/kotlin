package org.jetbrains.k2js.translate.declaration;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsInvocation;
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
import org.jetbrains.k2js.translate.reference.ReferenceTranslator;
import org.jetbrains.k2js.translate.utils.BindingUtils;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getPropertyDescriptorForConstructorParameter;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.findAncestorClass;

/**
 * @author Pavel Talanov
 */
public final class ClassTranslator extends AbstractTranslator {

    @NotNull
    public static JsInvocation translateClass(@NotNull JetClass classDeclaration, @NotNull TranslationContext context) {
        return (new ClassTranslator(classDeclaration, context)).translateClass();
    }

    @NotNull
    private final DeclarationBodyVisitor declarationBodyVisitor = new DeclarationBodyVisitor();

    @NotNull
    private final JetClass classDeclaration;

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
        for (JsExpression superClassReference : getSuperclassNameReferences()) {
            jsClassDeclaration.getArguments().add(superClassReference);
        }
    }

    @NotNull
    private List<JsExpression> getSuperclassNameReferences() {
        List<JsExpression> superclassReferences = new ArrayList<JsExpression>();
        List<ClassDescriptor> superclassDescriptors =
                BindingUtils.getSuperclassDescriptors(context().bindingContext(), classDeclaration);
        addAncestorClass(superclassReferences, superclassDescriptors);
        addTraits(superclassReferences, superclassDescriptors);
        return superclassReferences;
    }

    private void addTraits(@NotNull List<JsExpression> superclassReferences,
                           @NotNull List<ClassDescriptor> superclassDescriptors) {
        for (ClassDescriptor superClassDescriptor :
                superclassDescriptors) {
            assert (superClassDescriptor.getKind() == ClassKind.TRAIT) : "Only traits are expected here";
            superclassReferences.add(getClassReference(superClassDescriptor));
        }
    }

    private void addAncestorClass(@NotNull List<JsExpression> superclassReferences,
                                  @NotNull List<ClassDescriptor> superclassDescriptors) {
        //here we remove ancestor class from the list
        ClassDescriptor ancestorClass = findAndRemoveAncestorClass(superclassDescriptors);
        if (ancestorClass != null) {
            superclassReferences.add(getClassReference(ancestorClass));
        }
    }

    @NotNull
    private JsExpression getClassReference(@NotNull ClassDescriptor superClassDescriptor) {
        //TODO we actually know that in current implementation superclass must have an alias but
        // in future it might change
        if (aliaser().hasAliasForDeclaration(superClassDescriptor)) {
            return context().aliaser().getAliasForDeclaration(superClassDescriptor).makeRef();
        } else {
            return ReferenceTranslator.translateAsFQReference(superClassDescriptor, context());
        }
    }

    @Nullable
    private ClassDescriptor findAndRemoveAncestorClass(@NotNull List<ClassDescriptor> superclassDescriptors) {
        ClassDescriptor ancestorClass = findAncestorClass(superclassDescriptors);
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
                    getPropertyDescriptorForConstructorParameter(context().bindingContext(), parameter);
            if (descriptor != null) {
                result.addAll(PropertyTranslator.translateAccessors(descriptor, context()));
            }
        }
        return result;
    }
}
