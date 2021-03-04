/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.common.serialization.findSourceFile
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.isAny
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.deprecation.Deprecation
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.typeUtil.isInterface
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlin.utils.addIfNotNull

interface ObjCExportTranslator {
    fun generateBaseDeclarations(): List<ObjCTopLevel<*>>
    fun getClassIfExtension(receiverType: KotlinType): ClassDescriptor?
    fun translateFile(file: SourceFile, declarations: List<CallableMemberDescriptor>): ObjCInterface
    fun translateClass(descriptor: ClassDescriptor): ObjCInterface
    fun translateInterface(descriptor: ClassDescriptor): ObjCProtocol
    fun translateExtensions(classDescriptor: ClassDescriptor, declarations: List<CallableMemberDescriptor>): ObjCInterface
}

interface ObjCExportProblemCollector {
    fun reportWarning(text: String)
    fun reportWarning(method: FunctionDescriptor, text: String)
    fun reportException(throwable: Throwable)

    object SILENT : ObjCExportProblemCollector {
        override fun reportWarning(text: String) {}
        override fun reportWarning(method: FunctionDescriptor, text: String) {}
        override fun reportException(throwable: Throwable) {}
    }
}

internal class ObjCExportTranslatorImpl(
        private val generator: ObjCExportHeaderGenerator?,
        val mapper: ObjCExportMapper,
        val namer: ObjCExportNamer,
        val problemCollector: ObjCExportProblemCollector,
        val objcGenerics: Boolean
) : ObjCExportTranslator {

    private val kotlinAnyName = namer.kotlinAnyName

    override fun generateBaseDeclarations(): List<ObjCTopLevel<*>> = buildTopLevel {
        add {
            objCInterface(namer.kotlinAnyName, superClass = "NSObject", members = buildMembers {
                add { ObjCMethod(null, true, ObjCInstanceType, listOf("init"), emptyList(), listOf("unavailable")) }
                add { ObjCMethod(null, false, ObjCInstanceType, listOf("new"), emptyList(), listOf("unavailable")) }
                add { ObjCMethod(null, false, ObjCVoidType, listOf("initialize"), emptyList(), listOf("objc_requires_super")) }
            })
        }

        // TODO: add comment to the header.
        add {
            ObjCInterfaceImpl(
                    namer.kotlinAnyName.objCName,
                    superProtocols = listOf("NSCopying"),
                    categoryName = "${namer.kotlinAnyName.objCName}Copying"
            )
        }

        // TODO: only if appears
        add {
            objCInterface(
                    namer.mutableSetName,
                    generics = listOf("ObjectType"),
                    superClass = "NSMutableSet<ObjectType>"
            )
        }

        // TODO: only if appears
        add {
            objCInterface(
                    namer.mutableMapName,
                    generics = listOf("KeyType", "ObjectType"),
                    superClass = "NSMutableDictionary<KeyType, ObjectType>"
            )
        }

        val nsErrorCategoryName = "NSError${namer.topLevelNamePrefix}KotlinException"
        add {
            ObjCInterfaceImpl("NSError", categoryName = nsErrorCategoryName, members = buildMembers {
                add { ObjCProperty("kotlinException", null, ObjCNullableReferenceType(ObjCIdType), listOf("readonly")) }
            })
        }

        genKotlinNumbers()
    }

    private fun StubBuilder<ObjCTopLevel<*>>.genKotlinNumbers() {
        val members = buildMembers {
            NSNumberKind.values().forEach {
                add { nsNumberFactory(it, listOf("unavailable")) }
            }
            NSNumberKind.values().forEach {
                add { nsNumberInit(it, listOf("unavailable")) }
            }
        }
        add {
            objCInterface(
                    namer.kotlinNumberName,
                    superClass = "NSNumber",
                    members = members
            )
        }

        NSNumberKind.values().forEach {
            if (it.mappedKotlinClassId != null) add {
                genKotlinNumber(it.mappedKotlinClassId, it)
            }
        }
    }

    private fun genKotlinNumber(kotlinClassId: ClassId, kind: NSNumberKind): ObjCInterface {
        val name = namer.numberBoxName(kotlinClassId)

        val members = buildMembers {
            add { nsNumberFactory(kind) }
            add { nsNumberInit(kind) }
        }
        return objCInterface(
                name,
                superClass = namer.kotlinNumberName.objCName,
                members = members
        )
    }

    private fun nsNumberInit(kind: NSNumberKind, attributes: List<String> = emptyList()): ObjCMethod {
        return ObjCMethod(
                null,
                false,
                ObjCInstanceType,
                listOf(kind.factorySelector),
                listOf(ObjCParameter("value", null, kind.objCType)),
                attributes
        )
    }

    private fun nsNumberFactory(kind: NSNumberKind, attributes: List<String> = emptyList()): ObjCMethod {
        return ObjCMethod(
                null,
                true,
                ObjCInstanceType,
                listOf(kind.initSelector),
                listOf(ObjCParameter("value", null, kind.objCType)),
                attributes
        )
    }

    override fun getClassIfExtension(receiverType: KotlinType): ClassDescriptor? =
            mapper.getClassIfCategory(receiverType)

    internal fun translateUnexposedClassAsUnavailableStub(descriptor: ClassDescriptor): ObjCInterface = objCInterface(
            namer.getClassOrProtocolName(descriptor),
            descriptor = descriptor,
            superClass = "NSObject",
            attributes = attributesForUnexposed(descriptor)
    )

    internal fun translateUnexposedInterfaceAsUnavailableStub(descriptor: ClassDescriptor): ObjCProtocol = objCProtocol(
            namer.getClassOrProtocolName(descriptor),
            descriptor = descriptor,
            superProtocols = emptyList(),
            members = emptyList(),
            attributes = attributesForUnexposed(descriptor)
    )

    private fun attributesForUnexposed(descriptor: ClassDescriptor): List<String> {
        val message = when {
            descriptor.isKotlinObjCClass() -> "Kotlin subclass of Objective-C class "
            else -> ""
        } + "can't be imported"
        return listOf("unavailable(\"$message\")")
    }

    private fun referenceClass(descriptor: ClassDescriptor): ObjCExportNamer.ClassOrProtocolName {
        fun forwardDeclarationObjcClassName(objcGenerics: Boolean, descriptor: ClassDescriptor, namer:ObjCExportNamer): String {
            val className = translateClassOrInterfaceName(descriptor)
            val builder = StringBuilder(className.objCName)
            if (objcGenerics)
                formatGenerics(builder, descriptor.typeConstructor.parameters.map { typeParameterDescriptor ->
                    "${typeParameterDescriptor.variance.objcDeclaration()}${namer.getTypeParameterName(typeParameterDescriptor)}"
                })
            return builder.toString()
        }

        assert(mapper.shouldBeExposed(descriptor)) { "Shouldn't be exposed: $descriptor" }
        assert(!descriptor.isInterface)
        generator?.requireClassOrInterface(descriptor)

        return translateClassOrInterfaceName(descriptor).also {
            val objcName = forwardDeclarationObjcClassName(objcGenerics, descriptor, namer)
            generator?.referenceClass(objcName)
        }
    }

    private fun referenceProtocol(descriptor: ClassDescriptor): ObjCExportNamer.ClassOrProtocolName {
        assert(mapper.shouldBeExposed(descriptor)) { "Shouldn't be exposed: $descriptor" }
        assert(descriptor.isInterface)
        generator?.requireClassOrInterface(descriptor)

        return translateClassOrInterfaceName(descriptor).also {
            generator?.referenceProtocol(it.objCName)
        }
    }

    private fun translateClassOrInterfaceName(descriptor: ClassDescriptor): ObjCExportNamer.ClassOrProtocolName {
        assert(mapper.shouldBeExposed(descriptor)) { "Shouldn't be exposed: $descriptor" }
        if (ErrorUtils.isError(descriptor)) {
            return ObjCExportNamer.ClassOrProtocolName("ERROR", "ERROR")
        }

        return namer.getClassOrProtocolName(descriptor)
    }

    override fun translateInterface(descriptor: ClassDescriptor): ObjCProtocol {
        require(descriptor.isInterface)
        if (!mapper.shouldBeExposed(descriptor)) {
            return translateUnexposedInterfaceAsUnavailableStub(descriptor)
        }

        val name = translateClassOrInterfaceName(descriptor)
        val members: List<Stub<*>> = buildMembers { translateInterfaceMembers(descriptor) }
        val superProtocols: List<String> = descriptor.superProtocols

        return objCProtocol(name, descriptor, superProtocols, members)
    }

    private val ClassDescriptor.superProtocols: List<String>
        get() =
            getSuperInterfaces()
                    .asSequence()
                    .filter { mapper.shouldBeExposed(it) }
                    .map {
                        generator?.generateExtraInterfaceEarly(it)
                        referenceProtocol(it).objCName
                    }
                    .toList()

    override fun translateExtensions(
            classDescriptor: ClassDescriptor,
            declarations: List<CallableMemberDescriptor>
    ): ObjCInterface {
        generator?.generateExtraClassEarly(classDescriptor)

        val name = referenceClass(classDescriptor).objCName
        val members = buildMembers {
            translatePlainMembers(declarations, ObjCNoneExportScope)
        }
        return ObjCInterfaceImpl(name, categoryName = "Extensions", members = members)
    }

    override fun translateFile(file: SourceFile, declarations: List<CallableMemberDescriptor>): ObjCInterface {
        val name = namer.getFileClassName(file)

        // TODO: stop inheriting KotlinBase.
        val members = buildMembers {
            translatePlainMembers(declarations, ObjCNoneExportScope)
        }
        return objCInterface(
                name,
                superClass = namer.kotlinAnyName.objCName,
                members = members,
                attributes = listOf(OBJC_SUBCLASSING_RESTRICTED)
        )
    }

    override fun translateClass(descriptor: ClassDescriptor): ObjCInterface {
        require(!descriptor.isInterface)
        if (!mapper.shouldBeExposed(descriptor)) {
            return translateUnexposedClassAsUnavailableStub(descriptor)
        }

        val genericExportScope = if (objcGenerics) {
            ObjCClassExportScope(descriptor, namer)
        } else {
            ObjCNoneExportScope
        }

        fun superClassGenerics(genericExportScope: ObjCExportScope): List<ObjCNonNullReferenceType> {
            val parentType = computeSuperClassType(descriptor)
            return if(parentType != null) {
                parentType.arguments.map { typeProjection ->
                    mapReferenceTypeIgnoringNullability(typeProjection.type, genericExportScope)
                }
            } else {
                emptyList()
            }
        }

        val superClass = descriptor.getSuperClassNotAny()

        val superName = if (superClass == null) {
            kotlinAnyName
        } else {
            generator?.generateExtraClassEarly(superClass)
            referenceClass(superClass)
        }

        val superProtocols: List<String> = descriptor.superProtocols
        val members: List<Stub<*>> = buildMembers {
            val presentConstructors = mutableSetOf<String>()

            descriptor.constructors
                    .asSequence()
                    .filter { mapper.shouldBeExposed(it) }
                    .forEach {
                        val selector = getSelector(it)
                        if (!descriptor.isArray) presentConstructors += selector

                        add { buildMethod(it, it, genericExportScope) }
                        exportThrown(it)
                        if (selector == "init") add {
                            ObjCMethod(it, false, ObjCInstanceType, listOf("new"), emptyList(),
                                    listOf("availability(swift, unavailable, message=\"use object initializers instead\")"))
                        }
                    }

            if (descriptor.isArray || descriptor.kind == ClassKind.OBJECT || descriptor.kind == ClassKind.ENUM_CLASS) {
                add { ObjCMethod(null, false, ObjCInstanceType, listOf("alloc"), emptyList(), listOf("unavailable")) }

                val parameter = ObjCParameter("zone", null, ObjCRawType("struct _NSZone *"))
                add { ObjCMethod(descriptor, false, ObjCInstanceType, listOf("allocWithZone:"), listOf(parameter), listOf("unavailable")) }
            }

            // Hide "unimplemented" super constructors:
            superClass?.constructors
                    ?.asSequence()
                    ?.filter { mapper.shouldBeExposed(it) }
                    ?.forEach {
                        val selector = getSelector(it)
                        if (selector !in presentConstructors) {
                            add { buildMethod(it, it, ObjCNoneExportScope, unavailable = true) }

                            if (selector == "init") {
                                add { ObjCMethod(null, false, ObjCInstanceType, listOf("new"), emptyList(), listOf("unavailable")) }
                            }

                            // TODO: consider adding exception-throwing impls for these.
                        }
                    }

            // TODO: consider adding exception-throwing impls for these.
            when (descriptor.kind) {
                ClassKind.OBJECT -> add {
                    ObjCMethod(
                            null, false, ObjCInstanceType,
                            listOf(namer.getObjectInstanceSelector(descriptor)), emptyList(),
                            listOf(swiftNameAttribute("init()"))
                    )
                }
                ClassKind.ENUM_CLASS -> {
                    val type = mapType(descriptor.defaultType, ReferenceBridge, ObjCNoneExportScope)

                    descriptor.enumEntries.forEach {
                        val entryName = namer.getEnumEntrySelector(it)
                        add {
                            ObjCProperty(entryName, it, type, listOf("class", "readonly"),
                                    declarationAttributes = listOf(swiftNameAttribute(entryName)))
                        }
                    }

                    // Note: it is possible to support this function through a common machinery,
                    // but it is very special (static and synthetic), so apparently it is much easier
                    // to keep this ad hoc here than to add special cases to the most complicated parts
                    // of the machinery.
                    descriptor.getEnumValuesFunctionDescriptor()?.let { enumValues ->
                        add { buildEnumValuesMethod(enumValues, genericExportScope) }
                    }
                }
                else -> {
                    // Nothing special.
                }
            }

            translateClassMembers(descriptor, genericExportScope)
        }

        val attributes = if (descriptor.isFinalOrEnum) listOf(OBJC_SUBCLASSING_RESTRICTED) else emptyList()

        val name = translateClassOrInterfaceName(descriptor)

        val generics = if (objcGenerics) {
            descriptor.typeConstructor.parameters.map {
                "${it.variance.objcDeclaration()}${namer.getTypeParameterName(it)}"
            }
        } else {
            emptyList()
        }

        val superClassGenerics = if (objcGenerics) {
            superClassGenerics(genericExportScope)
        } else {
            emptyList()
        }

        return objCInterface(
                name,
                generics = generics,
                descriptor = descriptor,
                superClass = superName.objCName,
                superClassGenerics = superClassGenerics,
                superProtocols = superProtocols,
                members = members,
                attributes = attributes
        )
    }

    private fun buildEnumValuesMethod(
            enumValues: SimpleFunctionDescriptor,
            genericExportScope: ObjCExportScope
    ): ObjCMethod {
        val selector = namer.getEnumValuesSelector(enumValues)
        return ObjCMethod(
                enumValues,
                isInstanceMethod = false,
                returnType = mapReferenceType(enumValues.returnType!!, genericExportScope),
                selectors = splitSelector(selector),
                parameters = emptyList(),
                attributes = listOf(swiftNameAttribute("$selector()"))
        )
    }

    private fun ClassDescriptor.getExposedMembers(): List<CallableMemberDescriptor> =
            this.unsubstitutedMemberScope.getContributedDescriptors()
                    .asSequence()
                    .filterIsInstance<CallableMemberDescriptor>()
                    .filter { mapper.shouldBeExposed(it) }
                    .toList()

    private fun StubBuilder<Stub<*>>.translateClassMembers(descriptor: ClassDescriptor, objCExportScope: ObjCExportScope) {
        require(!descriptor.isInterface)
        translateClassMembers(descriptor.getExposedMembers(), objCExportScope)
    }

    private fun StubBuilder<Stub<*>>.translateInterfaceMembers(descriptor: ClassDescriptor) {
        require(descriptor.isInterface)
        translateBaseMembers(descriptor.getExposedMembers())
    }

    private fun List<CallableMemberDescriptor>.toObjCMembers(
            methodsBuffer: MutableList<FunctionDescriptor>,
            propertiesBuffer: MutableList<PropertyDescriptor>
    ) = this.forEach {
        when (it) {
            is FunctionDescriptor -> methodsBuffer += it
            is PropertyDescriptor -> if (mapper.isObjCProperty(it)) {
                propertiesBuffer += it
            } else {
                methodsBuffer.addIfNotNull(it.getter)
                methodsBuffer.addIfNotNull(it.setter)
            }
            else -> error(it)
        }
    }

    private fun StubBuilder<Stub<*>>.translateClassMembers(
            members: List<CallableMemberDescriptor>,
            objCExportScope: ObjCExportScope
    ) {
        // TODO: add some marks about modality.

        val methods = mutableListOf<FunctionDescriptor>()
        val properties = mutableListOf<PropertyDescriptor>()

        members.toObjCMembers(methods, properties)

        methods.forEach { exportThrown(it) }

        methods.retainAll { it.kind.isReal }
        properties.retainAll { it.kind.isReal }

        methods.forEach { method ->
            mapper.getBaseMethods(method)
                    .asSequence()
                    .distinctBy { namer.getSelector(it) }
                    .forEach { base -> add { buildMethod(method, base, objCExportScope) } }
        }

        properties.forEach { property ->
            mapper.getBaseProperties(property)
                    .asSequence()
                    .distinctBy { namer.getPropertyName(it) }
                    .forEach { base -> add { buildProperty(property, base, objCExportScope) } }
        }
    }

    private fun StubBuilder<Stub<*>>.translateBaseMembers(members: List<CallableMemberDescriptor>) {
        // TODO: add some marks about modality.

        val methods = mutableListOf<FunctionDescriptor>()
        val properties = mutableListOf<PropertyDescriptor>()

        members.toObjCMembers(methods, properties)

        methods.forEach { exportThrown(it) }

        methods.retainAll { mapper.isBaseMethod(it) }

        properties.retainAll {
            if (mapper.isBaseProperty(it)) {
                true
            } else {
                methods.addIfNotNull(it.setter?.takeIf(mapper::isBaseMethod))
                false
            }
        }

        translatePlainMembers(methods, properties, ObjCNoneExportScope)
    }

    private fun StubBuilder<Stub<*>>.translatePlainMembers(members: List<CallableMemberDescriptor>, objCExportScope: ObjCExportScope) {
        val methods = mutableListOf<FunctionDescriptor>()
        val properties = mutableListOf<PropertyDescriptor>()

        members.toObjCMembers(methods, properties)

        methods.forEach { exportThrown(it) }

        translatePlainMembers(methods, properties, objCExportScope)
    }

    private fun StubBuilder<Stub<*>>.translatePlainMembers(methods: List<FunctionDescriptor>, properties: List<PropertyDescriptor>, objCExportScope: ObjCExportScope) {
        methods.forEach { add { buildMethod(it, it, objCExportScope) } }
        properties.forEach { add { buildProperty(it, it, objCExportScope) } }
    }
    // TODO: consider checking that signatures for bases with same selector/name are equal.

    private fun getSelector(method: FunctionDescriptor): String {
        return namer.getSelector(method)
    }

    private fun buildProperty(property: PropertyDescriptor, baseProperty: PropertyDescriptor, objCExportScope: ObjCExportScope): ObjCProperty {
        assert(mapper.isBaseProperty(baseProperty))
        assert(mapper.isObjCProperty(baseProperty))

        val getterBridge = mapper.bridgeMethod(baseProperty.getter!!)
        val type = mapReturnType(getterBridge.returnBridge, property.getter!!, objCExportScope)
        val name = namer.getPropertyName(baseProperty)

        val attributes = mutableListOf<String>()

        if (!getterBridge.isInstance) {
            attributes += "class"
        }

        val setterName: String?
        val propertySetter = property.setter
        // Note: the condition below is similar to "toObjCMethods" logic in [ObjCExportedInterface.createCodeSpec].
        if (propertySetter != null && mapper.shouldBeExposed(propertySetter)) {
            val setterSelector = mapper.getBaseMethods(propertySetter).map { namer.getSelector(it) }.distinct().single()
            setterName = if (setterSelector != "set" + name.capitalize() + ":") setterSelector else null
        } else {
            attributes += "readonly"
            setterName = null
        }

        val getterSelector = getSelector(baseProperty.getter!!)
        val getterName: String? = if (getterSelector != name) getterSelector else null

        val declarationAttributes = mutableListOf(swiftNameAttribute(name))
        declarationAttributes.addIfNotNull(mapper.getDeprecation(property)?.toDeprecationAttribute())

        return ObjCProperty(name, property, type, attributes, setterName, getterName, declarationAttributes)
    }

    private fun buildMethod(
            method: FunctionDescriptor,
            baseMethod: FunctionDescriptor,
            objCExportScope: ObjCExportScope,
            unavailable: Boolean = false
    ): ObjCMethod {
        fun collectParameters(baseMethodBridge: MethodBridge, method: FunctionDescriptor): List<ObjCParameter> {
            fun unifyName(initialName: String, usedNames: Set<String>): String {
                var unique = initialName.toValidObjCSwiftIdentifier()
                while (unique in usedNames || unique in cKeywords) {
                    unique += "_"
                }
                return unique
            }

            val valueParametersAssociated = baseMethodBridge.valueParametersAssociated(method)

            val parameters = mutableListOf<ObjCParameter>()

            val usedNames = mutableSetOf<String>()

            valueParametersAssociated.forEach { (bridge: MethodBridgeValueParameter, p: ParameterDescriptor?) ->
                val candidateName: String = when (bridge) {
                    is MethodBridgeValueParameter.Mapped -> {
                        p!!
                        when {
                            p is ReceiverParameterDescriptor -> "receiver"
                            method is PropertySetterDescriptor -> "value"
                            else -> p.name.asString()
                        }
                    }
                    MethodBridgeValueParameter.ErrorOutParameter -> "error"
                    MethodBridgeValueParameter.SuspendCompletion -> "completionHandler"
                }

                val uniqueName = unifyName(candidateName, usedNames)
                usedNames += uniqueName

                val type = when (bridge) {
                    is MethodBridgeValueParameter.Mapped -> mapType(p!!.type, bridge.bridge, objCExportScope)
                    MethodBridgeValueParameter.ErrorOutParameter ->
                        ObjCPointerType(ObjCNullableReferenceType(ObjCClassType("NSError")), nullable = true)

                    MethodBridgeValueParameter.SuspendCompletion -> {
                        ObjCBlockPointerType(
                                returnType = ObjCVoidType,
                                parameterTypes = listOf(
                                        mapReferenceType(method.returnType!!, objCExportScope).makeNullable(),
                                        ObjCNullableReferenceType(ObjCClassType("NSError"))
                                )
                        )
                    }
                }

                parameters += ObjCParameter(uniqueName, p, type)
            }
            return parameters
        }

        assert(mapper.isBaseMethod(baseMethod))

        val baseMethodBridge = mapper.bridgeMethod(baseMethod)

        val isInstanceMethod: Boolean = baseMethodBridge.isInstance
        val returnType: ObjCType = mapReturnType(baseMethodBridge.returnBridge, method, objCExportScope)
        val parameters = collectParameters(baseMethodBridge, method)
        val selector = getSelector(baseMethod)
        val selectorParts: List<String> = splitSelector(selector)
        val swiftName = namer.getSwiftName(baseMethod)
        val attributes = mutableListOf<String>()

        attributes += swiftNameAttribute(swiftName)
        if (baseMethodBridge.returnBridge is MethodBridge.ReturnValue.WithError.ZeroForError
                && baseMethodBridge.returnBridge.successMayBeZero) {

            // Method may return zero on success, but
            // standard Objective-C convention doesn't suppose this happening.
            // Add non-standard convention hint for Swift:
            attributes += "swift_error(nonnull_error)" // Means "failure <=> (error != nil)".
        }

        if (method is ConstructorDescriptor && !method.constructedClass.isArray) { // TODO: check methodBridge instead.
            attributes += "objc_designated_initializer"
        }

        if (unavailable) {
            attributes += "unavailable"
        } else {
            attributes.addIfNotNull(mapper.getDeprecation(method)?.toDeprecationAttribute())
        }

        val comment = buildComment(method, baseMethodBridge)

        return ObjCMethod(method, isInstanceMethod, returnType, selectorParts, parameters, attributes, comment)
    }

    private fun splitSelector(selector: String): List<String> {
        return if (!selector.endsWith(":")) {
            listOf(selector)
        } else {
            selector.trimEnd(':').split(':').map { "$it:" }
        }
    }

    private fun buildComment(method: FunctionDescriptor, bridge: MethodBridge): ObjCComment? {
        if (method.isSuspend || bridge.returnsError) {
            val effectiveThrows = getEffectiveThrows(method).toSet()
            return when {
                effectiveThrows.contains(throwableClassId) -> {
                    ObjCComment("@note This method converts all Kotlin exceptions to errors.")
                }

                effectiveThrows.isNotEmpty() -> {
                    ObjCComment(
                            buildString {
                                append("@note This method converts instances of ")
                                effectiveThrows.joinTo(this) { it.relativeClassName.asString() }
                                append(" to errors.")
                            },
                            "Other uncaught Kotlin exceptions are fatal."
                    )
                }

                else -> {
                    // Shouldn't happen though.
                    ObjCComment("@warning All uncaught Kotlin exceptions are fatal.")
                }
            }
        }

        return null
    }

    private val throwableClassId = ClassId.topLevel(StandardNames.FqNames.throwable)

    private fun getEffectiveThrows(method: FunctionDescriptor): Sequence<ClassId> {
        method.overriddenDescriptors.firstOrNull()?.let { return getEffectiveThrows(it) }
        return getDefinedThrows(method).orEmpty()
    }

    private fun exportThrown(method: FunctionDescriptor) {
        getDefinedThrows(method)
                ?.mapNotNull { method.module.findClassAcrossModuleDependencies(it) }
                ?.forEach { generator?.requireClassOrInterface(it) }
    }

    private fun getDefinedThrows(method: FunctionDescriptor): Sequence<ClassId>? {
        if (!method.kind.isReal) return null

        val throwsAnnotation = method.annotations.findAnnotation(KonanFqNames.throws)

        if (throwsAnnotation != null) {
            val argumentsArrayValue = throwsAnnotation.firstArgument() as? ArrayValue
            return argumentsArrayValue?.value?.asSequence().orEmpty()
                    .filterIsInstance<KClassValue>()
                    .mapNotNull {
                        when (val value = it.value) {
                            is KClassValue.Value.NormalClass -> value.classId
                            is KClassValue.Value.LocalClass -> null
                        }
                    }
        }

        if (method.isSuspend && method.overriddenDescriptors.isEmpty()) {
            return implicitSuspendThrows
        }

        return null
    }

    private val implicitSuspendThrows = sequenceOf(ClassId.topLevel(KonanFqNames.cancellationException))

    private fun mapReturnType(returnBridge: MethodBridge.ReturnValue, method: FunctionDescriptor, objCExportScope: ObjCExportScope): ObjCType = when (returnBridge) {
        MethodBridge.ReturnValue.Suspend,
        MethodBridge.ReturnValue.Void -> ObjCVoidType
        MethodBridge.ReturnValue.HashCode -> ObjCPrimitiveType.NSUInteger
        is MethodBridge.ReturnValue.Mapped -> mapType(method.returnType!!, returnBridge.bridge, objCExportScope)
        MethodBridge.ReturnValue.WithError.Success -> ObjCPrimitiveType.BOOL
        is MethodBridge.ReturnValue.WithError.ZeroForError -> {
            val successReturnType = mapReturnType(returnBridge.successBridge, method, objCExportScope)

            if (!returnBridge.successMayBeZero) {
                check(successReturnType is ObjCNonNullReferenceType
                        || (successReturnType is ObjCPointerType && !successReturnType.nullable)) {
                    "Unexpected return type: $successReturnType in $method"
                }
            }

            successReturnType.makeNullableIfReferenceOrPointer()
        }

        MethodBridge.ReturnValue.Instance.InitResult,
        MethodBridge.ReturnValue.Instance.FactoryResult -> ObjCInstanceType
    }

    internal fun mapReferenceType(kotlinType: KotlinType, objCExportScope: ObjCExportScope): ObjCReferenceType =
            mapReferenceTypeIgnoringNullability(kotlinType, objCExportScope).withNullabilityOf(kotlinType)

    private fun ObjCNonNullReferenceType.withNullabilityOf(kotlinType: KotlinType): ObjCReferenceType =
            if (kotlinType.binaryRepresentationIsNullable()) {
                ObjCNullableReferenceType(this)
            } else {
                this
            }

    internal fun mapReferenceTypeIgnoringNullability(kotlinType: KotlinType, objCExportScope: ObjCExportScope): ObjCNonNullReferenceType {
        class TypeMappingMatch(val type: KotlinType, val descriptor: ClassDescriptor, val mapper: CustomTypeMapper)

        val typeMappingMatches = (listOf(kotlinType) + kotlinType.supertypes()).mapNotNull { type ->
            (type.constructor.declarationDescriptor as? ClassDescriptor)?.let { descriptor ->
                mapper.getCustomTypeMapper(descriptor)?.let { mapper ->
                    TypeMappingMatch(type, descriptor, mapper)
                }
            }
        }

        val mostSpecificMatches = typeMappingMatches.filter { match ->
            typeMappingMatches.all { otherMatch ->
                otherMatch.descriptor == match.descriptor ||
                        !otherMatch.descriptor.isSubclassOf(match.descriptor)
            }
        }

        if (mostSpecificMatches.size > 1) {
            val types = mostSpecificMatches.map { it.type }
            val firstType = types[0]
            val secondType = types[1]

            problemCollector.reportWarning(
                    "Exposed type '$kotlinType' is '$firstType' and '$secondType' at the same time. " +
                            "This most likely wouldn't work as expected.")

            // TODO: the same warning for such classes.
        }

        mostSpecificMatches.firstOrNull()?.let {
            return it.mapper.mapType(it.type, this, objCExportScope)
        }

        if(objcGenerics && kotlinType.isTypeParameter()){
            val genericTypeDeclaration = objCExportScope.getGenericDeclaration(TypeUtils.getTypeParameterDescriptorOrNull(kotlinType))
            if(genericTypeDeclaration != null)
                return genericTypeDeclaration
        }

        val classDescriptor = kotlinType.getErasedTypeClass()

        // TODO: translate `where T : BaseClass, T : SomeInterface` to `BaseClass* <SomeInterface>`

        // TODO: expose custom inline class boxes properly.
        if (isAny(classDescriptor) || classDescriptor.classId in mapper.hiddenTypes || classDescriptor.isInlined()) {
            return ObjCIdType
        }

        if (classDescriptor.defaultType.isObjCObjectType()) {
            return mapObjCObjectReferenceTypeIgnoringNullability(classDescriptor)
        }

        if (!mapper.shouldBeExposed(classDescriptor)) {
            // There are number of tricky corner cases getting here.
            return ObjCIdType
        }

        return if (classDescriptor.isInterface) {
            ObjCProtocolType(referenceProtocol(classDescriptor).objCName)
        } else {
            val typeArgs = if (objcGenerics) {
                kotlinType.arguments.map { typeProjection ->
                    if (typeProjection.isStarProjection) {
                        ObjCIdType // TODO: use Kotlin upper bound.
                    } else {
                        mapReferenceTypeIgnoringNullability(typeProjection.type, objCExportScope)
                    }
                }
            } else {
                emptyList()
            }
            ObjCClassType(referenceClass(classDescriptor).objCName, typeArgs)
        }
    }

    private tailrec fun mapObjCObjectReferenceTypeIgnoringNullability(descriptor: ClassDescriptor): ObjCNonNullReferenceType {
        // TODO: more precise types can be used.

        if (descriptor.isObjCMetaClass()) return ObjCMetaClassType
        if (descriptor.isObjCProtocolClass()) return foreignClassType("Protocol")

        if (descriptor.isExternalObjCClass() || descriptor.isObjCForwardDeclaration()) {
            return if (descriptor.isInterface) {
                val name = descriptor.name.asString().removeSuffix("Protocol")
                foreignProtocolType(name)
            } else {
                val name = descriptor.name.asString()
                foreignClassType(name)
            }
        }

        if (descriptor.isKotlinObjCClass()) {
            return mapObjCObjectReferenceTypeIgnoringNullability(descriptor.getSuperClassOrAny())
        }

        return ObjCIdType
    }

    private fun foreignProtocolType(name: String): ObjCProtocolType {
        generator?.referenceProtocol(name)
        return ObjCProtocolType(name)
    }

    private fun foreignClassType(name: String): ObjCClassType {
        generator?.referenceClass(name)
        return ObjCClassType(name)
    }

    internal fun mapFunctionTypeIgnoringNullability(
            functionType: KotlinType,
            objCExportScope: ObjCExportScope,
            returnsVoid: Boolean
    ): ObjCBlockPointerType {
        val parameterTypes = listOfNotNull(functionType.getReceiverTypeFromFunctionType()) +
                functionType.getValueParameterTypesFromFunctionType().map { it.type }

        return ObjCBlockPointerType(
                if (returnsVoid) {
                    ObjCVoidType
                } else {
                    mapReferenceType(functionType.getReturnTypeFromFunctionType(), objCExportScope)
                },
                parameterTypes.map { mapReferenceType(it, objCExportScope) }
        )
    }

    private fun mapFunctionType(
            kotlinType: KotlinType,
            objCExportScope: ObjCExportScope,
            typeBridge: BlockPointerBridge
    ): ObjCReferenceType {
        val expectedDescriptor = kotlinType.builtIns.getFunction(typeBridge.numberOfParameters)

        // Somewhat similar to mapType:
        val functionType = if (TypeUtils.getClassDescriptor(kotlinType) == expectedDescriptor) {
            kotlinType
        } else {
            kotlinType.supertypes().singleOrNull { TypeUtils.getClassDescriptor(it) == expectedDescriptor }
                    ?: expectedDescriptor.defaultType // Should not happen though.
        }

        return mapFunctionTypeIgnoringNullability(functionType, objCExportScope, typeBridge.returnsVoid)
                .withNullabilityOf(kotlinType)
    }

    private fun mapType(kotlinType: KotlinType, typeBridge: TypeBridge, objCExportScope: ObjCExportScope): ObjCType = when (typeBridge) {
        ReferenceBridge -> mapReferenceType(kotlinType, objCExportScope)
        is BlockPointerBridge -> mapFunctionType(kotlinType, objCExportScope, typeBridge)
        is ValueTypeBridge -> {
            when (typeBridge.objCValueType) {
                ObjCValueType.BOOL -> ObjCPrimitiveType.BOOL
                ObjCValueType.UNICHAR -> ObjCPrimitiveType.unichar
                ObjCValueType.CHAR -> ObjCPrimitiveType.int8_t
                ObjCValueType.SHORT -> ObjCPrimitiveType.int16_t
                ObjCValueType.INT -> ObjCPrimitiveType.int32_t
                ObjCValueType.LONG_LONG -> ObjCPrimitiveType.int64_t
                ObjCValueType.UNSIGNED_CHAR -> ObjCPrimitiveType.uint8_t
                ObjCValueType.UNSIGNED_SHORT -> ObjCPrimitiveType.uint16_t
                ObjCValueType.UNSIGNED_INT -> ObjCPrimitiveType.uint32_t
                ObjCValueType.UNSIGNED_LONG_LONG -> ObjCPrimitiveType.uint64_t
                ObjCValueType.FLOAT -> ObjCPrimitiveType.float
                ObjCValueType.DOUBLE -> ObjCPrimitiveType.double
                ObjCValueType.POINTER -> ObjCPointerType(ObjCVoidType, kotlinType.binaryRepresentationIsNullable())
            }
            // TODO: consider other namings.
        }
    }

    private inline fun buildTopLevel(block: StubBuilder<ObjCTopLevel<*>>.() -> Unit) = buildStubs(block)
    private inline fun buildMembers(block: StubBuilder<Stub<*>>.() -> Unit) = buildStubs(block)
    private inline fun <S : Stub<*>> buildStubs(block: StubBuilder<S>.() -> Unit): List<S> =
            StubBuilder<S>(problemCollector).apply(block).build()
}

