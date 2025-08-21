/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.allOverriddenDescriptors
import org.jetbrains.kotlin.backend.konan.descriptors.isArray
import org.jetbrains.kotlin.backend.konan.descriptors.isInterface
import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.config.nativeBinaryOptions.UnitSuspendFunctionObjCExport
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.objcinterop.isObjCObjectType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.DataClassResolver
import org.jetbrains.kotlin.resolve.deprecation.DeprecationInfo
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isUnit

@InternalKotlinNativeApi
class ObjCExportMapper(
    internal val deprecationResolver: DeprecationResolver? = null,
    private val local: Boolean = false,
    internal val unitSuspendFunctionExport: UnitSuspendFunctionObjCExport,
    internal val entryPoints: ObjCEntryPoints = ObjCEntryPoints.ALL,
) {
    fun getCustomTypeMapper(descriptor: ClassDescriptor): CustomTypeMapper? = CustomTypeMappers.getMapper(descriptor)

    val hiddenTypes: Set<ClassId> get() = CustomTypeMappers.hiddenTypes

    private val methodBridgeCache = mutableMapOf<FunctionDescriptor, MethodBridge>()

    fun bridgeMethod(descriptor: FunctionDescriptor): MethodBridge = if (local) {
        bridgeMethodImpl(descriptor)
    } else {
        methodBridgeCache.getOrPut(descriptor) {
            bridgeMethodImpl(descriptor)
        }
    }
}

internal fun isSpecialMapped(descriptor: ClassDescriptor): Boolean {
    // TODO: this method duplicates some of the [ObjCExportTranslatorImpl.mapReferenceType] logic.
    return KotlinBuiltIns.isAny(descriptor) ||
        descriptor.getAllSuperClassifiers().any { it is ClassDescriptor && CustomTypeMappers.hasMapper(it) }
}

/**
 * Return null when:
 * 1. callable [descriptor] is not top level function or property
 * 2. callable [descriptor] doesn't have extension
 * 3. see other cases at [getClassIfCategory] with param [KotlinType]
 *
 * In other cases returns extension type:
 * ```kotlin
 * fun Foo.bar() = Unit
 * getClassIfCategory(bar) > Foo
 * ```
 */
internal fun getClassIfCategory(descriptor: CallableMemberDescriptor): ClassDescriptor? {
    if (descriptor.dispatchReceiverParameter != null) return null

    val extensionReceiverType = descriptor.extensionReceiverParameter?.type ?: return null

    return getClassIfCategory(extensionReceiverType)
}

/**
 * Returns null when:
 * 1. [extensionReceiverType] is extension of [kotlinx.cinterop.ObjCObject]
 * 2. [extensionReceiverType] is interface
 * 3. [extensionReceiverType] is inlined
 * 4. [extensionReceiverType] is [isSpecialMapped] == true
 *
 * In other cases returns extension type:
 * ```kotlin
 * fun Foo.bar() = Unit
 * getClassIfCategory(bar) > Foo
 * ```
 */
internal fun getClassIfCategory(extensionReceiverType: KotlinType): ClassDescriptor? {
    // FIXME: this code must rely on type mapping instead of copying its logic.

    if (extensionReceiverType.isObjCObjectType()) return null

    val erasedClass = extensionReceiverType.getErasedTypeClass()
    return if (!erasedClass.isInterface && !erasedClass.isInlined() && !isSpecialMapped(erasedClass)) {
        erasedClass
    } else {
        // E.g. receiver is protocol, or some type with custom mapping.
        null
    }
}

private fun isSealedClassConstructor(descriptor: ConstructorDescriptor) = descriptor.constructedClass.isSealed()

/**
 * Check that given [method] is a synthetic .componentN() method of a data class.
 */
private fun isComponentNMethod(method: CallableMemberDescriptor): Boolean {
    if ((method as? FunctionDescriptor)?.isOperator != true) return false
    val parent = method.containingDeclaration
    if (parent is ClassDescriptor && parent.isData && DataClassResolver.isComponentLike(method.name)) {
        // componentN method of data class.
        return true
    }
    return false
}

