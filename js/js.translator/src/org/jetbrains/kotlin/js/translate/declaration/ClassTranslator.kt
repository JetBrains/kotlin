/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.descriptorUtils.hasPrimaryConstructor
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator
import org.jetbrains.kotlin.js.translate.context.*
import org.jetbrains.kotlin.js.translate.expression.translateAndAliasParameters
import org.jetbrains.kotlin.js.translate.expression.translateFunction
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator
import org.jetbrains.kotlin.js.translate.initializer.ClassInitializerTranslator
import org.jetbrains.kotlin.js.translate.reference.ReferenceTranslator
import org.jetbrains.kotlin.js.translate.utils.*
import org.jetbrains.kotlin.js.translate.utils.BindingUtils.getClassDescriptor
import org.jetbrains.kotlin.js.translate.utils.BindingUtils.getPropertyDescriptorForConstructorParameter
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.pureFqn
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils.getSupertypesWithoutFakes
import org.jetbrains.kotlin.js.translate.utils.PsiUtils.getPrimaryConstructorParameters
import org.jetbrains.kotlin.js.translate.utils.jsAstUtils.toInvocationWith
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils.getClassDescriptorForType
import org.jetbrains.kotlin.resolve.DescriptorUtils.getClassDescriptorForTypeConstructor
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.CommonSupertypes.topologicallySortSuperclassesAndRecordAllInstances
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.utils.DFS

/**
 * Generates a definition of a single class.
 */
