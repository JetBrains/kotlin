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

package org.jetbrains.kotlin.js.translate.declaration

import com.google.dart.compiler.backend.js.ast.*
import com.intellij.util.SmartList
import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.backend.common.bridges.Bridge
import org.jetbrains.kotlin.backend.common.bridges.generateBridgesForFunctionDescriptor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.descriptorUtils.hasPrimaryConstructor
import org.jetbrains.kotlin.js.resolve.diagnostics.ErrorsJs
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator
import org.jetbrains.kotlin.js.translate.context.DefinitionPlace
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.declaration.propertyTranslator.translateAccessors
import org.jetbrains.kotlin.js.translate.expression.FunctionTranslator
import org.jetbrains.kotlin.js.translate.expression.withCapturedParameters
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator
import org.jetbrains.kotlin.js.translate.initializer.ClassInitializerTranslator
import org.jetbrains.kotlin.js.translate.reference.ReferenceTranslator.translateAsFQReference
import org.jetbrains.kotlin.js.translate.utils.*
import org.jetbrains.kotlin.js.translate.utils.BindingUtils.getClassDescriptor
import org.jetbrains.kotlin.js.translate.utils.BindingUtils.getPropertyDescriptorForConstructorParameter
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils.getReceiverParameterForDeclaration
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils.getSupertypesWithoutFakes
import org.jetbrains.kotlin.js.translate.utils.PsiUtils.getPrimaryConstructorParameters
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils.simpleReturnFunction
import org.jetbrains.kotlin.js.translate.utils.jsAstUtils.toInvocationWith
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils.*
import org.jetbrains.kotlin.types.CommonSupertypes.topologicallySortSuperclassesAndRecordAllInstances
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.utils.identity
import java.util.*

/**
 * Generates a definition of a single class.
 */
