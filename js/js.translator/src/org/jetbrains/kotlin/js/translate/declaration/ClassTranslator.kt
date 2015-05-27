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
import org.jetbrains.kotlin.backend.common.bridges.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.translate.context.DefinitionPlace
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.declaration.propertyTranslator.*
import org.jetbrains.kotlin.js.translate.expression.*
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator
import org.jetbrains.kotlin.js.translate.initializer.ClassInitializerTranslator
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.*
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.psi.JetObjectDeclaration
import org.jetbrains.kotlin.psi.JetParameter
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeConstructor

import java.util.*

import org.jetbrains.kotlin.js.translate.reference.ReferenceTranslator.translateAsFQReference
import org.jetbrains.kotlin.js.translate.utils.BindingUtils.getClassDescriptor
import org.jetbrains.kotlin.js.translate.utils.BindingUtils.getPropertyDescriptorForConstructorParameter
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils.getReceiverParameterForDeclaration
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils.getSupertypesWithoutFakes
import org.jetbrains.kotlin.js.translate.utils.PsiUtils.getPrimaryConstructorParameters
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils.simpleReturnFunction
import org.jetbrains.kotlin.js.translate.utils.UtilsPackage.generateDelegateCall
import org.jetbrains.kotlin.resolve.DescriptorUtils.*
import org.jetbrains.kotlin.types.TypeUtils.topologicallySortSuperclassesAndRecordAllInstances

/**
 * Generates a definition of a single class.
 */
public class ClassTranslator private (private val classDeclaration: JetClassOrObject, context: TranslationContext) : AbstractTranslator(context) {

    private val descriptor: ClassDescriptor

    init {
        this.descriptor = getClassDescriptor(context.bindingContext(), classDeclaration)
    }

    private fun translateObjectLiteralExpression(): JsExpression {
        val containingClass = getContainingClass(descriptor) ?: return translate(context())

        return translateObjectInsideClass(context())
    }

    overloads public fun translate(declarationContext: TranslationContext = context()): JsInvocation {
        return JsInvocation(context().namer().classCreateInvocation(descriptor), getClassCreateInvocationArguments(declarationContext))
    }

    private fun isTrait(): Boolean {
        return descriptor.getKind() == ClassKind.INTERFACE
    }

