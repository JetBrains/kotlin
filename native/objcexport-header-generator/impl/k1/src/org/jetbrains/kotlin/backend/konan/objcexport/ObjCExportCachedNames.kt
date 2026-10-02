/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.common.serialization.findSourceFile
import org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi
import org.jetbrains.kotlin.backend.konan.descriptors.isInterface
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.library.metadata.DeserializedSourceFile
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.descriptorUtil.propertyIfAccessor
import org.jetbrains.kotlin.types.*

private const val SEPARATOR = '\u001F'

internal fun objCExportNameKey(element: Any): String? = when (element) {
    is ClassDescriptor -> element.classId?.let { "C$it" }
    is SourceFile -> fileKey(element)
    is TypeParameterDescriptor -> (element.containingDeclaration as? ClassDescriptor)?.classId?.let { "Y$it$SEPARATOR${element.index}" }
    is PropertyAccessorDescriptor -> objCExportNameKey(element.correspondingProperty)?.let { (if (element is PropertyGetterDescriptor) "G" else "S") + it }
    is ConstructorDescriptor -> element.constructedClass.classId?.let { "K$it$SEPARATOR${element.signature()}" }
    is CallableMemberDescriptor -> containerKey(element)?.let { container ->
        "${if (element is PropertyDescriptor) 'P' else 'F'}$container$SEPARATOR${element.name.asString()}$SEPARATOR${element.signature()}"
    }
    else -> null
}

private fun fileKey(file: SourceFile): String =
    if (file is DeserializedSourceFile) "L${file.library.uniqueName}$SEPARATOR${file.name}" else "L$SEPARATOR${file.name}$SEPARATOR${file.hashCode()}"

private fun containerKey(member: CallableMemberDescriptor): String? = when (val container = member.containingDeclaration) {
    is ClassDescriptor -> container.classId?.let { "c$it" }
    is PackageFragmentDescriptor -> "p${member.module.name.asString()}$SEPARATOR${container.fqName.asString()}"
    else -> null
}

private fun CallableMemberDescriptor.signature(): String = KonanManglerDesc.run { signatureString(false) }

internal class ObjCExportClassClaim(val classId: String, val supertypes: Set<String>, val isFinal: Boolean, val isInterface: Boolean)

internal class ObjCExportMemberClaim(
    val owner: String,
    val isTopLevel: Boolean,
    val isConstructor: Boolean,
    val hasNoDispatchReceiver: Boolean,
    val isSetter: Boolean,
    val kotlinName: String,
    val extensionReceiverType: String,
    val parameterTypes: String,
    val bridge: String,
)

internal class ObjCExportNameRecord(
    val kind: ObjCExportNameKind,
    val key: String,
    val name: String,
    val claim: ObjCExportMemberClaim?,
)

internal val ObjCExportNameRecord.typeParameterOwner: String
    get() = key.substring(1, key.lastIndexOf(SEPARATOR))

internal fun membersConflict(
    first: ObjCExportMemberClaim,
    second: ObjCExportMemberClaim,
    isProperty: Boolean,
    classes: (String) -> ObjCExportClassClaim?,
    ignoreInterfaceMethodCollisions: Boolean,
): Boolean {
    if (!canBeInheritedBySameClass(first, second, classes, ignoreInterfaceMethodCollisions)) return false
    if (first.hasNoDispatchReceiver || second.hasNoDispatchReceiver) return true
    if (first.kotlinName != second.kotlinName) return true
    if (isProperty) return first.bridge != second.bridge
    if (first.extensionReceiverType != second.extensionReceiverType) return true
    if (!(first.isSetter && second.isSetter) && first.parameterTypes != second.parameterTypes) return true
    return first.bridge != second.bridge
}

