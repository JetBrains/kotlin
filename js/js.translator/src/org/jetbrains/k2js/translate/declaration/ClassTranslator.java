/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.k2js.translate.declaration;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetObjectLiteralExpression;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.utils.JsAstUtils;

import java.util.ArrayList;
import java.util.List;

import static com.google.dart.compiler.util.AstUtil.newSequence;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getClassDescriptor;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getPropertyDescriptorForConstructorParameter;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.*;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getPrimaryConstructorParameters;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.getQualifiedReference;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.getThisObject;

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
        return (new ClassTranslator(classDeclaration, context)).translateClassOrObjectCreation();
    }

    @NotNull
    public static JsExpression generateObjectLiteralExpression(@NotNull JetObjectLiteralExpression objectLiteralExpression,
                                                               @NotNull TranslationContext context) {
        return (new ClassTranslator(objectLiteralExpression.getObjectDeclaration(), context)).translateObjectLiteralExpression();
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
    private JsExpression translateObjectLiteralExpression() {
        ClassDescriptor containingClass = getContainingClass(descriptor);
        if (containingClass == null) {
            return translateClassOrObjectCreation();
        }
        return translateAsObjectCreationExpressionWithEnclosingThisSaved(containingClass);
    }

    @NotNull
    private JsExpression translateAsObjectCreationExpressionWithEnclosingThisSaved(@NotNull ClassDescriptor containingClass) {
        TemporaryVariable thisAlias = context().declareTemporary(getThisObject(context(), containingClass));
        aliaser().setAliasForThis(containingClass, thisAlias.name());
        JsBinaryOperation result = newSequence(thisAlias.assignmentExpression(), translateClassOrObjectCreation());
        aliaser().removeAliasForThis(containingClass);
        return result;
    }

    @NotNull
    private JsInvocation translateClassOrObjectCreation() {
        JsInvocation jsClassDeclaration = classCreateMethodInvocation();
        addSuperclassReferences(jsClassDeclaration);
        addClassOwnDeclarations(jsClassDeclaration);
        return jsClassDeclaration;
    }

    @NotNull
    private JsInvocation classCreateMethodInvocation() {
        if (isObject()) {
            return AstUtil.newInvocation(context().namer().objectCreationMethodReference());
        }
        else if (isTrait()) {
            return AstUtil.newInvocation(context().namer().traitCreationMethodReference());
        }
        else {
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
        //NOTE: aliasing here is needed for the declaration generation step
        if (aliaser().hasAliasForDeclaration(superClassDescriptor)) {
            return aliaser().getAliasForDeclaration(superClassDescriptor).makeRef();
        }
        return getQualifiedReference(context(), superClassDescriptor);
    }

    @Nullable
    private static ClassDescriptor findAndRemoveAncestorClass(@NotNull List<ClassDescriptor> superclassDescriptors) {
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
        return JsAstUtils.newObjectLiteral(propertyList);
    }

    @NotNull
    private List<JsPropertyInitializer> translatePropertiesAsConstructorParameters() {
        List<JsPropertyInitializer> result = new ArrayList<JsPropertyInitializer>();
        for (JetParameter parameter : getPrimaryConstructorParameters(classDeclaration)) {
            PropertyDescriptor descriptor =
                    getPropertyDescriptorForConstructorParameter(bindingContext(), parameter);
            if (descriptor != null) {
                result.addAll(PropertyTranslator.translateAccessors(descriptor, context()));
            }
        }
        return result;
    }
}