// Note: partially duplicated in ObjCExportLazyImpl.translateTopLevels.
@InternalKotlinNativeApi
fun ObjCExportMapper.shouldBeExposed(descriptor: CallableMemberDescriptor): Boolean = when {
    !descriptor.isEffectivelyPublicApi -> false
    descriptor.isExpect -> false
    isHiddenByDeprecation(descriptor) -> false
    descriptor is ConstructorDescriptor && isSealedClassConstructor(descriptor) -> false
    // KT-42641. Don't expose componentN methods of data classes
    // because they are useless in Objective-C/Swift.
    isComponentNMethod(descriptor) && descriptor.overriddenDescriptors.isEmpty() -> false
    descriptor.isHiddenFromObjC() -> false
    !entryPoints.shouldBeExposed(descriptor) -> false
    else -> true
}

private fun AnnotationDescriptor.hidesFromObjC(): Boolean =
    annotationClass?.annotations?.any { it.fqName == KonanFqNames.hidesFromObjC } ?: false

private fun CallableMemberDescriptor.isHiddenFromObjC(): Boolean = when {
    // Note: the front-end checker requires all overridden descriptors to be either refined or not refined.
    overriddenDescriptors.isNotEmpty() -> overriddenDescriptors.first().isHiddenFromObjC()
    contextReceiverParameters.isNotEmpty() -> true
    else -> annotations.any(AnnotationDescriptor::hidesFromObjC)
}

/**
 * Check if the given class or its enclosing declaration is marked as @HiddenFromObjC.
 */
internal fun ClassDescriptor.isHiddenFromObjC(): Boolean = when {
    containingDeclaration.let { it as? ClassDescriptor }?.isHiddenFromObjC() ?: false -> true
    annotations.any(AnnotationDescriptor::hidesFromObjC) -> true
    else -> false
}

internal fun ObjCExportMapper.shouldBeExposed(descriptor: ClassDescriptor): Boolean =
    shouldBeVisible(descriptor) && !isSpecialMapped(descriptor) && !descriptor.defaultType.isObjCObjectType()

private fun ObjCExportMapper.isHiddenByDeprecation(descriptor: CallableMemberDescriptor): Boolean {
    // Note: ObjCExport generally expect overrides of exposed methods to be exposed.
    // So don't hide a "deprecated hidden" method which overrides non-hidden one:
    if (deprecationResolver != null && deprecationResolver.isDeprecatedHidden(descriptor) &&
        descriptor.overriddenDescriptors.all { isHiddenByDeprecation(it) }
    ) {
        return true
    }

    // Note: ObjCExport expects members of unexposed classes to be unexposed too.
    // So hide a declaration if it is from a hidden class:
    val containingDeclaration = descriptor.containingDeclaration
    if (containingDeclaration is ClassDescriptor && isHiddenByDeprecation(containingDeclaration)) {
        return true
    }

    return false
}

internal fun ObjCExportMapper.getDeprecation(descriptor: DeclarationDescriptor): DeprecationInfo? {
    deprecationResolver?.getDeprecations(descriptor).orEmpty().maxByOrNull {
        when (it.deprecationLevel) {
            DeprecationLevelValue.WARNING -> 1
            DeprecationLevelValue.ERROR -> 2
            DeprecationLevelValue.HIDDEN -> 3
        }
    }?.let { return it }

    (descriptor as? ConstructorDescriptor)?.let {
        // Note: a deprecation can't be applied to a class itself when generating header
        // since the class can be referred from the header.
        // Apply class deprecations to its constructors instead:
        return getDeprecation(it.constructedClass)
    }

    return null
}

private fun ObjCExportMapper.isHiddenByDeprecation(descriptor: ClassDescriptor): Boolean {
    if (deprecationResolver == null) return false
    if (deprecationResolver.isDeprecatedHidden(descriptor)) return true

    // Note: ObjCExport requires super class of exposed class to be exposed.
    // So hide a class if its super class is hidden:
    val superClass = descriptor.getSuperClassNotAny()
    if (superClass != null && isHiddenByDeprecation(superClass)) {
        return true
    }

    // Note: ObjCExport requires enclosing class of exposed class to be exposed.
    // Also in Kotlin hidden class members (including other classes) aren't directly accessible.
    // So hide a class if its enclosing class is hidden:
    val containingDeclaration = descriptor.containingDeclaration
    if (containingDeclaration is ClassDescriptor && isHiddenByDeprecation(containingDeclaration)) {
        return true
    }

    return false
}