private fun canBeInheritedBySameClass(
    first: ObjCExportMemberClaim,
    second: ObjCExportMemberClaim,
    classes: (String) -> ObjCExportClassClaim?,
    ignoreInterfaceMethodCollisions: Boolean,
): Boolean {
    if (first.isTopLevel || second.isTopLevel) return first.isTopLevel && second.isTopLevel && first.owner == second.owner
    val firstClass = classes(first.owner) ?: return true
    val secondClass = classes(second.owner) ?: return true
    if (first.isConstructor) {
        return firstClass.classId == secondClass.classId || !second.isConstructor && firstClass.isSubclassOf(secondClass)
    }
    if (second.isConstructor) {
        return secondClass.classId == firstClass.classId || !first.isConstructor && secondClass.isSubclassOf(firstClass)
    }
    if (firstClass.isSubclassOf(secondClass) || secondClass.isSubclassOf(firstClass)) return true
    if (firstClass.isFinal || secondClass.isFinal) return false
    return (firstClass.isInterface || secondClass.isInterface) && !ignoreInterfaceMethodCollisions
}

private fun ObjCExportClassClaim.isSubclassOf(other: ObjCExportClassClaim): Boolean = other.classId in supertypes

internal class ObjCExportNamePlace(val owner: String, val isExact: Boolean)

internal val ObjCExportNameRecord.place: ObjCExportNamePlace?
    get() = claim?.let { ObjCExportNamePlace(it.owner, isExact = it.isTopLevel) }

internal fun ObjCExportMapper.namePlace(member: CallableMemberDescriptor): ObjCExportNamePlace? =
    if (isTopLevel(member)) {
        ObjCExportNamePlace(claimOwnerKey(member), isExact = true)
    } else {
        claimOwner(member)?.classId?.let { ObjCExportNamePlace(it.toString(), isExact = false) }
    }

internal class ObjCExportNameIndex<E : Any>(
    private val classes: (String) -> ObjCExportClassClaim?,
    private val ignoreInterfaceMethodCollisions: Boolean,
) {
    private val order = HashMap<E, Int>()
    private val all = mutableListOf<E>()
    private val byOwner = HashMap<String, MutableList<E>>()
    private val bySupertype = HashMap<String, MutableList<E>>()
    private val withOpenOwner = mutableListOf<E>()
    private val withInterfaceOwner = mutableListOf<E>()
    private val withUnknownOwner = mutableListOf<E>()

    fun add(entry: E, place: ObjCExportNamePlace?) {
        order[entry] = order.size
        all += entry
        if (place == null) {
            withUnknownOwner += entry
            return
        }
        byOwner.getOrPut(place.owner) { mutableListOf() } += entry
        if (place.isExact) return
        val ownerClass = classes(place.owner)
        if (ownerClass == null) {
            withUnknownOwner += entry
            return
        }
        ownerClass.supertypes.forEach { bySupertype.getOrPut(it) { mutableListOf() } += entry }
        if (!ownerClass.isFinal) withOpenOwner += entry
        if (ownerClass.isInterface) withInterfaceOwner += entry
    }

    fun candidates(place: ObjCExportNamePlace?): List<E> {
        if (place == null) return all
        val ownerClass = if (place.isExact) null else classes(place.owner) ?: return all
        val result = HashSet<E>(withUnknownOwner)
        byOwner[place.owner]?.let(result::addAll)
        if (ownerClass != null) {
            ownerClass.supertypes.forEach { byOwner[it]?.let(result::addAll) }
            bySupertype[place.owner]?.let(result::addAll)
            if (!ownerClass.isFinal && !ignoreInterfaceMethodCollisions) {
                result += if (ownerClass.isInterface) withOpenOwner else withInterfaceOwner
            }
        }
        return if (result.size == all.size) all else result.sortedBy(order::getValue)
    }
}

internal fun classClaim(descriptor: ClassDescriptor): ObjCExportClassClaim = ObjCExportClassClaim(
    descriptor.classId.toString(),
    descriptor.getAllSuperClassifiers().mapNotNull { (it as? ClassDescriptor)?.classId?.toString() }.toSet(),
    descriptor.isFinalClass,
    descriptor.isInterface,
)

internal fun ObjCExportMapper.claimOwner(member: CallableMemberDescriptor): ClassDescriptor? =
    if (isTopLevel(member)) null else getClassIfCategory(member) ?: member.containingDeclaration as? ClassDescriptor