    private fun getClassCreateInvocationArguments(declarationContext: TranslationContext): List<JsExpression> {
        var declarationContext = declarationContext
        val invocationArguments = ArrayList<JsExpression>()

        val properties = SmartList<JsPropertyInitializer>()
        val staticProperties = SmartList<JsPropertyInitializer>()

        val isTopLevelDeclaration = context() == declarationContext

        var qualifiedReference: JsNameRef? = null
        if (isTopLevelDeclaration) {
            var definitionPlace: DefinitionPlace? = null

            if (!descriptor.getKind().isSingleton() && !isAnonymousObject(descriptor)) {
                qualifiedReference = declarationContext.getQualifiedReference(descriptor)
                val scope = context().getScopeForDescriptor(descriptor)
                definitionPlace = DefinitionPlace(scope as JsObjectScope, qualifiedReference, staticProperties)
            }

            declarationContext = declarationContext.newDeclaration(descriptor, definitionPlace)
        }

        declarationContext = fixContextForCompanionObjectAccessing(declarationContext)

        invocationArguments.add(getSuperclassReferences(declarationContext))
        val delegationTranslator = DelegationTranslator(classDeclaration, context())
        if (!isTrait()) {
            val initializer = ClassInitializerTranslator(classDeclaration, declarationContext).generateInitializeMethod(delegationTranslator)
            invocationArguments.add(if (initializer.getBody().getStatements().isEmpty()) JsLiteral.NULL else initializer)
        }

        translatePropertiesAsConstructorParameters(declarationContext, properties)
        val bodyVisitor = DeclarationBodyVisitor(properties, staticProperties)
        bodyVisitor.traverseContainer(classDeclaration, declarationContext)
        delegationTranslator.generateDelegated(properties)

        if (KotlinBuiltIns.isData(descriptor)) {
            JsDataClassGenerator(classDeclaration, declarationContext, properties).generate()
        }

        if (isEnumClass(descriptor)) {
            val enumEntries = JsObjectLiteral(bodyVisitor.getEnumEntryList(), true)
            val function = simpleReturnFunction(declarationContext.getScopeForDescriptor(descriptor), enumEntries)
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

    private fun fixContextForCompanionObjectAccessing(declarationContext: TranslationContext): TranslationContext {
        var declarationContext = declarationContext
        // In Kotlin we can access to companion object members without qualifier just by name, but we should translate it to access with FQ name.
        // So create alias for companion object receiver parameter.
        val companionObjectDescriptor = descriptor.getCompanionObjectDescriptor()
        if (companionObjectDescriptor != null) {
            val referenceToClass = translateAsFQReference(companionObjectDescriptor.getContainingDeclaration(), declarationContext)
            val companionObjectAccessor = Namer.getCompanionObjectAccessor(referenceToClass)
            val companionObjectReceiver = getReceiverParameterForDeclaration(companionObjectDescriptor)
            declarationContext.aliasingContext().registerAlias(companionObjectReceiver, companionObjectAccessor)
        }

        // Overlap alias of companion object receiver for accessing from containing class(see previous if block),
        // because inside companion object we should use simple name for access.
        if (isCompanionObject(descriptor)) {
            declarationContext = declarationContext.innerContextWithAliased(descriptor.getThisAsReceiverParameter(), JsLiteral.THIS)
        }

        return declarationContext
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
        if (supertypes.size() == 1) {
            val type = supertypes.get(0)
            val supertypeDescriptor = getClassDescriptorForType(type)
            return listOf<JsExpression>(getClassReference(supertypeDescriptor))
        }

        val supertypeConstructors = HashSet<TypeConstructor>()
        for (type in supertypes) {
            supertypeConstructors.add(type.getConstructor())
        }
        val sortedAllSuperTypes = topologicallySortSuperclassesAndRecordAllInstances(descriptor.getDefaultType(), HashMap<TypeConstructor, Set<JetType>>(), HashSet<TypeConstructor>())
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
        val `fun` = JsFunction(outerClassContext.scope(), JsBlock(), "initializer for " + descriptor.getName().asString())
        val funContext = outerClassContext.newFunctionBodyWithUsageTracker(`fun`, descriptor)

        `fun`.getBody().getStatements().add(JsReturn(translate(funContext)))

        return `fun`.withCapturedParameters(funContext, outerClassContext, descriptor)
    }

    private fun generatedBridgeMethods(properties: MutableList<JsPropertyInitializer>) {
        if (isTrait()) return

        generateBridgesToTraitImpl(properties)

        generateOtherBridges(properties)
    }

    private fun generateBridgesToTraitImpl(properties: MutableList<JsPropertyInitializer>) {
        for (entry in CodegenUtil.getTraitMethods(descriptor).entrySet()) {
            if (!areNamesEqual(entry.getKey(), entry.getValue())) {
                properties.add(generateDelegateCall(entry.getValue(), entry.getKey(), JsLiteral.THIS, context()))
            }
        }
    }

    private fun generateOtherBridges(properties: MutableList<JsPropertyInitializer>) {
        for (memberDescriptor in descriptor.getDefaultType().getMemberScope().getAllDescriptors()) {
            if (memberDescriptor is FunctionDescriptor) {
                val bridgesToGenerate = generateBridgesForFunctionDescriptor<FunctionDescriptor>(memberDescriptor, ID)

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

        if (fromDescriptor.getKind().isReal() && fromDescriptor.getModality() != Modality.ABSTRACT && !toDescriptor.getKind().isReal())
            return

        properties.add(generateDelegateCall(fromDescriptor, toDescriptor, JsLiteral.THIS, context()))
    }

    private fun areNamesEqual(first: FunctionDescriptor, second: FunctionDescriptor): Boolean {
        val firstName = context().getNameForDescriptor(first)
        val secondName = context().getNameForDescriptor(second)
        return firstName.getIdent() == secondName.getIdent()
    }

    companion object {

        public fun generateClassCreation(classDeclaration: JetClassOrObject, context: TranslationContext): JsInvocation {
            return ClassTranslator(classDeclaration, context).translate()
        }

        public fun generateObjectLiteral(objectDeclaration: JetObjectDeclaration, context: TranslationContext): JsExpression {
            return ClassTranslator(objectDeclaration, context).translateObjectLiteralExpression()
        }
    }
}
