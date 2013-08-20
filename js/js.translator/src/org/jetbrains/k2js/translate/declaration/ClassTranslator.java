/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.Trinity;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetObjectDeclaration;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.k2js.translate.LabelGenerator;
import org.jetbrains.k2js.translate.context.Namer;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.initializer.ClassInitializerTranslator;
import org.jetbrains.k2js.translate.utils.AnnotationsUtils;
import org.jetbrains.k2js.translate.utils.JsAstUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getClassDescriptorForType;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isNotAny;
import static org.jetbrains.k2js.translate.expression.LiteralFunctionTranslator.createPlace;
import static org.jetbrains.k2js.translate.initializer.InitializerUtils.createPropertyInitializer;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getClassDescriptor;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getPropertyDescriptorForConstructorParameter;
import static org.jetbrains.k2js.translate.utils.JsDescriptorUtils.getContainingClass;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getPrimaryConstructorParameters;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.getQualifiedReference;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.simpleReturnFunction;

/**
 * Generates a definition of a single class.
 */
public final class ClassTranslator extends AbstractTranslator {
    @NotNull
    private final JetClassOrObject classDeclaration;

    @NotNull
    private final ClassDescriptor descriptor;

    @Nullable
    private final ClassAliasingMap aliasingMap;

    @NotNull
    public static JsInvocation generateClassCreation(@NotNull JetClassOrObject classDeclaration, @NotNull TranslationContext context) {
        return new ClassTranslator(classDeclaration, null, context).translate();
    }

    @NotNull
    public static JsInvocation generateClassCreation(@NotNull JetClassOrObject classDeclaration,
            @NotNull ClassDescriptor descriptor,
            @NotNull TranslationContext context) {
        return new ClassTranslator(classDeclaration, descriptor, null, context).translate();
    }

    @NotNull
    public static JsExpression generateObjectLiteral(
            @NotNull JetObjectDeclaration objectDeclaration,
            @NotNull TranslationContext context
    ) {
        return new ClassTranslator(objectDeclaration, null, context).translateObjectLiteralExpression();
    }

    @NotNull
    public static JsExpression generateObjectLiteral(
            @NotNull JetObjectDeclaration objectDeclaration,
            @NotNull ClassDescriptor descriptor,
            @NotNull TranslationContext context
    ) {
        return new ClassTranslator(objectDeclaration, descriptor, null, context).translateObjectLiteralExpression();
    }

    ClassTranslator(
            @NotNull JetClassOrObject classDeclaration,
            @Nullable ClassAliasingMap aliasingMap,
            @NotNull TranslationContext context
    ) {
        this(classDeclaration, getClassDescriptor(context.bindingContext(), classDeclaration), aliasingMap, context);
    }

    ClassTranslator(@NotNull JetClassOrObject classDeclaration,
            @NotNull ClassDescriptor descriptor,
            @Nullable ClassAliasingMap aliasingMap,
            @NotNull TranslationContext context) {
        super(context);
        this.aliasingMap = aliasingMap;
        this.descriptor = descriptor;
        this.classDeclaration = classDeclaration;
    }

    @NotNull
    private JsExpression translateObjectLiteralExpression() {
        ClassDescriptor containingClass = getContainingClass(descriptor);
        if (containingClass == null) {
            return translate(context());
        }
        return context().literalFunctionTranslator().translate(containingClass, context(), classDeclaration, descriptor, this);
    }

    @NotNull
    public JsInvocation translate() {
        return translate(context());
    }

    @NotNull
    public JsInvocation translate(@NotNull TranslationContext declarationContext) {
        JsInvocation createInvocation = context().namer().classCreateInvocation(descriptor);
        translate(createInvocation, declarationContext);
        return createInvocation;
    }

    private void translate(@NotNull JsInvocation createInvocation, @NotNull TranslationContext context) {
        addSuperclassReferences(createInvocation);
        addClassOwnDeclarations(createInvocation.getArguments(), context);
    }

    private boolean isTrait() {
        return descriptor.getKind().equals(ClassKind.TRAIT);
    }

