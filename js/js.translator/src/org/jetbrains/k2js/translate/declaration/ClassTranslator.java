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
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetObjectDeclaration;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.k2js.translate.context.DefinitionPlace;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.expression.LiteralFunctionTranslator;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.initializer.ClassInitializerTranslator;
import org.jetbrains.k2js.translate.utils.JsAstUtils;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.*;
import static org.jetbrains.jet.lang.types.TypeUtils.topologicallySortSuperclassesAndRecordAllInstances;
import static org.jetbrains.k2js.translate.initializer.InitializerUtils.createClassObjectInitializer;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getClassDescriptor;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getPropertyDescriptorForConstructorParameter;
import static org.jetbrains.k2js.translate.utils.JsDescriptorUtils.getContainingClass;
import static org.jetbrains.k2js.translate.utils.JsDescriptorUtils.getSupertypesWithoutFakes;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getPrimaryConstructorParameters;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.simpleReturnFunction;

/**
 * Generates a definition of a single class.
 */
public final class ClassTranslator extends AbstractTranslator {
    @NotNull
    private final JetClassOrObject classDeclaration;

    @NotNull
    private final ClassDescriptor descriptor;

    @NotNull
    public static JsInvocation generateClassCreation(@NotNull JetClassOrObject classDeclaration, @NotNull TranslationContext context) {
        return new ClassTranslator(classDeclaration, context).translate();
    }

    @NotNull
    public static JsInvocation generateClassCreation(@NotNull JetClassOrObject classDeclaration,
            @NotNull ClassDescriptor descriptor,
            @NotNull TranslationContext context) {
        return new ClassTranslator(classDeclaration, descriptor, context).translate();
    }

    @NotNull
    public static JsExpression generateObjectLiteral(
            @NotNull JetObjectDeclaration objectDeclaration,
            @NotNull TranslationContext context
    ) {
        return new ClassTranslator(objectDeclaration, context).translateObjectLiteralExpression();
    }

    @NotNull
    public static JsExpression generateObjectLiteral(
            @NotNull JetObjectDeclaration objectDeclaration,
            @NotNull ClassDescriptor descriptor,
            @NotNull TranslationContext context
    ) {
        return new ClassTranslator(objectDeclaration, descriptor, context).translateObjectLiteralExpression();
    }

    ClassTranslator(
            @NotNull JetClassOrObject classDeclaration,
            @NotNull TranslationContext context
    ) {
        this(classDeclaration, getClassDescriptor(context.bindingContext(), classDeclaration), context);
    }

    ClassTranslator(
            @NotNull JetClassOrObject classDeclaration,
            @NotNull ClassDescriptor descriptor,
            @NotNull TranslationContext context
    ) {
        super(context);
        this.descriptor = descriptor;
        this.classDeclaration = classDeclaration;
    }

    @NotNull
    private JsExpression translateObjectLiteralExpression() {
        ClassDescriptor containingClass = getContainingClass(descriptor);
        if (containingClass == null) {
            return translate(context());
        }
        return new LiteralFunctionTranslator(context()).translate(containingClass, context(), classDeclaration, descriptor, this);
    }

    @NotNull
    public JsInvocation translate() {
        return translate(context());
    }

    @NotNull
    public JsInvocation translate(@NotNull TranslationContext declarationContext) {
        return new JsInvocation(context().namer().classCreateInvocation(descriptor), getClassCreateInvocationArguments(declarationContext));
    }

    private boolean isTrait() {
        return descriptor.getKind().equals(ClassKind.TRAIT);
    }

