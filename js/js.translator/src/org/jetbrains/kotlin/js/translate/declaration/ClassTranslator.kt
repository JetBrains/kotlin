/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator
import org.jetbrains.kotlin.js.translate.context.DefinitionPlace
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.context.*
import org.jetbrains.kotlin.js.translate.expression.FunctionTranslator
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator
import org.jetbrains.kotlin.js.translate.initializer.ClassInitializerTranslator
import org.jetbrains.kotlin.js.translate.utils.*
import org.jetbrains.kotlin.js.translate.utils.BindingUtils.getClassDescriptor
import org.jetbrains.kotlin.js.translate.utils.BindingUtils.getPropertyDescriptorForConstructorParameter
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

    private fun translate(declarationContext: TranslationContext = context()): JsInvocation {
        return JsInvocation(context().namer().classCreateInvocation(descriptor), getClassCreateInvocationArguments(declarationContext))
    }

    private fun isTrait(): Boolean = descriptor.kind == ClassKind.INTERFACE

    private fun getClassCreateInvocationArguments(declarationContext: TranslationContext): List<JsExpression> {
        var context = declarationContext
        val invocationArguments = ArrayList<JsExpression>()

        val properties = SmartList<JsPropertyInitializer>()
        val staticProperties = SmartList<JsPropertyInitializer>()

        val qualifiedReference = context.getQualifiedReference(descriptor)
        val scope = context().getScopeForDescriptor(descriptor)
        val definitionPlace = DefinitionPlace(scope as JsObjectScope, qualifiedReference, staticProperties)

        context = context.newDeclaration(descriptor, definitionPlace)
        context = fixContextForCompanionObjectAccessing(context)

        invocationArguments.add(getSuperclassReferences(context))
        val delegationTranslator = DelegationTranslator(classDeclaration, context())
        var initializer: JsFunction? = null
        if (!isTrait()) {
            initializer = ClassInitializerTranslator(classDeclaration, context).generateInitializeMethod(delegationTranslator)
            invocationArguments.add(initializer)
        }

        translatePropertiesAsConstructorParameters(context, properties)
        val bodyVisitor = DeclarationBodyVisitor(properties, staticProperties)
        bodyVisitor.traverseContainer(classDeclaration, context)
        delegationTranslator.generateDelegated(properties)

        if (descriptor.isData) {
            JsDataClassGenerator(classDeclaration, context, properties).generate()
        }

        if (isEnumClass(descriptor)) {
            val enumEntries = JsObjectLiteral(bodyVisitor.enumEntryList, true)
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
                // about "prototype" - see http://code.google.com/p/jsdoc-toolkit/wiki/TagLends
                invocationArguments.add(JsDocComment(JsAstUtils.LENDS_JS_DOC_TAG, JsNameRef("prototype", qualifiedReference)))
                invocationArguments.add(JsObjectLiteral(properties, true))
            }
        }
        if (hasStaticProperties) {
            invocationArguments.add(JsDocComment(JsAstUtils.LENDS_JS_DOC_TAG, qualifiedReference))
            invocationArguments.add(JsObjectLiteral(staticProperties, true))
        }

        val tracker = context.usageTracker()
        if (tracker != null && initializer != null && tracker.hasCapturedExceptContaining()) {
            val captured = tracker.capturedDescriptorToJsName
            val keysAsList = captured.keys.toList()
            for ((i, key) in keysAsList.withIndex()) {
                val name = captured[key]!!
                initializer.parameters.add(i, JsParameter(name))
                initializer.body.statements.add(i, JsAstUtils.defineSimpleProperty(name.ident, name.makeRef()))
            }
            context.putLocalClassClosure(descriptor, keysAsList)
        }

        if (initializer != null && initializer.body.isEmpty) {
            invocationArguments.replaceAll { if (it == initializer) JsLiteral.NULL else it }
        }

        return invocationArguments
    }

    private fun fixContextForCompanionObjectAccessing(context: TranslationContext): TranslationContext {
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
            val type = supertypes[0]
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
        var outerDeclaration = descriptor.containingDeclaration.containingDeclaration
        if (outerDeclaration != null && outerDeclaration !is ClassDescriptor) {
            outerDeclaration = DescriptorUtils.getContainingClass(outerDeclaration)
        }
        val scope = if (outerDeclaration != null)
            outerClassContext.getScopeForDescriptor(outerDeclaration)
        else
            outerClassContext.rootScope

        val classContext = outerClassContext.innerWithUsageTracker(scope, descriptor)

        var declarationArgs = getClassCreateInvocationArguments(classContext)
        val jsClass = JsInvocation(context().namer().classCreationMethodReference(), declarationArgs)

        val name = outerClassContext.getNameForDescriptor(descriptor)
        val constructor = outerClassContext.define(name, jsClass)

        val closure = outerClassContext.getLocalClassClosure(descriptor)
        var closureArgs = emptyList<JsExpression>()
        if (closure != null) {
            closureArgs = closure.map { context().getParameterNameRefForInvocation(it) }.toList()
        }

        return JsNew(constructor, closureArgs)
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
            return ClassTranslator(objectDeclaration, context).translateObjectInsideClass(context)
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