abstract class ObjCExportHeaderGenerator internal constructor(
        val moduleDescriptors: List<ModuleDescriptor>,
        internal val mapper: ObjCExportMapper,
        val namer: ObjCExportNamer,
        val objcGenerics: Boolean,
        problemCollector: ObjCExportProblemCollector
) {
    private val stubs = mutableListOf<Stub<*>>()

    private val classForwardDeclarations = linkedSetOf<String>()
    private val protocolForwardDeclarations = linkedSetOf<String>()
    private val extraClassesToTranslate = mutableSetOf<ClassDescriptor>()

    private val translator = ObjCExportTranslatorImpl(this, mapper, namer, problemCollector, objcGenerics)

    private val generatedClasses = mutableSetOf<ClassDescriptor>()
    private val extensions = mutableMapOf<ClassDescriptor, MutableList<CallableMemberDescriptor>>()
    private val topLevel = mutableMapOf<SourceFile, MutableList<CallableMemberDescriptor>>()

    open val shouldExportKDoc = false

    fun build(): List<String> = mutableListOf<String>().apply {
        addImports(foundationImports)
        addImports(getAdditionalImports())
        add("")

        if (classForwardDeclarations.isNotEmpty()) {
            add("@class ${classForwardDeclarations.joinToString()};")
            add("")
        }

        if (protocolForwardDeclarations.isNotEmpty()) {
            add("@protocol ${protocolForwardDeclarations.joinToString()};")
            add("")
        }

        add("NS_ASSUME_NONNULL_BEGIN")
        add("#pragma clang diagnostic push")
        listOf(
                "-Wunknown-warning-option",

                // Protocols don't have generics, classes do. So generated header may contain
                // overriding property with "incompatible" type, e.g. `Generic<T>`-typed property
                // overriding `Generic<id>`. Suppress these warnings:
                "-Wincompatible-property-type",

                "-Wnullability"
        ).forEach {
            add("#pragma clang diagnostic ignored \"$it\"")
        }
        add("")

        stubs.forEach {
            addAll(StubRenderer.render(it, shouldExportKDoc))
            add("")
        }

        add("#pragma clang diagnostic pop")
        add("NS_ASSUME_NONNULL_END")
    }

    internal fun buildInterface(): ObjCExportedInterface {
        val headerLines = build()
        return ObjCExportedInterface(generatedClasses, extensions, topLevel, headerLines, namer, mapper)
    }

    fun getExportStubs(): ObjCExportedStubs =
        ObjCExportedStubs(classForwardDeclarations, protocolForwardDeclarations, stubs)

    protected open fun getAdditionalImports(): List<String> = emptyList()

    fun translateModule() {
        // TODO: make the translation order stable
        // to stabilize name mangling.
        translateBaseDeclarations()
        translateModuleDeclarations()
    }

    fun translateBaseDeclarations() {
        stubs += translator.generateBaseDeclarations()
    }

    fun translateModuleDeclarations() {
        translatePackageFragments()
        translateExtraClasses()
    }

    private fun translatePackageFragments() {
        val packageFragments = moduleDescriptors.flatMap { it.getPackageFragments() }

        packageFragments.forEach { packageFragment ->
            packageFragment.getMemberScope().getContributedDescriptors()
                    .asSequence()
                    .filterIsInstance<CallableMemberDescriptor>()
                    .filter { mapper.shouldBeExposed(it) }
                    .forEach {
                        val classDescriptor = mapper.getClassIfCategory(it)
                        if (classDescriptor != null) {
                            extensions.getOrPut(classDescriptor, { mutableListOf() }) += it
                        } else {
                            topLevel.getOrPut(it.findSourceFile(), { mutableListOf() }) += it
                        }
                    }

        }

        fun MemberScope.translateClasses() {
            getContributedDescriptors()
                    .asSequence()
                    .filterIsInstance<ClassDescriptor>()
                    .forEach {
                        if (mapper.shouldBeExposed(it)) {
                            if (it.isInterface) {
                                generateInterface(it)
                            } else {
                                generateClass(it)
                            }

                            it.unsubstitutedMemberScope.translateClasses()
                        } else if (mapper.shouldBeVisible(it)) {
                            stubs += if (it.isInterface) {
                                translator.translateUnexposedInterfaceAsUnavailableStub(it)
                            } else {
                                translator.translateUnexposedClassAsUnavailableStub(it)
                            }
                        }
                    }
        }

        packageFragments.forEach { packageFragment ->
            packageFragment.getMemberScope().translateClasses()
        }

        extensions.forEach { classDescriptor, declarations ->
            generateExtensions(classDescriptor, declarations)
        }

        topLevel.forEach { sourceFile, declarations ->
            generateFile(sourceFile, declarations)
        }
    }

    /**
     * Translates additional classes referenced from the module's declarations, such as parameter types, return types,
     * thrown exception types, and underlying enum types.
     *
     * This is required for classes from dependencies to be exported correctly. However, we also currently rely on this
     * for a few edge cases, such as some inner classes. Sub classes may reject certain descriptors to be translated.
     * Some referenced descriptors may be translated early for ordering reasons.
     * @see shouldTranslateExtraClass
     * @see generateExtraClassEarly
     * @see generateExtraInterfaceEarly
     */
    private fun translateExtraClasses() {
        while (extraClassesToTranslate.isNotEmpty()) {
            val descriptor = extraClassesToTranslate.first()
            extraClassesToTranslate -= descriptor

            assert(shouldTranslateExtraClass(descriptor)) { "Shouldn't be queued for translation: $descriptor" }
            if (descriptor.isInterface) {
                generateInterface(descriptor)
            } else {
                generateClass(descriptor)
            }
        }
    }

    private fun generateFile(sourceFile: SourceFile, declarations: List<CallableMemberDescriptor>) {
        stubs.add(translator.translateFile(sourceFile, declarations))
    }

    private fun generateExtensions(classDescriptor: ClassDescriptor, declarations: List<CallableMemberDescriptor>) {
        stubs.add(translator.translateExtensions(classDescriptor, declarations))
    }

    protected open fun shouldTranslateExtraClass(descriptor: ClassDescriptor): Boolean = true

    internal fun generateExtraClassEarly(descriptor: ClassDescriptor) {
        if (shouldTranslateExtraClass(descriptor)) generateClass(descriptor)
    }

    internal fun generateExtraInterfaceEarly(descriptor: ClassDescriptor) {
        if (shouldTranslateExtraClass(descriptor)) generateInterface(descriptor)
    }

    private fun generateClass(descriptor: ClassDescriptor) {
        if (!generatedClasses.add(descriptor)) return
        stubs.add(translator.translateClass(descriptor))
    }

    private fun generateInterface(descriptor: ClassDescriptor) {
        if (!generatedClasses.add(descriptor)) return
        stubs.add(translator.translateInterface(descriptor))
    }

    internal fun requireClassOrInterface(descriptor: ClassDescriptor) {
        if (shouldTranslateExtraClass(descriptor) && descriptor !in generatedClasses) {
            extraClassesToTranslate += descriptor
        }
    }

    internal fun referenceClass(objCName: String) {
        classForwardDeclarations += objCName
    }

    internal fun referenceProtocol(objCName: String) {
        protocolForwardDeclarations += objCName
    }

    companion object {
        val foundationImports = listOf(
            "Foundation/NSArray.h",
            "Foundation/NSDictionary.h",
            "Foundation/NSError.h",
            "Foundation/NSObject.h",
            "Foundation/NSSet.h",
            "Foundation/NSString.h",
            "Foundation/NSValue.h"
        )

        private fun MutableList<String>.addImports(imports: Iterable<String>) {
            imports.forEach {
                add("#import <$it>")
            }
        }
    }
}

