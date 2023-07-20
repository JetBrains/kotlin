/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.common.serialization.findSourceFile
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.isAny
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.deprecation.DeprecationInfo
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
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
            val generics = listOf("ObjectType")
            objCInterface(
                    namer.mutableSetName,
                    generics = generics,
                    superClass = "NSMutableSet",
                    superClassGenerics = generics
            )
        }

        // TODO: only if appears
        add {
            val generics = listOf("KeyType", "ObjectType")
            objCInterface(
                    namer.mutableMapName,
                    generics = generics,
                    superClass = "NSMutableDictionary",
                    superClassGenerics = generics
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
            NSNumberKind.entries.forEach {
                add { nsNumberFactory(it, listOf("unavailable")) }
            }
            NSNumberKind.entries.forEach {
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

        NSNumberKind.entries.forEach {
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
        assert(mapper.shouldBeExposed(descriptor)) { "Shouldn't be exposed: $descriptor" }
        assert(!descriptor.isInterface)
        generator?.requireClassOrInterface(descriptor)

        return translateClassOrInterfaceName(descriptor).also { className ->
            val generics = mapTypeConstructorParameters(descriptor)
            val forwardDeclaration = ObjCClassForwardDeclaration(className.objCName, generics)
            generator?.referenceClass(forwardDeclaration)
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
        val comment = objCCommentOrNull(mustBeDocumentedAttributeList(descriptor.annotations))

        return objCProtocol(name, descriptor, superProtocols, members, comment = comment)
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
            translatePlainMembers(declarations, ObjCRootExportScope)
        }
        return ObjCInterfaceImpl(name, categoryName = "Extensions", members = members)
    }

    override fun translateFile(file: SourceFile, declarations: List<CallableMemberDescriptor>): ObjCInterface {
        val name = namer.getFileClassName(file)

        // TODO: stop inheriting KotlinBase.
        val members = buildMembers {
            translatePlainMembers(declarations, ObjCRootExportScope)
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

        val genericExportScope = createGenericExportScope(descriptor)

        fun superClassGenerics(genericExportScope: ObjCExportScope): List<ObjCNonNullReferenceType> {
            if (objcGenerics) {
                computeSuperClassType(descriptor)?.let { parentType ->
                    return parentType.arguments.map { typeProjection ->
                        mapReferenceTypeIgnoringNullability(typeProjection.type, genericExportScope)
                    }
                }
            }
            return emptyList()
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
                    .makeMethodsOrderStable()
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
                    ?.makeMethodsOrderStable()
                    ?.asSequence()
                    ?.filter { mapper.shouldBeExposed(it) }
                    ?.forEach {
                        val selector = getSelector(it)
                        if (selector !in presentConstructors) {
                            add { buildMethod(it, it, ObjCRootExportScope, unavailable = true) }

                            if (selector == "init") {
                                add { ObjCMethod(null, false, ObjCInstanceType, listOf("new"), emptyList(), listOf("unavailable")) }
                            }

                            // TODO: consider adding exception-throwing impls for these.
                        }
                    }

            if (descriptor.needCompanionObjectProperty(namer, mapper)) {
                add {
                    ObjCProperty(
                            ObjCExportNamer.companionObjectPropertyName, null,
                            mapReferenceType(descriptor.companionObjectDescriptor!!.defaultType, genericExportScope),
                            listOf("class", "readonly"),
                            getterName = namer.getCompanionObjectPropertySelector(descriptor),
                            declarationAttributes = listOf(swiftNameAttribute(ObjCExportNamer.companionObjectPropertyName))
                    )
                }
            }
            // TODO: consider adding exception-throwing impls for these.
            when (descriptor.kind) {
                ClassKind.OBJECT -> {
                    add {
                        ObjCMethod(
                                null, false, ObjCInstanceType,
                                listOf(namer.getObjectInstanceSelector(descriptor)), emptyList(),
                                listOf(swiftNameAttribute("init()"))
                        )
                    }
                    add {
                        ObjCProperty(
                                ObjCExportNamer.objectPropertyName, null,
                                mapReferenceType(descriptor.defaultType, genericExportScope), listOf("class", "readonly"),
                                getterName = namer.getObjectPropertySelector(descriptor),
                                declarationAttributes = listOf(swiftNameAttribute(ObjCExportNamer.objectPropertyName))
                        )
                    }
                }
                ClassKind.ENUM_CLASS -> {
                    val type = mapType(descriptor.defaultType, ReferenceBridge, ObjCRootExportScope)

                    descriptor.enumEntries.forEach {
                        val entryName = namer.getEnumEntrySelector(it)
                        val swiftName = namer.getEnumEntrySwiftName(it)
                        add {
                            ObjCProperty(entryName, it, type, listOf("class", "readonly"),
                                    declarationAttributes = listOf(swiftNameAttribute(swiftName)))
                        }
                    }

                    // Note: it is possible to support this function through a common machinery,
                    // but it is very special (static and synthetic), so apparently it is much easier
                    // to keep this ad hoc here than to add special cases to the most complicated parts
                    // of the machinery.
                    descriptor.getEnumValuesFunctionDescriptor()?.let { enumValues ->
                        add { buildEnumValuesMethod(enumValues, genericExportScope) }
                    }
                    descriptor.getEnumEntriesPropertyDescriptor()?.let { enumEntries ->
                        add { buildEnumEntriesProperty(enumEntries, genericExportScope) }
                    }
                }
                else -> {
                    // Nothing special.
                }
            }

            translateClassMembers(descriptor, genericExportScope)

            if (KotlinBuiltIns.isThrowable(descriptor)) {
                add { buildThrowableAsErrorMethod() }
            }
        }

        val attributes = if (descriptor.isFinalOrEnum) listOf(OBJC_SUBCLASSING_RESTRICTED) else emptyList()

        val name = translateClassOrInterfaceName(descriptor)
        val generics = mapTypeConstructorParameters(descriptor)
        val superClassGenerics = superClassGenerics(genericExportScope)

        return objCInterface(
                name,
                generics = generics,
                descriptor = descriptor,
                superClass = superName.objCName,
                superClassGenerics = superClassGenerics,
                superProtocols = superProtocols,
                members = members,
                attributes = attributes,
                comment = objCCommentOrNull(mustBeDocumentedAttributeList(descriptor.annotations))
        )
    }

    internal fun createGenericExportScope(descriptor: ClassDescriptor): ObjCExportScope = if (objcGenerics) {
        ObjCRootExportScope.deriveForClass(descriptor, namer)
    } else {
        ObjCRootExportScope
    }

    private fun buildThrowableAsErrorMethod(): ObjCMethod {
        val asError = ObjCExportNamer.kotlinThrowableAsErrorMethodName
        return ObjCMethod(
                descriptor = null,
                isInstanceMethod = true,
                returnType = ObjCClassType("NSError"),
                selectors = listOf(asError),
                parameters = emptyList(),
                attributes = listOf(swiftNameAttribute("$asError()"))
        )
    }

    private fun mapTypeConstructorParameters(descriptor: ClassDescriptor): List<ObjCGenericTypeParameterDeclaration> {
        if (objcGenerics) {
            return descriptor.typeConstructor.parameters.map {
                ObjCGenericTypeParameterDeclaration(it, namer)
            }
        }
        return emptyList()
    }

    private fun buildEnumValuesMethod(
            enumValues: SimpleFunctionDescriptor,
            genericExportScope: ObjCExportScope
    ): ObjCMethod {
        val selector = namer.getEnumStaticMemberSelector(enumValues)
        return ObjCMethod(
                enumValues,
                isInstanceMethod = false,
                returnType = mapReferenceType(enumValues.returnType!!, genericExportScope),
                selectors = splitSelector(selector),
                parameters = emptyList(),
                attributes = listOf(swiftNameAttribute("$selector()"))
        )
    }

    private fun buildEnumEntriesProperty(
            enumEntries: PropertyDescriptor,
            genericExportScope: ObjCExportScope
    ): ObjCProperty {
        val selector = namer.getEnumStaticMemberSelector(enumEntries)
        return ObjCProperty(
                selector,
                enumEntries,
                type = mapReferenceType(enumEntries.type, genericExportScope),
                propertyAttributes = listOf("class", "readonly"),
                declarationAttributes = listOf(swiftNameAttribute(selector))
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

        methods.makeMethodsOrderStable().forEach { method ->
            mapper.getBaseMethods(method)
                    .makeMethodsOrderStable()
                    .asSequence()
                    .distinctBy { namer.getSelector(it) }
                    .forEach { base -> add { buildMethod(method, base, objCExportScope) } }
        }

        properties.makePropertiesOrderStable().forEach { property ->
            mapper.getBaseProperties(property)
                    .makePropertiesOrderStable()
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

        translatePlainMembers(methods, properties, ObjCRootExportScope)
    }

    private fun StubBuilder<Stub<*>>.translatePlainMembers(members: List<CallableMemberDescriptor>, objCExportScope: ObjCExportScope) {
        val methods = mutableListOf<FunctionDescriptor>()
        val properties = mutableListOf<PropertyDescriptor>()

        members.toObjCMembers(methods, properties)

        methods.forEach { exportThrown(it) }

        translatePlainMembers(methods, properties, objCExportScope)
    }

    private fun StubBuilder<Stub<*>>.translatePlainMembers(methods: List<FunctionDescriptor>, properties: List<PropertyDescriptor>, objCExportScope: ObjCExportScope) {
        methods.makeMethodsOrderStable().forEach { add { buildMethod(it, it, objCExportScope) } }
        properties.makePropertiesOrderStable().forEach { add { buildProperty(it, it, objCExportScope) } }
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
        val propertyName = namer.getPropertyName(baseProperty)
        val name = propertyName.objCName

        val attributes = mutableListOf<String>()

        if (!getterBridge.isInstance) {
            attributes += "class"
        }

        val setterName: String?
        val propertySetter = property.setter
        // Note: the condition below is similar to "toObjCMethods" logic in [ObjCExportedInterface.createCodeSpec].
        if (propertySetter != null && mapper.shouldBeExposed(propertySetter)) {
            val setterSelector = mapper.getBaseMethods(propertySetter).map { namer.getSelector(it) }.distinct().single()
            setterName = if (setterSelector != "set" + name.replaceFirstChar(Char::uppercaseChar) + ":") setterSelector else null
        } else {
            attributes += "readonly"
            setterName = null
        }

        val getterSelector = getSelector(baseProperty.getter!!)
        val getterName: String? = if (getterSelector != name) getterSelector else null

        val declarationAttributes = mutableListOf(property.getSwiftPrivateAttribute() ?: swiftNameAttribute(propertyName.swiftName))
        declarationAttributes.addIfNotNull(mapper.getDeprecation(property)?.toDeprecationAttribute())

        val visibilityComments = visibilityComments(property.visibility, "property")

        val commentOrNull = objCCommentOrNull(mustBeDocumentedAttributeList(property.annotations) + visibilityComments)
        return ObjCProperty(name, property, type, attributes, setterName, getterName, declarationAttributes, commentOrNull)
    }

    internal fun buildMethod(
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
                            else -> namer.getParameterName(p)
                        }
                    }
                    MethodBridgeValueParameter.ErrorOutParameter -> "error"
                    is MethodBridgeValueParameter.SuspendCompletion -> "completionHandler"
                }

                val uniqueName = unifyName(candidateName, usedNames)
                usedNames += uniqueName

                val type = when (bridge) {
                    is MethodBridgeValueParameter.Mapped -> mapType(p!!.type, bridge.bridge, objCExportScope)
                    MethodBridgeValueParameter.ErrorOutParameter ->
                        ObjCPointerType(ObjCNullableReferenceType(ObjCClassType("NSError")), nullable = true)

                    is MethodBridgeValueParameter.SuspendCompletion -> {
                        val resultType = if (bridge.useUnitCompletion) {
                            null
                        } else {
                            when (val it = mapReferenceType(method.returnType!!, objCExportScope)) {
                                is ObjCNonNullReferenceType -> ObjCNullableReferenceType(it, isNullableResult = false)
                                is ObjCNullableReferenceType -> ObjCNullableReferenceType(it.nonNullType, isNullableResult = true)
                            }
                        }
                        ObjCBlockPointerType(
                                returnType = ObjCVoidType,
                                parameterTypes = listOfNotNull(
                                        resultType,
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

        attributes += method.getSwiftPrivateAttribute() ?: swiftNameAttribute(swiftName)
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
            attributes.addIfNotNull(getDeprecationAttribute(method))
        }

        val comment = buildComment(method, baseMethodBridge, parameters)

        return ObjCMethod(method, isInstanceMethod, returnType, selectorParts, parameters, attributes, comment)
    }

    private fun getDeprecationAttribute(method: FunctionDescriptor): String? {
        return mapper.getDeprecation(method)?.toDeprecationAttribute()
    }

    private fun splitSelector(selector: String): List<String> {
        return if (!selector.endsWith(":")) {
            listOf(selector)
        } else {
            selector.trimEnd(':').split(':').map { "$it:" }
        }
    }

    private fun buildComment(method: FunctionDescriptor, bridge: MethodBridge, parameters: List<ObjCParameter>): ObjCComment? {
        val throwsComments = if (method.isSuspend || bridge.returnsError) {
            val effectiveThrows = getEffectiveThrows(method).toSet()
            when {
                effectiveThrows.contains(throwableClassId) -> {
                    listOf("@note This method converts all Kotlin exceptions to errors.")
                }

                effectiveThrows.isNotEmpty() -> {
                    listOf(
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
                    listOf("@warning All uncaught Kotlin exceptions are fatal.")
                }
            }
        } else emptyList()

        val visibilityComments = visibilityComments(method.visibility, "method")
        val paramComments = parameters.flatMap { parameter ->
            parameter.descriptor?.let { mustBeDocumentedParamAttributeList(parameter, descriptor = it) } ?: emptyList()
        }
        val annotationsComments = mustBeDocumentedAttributeList(method.annotations)
        return objCCommentOrNull(annotationsComments + paramComments + throwsComments + visibilityComments)
    }

    private fun visibilityComments(visibility: DescriptorVisibility, kind: String): List<String> {
        return when (visibility) {
            DescriptorVisibilities.PROTECTED -> listOf("@note This $kind has protected visibility in Kotlin source and is intended only for use by subclasses.")
            else -> emptyList()
        }
    }

    private fun mustBeDocumentedParamAttributeList(parameter: ObjCParameter, descriptor: ParameterDescriptor): List<String> {
        val mbdAnnotations = mustBeDocumentedAnnotations(descriptor.annotations).joinToString(" ")
        return if (mbdAnnotations.isNotEmpty()) listOf("@param ${parameter.name} annotations $mbdAnnotations") else emptyList()
    }

    private fun mustBeDocumentedAttributeList(annotations: Annotations): List<String> {
        val mustBeDocumentedAnnotations = mustBeDocumentedAnnotations(annotations)
        return if (mustBeDocumentedAnnotations.isNotEmpty()) {
            listOf("@note annotations") + mustBeDocumentedAnnotations.map { "  $it" }
        } else emptyList()
    }

    private fun objCCommentOrNull(commentLines: List<String>): ObjCComment? {
        return if (commentLines.isNotEmpty()) ObjCComment(commentLines) else null
    }

    private fun renderAnnotation(descriptor: AnnotationDescriptor, clazz: ClassDescriptor): String {
        return buildString {
            append(clazz.fqNameSafe)
            if (descriptor.allValueArguments.isNotEmpty()) {
                append('(')
                descriptor.allValueArguments.entries.joinTo(this)
                append(')')
            }
        }
    }

    private val mustBeDocumentedAnnotationsStopList = setOf(StandardNames.FqNames.deprecated, KonanFqNames.objCName, KonanFqNames.shouldRefineInSwift)
    private fun mustBeDocumentedAnnotations(annotations: Annotations): List<String> {
        return annotations.mapNotNull { it ->
            it.annotationClass?.let { annotationClass ->
                if (!mustBeDocumentedAnnotationsStopList.contains(annotationClass.fqNameSafe) && annotationClass.annotations.any { metaAnnotation ->
                            metaAnnotation.fqName == StandardNames.FqNames.mustBeDocumented
                        })
                    renderAnnotation(it, annotationClass)
                else null
            }
        }
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
            try {
                return it.mapper.mapType(it.type, this, objCExportScope.deriveForType(it.type))
            } catch (e: ObjCExportScope.RecursionBreachException) {
                return ObjCIdType
            }
        }

        if (objcGenerics && kotlinType.isTypeParameter()) {
            val genericTypeUsage = objCExportScope
                    .nearestScopeOfType<ObjCClassExportScope>()
                    ?.getGenericTypeUsage(TypeUtils.getTypeParameterDescriptorOrNull(kotlinType))
            if (genericTypeUsage != null)
                return genericTypeUsage
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
        generator?.referenceClass(ObjCClassForwardDeclaration(name))
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
                parameterTypes.map {
                    mapReferenceType(it, objCExportScope)
                }
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

    private val classForwardDeclarations = linkedSetOf<ObjCClassForwardDeclaration>()
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
            add("@class ${
                classForwardDeclarations.joinToString {
                    buildString {
                        append(it.className)
                        formatGenerics(this, it.typeDeclarations)
                    }
                }
            };")
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

        // If _Nullable_result is not supported, then use _Nullable:
        add("#pragma push_macro(\"$objcNullableResultAttribute\")")
        add("#if !__has_feature(nullability_nullable_result)")
        add("#undef $objcNullableResultAttribute")
        add("#define $objcNullableResultAttribute $objcNullableAttribute")
        add("#endif")
        add("")

        stubs.forEach {
            addAll(StubRenderer.render(it, shouldExportKDoc))
            add("")
        }

        add("#pragma pop_macro(\"$objcNullableResultAttribute\")")
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

    private fun translateClass(descriptor: ClassDescriptor) {
        if (mapper.shouldBeExposed(descriptor)) {
            if (descriptor.isInterface) {
                generateInterface(descriptor)
            } else {
                generateClass(descriptor)
            }
        } else if (mapper.shouldBeVisible(descriptor)) {
            stubs += if (descriptor.isInterface) {
                translator.translateUnexposedInterfaceAsUnavailableStub(descriptor)
            } else {
                translator.translateUnexposedClassAsUnavailableStub(descriptor)
            }
        }
    }

    /**
     * Recursively collect classes into [collector].
     * We need to do so because we want to make the order of declarations stable.
     */
    private fun MemberScope.collectClasses(collector: MutableCollection<ClassDescriptor>) {
        getContributedDescriptors()
                .asSequence()
                .filterIsInstance<ClassDescriptor>()
                .forEach {
                    collector += it
                    // Avoid collecting nested declarations from unexposed classes.
                    if (mapper.shouldBeExposed(it)) {
                        it.unsubstitutedMemberScope.collectClasses(collector)
                    }
                }
    }

    private fun translatePackageFragments() {
        val packageFragments = moduleDescriptors
                .flatMap { it.getPackageFragments() }
                .makePackagesOrderStable()

        packageFragments.forEach { packageFragment ->
            packageFragment.getMemberScope().getContributedDescriptors()
                    .asSequence()
                    .filterIsInstance<CallableMemberDescriptor>()
                    .filter { mapper.shouldBeExposed(it) }
                    .forEach {
                        val classDescriptor = mapper.getClassIfCategory(it)
                        if (classDescriptor != null) {
                            // If a class is hidden from Objective-C API then it is meaningless
                            // to export its extensions.
                            if (!classDescriptor.isHiddenFromObjC()) {
                                extensions.getOrPut(classDescriptor, { mutableListOf() }) += it
                            }
                        } else {
                            topLevel.getOrPut(it.findSourceFile(), { mutableListOf() }) += it
                        }
                    }
        }

        val classesToTranslate = mutableListOf<ClassDescriptor>()

        packageFragments.forEach { packageFragment ->
            packageFragment.getMemberScope().collectClasses(classesToTranslate)
        }

        classesToTranslate.makeClassesOrderStable().forEach { translateClass(it) }

        extensions.makeCategoriesOrderStable().forEach { (classDescriptor, declarations) ->
            generateExtensions(classDescriptor, declarations)
        }

        topLevel.makeFilesOrderStable().forEach { (sourceFile, declarations) ->
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

    internal fun referenceClass(forwardDeclaration: ObjCClassForwardDeclaration) {
        classForwardDeclarations += forwardDeclaration
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
        generics: List<String>,
        superClass: String,
        superClassGenerics: List<String>
): ObjCInterface = objCInterface(
        name,
        generics = generics.map { ObjCGenericTypeRawDeclaration(it) },
        superClass = superClass,
        superClassGenerics = superClassGenerics.map { ObjCGenericTypeRawUsage(it) }
)

private fun objCInterface(
        name: ObjCExportNamer.ClassOrProtocolName,
        generics: List<ObjCGenericTypeDeclaration> = emptyList(),
        descriptor: ClassDescriptor? = null,
        superClass: String? = null,
        superClassGenerics: List<ObjCNonNullReferenceType> = emptyList(),
        superProtocols: List<String> = emptyList(),
        members: List<Stub<*>> = emptyList(),
        attributes: List<String> = emptyList(),
        comment: ObjCComment? = null
): ObjCInterface = ObjCInterfaceImpl(
        name.objCName,
        generics,
        descriptor,
        superClass,
        superClassGenerics,
        superProtocols,
        null,
        members,
        attributes + name.toNameAttributes(),
        comment
)

private fun objCProtocol(
        name: ObjCExportNamer.ClassOrProtocolName,
        descriptor: ClassDescriptor,
        superProtocols: List<String>,
        members: List<Stub<*>>,
        attributes: List<String> = emptyList(),
        comment: ObjCComment? = null
): ObjCProtocol = ObjCProtocolImpl(
        name.objCName,
        descriptor,
        superProtocols,
        members,
        attributes + name.toNameAttributes(),
        comment
)

internal fun ObjCExportNamer.ClassOrProtocolName.toNameAttributes(): List<String> = listOfNotNull(
        binaryName.takeIf { it != objCName }?.let { objcRuntimeNameAttribute(it) },
        swiftName.takeIf { it != objCName }?.let { swiftNameAttribute(it) }
)

private fun swiftNameAttribute(swiftName: String) = "swift_name(\"$swiftName\")"
private fun objcRuntimeNameAttribute(name: String) = "objc_runtime_name(\"$name\")"

private fun computeSuperClassType(descriptor: ClassDescriptor): KotlinType? =
        descriptor.typeConstructor.supertypes.firstOrNull { !it.isInterface() }

internal const val OBJC_SUBCLASSING_RESTRICTED = "objc_subclassing_restricted"

internal fun ClassDescriptor.needCompanionObjectProperty(namer: ObjCExportNamer, mapper: ObjCExportMapper): Boolean {
    val companionObject = companionObjectDescriptor
    if (companionObject == null || !mapper.shouldBeExposed(companionObject)) return false

    if (kind == ClassKind.ENUM_CLASS && enumEntries.any {
                namer.getEnumEntrySelector(it) == ObjCExportNamer.companionObjectPropertyName ||
                        namer.getEnumEntrySwiftName(it) == ObjCExportNamer.companionObjectPropertyName
            }
    ) return false // 'companion' property would clash with enum entry, don't generate it.

    return true
}

private fun DeprecationInfo.toDeprecationAttribute(): String {
    val attribute = when (deprecationLevel) {
        DeprecationLevelValue.WARNING -> "deprecated"
        DeprecationLevelValue.ERROR, DeprecationLevelValue.HIDDEN -> "unavailable"
    }

    // TODO: consider avoiding code generation for unavailable.

    val message = this.message.orEmpty()

    return renderDeprecationAttribute(attribute, message)
}

private fun renderDeprecationAttribute(attribute: String, message: String) = "$attribute(${quoteAsCStringLiteral(message)})"

private fun CallableMemberDescriptor.isRefinedInSwift(): Boolean = when {
    // Note: the front-end checker requires all overridden descriptors to be either refined or not refined.
    overriddenDescriptors.isNotEmpty() -> overriddenDescriptors.first().isRefinedInSwift()
    else -> annotations.any { annotation ->
        annotation.annotationClass?.annotations?.any { it.fqName == KonanFqNames.refinesInSwift } == true
    }
}

private fun CallableMemberDescriptor.getSwiftPrivateAttribute(): String? =
        if (isRefinedInSwift()) "swift_private" else null

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

// This family of `make*OrderStable` functions helps ObjCExport generate declarations in a stable order.
// See KT-58863.
private fun List<PropertyDescriptor>.makePropertiesOrderStable() =
        this.sortedBy { it.name }

private fun Collection<FunctionDescriptor>.makeMethodsOrderStable() =
        // The crucial part here is that we sort methods here by their signatures, which means
        // that we should be extra careful with signature evolution as it might affect method order.
        // Comparison of method names and number of parameters reduces the influence of signatures on the order of methods.
        // Also, it acts as an optimization.
        this.sortedWith(
                compareBy(
                        { it.name },
                        { it.valueParameters.size },
                        { KonanManglerDesc.run { it.signatureString(false) } }
                )
        )

private fun List<PackageFragmentDescriptor>.makePackagesOrderStable() =
        this.sortedBy { it.fqName.asString() }

/**
 * Sort order of files. Order of declarations will be stabilized in the corresponding functions later.
 */
private fun Map<SourceFile, MutableList<CallableMemberDescriptor>>.makeFilesOrderStable() =
        this.entries.sortedBy { it.key.name }

/**
 * Sort order of categories. Order of extensions will be stabilized in the corresponding functions later.
 */
private fun Map<ClassDescriptor, MutableList<CallableMemberDescriptor>>.makeCategoriesOrderStable() =
        this.entries.sortedBy { it.key.classId.toString() }

private fun List<ClassDescriptor>.makeClassesOrderStable() =
        this.sortedBy { it.classId.toString() }