class ClassTranslator private constructor(
        private val classDeclaration: KtClassOrObject,
        context: TranslationContext,
        private val enumInitializerName: JsName?,
        private val ordinal: Int?
) : AbstractTranslator(context) {

    private val descriptor = getClassDescriptor(context.bindingContext(), classDeclaration)
    private val secondaryConstructors = mutableListOf<ConstructorInfo>()
    private var primaryConstructor: ConstructorInfo? = null
    private lateinit var cachedInstanceName: JsName
    private val metadataLiteral = JsObjectLiteral(true)

    private fun isTrait(): Boolean = descriptor.kind == ClassKind.INTERFACE

    private fun translate() {
        val context = context().newDeclaration(descriptor)

        val constructorFunction = descriptor.unsubstitutedPrimaryConstructor?.let { context.getFunctionObject(it) } ?:
                                  context.createRootScopedFunction(descriptor)
        constructorFunction.name = context.getInnerNameForDescriptor(descriptor)
        context.addDeclarationStatement(constructorFunction.makeStmt())
        val enumInitFunction = if (descriptor.kind == ClassKind.ENUM_CLASS) createEnumInitFunction() else null

        val nonConstructorContext = context.withUsageTrackerIfNecessary(descriptor)
        nonConstructorContext.startDeclaration()
        val delegationTranslator = DelegationTranslator(classDeclaration, nonConstructorContext)
        translatePropertiesAsConstructorParameters(nonConstructorContext)
        val bodyVisitor = DeclarationBodyVisitor(descriptor, nonConstructorContext, enumInitFunction)
        bodyVisitor.traverseContainer(classDeclaration, nonConstructorContext)
        mayBeAddThrowableProperties(context)
        constructorFunction.body.statements += bodyVisitor.initializerStatements
        delegationTranslator.generateDelegated()

        val companionDescriptor = descriptor.companionObjectDescriptor
        if (enumInitFunction != null && companionDescriptor != null) {
            val initInvocation = JsInvocation(JsAstUtils.pureFqn(context().getNameForObjectInstance(companionDescriptor), null))
            enumInitFunction.body.statements += JsAstUtils.asSyntheticStatement(initInvocation.source(companionDescriptor.source.getPsi()))
        }

        translatePrimaryConstructor(constructorFunction, context, delegationTranslator)
        addMetadataObject()
        addMetadataType()
        context.addClass(descriptor)
        addSuperclassReferences()
        classDeclaration.secondaryConstructors.forEach { generateSecondaryConstructor(context, it) }

        if (descriptor.isData) {
            JsDataClassGenerator(classDeclaration, context).generate()
        }

        emitConstructors(nonConstructorContext, nonConstructorContext.endDeclaration())
        for (constructor in allConstructors) {
            addClosureParameters(constructor, nonConstructorContext)
        }

        if (isObjectLike()) {
            addObjectMethods()
        }

        if (descriptor.kind == ClassKind.ENUM_CLASS) {
            generateEnumStandardMethods(bodyVisitor.enumEntries)
        }

        // We don't use generated name. However, by generating the name, we generate corresponding entry in inter-fragment import table.
        // This is required to properly merge fragments when one contains super-class and another contains derived class.
        descriptor.getSuperClassNotAny()?.let { ReferenceTranslator.translateAsTypeReference(it, context) }
    }

    private fun TranslationContext.withUsageTrackerIfNecessary(innerDescriptor: MemberDescriptor): TranslationContext {
        return if (isLocalClass) {
            innerWithUsageTracker(innerDescriptor)
        }
        else {
            inner(innerDescriptor)
        }
    }

    private val isLocalClass
        get() = descriptor.containingDeclaration !is ClassOrPackageFragmentDescriptor

    private fun translatePrimaryConstructor(
            constructorFunction: JsFunction,
            classContext: TranslationContext,
            delegationTranslator: DelegationTranslator
    ) {
        if (!isTrait()) {
            val constructorContext = classContext.innerWithUsageTracker(descriptor)
            if (isObjectLike()) {
                addObjectCache(constructorFunction.body.statements)
            }
            ClassInitializerTranslator(classDeclaration, constructorContext, constructorFunction).apply {
                if (ordinal != null) {
                    setOrdinal(ordinal)
                }
                generateInitializeMethod(delegationTranslator)
            }

            primaryConstructor = ConstructorInfo(constructorFunction, constructorContext, descriptor)
        }
    }

    private fun createEnumInitFunction(): JsFunction {
        val function = context().createRootScopedFunction(descriptor)
        function.name = JsScope.declareTemporaryName(StaticContext.getSuggestedName(descriptor) + "_initFields")
        val emptyFunction = context().createRootScopedFunction(descriptor)
        function.body.statements += JsAstUtils.assignment(JsAstUtils.pureFqn(function.name, null), emptyFunction)
                .source(classDeclaration).makeStmt()
        context().addDeclarationStatement(function.makeStmt())
        return function
    }

    private fun isObjectLike() = when (descriptor.kind) {
        ClassKind.OBJECT,
        ClassKind.ENUM_ENTRY -> true
        else -> false
    }

    private fun addMetadataObject() {
        context().addDeclarationStatement(JsAstUtils.assignment(createMetadataRef(), metadataLiteral).makeStmt())
    }

    private fun createMetadataRef() = JsNameRef(Namer.METADATA, context().getInnerReference(descriptor))

    private fun addMetadataType() {
        val kotlinType = JsNameRef(Namer.CLASS_KIND_ENUM, Namer.KOTLIN_NAME)
        val typeRef = when {
            DescriptorUtils.isInterface(descriptor) -> JsNameRef(Namer.CLASS_KIND_INTERFACE, kotlinType)
            DescriptorUtils.isObject(descriptor) -> JsNameRef(Namer.CLASS_KIND_OBJECT, kotlinType)
            else -> JsNameRef(Namer.CLASS_KIND_CLASS, kotlinType)
        }

        metadataLiteral.propertyInitializers += JsPropertyInitializer(JsNameRef(Namer.METADATA_CLASS_KIND), typeRef)

        val simpleName = descriptor.name
        if (!simpleName.isSpecial) {
            val simpleNameProp = JsPropertyInitializer(JsNameRef(Namer.METADATA_SIMPLE_NAME), JsStringLiteral(simpleName.identifier))
            metadataLiteral.propertyInitializers += simpleNameProp
        }
    }

    private fun generateSecondaryConstructor(classContext: TranslationContext, constructor: KtSecondaryConstructor) {
        // Prepare
        val constructorDescriptor = BindingUtils.getDescriptorForElement(classContext.bindingContext(), constructor)
                as ClassConstructorDescriptor
        val classDescriptor = constructorDescriptor.containingDeclaration

        val thisName = JsScope.declareTemporaryName(Namer.ANOTHER_THIS_PARAMETER_NAME)
        val thisNameRef = thisName.makeRef()
        val receiverDescriptor = classDescriptor.thisAsReceiverParameter

        var context = classContext
                .innerWithUsageTracker(constructorDescriptor)
                .innerContextWithAliased(receiverDescriptor, thisNameRef)

        val outerClassName = context.getOuterClassReference(classDescriptor)
        val outerClass = DescriptorUtils.getContainingClass(classDescriptor)
        if (outerClassName != null) {
            val outerClassReceiver = outerClass!!.thisAsReceiverParameter
            context = context.innerContextWithAliased(outerClassReceiver, outerClassName.makeRef())
        }

        // Translate constructor body
        val constructorInitializer = context.getFunctionObject(constructorDescriptor)
        constructorInitializer.name = context.getInnerNameForDescriptor(constructorDescriptor)
        context.addDeclarationStatement(constructorInitializer.makeStmt())

        context = context.contextWithScope(constructorInitializer)
        context.translateAndAliasParameters(constructorDescriptor, constructorInitializer.parameters)
                .translateFunction(constructor, constructorInitializer)

        // Translate super/this call
        val superCallGenerators = mutableListOf<(MutableList<JsStatement>) -> Unit>()
        val referenceToClass = context.getInnerReference(classDescriptor)

        superCallGenerators += { it += FunctionBodyTranslator.setDefaultValueForArguments(constructorDescriptor, context) }

        val createInstance = Namer.createObjectWithPrototypeFrom(referenceToClass)
        val instanceVar = JsAstUtils.assignment(thisNameRef.deepCopy(), JsAstUtils.or(thisNameRef.deepCopy(), createInstance))
                .source(constructor).makeStmt()
        superCallGenerators += { it += instanceVar }

        // Add parameter for outer instance
        val leadingArgs = mutableListOf<JsExpression>()

        if (descriptor.kind == ClassKind.ENUM_CLASS) {
            val nameParamName = JsScope.declareTemporaryName("name")
            val ordinalParamName = JsScope.declareTemporaryName("ordinal")
            constructorInitializer.parameters.addAll(0, listOf(JsParameter(nameParamName), JsParameter(ordinalParamName)))
            leadingArgs += listOf(nameParamName.makeRef(), ordinalParamName.makeRef())
        }
        if (outerClassName != null) {
            constructorInitializer.parameters.add(0, JsParameter(outerClassName))
            leadingArgs += outerClassName.makeRef()
        }

        constructorInitializer.parameters += JsParameter(thisName)

        // Generate super/this call to insert to beginning of the function
        val resolvedCall = BindingContextUtils.getDelegationConstructorCall(context.bindingContext(), constructorDescriptor)
        val delegationClassDescriptor = (resolvedCall?.resultingDescriptor as? ClassConstructorDescriptor)?.constructedClass

        if (resolvedCall != null && !KotlinBuiltIns.isAny(delegationClassDescriptor!!)) {
            if (JsDescriptorUtils.isImmediateSubtypeOfError(classDescriptor)) {
                superCallGenerators += {
                    val innerContext = context().innerBlock()
                    ClassInitializerTranslator.emulateSuperCallToNativeError(
                            innerContext, classDescriptor, resolvedCall, thisNameRef.deepCopy())
                    it += innerContext.currentBlock.statements
                }
            }
            else {
                superCallGenerators += {
                    val delegationConstructor = resolvedCall.resultingDescriptor
                    val innerContext = context.innerBlock()
                    val statement = CallTranslator.translate(innerContext, resolvedCall).toInvocationWith(
                            leadingArgs, delegationConstructor.valueParameters.size, thisNameRef.deepCopy())
                            .source(resolvedCall.call.callElement)
                            .makeStmt()
                    it += innerContext.currentBlock.statements
                    it += statement
                }
            }
        }

        val delegationCtorInTheSameClass = delegationClassDescriptor == classDescriptor
        if (!delegationCtorInTheSameClass && !classDescriptor.hasPrimaryConstructor()) {
            superCallGenerators += {
                val usageTracker = context.usageTracker()!!
                val closure = context.getClassOrConstructorClosure(classDescriptor).orEmpty().map {
                    usageTracker.getNameForCapturedDescriptor(it)!!.makeRef()
                }
                it += JsInvocation(Namer.getFunctionCallRef(referenceToClass), listOf(thisNameRef.deepCopy()) + closure + leadingArgs)
                        .source(classDeclaration).makeStmt()
            }
        }

        constructorInitializer.body.statements += JsReturn(thisNameRef.deepCopy()).apply {
            source = constructor
        }

        val compositeSuperCallGenerator: () -> Unit = {
            val additionalStatements = mutableListOf<JsStatement>()
            for (partGenerator in superCallGenerators) {
                partGenerator(additionalStatements)
            }
            constructorInitializer.body.statements.addAll(0, additionalStatements)
        }

        secondaryConstructors += ConstructorInfo(constructorInitializer, context, constructorDescriptor, compositeSuperCallGenerator)

        context.export(constructorDescriptor)
    }

    private val allConstructors: Sequence<ConstructorInfo>
        get() {
            val primary = primaryConstructor
            return if (primary != null) sequenceOf(primary) + secondaryConstructors else secondaryConstructors.asSequence()
        }

    private fun emitConstructors(nonConstructorContext: TranslationContext, callSites: List<DeferredCallSite>) {
        // Build map that maps constructor to all constructors called via this()
        val constructorMap = allConstructors.map { it.descriptor to it }.toMap()

        val callSiteMap = callSites.groupBy {
            val constructor = it.constructor
            val result: DeclarationDescriptor = if (constructor.isPrimary) constructor.containingDeclaration else constructor
            result
        }

        val thisCalls = secondaryConstructors.map {
            val set = mutableSetOf<ConstructorInfo>()
            val descriptor = it.descriptor
            if (descriptor is ConstructorDescriptor) {
                val resolvedCall = BindingContextUtils.getDelegationConstructorCall(context().bindingContext(), descriptor)
                if (resolvedCall != null) {
                    val callee = constructorMap[resolvedCall.resultingDescriptor]
                    if (callee != null) {
                        set += callee
                    }
                }
            }
            Pair(it, set)
        }.toMap()

        val sortedConstructors = DFS.topologicalOrder(allConstructors.asIterable()) { thisCalls[it].orEmpty() }.reversed()
        for (constructor in sortedConstructors) {
            constructor.superCallGenerator()

            val nonConstructorUsageTracker = nonConstructorContext.usageTracker()
            val usageTracker = constructor.context.usageTracker()!!

            val nonConstructorCapturedVars = if (isLocalClass) nonConstructorUsageTracker!!.capturedDescriptors else setOf()
            val constructorCapturedVars = usageTracker.capturedDescriptors

            val capturedVars = (nonConstructorCapturedVars + constructorCapturedVars).distinct()

            val descriptor = constructor.descriptor
            val classDescriptor = DescriptorUtils.getParentOfType(descriptor, ClassDescriptor::class.java, false)!!
            nonConstructorContext.putClassOrConstructorClosure(descriptor, capturedVars)

            val constructorCallSites = callSiteMap[constructor.descriptor].orEmpty()

            for (callSite in constructorCallSites) {
                val closureQualifier = callSite.context.getArgumentForClosureConstructor(classDescriptor.thisAsReceiverParameter)
                capturedVars.forEach { nonConstructorUsageTracker!!.used(it) }
                val closureArgs = capturedVars.map {
                    val name = nonConstructorUsageTracker!!.getNameForCapturedDescriptor(it)!!
                    JsAstUtils.pureFqn(name, closureQualifier)
                }
                callSite.invocationArgs.addAll(0, closureArgs)
            }
        }
    }

    private fun addClosureParameters(constructor: ConstructorInfo, nonConstructorContext: TranslationContext) {
        val usageTracker = constructor.context.usageTracker()!!
        val capturedVars = context().getClassOrConstructorClosure(constructor.descriptor) ?: return
        val nonConstructorUsageTracker = nonConstructorContext.usageTracker()

        val function = constructor.function
        val additionalStatements = mutableListOf<JsStatement>()
        for ((i, capturedVar) in capturedVars.withIndex()) {
            val fieldName = nonConstructorUsageTracker?.capturedDescriptorToJsName?.get(capturedVar)
            val name = usageTracker.capturedDescriptorToJsName[capturedVar] ?: fieldName!!

            function.parameters.add(i, JsParameter(name))
            if (fieldName != null && constructor == primaryConstructor) {
                val source = (constructor.descriptor as? DeclarationDescriptorWithSource)?.source
                additionalStatements += JsAstUtils.defineSimpleProperty(fieldName.ident, name.makeRef(), source)
            }
        }

        function.body.statements.addAll(0, additionalStatements)
    }

    private fun addSuperclassReferences() {
        val supertypeReferences = JsArrayLiteral(getSupertypesNameReferences())
        metadataLiteral.propertyInitializers += JsPropertyInitializer(JsNameRef(Namer.METADATA_SUPERTYPES), supertypeReferences)
    }

    private fun getSupertypesNameReferences(): List<JsExpression> {
        val supertypes = getSupertypesWithoutFakes(descriptor)
                .filter { it.constructor.declarationDescriptor !is FunctionClassDescriptor }
        if (supertypes.isEmpty()) {
            return emptyList()
        }
        if (supertypes.size == 1) {
            val type = supertypes[0]
            val supertypeDescriptor = getClassDescriptorForType(type)
            return if (!AnnotationsUtils.isNativeObject(supertypeDescriptor)) {
                listOf(ReferenceTranslator.translateAsTypeReference(supertypeDescriptor, context()))
            }
            else {
                listOf()
            }
        }

        val supertypeConstructors = mutableSetOf<TypeConstructor>()
        for (type in supertypes) {
            supertypeConstructors += type.constructor
        }
        val sortedAllSuperTypes = topologicallySortSuperclassesAndRecordAllInstances(
            descriptor.defaultType,
            mutableMapOf<TypeConstructor, Set<SimpleType>>(),
            mutableSetOf<TypeConstructor>()
        )
        val supertypesRefs = mutableListOf<JsExpression>()
        for (typeConstructor in sortedAllSuperTypes) {
            if (supertypeConstructors.contains(typeConstructor)) {
                val supertypeDescriptor = getClassDescriptorForTypeConstructor(typeConstructor)
                if (!AnnotationsUtils.isNativeObject(supertypeDescriptor)) {
                    supertypesRefs += ReferenceTranslator.translateAsTypeReference(supertypeDescriptor, context())
                }
            }
        }
        return supertypesRefs
    }

    private fun translatePropertiesAsConstructorParameters(classDeclarationContext: TranslationContext) {
        for (parameter in getPrimaryConstructorParameters(classDeclaration)) {
            val descriptor = getPropertyDescriptorForConstructorParameter(bindingContext(), parameter)
            if (descriptor != null) {
                val literal = JsObjectLiteral(true)
                translateAccessors(descriptor, literal.propertyInitializers, classDeclarationContext)
                if (literal.propertyInitializers.isNotEmpty()) {
                    classDeclarationContext.addAccessorsToPrototype(this.descriptor, descriptor, literal)
                }
            }
        }
    }

    private fun addObjectCache(statements: MutableList<JsStatement>) {
        cachedInstanceName = JsScope.declareTemporaryName(StaticContext.getSuggestedName(descriptor) + Namer.OBJECT_INSTANCE_VAR_SUFFIX)
        statements += JsAstUtils.assignment(cachedInstanceName.makeRef(), JsThisRef()).source(classDeclaration).makeStmt()
    }

    private fun addObjectMethods() {
        context().addDeclarationStatement(JsAstUtils.newVar(cachedInstanceName, JsNullLiteral()))

        val instanceFun = context().createRootScopedFunction("Instance function: " + descriptor)
        instanceFun.name = context().getNameForObjectInstance(descriptor)

        if (enumInitializerName != null) {
            instanceFun.body.statements += JsInvocation(pureFqn(enumInitializerName, null)).source(classDeclaration).makeStmt()
        }
        if (descriptor.kind != ClassKind.ENUM_ENTRY) {
            val instanceCreatedCondition = JsAstUtils.equality(cachedInstanceName.makeRef(), JsNullLiteral())
                    .source(classDeclaration)
            val instanceCreationBlock = JsBlock()
            val instanceCreatedGuard = JsIf(instanceCreatedCondition, instanceCreationBlock)
            instanceFun.body.statements += instanceCreatedGuard

            val objectRef = context().getInnerReference(descriptor)
            instanceCreationBlock.statements += JsNew(objectRef).source(classDeclaration).makeStmt()
        }

        instanceFun.body.statements += JsReturn(cachedInstanceName.makeRef().source(classDeclaration))

        context().addDeclarationStatement(instanceFun.makeStmt())
    }

    private fun generateEnumStandardMethods(entries: List<ClassDescriptor>) {
        EnumTranslator(context(), descriptor, entries, classDeclaration).generateStandardMethods()
    }

    private fun mayBeAddThrowableProperties(context: TranslationContext) {
        if (!JsDescriptorUtils.isImmediateSubtypeOfError(descriptor)) return

        val properties = listOf("message", "cause")
                .map { Name.identifier(it) }
                .map { DescriptorUtils.getPropertyByName(descriptor.unsubstitutedMemberScope, it) }
                .filter { !it.kind.isReal }
        for (property in properties) {
            val propertyTranslator = DefaultPropertyTranslator(property, context, JsNullLiteral())
            val literal = JsObjectLiteral(true)
            val getterFunction = context.getFunctionObject(property.getter!!)
            propertyTranslator.generateDefaultGetterFunction(property.getter!!, getterFunction)
            literal.propertyInitializers += JsPropertyInitializer(JsStringLiteral("get"), getterFunction)
            context.addAccessorsToPrototype(descriptor, property, literal)
        }
    }

    companion object {
        @JvmStatic fun translate(classDeclaration: KtClassOrObject, context: TranslationContext, enumInitializerName: JsName?) {
            return ClassTranslator(classDeclaration, context, enumInitializerName, null).translate()
        }

        @JvmStatic fun translate(classDeclaration: KtEnumEntry, context: TranslationContext, enumInitializerName: JsName, ordinal: Int) {
            return ClassTranslator(classDeclaration, context, enumInitializerName, ordinal).translate()
        }
    }

    private class ConstructorInfo(
            val function: JsFunction,
            val context: TranslationContext,
            val descriptor: MemberDescriptor,
            val superCallGenerator: (() -> Unit) = { }
    )
}