private fun objCInterface(
        name: ObjCExportNamer.ClassOrProtocolName,
        generics: List<String> = emptyList(),
        descriptor: ClassDescriptor? = null,
        superClass: String? = null,
        superClassGenerics: List<ObjCNonNullReferenceType> = emptyList(),
        superProtocols: List<String> = emptyList(),
        members: List<Stub<*>> = emptyList(),
        attributes: List<String> = emptyList()
): ObjCInterface = ObjCInterfaceImpl(
        name.objCName,
        generics,
        descriptor,
        superClass,
        superClassGenerics,
        superProtocols,
        null,
        members,
        attributes + name.toNameAttributes()
)

private fun objCProtocol(
        name: ObjCExportNamer.ClassOrProtocolName,
        descriptor: ClassDescriptor,
        superProtocols: List<String>,
        members: List<Stub<*>>,
        attributes: List<String> = emptyList()
): ObjCProtocol = ObjCProtocolImpl(
        name.objCName,
        descriptor,
        superProtocols,
        members,
        attributes + name.toNameAttributes()
)

internal fun ObjCExportNamer.ClassOrProtocolName.toNameAttributes(): List<String> = listOfNotNull(
        binaryName.takeIf { it != objCName }?.let { objcRuntimeNameAttribute(it) },
        swiftName.takeIf { it != objCName }?.let { swiftNameAttribute(it) }
)

