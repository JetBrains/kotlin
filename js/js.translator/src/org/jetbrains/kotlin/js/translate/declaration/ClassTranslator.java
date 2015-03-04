/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.translate.declaration;

import com.google.dart.compiler.backend.js.ast.*;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.backend.common.CodegenUtil;
import org.jetbrains.kotlin.backend.common.bridges.Bridge;
import org.jetbrains.kotlin.backend.common.bridges.BridgesPackage;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.js.translate.context.DefinitionPlace;
import org.jetbrains.kotlin.js.translate.context.Namer;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.declaration.propertyTranslator.PropertyTranslatorPackage;
import org.jetbrains.kotlin.js.translate.expression.ExpressionPackage;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.js.translate.initializer.ClassInitializerTranslator;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.js.translate.utils.UtilsPackage;
import org.jetbrains.kotlin.psi.JetClassOrObject;
import org.jetbrains.kotlin.psi.JetObjectDeclaration;
import org.jetbrains.kotlin.psi.JetParameter;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeConstructor;

import java.util.*;

import static org.jetbrains.kotlin.js.translate.reference.ReferenceTranslator.translateAsFQReference;
import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getClassDescriptor;
import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getPropertyDescriptorForConstructorParameter;
import static org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils.getReceiverParameterForDeclaration;
import static org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils.getSupertypesWithoutFakes;
import static org.jetbrains.kotlin.js.translate.utils.PsiUtils.getPrimaryConstructorParameters;
import static org.jetbrains.kotlin.js.translate.utils.TranslationUtils.simpleReturnFunction;
import static org.jetbrains.kotlin.js.translate.utils.UtilsPackage.generateDelegateCall;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.*;
import static org.jetbrains.kotlin.types.TypeUtils.topologicallySortSuperclassesAndRecordAllInstances;

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
    public static JsExpression generateObjectLiteral(@NotNull JetObjectDeclaration objectDeclaration, @NotNull TranslationContext context) {
        return new ClassTranslator(objectDeclaration, context).translateObjectLiteralExpression();
    }

    private ClassTranslator(@NotNull JetClassOrObject classDeclaration, @NotNull TranslationContext context) {
        super(context);
        this.classDeclaration = classDeclaration;
        this.descriptor = getClassDescriptor(context.bindingContext(), classDeclaration);
    }

    @NotNull
    private JsExpression translateObjectLiteralExpression() {
        ClassDescriptor containingClass = getContainingClass(descriptor);
        if (containingClass == null) {
            return translate(context());
        }

        return translateObjectInsideClass(context());
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

    @NotNull
    private List<JsExpression> getClassCreateInvocationArguments(@NotNull TranslationContext declarationContext) {
        List<JsExpression> invocationArguments = new ArrayList<JsExpression>();

        List<JsPropertyInitializer> properties = new SmartList<JsPropertyInitializer>();
        List<JsPropertyInitializer> staticProperties = new SmartList<JsPropertyInitializer>();

        boolean isTopLevelDeclaration = context() == declarationContext;

        JsNameRef qualifiedReference = null;
        if (isTopLevelDeclaration) {
            DefinitionPlace definitionPlace = null;

            if (!descriptor.getKind().isSingleton() && !isAnonymousObject(descriptor)) {
                qualifiedReference = declarationContext.getQualifiedReference(descriptor);
                JsScope scope = context().getScopeForDescriptor(descriptor);
                definitionPlace = new DefinitionPlace((JsObjectScope) scope, qualifiedReference, staticProperties);
            }

            declarationContext = declarationContext.newDeclaration(descriptor, definitionPlace);
        }

        declarationContext = fixContextForDefaultObjectAccessing(declarationContext);

        invocationArguments.add(getSuperclassReferences(declarationContext));
        DelegationTranslator delegationTranslator = new DelegationTranslator(classDeclaration, context());
        if (!isTrait()) {
            JsFunction initializer = new ClassInitializerTranslator(classDeclaration, declarationContext).generateInitializeMethod(delegationTranslator);
            invocationArguments.add(initializer.getBody().getStatements().isEmpty() ? JsLiteral.NULL : initializer);
        }

        translatePropertiesAsConstructorParameters(declarationContext, properties);
        DeclarationBodyVisitor bodyVisitor = new DeclarationBodyVisitor(properties, staticProperties);
        bodyVisitor.traverseContainer(classDeclaration, declarationContext);
        delegationTranslator.generateDelegated(properties);

        if (KotlinBuiltIns.isData(descriptor)) {
            new JsDataClassGenerator(classDeclaration, declarationContext, properties).generate();
        }

        if (isEnumClass(descriptor)) {
            JsObjectLiteral enumEntries = new JsObjectLiteral(bodyVisitor.getEnumEntryList(), true);
            JsFunction function = simpleReturnFunction(declarationContext.getScopeForDescriptor(descriptor), enumEntries);
            invocationArguments.add(function);
        }

        generatedBridgeMethods(properties);

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

    private TranslationContext fixContextForDefaultObjectAccessing(TranslationContext declarationContext) {
        // In Kotlin we can access to default object members without qualifier just by name, but we should translate it to access with FQ name.
        // So create alias for default object receiver parameter.
        ClassDescriptor defaultObjectDescriptor = descriptor.getDefaultObjectDescriptor();
        if (defaultObjectDescriptor != null) {
            JsExpression referenceToClass = translateAsFQReference(defaultObjectDescriptor.getContainingDeclaration(), declarationContext);
            JsExpression defaultObjectAccessor = Namer.getDefaultObjectAccessor(referenceToClass);
            ReceiverParameterDescriptor defaultObjectReceiver = getReceiverParameterForDeclaration(defaultObjectDescriptor);
            declarationContext.aliasingContext().registerAlias(defaultObjectReceiver, defaultObjectAccessor);
        }

        // Overlap alias of class object receiver for accessing from containing class(see previous if block),
        // because inside class object we should use simple name for access.
        if (isDefaultObject(descriptor)) {
            declarationContext = declarationContext.innerContextWithAliased(descriptor.getThisAsReceiverParameter(), JsLiteral.THIS);
        }

        return declarationContext;
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
                PropertyTranslatorPackage.translateAccessors(descriptor, result, classDeclarationContext);
            }
        }
    }

    @NotNull
    private JsExpression translateObjectInsideClass(@NotNull TranslationContext outerClassContext) {
        JsFunction fun = new JsFunction(outerClassContext.scope(), new JsBlock(), "initializer for " + descriptor.getName().asString());
        TranslationContext funContext = outerClassContext.newFunctionBodyWithUsageTracker(fun, descriptor);

        fun.getBody().getStatements().add(new JsReturn(translate(funContext)));

        return ExpressionPackage.withCapturedParameters(fun, funContext, outerClassContext, descriptor);
    }

    private void generatedBridgeMethods(@NotNull List<JsPropertyInitializer> properties) {
        if (isTrait()) return;

        generateBridgesToTraitImpl(properties);

        generateOtherBridges(properties);
    }

    private void generateBridgesToTraitImpl(List<JsPropertyInitializer> properties) {
        for(Map.Entry<FunctionDescriptor, FunctionDescriptor> entry : CodegenUtil.getTraitMethods(descriptor).entrySet()) {
            if (!areNamesEqual(entry.getKey(), entry.getValue())) {
                properties.add(generateDelegateCall(entry.getValue(), entry.getKey(), JsLiteral.THIS, context()));
            }
        }
    }

    private void generateOtherBridges(List<JsPropertyInitializer> properties) {
        for (DeclarationDescriptor memberDescriptor : descriptor.getDefaultType().getMemberScope().getAllDescriptors()) {
            if (memberDescriptor instanceof FunctionDescriptor) {
                FunctionDescriptor functionDescriptor = (FunctionDescriptor) memberDescriptor;
                Set<Bridge<FunctionDescriptor>> bridgesToGenerate =
                        BridgesPackage.generateBridgesForFunctionDescriptor(functionDescriptor, UtilsPackage.<FunctionDescriptor>getID());

                for (Bridge<FunctionDescriptor> bridge : bridgesToGenerate) {
                    generateBridge(bridge, properties);
                }
            }
        }
    }

    private void generateBridge(
            @NotNull Bridge<FunctionDescriptor> bridge,
            @NotNull List<JsPropertyInitializer> properties
    ) {
        FunctionDescriptor fromDescriptor = bridge.getFrom();
        FunctionDescriptor toDescriptor = bridge.getTo();
        if (areNamesEqual(fromDescriptor, toDescriptor)) return;

        if (fromDescriptor.getKind().isReal() &&
            fromDescriptor.getModality() != Modality.ABSTRACT &&
            !toDescriptor.getKind().isReal()) return;

        properties.add(generateDelegateCall(fromDescriptor, toDescriptor, JsLiteral.THIS, context()));
    }

    private boolean areNamesEqual(@NotNull FunctionDescriptor first, @NotNull FunctionDescriptor second) {
        JsName firstName = context().getNameForDescriptor(first);
        JsName secondName = context().getNameForDescriptor(second);
        return firstName.getIdent().equals(secondName.getIdent());
    }
}