// Note: the logic is partially duplicated in ObjCExportLazyImpl.translateClasses.
internal fun ObjCExportMapper.shouldBeVisible(descriptor: ClassDescriptor): Boolean =
    descriptor.isEffectivelyPublicApi &&
        when (descriptor.kind) {
            ClassKind.CLASS, ClassKind.INTERFACE, ClassKind.ENUM_CLASS, ClassKind.OBJECT -> true
            ClassKind.ENUM_ENTRY, ClassKind.ANNOTATION_CLASS -> false
        } &&
        !descriptor.isExpect &&
        !descriptor.isInlined() &&
        !isHiddenByDeprecation(descriptor) &&
        !descriptor.isHiddenFromObjC()

private fun ObjCExportMapper.isBase(descriptor: CallableMemberDescriptor): Boolean =
    descriptor.overriddenDescriptors.all { !shouldBeExposed(it) }
// e.g. it is not `override`, or overrides only unexposed methods.

/**
 * Check that given [descriptor] is a so-called "base method", i.e. method
 * that doesn't override anything in a generated Objective-C interface.
 * Note that it does not mean that it has no "override" keyword.
 * Consider example:
 * ```kotlin
 * private interface I {
 *     fun f()
 * }
 *
 * class C : I {
 *     override fun f() {}
 * }
 * ```
 * Interface `I` is not exposed to the generated header, so C#f is considered to be a base method even though it has an "override" keyword.
 */
@InternalKotlinNativeApi
fun ObjCExportMapper.isBaseMethod(descriptor: FunctionDescriptor) =
    this.isBase(descriptor)

@InternalKotlinNativeApi
fun ObjCExportMapper.getBaseMethods(descriptor: FunctionDescriptor): List<FunctionDescriptor> =
    if (isBaseMethod(descriptor)) {
        listOf(descriptor)
    } else {
        descriptor.overriddenDescriptors.filter { shouldBeExposed(it) }
            .flatMap { getBaseMethods(it.original) }
            .distinct()
    }

@InternalKotlinNativeApi
fun ObjCExportMapper.isBaseProperty(descriptor: PropertyDescriptor) =
    isBase(descriptor)

@InternalKotlinNativeApi
fun ObjCExportMapper.getBaseProperties(descriptor: PropertyDescriptor): List<PropertyDescriptor> =
    if (isBaseProperty(descriptor)) {
        listOf(descriptor)
    } else {
        descriptor.overriddenDescriptors
            .flatMap { getBaseProperties(it.original) }
            .distinct()
    }

@Suppress("NO_TAIL_CALLS_FOUND", "NON_TAIL_RECURSIVE_CALL") // Suppressed due to KT-73420
internal tailrec fun KotlinType.getErasedTypeClass(): ClassDescriptor =
    TypeUtils.getClassDescriptor(this) ?: this.constructor.supertypes.first().getErasedTypeClass()

internal fun isTopLevel(descriptor: CallableMemberDescriptor): Boolean =
    descriptor.containingDeclaration !is ClassDescriptor && getClassIfCategory(descriptor) == null

internal fun isObjCProperty(property: PropertyDescriptor): Boolean =
    property.extensionReceiverParameter == null || getClassIfCategory(property) != null

@InternalKotlinNativeApi
fun ClassDescriptor.getEnumValuesFunctionDescriptor(): SimpleFunctionDescriptor? {
    require(this.kind == ClassKind.ENUM_CLASS)

    return this.staticScope.getContributedFunctions(
        StandardNames.ENUM_VALUES,
        NoLookupLocation.FROM_BACKEND
    ).singleOrNull { it.extensionReceiverParameter == null && it.valueParameters.size == 0 }
}

