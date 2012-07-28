/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.initializer.InitializerUtils;
import org.jetbrains.k2js.translate.utils.BindingUtils;
import org.jetbrains.k2js.translate.utils.JsAstUtils;

import java.util.ArrayList;
import java.util.List;

import static com.google.dart.compiler.util.AstUtil.newSequence;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getClassDescriptor;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getPropertyDescriptorForConstructorParameter;
import static org.jetbrains.k2js.translate.utils.JsDescriptorUtils.*;
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
        JsExpression classCreationExpression =
                generateClassCreationExpression(classDeclaration, context);
        JsName className = context.getNameForElement(classDeclaration);
        return new JsPropertyInitializer(className.makeRef(), classCreationExpression);
    }

    @NotNull
    public static JsExpression generateClassCreationExpression(@NotNull JetClassOrObject classDeclaration,
            @NotNull ClassAliasingMap aliasingMap,
            @NotNull TranslationContext context) {
        return (new ClassTranslator(classDeclaration, aliasingMap, context)).translateClassOrObjectCreation();
    }

    @NotNull
    public static JsExpression generateClassCreationExpression(@NotNull JetClassOrObject classDeclaration,
            @NotNull TranslationContext context) {
        return (new ClassTranslator(classDeclaration, null, context)).translateClassOrObjectCreation();
    }

    @NotNull
    public static JsExpression generateObjectLiteralExpression(@NotNull JetObjectLiteralExpression objectLiteralExpression,
            @NotNull TranslationContext outerContext) {
        JetObjectDeclaration objectDeclaration = objectLiteralExpression.getObjectDeclaration();
        ClassTranslator classTranslator = new ClassTranslator(objectDeclaration, null, outerContext);
        ClassDescriptor containingClass = getContainingClass(classTranslator.descriptor);
        if (containingClass == null) {
            return classTranslator.translateClassOrObjectCreation();
        }
        return classTranslator.translateObjectLiteralWithThisAliased(containingClass, outerContext);
    }


    @NotNull
    private final DeclarationBodyVisitor declarationBodyVisitor = new DeclarationBodyVisitor();

    @NotNull
    private final JetClassOrObject classDeclaration;

    @Nullable
    private TemporaryVariable aliasForContainingClassThis = null;

    @NotNull
    private final ClassDescriptor descriptor;

    @Nullable
    private final ClassAliasingMap aliasingMap;

    private ClassTranslator(@NotNull JetClassOrObject classDeclaration,
            @Nullable ClassAliasingMap aliasingMap,
            @NotNull TranslationContext context) {
        super(context.newDeclaration(classDeclaration));
        this.aliasingMap = aliasingMap;
        this.descriptor = getClassDescriptor(context.bindingContext(), classDeclaration);
        this.classDeclaration = classDeclaration;
    }

    @NotNull
    private JsExpression translateClassOrObjectCreation() {
        JsInvocation jsClassDeclaration = classCreateMethodInvocation();
        if (!isObject()) {
            addSuperclassReferences(jsClassDeclaration);
        }
        addClassOwnDeclarations(jsClassDeclaration);
        return jsClassDeclaration;
    }

    @NotNull
    private JsInvocation classCreateMethodInvocation() {
        if (isObject()) {
            return AstUtil.newInvocation(context().namer().objectCreationMethodReference());
        }
        else if (isTrait() && !context().isEcma5()) {
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
        TranslationContext classDeclarationContext = getClassDeclarationContext();
        JsObjectLiteral properties = new JsObjectLiteral();
        List<JsPropertyInitializer> propertyList = properties.getPropertyInitializers();
        if (!isTrait()) {
            JsFunction initializer = Translation.generateClassInitializerMethod(classDeclaration, classDeclarationContext);
            if (context().isEcma5()) {
                jsClassDeclaration.getArguments()
                        .add(initializer.getName() == null ? initializer : JsAstUtils.encloseFunction(initializer));
            }
            else {
                propertyList.add(InitializerUtils.generateInitializeMethod(initializer));
            }
        }
        else if (context().isEcma5()) {
            jsClassDeclaration.getArguments().add(context().program().getNullLiteral());
        }

        propertyList.addAll(translatePropertiesAsConstructorParameters(classDeclarationContext));
        propertyList.addAll(declarationBodyVisitor.traverseClass(classDeclaration, classDeclarationContext));

        if (!propertyList.isEmpty() || !context().isEcma5()) {
            jsClassDeclaration.getArguments().add(properties);
        }
    }

    private void addSuperclassReferences(@NotNull JsInvocation jsClassDeclaration) {
        List<JsExpression> superClassReferences = getSuperclassNameReferences();
        List<JsExpression> expressions = jsClassDeclaration.getArguments();
        if (context().isEcma5()) {
            if (superClassReferences.isEmpty()) {
                jsClassDeclaration.getArguments().add(context().program().getNullLiteral());
                return;
            }
            else if (superClassReferences.size() > 1) {
                JsArrayLiteral arrayLiteral = new JsArrayLiteral();
                jsClassDeclaration.getArguments().add(arrayLiteral);
                expressions = arrayLiteral.getExpressions();
            }
        }

        for (JsExpression superClassReference : superClassReferences) {
            expressions.add(superClassReference);
        }
    }

    @NotNull
    private List<JsExpression> getSuperclassNameReferences() {
        List<JsExpression> superclassReferences = new ArrayList<JsExpression>();
        List<ClassDescriptor> superclassDescriptors = DescriptorUtils.getSuperclassDescriptors(descriptor);
        addAncestorClass(superclassReferences, superclassDescriptors);
        addTraits(superclassReferences, superclassDescriptors);
        return superclassReferences;
    }

    private void addTraits(@NotNull List<JsExpression> superclassReferences,
            @NotNull List<ClassDescriptor> superclassDescriptors) {
        for (ClassDescriptor superClassDescriptor : superclassDescriptors) {
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
        // aliasing here is needed for the declaration generation step
        if (aliasingMap != null) {
            JsNameRef name = aliasingMap.get(BindingUtils.getClassForDescriptor(bindingContext(), superClassDescriptor),
                                             (JetClass) classDeclaration);
            if (name != null) {
                return name;
            }
        }

        // from library
        return getQualifiedReference(context(), superClassDescriptor);
    }

    @Nullable
    private static ClassDescriptor findAndRemoveAncestorClass(@NotNull List<ClassDescriptor> superclassDescriptors) {
        ClassDescriptor ancestorClass = findAncestorClass(superclassDescriptors);
        superclassDescriptors.remove(ancestorClass);
        return ancestorClass;
    }

    @NotNull
    private List<JsPropertyInitializer> translatePropertiesAsConstructorParameters(@NotNull TranslationContext classDeclarationContext) {
        List<JsPropertyInitializer> result = new ArrayList<JsPropertyInitializer>();
        for (JetParameter parameter : getPrimaryConstructorParameters(classDeclaration)) {
            PropertyDescriptor descriptor =
                    getPropertyDescriptorForConstructorParameter(bindingContext(), parameter);
            if (descriptor != null) {
                result.addAll(PropertyTranslator.translateAccessors(descriptor, classDeclarationContext));
            }
        }
        return result;
    }

    @NotNull
    private TranslationContext getClassDeclarationContext() {
        if (aliasForContainingClassThis == null) {
            return context();
        }
        ClassDescriptor containingClass = getContainingClass(descriptor);
        assert containingClass != null;
        return context().innerContextWithThisAliased(containingClass, aliasForContainingClassThis.name());
    }

    @NotNull
    private JsExpression translateObjectLiteralWithThisAliased(@NotNull ClassDescriptor containingClass,
            @NotNull TranslationContext outerContext) {
        aliasForContainingClassThis = outerContext.declareTemporary(getThisObject(outerContext, containingClass));
        return newSequence(aliasForContainingClassThis.assignmentExpression(), translateClassOrObjectCreation());
    }
}