internal fun ObjCExportMapper.memberClaim(method: FunctionDescriptor): ObjCExportMemberClaim = ObjCExportMemberClaim(
    owner = claimOwnerKey(method),
    isTopLevel = isTopLevel(method),
    isConstructor = method is ConstructorDescriptor,
    hasNoDispatchReceiver = method.dispatchReceiverParameter == null,
    isSetter = method is PropertySetterDescriptor,
    kotlinName = method.name.asString(),
    extensionReceiverType = method.extensionReceiverParameter?.type?.let(::renderClaimType).orEmpty(),
    parameterTypes = method.valueParameters.joinToString(",") { renderClaimType(it.type) },
    bridge = bridgeMethod(method).encode(),
)

internal fun ObjCExportMapper.propertyClaim(property: PropertyDescriptor): ObjCExportMemberClaim = ObjCExportMemberClaim(
    owner = claimOwnerKey(property),
    isTopLevel = isTopLevel(property),
    isConstructor = false,
    hasNoDispatchReceiver = property.dispatchReceiverParameter == null,
    isSetter = false,
    kotlinName = property.name.asString(),
    extensionReceiverType = "",
    parameterTypes = "",
    bridge = bridgePropertyType(property).encode(),
)

private fun ObjCExportMapper.claimOwnerKey(member: CallableMemberDescriptor): String =
    claimOwner(member)?.classId?.toString() ?: fileKey(member.propertyIfAccessor.findSourceFile())

private fun renderClaimType(type: KotlinType): String = buildString { appendClaimType(type) }

private fun StringBuilder.appendClaimType(type: KotlinType) {
    when (val unwrapped = type.unwrap()) {
        is FlexibleType -> {
            append('(')
            appendClaimType(unwrapped.lowerBound)
            append("..")
            appendClaimType(unwrapped.upperBound)
            append(')')
            return
        }
        is DefinitelyNotNullType -> {
            appendClaimType(unwrapped.original)
            append("!!")
            return
        }
        else -> {}
    }
    when (val declaration = type.constructor.declarationDescriptor) {
        is TypeParameterDescriptor -> append('#').append(objCExportNameKey(declaration.containingDeclaration)).append(SEPARATOR).append(declaration.index)
        is ClassDescriptor -> append(declaration.classId?.toString() ?: declaration.name.asString())
        else -> append(type.constructor.toString())
    }
    if (type.arguments.isNotEmpty()) {
        append('<')
        type.arguments.forEachIndexed { index, argument ->
            if (index > 0) append(',')
            if (argument.isStarProjection) {
                append('*')
            } else {
                append(argument.projectionKind.label).append(' ')
                appendClaimType(argument.type)
            }
        }
        append('>')
    }
    if (type.isMarkedNullable) append('?')
}

private fun TypeBridge.encode(): String = when (this) {
    ReferenceBridge -> "R"
    is BlockPointerBridge -> "B$numberOfParameters${if (returnsVoid) "v" else "r"}"
    is ValueTypeBridge -> "V${objCValueType.name}"
}

private fun MethodBridge.ReturnValue.encode(): String = when (this) {
    MethodBridge.ReturnValue.Void -> "v"
    MethodBridge.ReturnValue.HashCode -> "h"
    is MethodBridge.ReturnValue.Mapped -> "m${bridge.encode()}"
    MethodBridge.ReturnValue.Instance.InitResult -> "i"
    MethodBridge.ReturnValue.Instance.FactoryResult -> "f"
    MethodBridge.ReturnValue.WithError.Success -> "s"
    is MethodBridge.ReturnValue.WithError.ZeroForError -> "z(${successBridge.encode()},$successMayBeZero)"
    MethodBridge.ReturnValue.Suspend -> "S"
}

private fun MethodBridgeReceiver.encode(): String = when (this) {
    MethodBridgeReceiver.Static -> "s"
    MethodBridgeReceiver.Factory -> "f"
    MethodBridgeReceiver.Instance -> "i"
}

private fun MethodBridgeValueParameter.encode(): String = when (this) {
    is MethodBridgeValueParameter.Mapped -> "m${bridge.encode()}"
    MethodBridgeValueParameter.ErrorOutParameter -> "e"
    is MethodBridgeValueParameter.SuspendCompletion -> "c$useUnitCompletion"
}

private fun MethodBridge.encode(): String =
    "${returnBridge.encode()};${receiver.encode()};${valueParameters.joinToString(",") { it.encode() }}"