@InternalKotlinNativeApi
fun ClassDescriptor.getEnumEntriesPropertyDescriptor(): PropertyDescriptor? {
    require(this.kind == ClassKind.ENUM_CLASS)

    return this.staticScope.getContributedVariables(
        StandardNames.ENUM_ENTRIES,
        NoLookupLocation.FROM_BACKEND
    ).singleOrNull { it.extensionReceiverParameter == null }
}

internal fun doesThrow(method: FunctionDescriptor): Boolean = method.allOverriddenDescriptors.any {
    it.overriddenDescriptors.isEmpty() && it.annotations.hasAnnotation(KonanFqNames.throws)
}

private fun ObjCExportMapper.bridgeType(
    kotlinType: KotlinType,
): TypeBridge = kotlinType.unwrapToPrimitiveOrReference<TypeBridge>(
    eachInlinedClass = { inlinedClass, _ ->
        when (inlinedClass.classId) {
            UnsignedType.UBYTE.classId -> return ValueTypeBridge(ObjCValueType.UNSIGNED_CHAR)
            UnsignedType.USHORT.classId -> return ValueTypeBridge(ObjCValueType.UNSIGNED_SHORT)
            UnsignedType.UINT.classId -> return ValueTypeBridge(ObjCValueType.UNSIGNED_INT)
            UnsignedType.ULONG.classId -> return ValueTypeBridge(ObjCValueType.UNSIGNED_LONG_LONG)
        }
    },
    ifPrimitive = { primitiveType, _ ->
        val objCValueType = when (primitiveType) {
            KonanPrimitiveType.BOOLEAN -> ObjCValueType.BOOL
            KonanPrimitiveType.CHAR -> ObjCValueType.UNICHAR
            KonanPrimitiveType.BYTE -> ObjCValueType.CHAR
            KonanPrimitiveType.SHORT -> ObjCValueType.SHORT
            KonanPrimitiveType.INT -> ObjCValueType.INT
            KonanPrimitiveType.LONG -> ObjCValueType.LONG_LONG
            KonanPrimitiveType.FLOAT -> ObjCValueType.FLOAT
            KonanPrimitiveType.DOUBLE -> ObjCValueType.DOUBLE
            KonanPrimitiveType.NON_NULL_NATIVE_PTR -> ObjCValueType.POINTER
            KonanPrimitiveType.VECTOR128 -> ObjCValueType.VECTOR_FLOAT_128
        }
        ValueTypeBridge(objCValueType)
    },
    ifReference = {
        if (kotlinType.isFunctionType) {
            bridgeFunctionType(kotlinType)
        } else {
            ReferenceBridge
        }
    }
)

private fun bridgeFunctionType(kotlinType: KotlinType): TypeBridge {
    // kotlinType.arguments include return type: <P1, P2, ..., Pn, R>
    val numberOfParameters = kotlinType.arguments.size - 1

    val returnType = kotlinType.getReturnTypeFromFunctionType()
    val returnsVoid = returnType.isUnit() || returnType.isNothing()
    // Note: this is correct because overriding method can't turn this into false
    // neither for a parameter nor for a return type.

    return BlockPointerBridge(numberOfParameters, returnsVoid)
}

private fun ObjCExportMapper.bridgeParameter(parameter: ParameterDescriptor): MethodBridgeValueParameter =
    MethodBridgeValueParameter.Mapped(bridgeType(parameter.type))