private fun swiftNameAttribute(swiftName: String) = "swift_name(\"$swiftName\")"
private fun objcRuntimeNameAttribute(name: String) = "objc_runtime_name(\"$name\")"

interface ObjCExportScope{
    fun getGenericDeclaration(typeParameterDescriptor: TypeParameterDescriptor?): ObjCGenericTypeDeclaration?
}

internal class ObjCClassExportScope constructor(container:DeclarationDescriptor, val namer: ObjCExportNamer): ObjCExportScope {
    private val typeNames = if(container is ClassDescriptor && !container.isInterface) {
        container.typeConstructor.parameters
    } else {
        emptyList<TypeParameterDescriptor>()
    }

    override fun getGenericDeclaration(typeParameterDescriptor: TypeParameterDescriptor?): ObjCGenericTypeDeclaration? {
        val localTypeParam = typeNames.firstOrNull {
            typeParameterDescriptor != null &&
                    (it == typeParameterDescriptor || (it.isCapturedFromOuterDeclaration && it.original == typeParameterDescriptor))
        }

        return if(localTypeParam == null) {
            null
        } else {
            ObjCGenericTypeDeclaration(localTypeParam, namer)
        }
    }
}

internal object ObjCNoneExportScope: ObjCExportScope{
    override fun getGenericDeclaration(typeParameterDescriptor: TypeParameterDescriptor?): ObjCGenericTypeDeclaration? = null
}

internal fun Variance.objcDeclaration():String = when(this){
    Variance.OUT_VARIANCE -> "__covariant "
    Variance.IN_VARIANCE -> "__contravariant "
    else -> ""
}

private fun computeSuperClassType(descriptor: ClassDescriptor): KotlinType? = descriptor.typeConstructor.supertypes.filter { !it.isInterface() }.firstOrNull()

internal const val OBJC_SUBCLASSING_RESTRICTED = "objc_subclassing_restricted"

private fun Deprecation.toDeprecationAttribute(): String {
    val attribute = when (deprecationLevel) {
        DeprecationLevelValue.WARNING -> "deprecated"
        DeprecationLevelValue.ERROR, DeprecationLevelValue.HIDDEN -> "unavailable"
    }

    // TODO: consider avoiding code generation for unavailable.

    val message = this.message.orEmpty()

    return "$attribute(${quoteAsCStringLiteral(message)})"
}

private fun quoteAsCStringLiteral(str: String): String = buildString {
    append('"')
    for (c in str) {
        when (c) {
            '\n' -> append("\\n")

            '\r' -> append("\\r")

            '"', '\\' -> append('\\').append(c)

            // TODO: handle more special cases.
            else -> append(c)
        }
    }
    append('"')
}
