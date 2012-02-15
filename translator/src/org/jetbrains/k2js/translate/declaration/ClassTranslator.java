package org.jetbrains.k2js.translate.declaration;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getClassDescriptor;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getPropertyDescriptorForConstructorParameter;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.findAncestorClass;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.getSuperclassDescriptors;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getPrimaryConstructorParameters;

/**
 * @author Pavel Talanov
 *         <p/>
 *         Generates a definition of a single class.
 */
public final class ClassTranslator extends AbstractTranslator {

    @NotNull
    public static JsPropertyInitializer translateAsProperty(@NotNull JetClassOrObject classDeclaration,
                                                            @NotNull TranslationContext context) {
        JsInvocation classCreationExpression = generateClassCreationExpression(classDeclaration, context);
        JsName className = context.getNameForElement(classDeclaration);
        return new JsPropertyInitializer(className.makeRef(), classCreationExpression);
    }

    @NotNull
    public static JsInvocation generateClassCreationExpression(@NotNull JetClassOrObject classDeclaration,
                                                               @NotNull TranslationContext context) {
        return (new ClassTranslator(classDeclaration, context)).translateClass();
    }

    @NotNull
    private final DeclarationBodyVisitor declarationBodyVisitor = new DeclarationBodyVisitor();

    @NotNull
    private final JetClassOrObject classDeclaration;

    @NotNull
    private final ClassDescriptor descriptor;

    private ClassTranslator(@NotNull JetClassOrObject classDeclaration, @NotNull TranslationContext context) {
        super(context.newDeclaration(classDeclaration));
        this.descriptor = getClassDescriptor(context.bindingContext(), classDeclaration);
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
        if (isObject()) {
            return AstUtil.newInvocation(context().namer().objectCreationMethodReference());
        } else if (isTrait()) {
            return AstUtil.newInvocation(context().namer().traitCreationMethodReference());
        } else {
            return AstUtil.newInvocation(context().namer().classCreationMethodReference());
        }
    }

    private boolean isObject() {
        return descriptor.getKind().equals(ClassKind.OBJECT);
    }

    private boolean isTrait() {
        return descriptor.getKind().equals(ClassKind.TRAIT);
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
        List<ClassDescriptor> superclassDescriptors = getSuperclassDescriptors(descriptor);
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
        }
        throw new AssertionError("Inherited from unknown class");
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
        if (!isTrait()) {
            propertyList.add(Translation.generateClassInitializerMethod(classDeclaration, context()));
        }
        propertyList.addAll(translatePropertiesAsConstructorParameters());
        propertyList.addAll(declarationBodyVisitor.traverseClass(classDeclaration, context()));
        return new JsObjectLiteral(propertyList);
    }

    @NotNull
    private List<JsPropertyInitializer> translatePropertiesAsConstructorParameters() {
        List<JsPropertyInitializer> result = new ArrayList<JsPropertyInitializer>();
        for (JetParameter parameter : getPrimaryConstructorParameters(classDeclaration)) {
            PropertyDescriptor descriptor =
                    getPropertyDescriptorForConstructorParameter(context().bindingContext(), parameter);
            if (descriptor != null) {
                result.addAll(PropertyTranslator.translateAccessors(descriptor, context()));
            }
        }
        return result;
    }
}