class ClassTranslator private constructor(
        private val classDeclaration: KtClassOrObject,
        context: TranslationContext
) : AbstractTranslator(context) {

    private val descriptor = getClassDescriptor(context.bindingContext(), classDeclaration)

    private fun translateObjectLiteralExpression(): JsExpression {
        getContainingClass(descriptor) ?: return translate(context())

        return translateObjectInsideClass(context())
    }

    private fun translate(declarationContext: TranslationContext = context()): JsInvocation {
        return JsInvocation(context().namer().classCreateInvocation(descriptor), getClassCreateInvocationArguments(declarationContext))
    }

    private fun isTrait(): Boolean = descriptor.kind == ClassKind.INTERFACE

    private fun getClassCreateInvocationArguments(declarationContext: TranslationContext): List<JsExpression> {
        if (!DescriptorUtils.isAnonymousObject(descriptor) && !DescriptorUtils.isObject(descriptor) &&
            declarationContext.hasEnclosingFunction()) {
            declarationContext.bindingTrace().report(ErrorsJs.NOT_SUPPORTED.on(classDeclaration, classDeclaration))
            return emptyList()
        }
        var context = declarationContext
        val invocationArguments = ArrayList<JsExpression>()

        val properties = SmartList<JsPropertyInitializer>()
        val staticProperties = SmartList<JsPropertyInitializer>()

        val isTopLevelDeclaration = context() == context

        var qualifiedReference: JsNameRef? = null
        if (isTopLevelDeclaration) {
            var definitionPlace: DefinitionPlace? = null

            if (!descriptor.kind.isSingleton && !isAnonymousObject(descriptor)) {
                qualifiedReference = context.getQualifiedReference(descriptor)
                val scope = context().getScopeForDescriptor(descriptor)
                definitionPlace = DefinitionPlace(scope as JsObjectScope, qualifiedReference, staticProperties)
            }

            context = context.newDeclaration(descriptor, definitionPlace)
        }

        context = fixContextForCompanionObjectAccessing(context)

        invocationArguments.add(getSuperclassReferences(context))
        val delegationTranslator = DelegationTranslator(classDeclaration, context())
        if (!isTrait()) {
            val initializer = ClassInitializerTranslator(classDeclaration, context).generateInitializeMethod(delegationTranslator)
            invocationArguments.add(if (initializer.body.statements.isEmpty()) JsLiteral.NULL else initializer)
        }

        translatePropertiesAsConstructorParameters(context, properties)
        val bodyVisitor = DeclarationBodyVisitor(properties, staticProperties)
        bodyVisitor.traverseContainer(classDeclaration, context)
        delegationTranslator.generateDelegated(properties)

        if (descriptor.isData) {
            JsDataClassGenerator(classDeclaration, context, properties).generate()
        }

        if (isEnumClass(descriptor)) {
            val enumEntries = JsObjectLiteral(bodyVisitor.getEnumEntryList(), true)
            val function = simpleReturnFunction(context.getScopeForDescriptor(descriptor), enumEntries)
            invocationArguments.add(function)
        }

        generatedBridgeMethods(properties)

        val hasStaticProperties = !staticProperties.isEmpty()
        if (!properties.isEmpty() || hasStaticProperties) {
            if (properties.isEmpty()) {
                invocationArguments.add(JsLiteral.NULL)
            }
            else {
                if (qualifiedReference != null) {
                    // about "prototype" - see http://code.google.com/p/jsdoc-toolkit/wiki/TagLends
                    invocationArguments.add(JsDocComment(JsAstUtils.LENDS_JS_DOC_TAG, JsNameRef("prototype", qualifiedReference)))
                }
                invocationArguments.add(JsObjectLiteral(properties, true))
            }
        }
        if (hasStaticProperties) {
            invocationArguments.add(JsDocComment(JsAstUtils.LENDS_JS_DOC_TAG, qualifiedReference))
            invocationArguments.add(JsObjectLiteral(staticProperties, true))
        }

        return invocationArguments
    }

    private fun fixContextForCompanionObjectAccessing(context: TranslationContext): TranslationContext {
        // In Kotlin we can access to companion object members without qualifier just by name, but we should translate it to access with FQ name.
        // So create alias for companion object receiver parameter.
        val companionObjectDescriptor = descriptor.companionObjectDescriptor
        if (companionObjectDescriptor != null) {
            val referenceToClass = translateAsFQReference(companionObjectDescriptor.containingDeclaration, context)
            val companionObjectAccessor = Namer.getCompanionObjectAccessor(referenceToClass)
            val companionObjectReceiver = getReceiverParameterForDeclaration(companionObjectDescriptor)
            context.aliasingContext().registerAlias(companionObjectReceiver, companionObjectAccessor)
        }

        // Overlap alias of companion object receiver for accessing from containing class(see previous if block),
        // because inside companion object we should use simple name for access.
        if (isCompanionObject(descriptor)) {
            return context.innerContextWithAliased(descriptor.thisAsReceiverParameter, JsLiteral.THIS)
        }

        return context
    }

    private fun getSuperclassReferences(declarationContext: TranslationContext): JsExpression {
        val superClassReferences = getSupertypesNameReferences()
        if (superClassReferences.isEmpty()) {
            return JsLiteral.NULL
        }
        else {
            return simpleReturnFunction(declarationContext.scope(), JsArrayLiteral(superClassReferences))
        }
    }

    private fun getSupertypesNameReferences(): List<JsExpression> {
        val supertypes = getSupertypesWithoutFakes(descriptor)
        if (supertypes.isEmpty()) {
            return emptyList()
        }
        if (supertypes.size == 1) {
            val type = supertypes.get(0)
            val supertypeDescriptor = getClassDescriptorForType(type)
            return listOf<JsExpression>(getClassReference(supertypeDescriptor))
        }

        val supertypeConstructors = HashSet<TypeConstructor>()
        for (type in supertypes) {
            supertypeConstructors.add(type.constructor)
        }
        val sortedAllSuperTypes = topologicallySortSuperclassesAndRecordAllInstances(descriptor.defaultType, HashMap<TypeConstructor, Set<KotlinType>>(), HashSet<TypeConstructor>())
        val supertypesRefs = ArrayList<JsExpression>()
        for (typeConstructor in sortedAllSuperTypes) {
            if (supertypeConstructors.contains(typeConstructor)) {
                val supertypeDescriptor = getClassDescriptorForTypeConstructor(typeConstructor)
                supertypesRefs.add(getClassReference(supertypeDescriptor))
            }
        }
        return supertypesRefs
    }

    private fun getClassReference(superClassDescriptor: ClassDescriptor): JsNameRef {
        return context().getQualifiedReference(superClassDescriptor)
    }

    private fun translatePropertiesAsConstructorParameters(classDeclarationContext: TranslationContext, result: MutableList<JsPropertyInitializer>) {
        for (parameter in getPrimaryConstructorParameters(classDeclaration)) {
            val descriptor = getPropertyDescriptorForConstructorParameter(bindingContext(), parameter)
            if (descriptor != null) {
                translateAccessors(descriptor, result, classDeclarationContext)
            }
        }
    }

    private fun translateObjectInsideClass(outerClassContext: TranslationContext): JsExpression {
        val function = JsFunction(outerClassContext.scope(), JsBlock(), "initializer for " + descriptor.name.asString())
        val funContext = outerClassContext.newFunctionBodyWithUsageTracker(function, descriptor)

        function.body.statements.add(JsReturn(translate(funContext)))

        return function.withCapturedParameters(funContext, outerClassContext, descriptor)
    }

    private fun generatedBridgeMethods(properties: MutableList<JsPropertyInitializer>) {
        if (isTrait()) return

        generateBridgesToTraitImpl(properties)

        generateOtherBridges(properties)
    }

    private fun generateBridgesToTraitImpl(properties: MutableList<JsPropertyInitializer>) {
        for (entry in CodegenUtil.getNonPrivateTraitMethods(descriptor).entries) {
            if (!areNamesEqual(entry.key, entry.value)) {
                properties.add(generateDelegateCall(entry.value, entry.key, JsLiteral.THIS, context()))
            }
        }
    }

    private fun generateOtherBridges(properties: MutableList<JsPropertyInitializer>) {
        for (memberDescriptor in descriptor.defaultType.memberScope.getContributedDescriptors()) {
            if (memberDescriptor is FunctionDescriptor) {
                val bridgesToGenerate = generateBridgesForFunctionDescriptor(memberDescriptor, identity())

                for (bridge in bridgesToGenerate) {
                    generateBridge(bridge, properties)
                }
            }
        }
    }

    private fun generateBridge(bridge: Bridge<FunctionDescriptor>, properties: MutableList<JsPropertyInitializer>) {
        val fromDescriptor = bridge.from
        val toDescriptor = bridge.to
        if (areNamesEqual(fromDescriptor, toDescriptor)) return

        if (fromDescriptor.kind.isReal && fromDescriptor.modality != Modality.ABSTRACT && !toDescriptor.kind.isReal)
            return

        properties.add(generateDelegateCall(fromDescriptor, toDescriptor, JsLiteral.THIS, context()))
    }

    private fun areNamesEqual(first: FunctionDescriptor, second: FunctionDescriptor): Boolean {
        val firstName = context().getNameForDescriptor(first)
        val secondName = context().getNameForDescriptor(second)
        return firstName.ident == secondName.ident
    }

    companion object {
        fun translate(classDeclaration: KtClass, context: TranslationContext): List<JsPropertyInitializer> {
            val result = arrayListOf<JsPropertyInitializer>()

            val classDescriptor = getClassDescriptor(context.bindingContext(), classDeclaration)
            val classNameRef = context.getNameForDescriptor(classDescriptor).makeRef()
            val classCreation = generateClassCreation(classDeclaration, context)

            result.add(JsPropertyInitializer(classNameRef, classCreation))

            classDeclaration.getSecondaryConstructors().forEach {
                result.add(generateSecondaryConstructor(it, context))
            }

            return result
        }

        @JvmStatic fun generateClassCreation(classDeclaration: KtClassOrObject, context: TranslationContext): JsInvocation {
            return ClassTranslator(classDeclaration, context).translate()
        }

        @JvmStatic fun generateObjectLiteral(objectDeclaration: KtObjectDeclaration, context: TranslationContext): JsExpression {
            return ClassTranslator(objectDeclaration, context).translateObjectLiteralExpression()
        }

        private fun generateSecondaryConstructor(constructor: KtSecondaryConstructor, context: TranslationContext): JsPropertyInitializer {
            val constructorDescriptor = BindingUtils.getDescriptorForElement(context.bindingContext(), constructor) as ConstructorDescriptor
            val classDescriptor = constructorDescriptor.containingDeclaration

            val constructorScope = context.getScopeForDescriptor(constructorDescriptor)
            val thisName = constructorScope.declareName(Namer.ANOTHER_THIS_PARAMETER_NAME)
            val thisNameRef = thisName.makeRef()
            val receiverDescriptor = JsDescriptorUtils.getReceiverParameterForDeclaration(classDescriptor)
            val translationContext = context.innerContextWithAliased(receiverDescriptor, thisNameRef)

            val constructorInitializer = FunctionTranslator.newInstance(constructor, translationContext).translateAsMethod()
            val constructorFunction = constructorInitializer.valueExpr as JsFunction

            constructorFunction.parameters.add(JsParameter(thisName))

            val referenceToClass = context.getQualifiedReference(classDescriptor)

            val forAddToBeginning: List<JsStatement> =
                    with(arrayListOf<JsStatement>()) {
                        addAll(FunctionBodyTranslator.setDefaultValueForArguments(constructorDescriptor, context))

                        val createInstance = Namer.createObjectWithPrototypeFrom(referenceToClass)
                        val instanceVar = JsAstUtils.assignment(thisNameRef, JsAstUtils.or(thisNameRef, createInstance)).makeStmt()
                        add(instanceVar)

                        val resolvedCall = BindingContextUtils.getDelegationConstructorCall(context.bindingContext(), constructorDescriptor)
                        val delegationClassDescriptor = resolvedCall?.resultingDescriptor?.containingDeclaration

                        if (resolvedCall != null && !KotlinBuiltIns.isAny(delegationClassDescriptor!!)) {
                            val superCall = CallTranslator.translate(context, resolvedCall)
                            add(superCall.toInvocationWith(thisNameRef).makeStmt())
                        }

                        val delegationCtorInTheSameClass = delegationClassDescriptor == classDescriptor
                        if (!delegationCtorInTheSameClass && !classDescriptor.hasPrimaryConstructor()) {
                            add(JsInvocation(Namer.getFunctionCallRef(referenceToClass), thisNameRef).makeStmt())
                        }

                        this
                    }

            with(constructorFunction.body.statements) {
                addAll(0, forAddToBeginning)
                add(JsReturn(thisNameRef))
            }

            return constructorInitializer
        }
    }
}