    private List<JsExpression> getClassCreateInvocationArguments(@NotNull TranslationContext declarationContext) {
        List<JsExpression> invocationArguments = new ArrayList<JsExpression>();

        List<JsPropertyInitializer> properties = new SmartList<JsPropertyInitializer>();
        List<JsPropertyInitializer> staticProperties = new SmartList<JsPropertyInitializer>();

        boolean isTopLevelDeclaration = context() == declarationContext;

        JsNameRef qualifiedReference;
        if (!isTopLevelDeclaration) {
            qualifiedReference = null;
        }
        else {
            DefinitionPlace definitionPlace;
            if (descriptor.getKind().isSingleton() || isAnonymousObject(descriptor)) {
                qualifiedReference = null;
                definitionPlace = new DefinitionPlace(properties, context().getThisObject(descriptor));
            }
            else {
                qualifiedReference = declarationContext.getQualifiedReference(descriptor);
                definitionPlace = new DefinitionPlace(staticProperties, qualifiedReference);
            }
            declarationContext = declarationContext.newDeclaration(descriptor, definitionPlace);
        }

        invocationArguments.add(getSuperclassReferences(declarationContext));
        if (!isTrait()) {
            JsFunction initializer = new ClassInitializerTranslator(classDeclaration, declarationContext).generateInitializeMethod();
            invocationArguments.add(initializer.getBody().getStatements().isEmpty() ? JsLiteral.NULL : initializer);
        }

        translatePropertiesAsConstructorParameters(declarationContext, properties);
        DeclarationBodyVisitor bodyVisitor = new DeclarationBodyVisitor(properties, staticProperties);
        bodyVisitor.traverseContainer(classDeclaration, declarationContext);
        mayBeAddEnumEntry(bodyVisitor.getEnumEntryList(), staticProperties, declarationContext);

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
        return invocationArguments;
    }

    private void mayBeAddEnumEntry(@NotNull List<JsPropertyInitializer> enumEntryList,
            @NotNull List<JsPropertyInitializer> staticProperties,
            @NotNull TranslationContext declarationContext
    ) {
        if (descriptor.getKind() == ClassKind.ENUM_CLASS) {
            JsInvocation invocation = context().namer().enumEntriesObjectCreateInvocation();
            invocation.getArguments().add(new JsObjectLiteral(enumEntryList, true));

            JsFunction fun = simpleReturnFunction(declarationContext.getScopeForDescriptor(descriptor), invocation);
            staticProperties.add(createClassObjectInitializer(fun, declarationContext));
        } else {
            assert enumEntryList.isEmpty(): "Only enum class may have enum entry. Class kind is: " + descriptor.getKind();
        }
    }

    private JsExpression getSuperclassReferences(@NotNull TranslationContext declarationContext) {
        List<JsExpression> superClassReferences = getSupertypesNameReferences();
        if (superClassReferences.isEmpty()) {
            return JsLiteral.NULL;
        } else {
            return simpleReturnFunction(declarationContext.scope(), new JsArrayLiteral(superClassReferences));
        }
    }

    @NotNull
    private List<JsExpression> getSupertypesNameReferences() {
        List<JetType> supertypes = getSupertypesWithoutFakes(descriptor);
        if (supertypes.isEmpty()) {
            return Collections.emptyList();
        }
        if (supertypes.size() == 1) {
            JetType type = supertypes.get(0);
            ClassDescriptor supertypeDescriptor = getClassDescriptorForType(type);
            return Collections.<JsExpression>singletonList(getClassReference(supertypeDescriptor));
        }

        Set<TypeConstructor> supertypeConstructors = new HashSet<TypeConstructor>();
        for (JetType type : supertypes) {
            supertypeConstructors.add(type.getConstructor());
        }
        List<TypeConstructor> sortedAllSuperTypes = topologicallySortSuperclassesAndRecordAllInstances(descriptor.getDefaultType(),
                                                                                                       new HashMap<TypeConstructor, Set<JetType>>(),
                                                                                                       new HashSet<TypeConstructor>());
        List<JsExpression> supertypesRefs = new ArrayList<JsExpression>();
        for (TypeConstructor typeConstructor : sortedAllSuperTypes) {
            if (supertypeConstructors.contains(typeConstructor)) {
                ClassDescriptor supertypeDescriptor = getClassDescriptorForTypeConstructor(typeConstructor);
                supertypesRefs.add(getClassReference(supertypeDescriptor));
            }
        }
        return supertypesRefs;
    }

    @NotNull
    private JsNameRef getClassReference(@NotNull ClassDescriptor superClassDescriptor) {
        return context().getQualifiedReference(superClassDescriptor);
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