    private void addClassOwnDeclarations(@NotNull List<JsExpression> invocationArguments, @NotNull TranslationContext declarationContext) {
        final List<JsPropertyInitializer> properties = new SmartList<JsPropertyInitializer>();

        final List<JsPropertyInitializer> staticProperties = new SmartList<JsPropertyInitializer>();
        boolean isTopLevelDeclaration = context() == declarationContext;
        final JsNameRef qualifiedReference;
        if (!isTopLevelDeclaration) {
            qualifiedReference = null;
        }
        else if (descriptor.getKind().isObject()) {
            qualifiedReference = null;
            declarationContext.literalFunctionTranslator().setDefinitionPlace(
                    new NotNullLazyValue<Trinity<List<JsPropertyInitializer>, LabelGenerator, JsExpression>>() {
                        @Override
                        @NotNull
                        public Trinity<List<JsPropertyInitializer>, LabelGenerator, JsExpression> compute() {
                            return createPlace(properties, context().getThisObject(descriptor));
                        }
                    });
        }
        else {
            qualifiedReference = getQualifiedReference(declarationContext, descriptor);
            declarationContext.literalFunctionTranslator().setDefinitionPlace(
                    new NotNullLazyValue<Trinity<List<JsPropertyInitializer>, LabelGenerator, JsExpression>>() {
                        @Override
                        @NotNull
                        public Trinity<List<JsPropertyInitializer>, LabelGenerator, JsExpression> compute() {
                            return createPlace(staticProperties, qualifiedReference);
                        }
                    });
        }

        if (!isTrait()) {
            JsFunction initializer = new ClassInitializerTranslator(classDeclaration, declarationContext).generateInitializeMethod();
            if (context().isEcma5()) {
                invocationArguments.add(initializer.getBody().getStatements().isEmpty() ? JsLiteral.NULL : initializer);
            }
            else {
                properties.add(new JsPropertyInitializer(Namer.initializeMethodReference(), initializer));
            }
        }

        translatePropertiesAsConstructorParameters(declarationContext, properties);
        DeclarationBodyVisitor bodyVisitor = new DeclarationBodyVisitor(properties, staticProperties);
        bodyVisitor.traverseContainer(classDeclaration, declarationContext);
        mayBeAddEnumEntry(bodyVisitor.getEnumEntryList(), staticProperties, declarationContext);

        if (isTopLevelDeclaration) {
            declarationContext.literalFunctionTranslator().setDefinitionPlace(null);
        }

        boolean hasStaticProperties = !staticProperties.isEmpty();
        if (!properties.isEmpty() || hasStaticProperties) {
            if (properties.isEmpty()) {
                invocationArguments.add(JsLiteral.NULL);
            }
            else {
                if (qualifiedReference != null) {
                    // about "prototype" - see http://code.google.com/p/jsdoc-toolkit/wiki/TagLends
                    invocationArguments.add(new JsDocComment(JsAstUtils.LENDS_JS_DOC_TAG, new JsNameRef("prototype", qualifiedReference)));
                }
                invocationArguments.add(new JsObjectLiteral(properties, true));
            }
        }
        if (hasStaticProperties) {
            invocationArguments.add(new JsDocComment(JsAstUtils.LENDS_JS_DOC_TAG, qualifiedReference));
            invocationArguments.add(new JsObjectLiteral(staticProperties, true));
        }
    }

    private void mayBeAddEnumEntry(@NotNull List<JsPropertyInitializer> enumEntryList,
            @NotNull List<JsPropertyInitializer> staticProperties,
            @NotNull TranslationContext declarationContext
    ) {
        if (descriptor.getKind() == ClassKind.ENUM_CLASS) {
            JsInvocation invocation = context().namer().enumEntriesObjectCreateInvocation();
            invocation.getArguments().add(new JsObjectLiteral(enumEntryList, true));

            JsFunction fun = simpleReturnFunction(declarationContext.getScopeForDescriptor(descriptor), invocation);
            staticProperties.add(createPropertyInitializer(Namer.getNamedForClassObjectInitializer(), fun, declarationContext));
        } else {
            assert enumEntryList.isEmpty(): "Only enum class may have enum entry. Class kind is: " + descriptor.getKind();
        }
    }

    private void addSuperclassReferences(@NotNull JsInvocation jsClassDeclaration) {
        List<JsExpression> superClassReferences = getSupertypesNameReferences();
        if (superClassReferences.isEmpty()) {
            if (!isTrait() || context().isEcma5()) {
                jsClassDeclaration.getArguments().add(JsLiteral.NULL);
            }
            return;
        }

        List<JsExpression> expressions;
        if (superClassReferences.size() > 1) {
            JsArrayLiteral arrayLiteral = new JsArrayLiteral();
            jsClassDeclaration.getArguments().add(arrayLiteral);
            expressions = arrayLiteral.getExpressions();
        }
        else {
            expressions = jsClassDeclaration.getArguments();
        }

        for (JsExpression superClassReference : superClassReferences) {
            expressions.add(superClassReference);
        }
    }

    @NotNull
    private List<JsExpression> getSupertypesNameReferences() {
        Collection<JetType> supertypes = descriptor.getTypeConstructor().getSupertypes();
        if (supertypes.isEmpty()) {
            return Collections.emptyList();
        }

        JsExpression base = null;
        List<JsExpression> list = null;
        for (JetType type : supertypes) {
            ClassDescriptor result = getClassDescriptorForType(type);
            if (isNotAny(result) && !AnnotationsUtils.isNativeObject(result)) {
                switch (result.getKind()) {
                    case CLASS:
                        base = getClassReference(result);
                        break;
                    case TRAIT:
                        if (list == null) {
                            list = new ArrayList<JsExpression>();
                        }
                        list.add(getClassReference(result));
                        break;
                    case ENUM_CLASS:
                        base = getClassReference(result);
                        break;

                    default:
                        throw new UnsupportedOperationException("unsupported super class kind " + result.getKind().name());
                }
            }
        }

        if (list == null) {
            return base == null ? Collections.<JsExpression>emptyList() : Collections.singletonList(base);
        }
        else if (base != null) {
            list.add(0, base);
        }

        return list;
    }

    @NotNull
    private JsNameRef getClassReference(@NotNull ClassDescriptor superClassDescriptor) {
        // aliasing here is needed for the declaration generation step
        if (aliasingMap != null) {
            JsNameRef name = aliasingMap.get(superClassDescriptor, descriptor);
            if (name != null) {
                return name;
            }
        }

        // from library
        return getQualifiedReference(context(), superClassDescriptor);
    }

    private void translatePropertiesAsConstructorParameters(@NotNull TranslationContext classDeclarationContext,
            @NotNull List<JsPropertyInitializer> result) {
        for (JetParameter parameter : getPrimaryConstructorParameters(classDeclaration)) {
            PropertyDescriptor descriptor = getPropertyDescriptorForConstructorParameter(bindingContext(), parameter);
            if (descriptor != null) {
                PropertyTranslator.translateAccessors(descriptor, result, classDeclarationContext);
            }
        }
    }
}