private fun ObjCExportMapper.bridgeReturnType(
    descriptor: FunctionDescriptor,
    convertExceptionsToErrors: Boolean,
): MethodBridge.ReturnValue {
    val returnType = descriptor.returnType!!
    return when {
        descriptor.isSuspend -> MethodBridge.ReturnValue.Suspend

        descriptor is ConstructorDescriptor -> if (descriptor.constructedClass.isArray) {
            MethodBridge.ReturnValue.Instance.FactoryResult
        } else {
            MethodBridge.ReturnValue.Instance.InitResult
        }.let {
            if (convertExceptionsToErrors) {
                MethodBridge.ReturnValue.WithError.ZeroForError(it, successMayBeZero = false)
            } else {
                it
            }
        }

        descriptor.containingDeclaration.let { it is ClassDescriptor && KotlinBuiltIns.isAny(it) } &&
            descriptor.name.asString() == "hashCode" -> {
            assert(!convertExceptionsToErrors)
            MethodBridge.ReturnValue.HashCode
        }

        descriptor is PropertyGetterDescriptor -> {
            assert(!convertExceptionsToErrors)
            MethodBridge.ReturnValue.Mapped(bridgePropertyType(descriptor.correspondingProperty))
        }

        returnType.isUnit() || returnType.isNothing() -> if (convertExceptionsToErrors) {
            MethodBridge.ReturnValue.WithError.Success
        } else {
            MethodBridge.ReturnValue.Void
        }

        else -> {
            val returnTypeBridge = bridgeType(returnType)
            val successReturnValueBridge = MethodBridge.ReturnValue.Mapped(returnTypeBridge)
            if (convertExceptionsToErrors) {
                val canReturnZero = !returnTypeBridge.isReferenceOrPointer() || TypeUtils.isNullableType(returnType)
                MethodBridge.ReturnValue.WithError.ZeroForError(
                    successReturnValueBridge,
                    successMayBeZero = canReturnZero
                )
            } else {
                successReturnValueBridge
            }
        }
    }
}

private fun TypeBridge.isReferenceOrPointer(): Boolean = when (this) {
    ReferenceBridge, is BlockPointerBridge -> true
    is ValueTypeBridge -> this.objCValueType == ObjCValueType.POINTER
}

private fun ObjCExportMapper.bridgeMethodImpl(descriptor: FunctionDescriptor): MethodBridge {
    assert(isBaseMethod(descriptor))

    val convertExceptionsToErrors = doesThrow(descriptor)

    val kotlinParameters = descriptor.allParameters.iterator()

    val isTopLevel = isTopLevel(descriptor)

    val receiver = if (descriptor is ConstructorDescriptor && descriptor.constructedClass.isArray) {
        kotlinParameters.next()
        MethodBridgeReceiver.Factory
    } else if (isTopLevel) {
        MethodBridgeReceiver.Static
    } else {
        kotlinParameters.next()
        MethodBridgeReceiver.Instance
    }

    val valueParameters = mutableListOf<MethodBridgeValueParameter>()
    kotlinParameters.forEach {
        valueParameters += bridgeParameter(it)
    }

    val returnBridge = bridgeReturnType(descriptor, convertExceptionsToErrors)

    if (descriptor.isSuspend) {
        val useUnitCompletion = (unitSuspendFunctionExport == UnitSuspendFunctionObjCExport.PROPER) && (descriptor.returnType!!.isUnit())
        valueParameters += MethodBridgeValueParameter.SuspendCompletion(useUnitCompletion)
    } else if (convertExceptionsToErrors) {
        // Add error out parameter before tail block parameters. The convention allows this.
        // Placing it after would trigger https://bugs.swift.org/browse/SR-12201
        // (see also https://github.com/JetBrains/kotlin-native/issues/3825).
        val tailBlocksCount = valueParameters.reversed().takeWhile { it.isBlockPointer() }.count()
        valueParameters.add(valueParameters.size - tailBlocksCount, MethodBridgeValueParameter.ErrorOutParameter)
    }

    return MethodBridge(returnBridge, receiver, valueParameters)
}

private fun MethodBridgeValueParameter.isBlockPointer(): Boolean = when (this) {
    is MethodBridgeValueParameter.Mapped -> when (this.bridge) {
        ReferenceBridge, is ValueTypeBridge -> false
        is BlockPointerBridge -> true
    }
    MethodBridgeValueParameter.ErrorOutParameter -> false
    is MethodBridgeValueParameter.SuspendCompletion -> true
}

internal fun ObjCExportMapper.bridgePropertyType(descriptor: PropertyDescriptor): TypeBridge {
    assert(isBaseProperty(descriptor))

    return bridgeType(descriptor.type)
}
