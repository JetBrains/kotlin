/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.util

import org.jetbrains.kotlin.sir.*

val SirCallable.allParameters: List<SirParameter>
    get() = when (this) {
        is SirFunction -> listOfNotNull(this.extensionReceiverParameter) + this.parameters
        is SirInit -> this.parameters
        is SirSetter -> listOf(SirParameter(parameterName = parameterName, type = this.valueType))
        is SirGetter -> listOf()
    }

val SirCallable.returnType: SirType
    get() = when (this) {
        is SirFunction -> this.returnType
        is SirGetter -> this.valueType
        is SirSetter, is SirInit -> SirNominalType(SirSwiftModule.void)
    }

val SirAccessor.valueType: SirType
    get() = this.parent.let {
        when (it) {
            is SirVariable -> it.type
            else -> error("Invalid accessor parent $parent")
        }
    }

val SirVariable.accessors: List<SirAccessor>
    get() = listOfNotNull(
        getter,
        setter,
    )

val SirSubscript.accessors: List<SirAccessor>
    get() = listOfNotNull(
        getter,
        setter,
    )

val SirEnum.cases: List<SirEnumCase>
    get() = declarations.filterIsInstance<SirEnumCase>()

val SirParameter.name: String? get() = parameterName ?: argumentName

val SirType.isVoid: Boolean get() = this is SirNominalType && this.typeDeclaration == SirSwiftModule.void
val SirType.isNever: Boolean get() = this is SirNominalType && this.typeDeclaration == SirSwiftModule.never

fun <T : SirDeclaration> SirMutableDeclarationContainer.addChild(producer: () -> T): T {
    val child = producer()
    child.parent = this
    if (!declarations.contains(child)) {
        declarations += child
    }
    return child
}

val SirType.swiftName
    get(): String = when (this) {
        is SirExistentialType -> protocols.takeIf { it.isNotEmpty() }?.joinToString(prefix = "any ", separator = " & ") { it.swiftFqName }
            ?: "Any"
        is SirNominalType -> listOfNotNull(
            parent?.swiftName?.let { "$it." },
            typeDeclaration.swiftFqName,
            typeArguments.takeIf { it.isNotEmpty() }?.let { it.joinToString(prefix = "<", postfix = ">", separator = ",") { it.swiftName } }
        ).joinToString("")
        is SirErrorType -> "ERROR_TYPE"
        is SirUnsupportedType -> "Swift.Never"
        is SirFunctionalType -> "(${parameterTypes.joinToString { it.annotatedSwiftName }})${" async".takeIf { isAsync } ?: ""} -> ${returnType.swiftName}"
        is SirTupleType -> "(${types.joinToString { (name, type) -> "${name?.let { "$it: " } ?: ""}${type.swiftName}" }})"
    }

val SirType.annotatedSwiftName
    get(): String = (this.attributes.map {
        assert(it.arguments.isNullOrEmpty()) { "Rendering swift attributes with arguments is not supported" }
        "@${it.identifier.swiftIdentifier}${it.arguments?.let { "()" } ?: ""}"
    } + this.swiftName).joinToString(" ")

val SirDeclaration.swiftParentNamePrefix: String?
    get() = this.parent.swiftFqNameOrNull

val SirDeclarationParent.swiftFqNameOrNull: String?
    get() = (this as? SirScopeDefiningDeclaration)?.swiftFqName
        ?: ((this as? SirScopeDefiningElement)?.name?.swiftSanitizedName)
        ?: ((this as? SirExtension)?.extendedType?.swiftName)

val SirScopeDefiningDeclaration.swiftFqName: String
    get() = swiftParentNamePrefix?.let { "$it.${name.swiftSanitizedName.swiftIdentifier}" } ?: name.swiftSanitizedName.swiftIdentifier

val SirNominalType.isValueType: Boolean
    get() = when (typeDeclaration) {
        is SirEnum -> true
        is SirStruct -> true
        is SirProtocol -> false
        is SirClass -> false
        is SirTypealias -> (typeDeclaration.expandedType as? SirNominalType)?.isValueType == true
    }

val SirFunction.swiftFqName: String
    get() = swiftParentNamePrefix?.let { "$it.${name.swiftSanitizedName}" } ?: name.swiftSanitizedName

val SirVariable.swiftFqName: String
    get() = swiftParentNamePrefix?.let { "$it.${name.swiftSanitizedName}" } ?: name.swiftSanitizedName

val SirTypealias.expandedType: SirType
    get() = ((type as? SirNominalType)?.typeDeclaration as? SirTypealias)?.expandedType ?: type


private val SirFunction.isConfusable: Boolean get() = this.parameters.isEmpty() && this.extensionReceiverParameter == null

fun SirDeclaration.conflictsWith(other: SirDeclaration): Boolean = when (this) {
    is SirFunction -> when (other) {
        is SirFunction -> this.name == other.name
                && this.isInstance == other.isInstance
                && this.extensionReceiverParameter == other.extensionReceiverParameter
                && this.errorType == other.errorType
                && this.parameters == other.parameters
        is SirVariable -> this.name == other.name && this.isInstance == other.isInstance && this.isConfusable
        is SirScopeDefiningDeclaration -> this.name == other.name && this.isInstance.not()
        else -> false
    }
    is SirVariable -> when (other) {
        is SirFunction -> this.name == other.name && this.isInstance == other.isInstance && other.isConfusable
        is SirVariable -> this.name == other.name && this.isInstance == other.isInstance
        is SirScopeDefiningDeclaration -> this.name == other.name && this.isInstance.not()
        else -> false
    }
    is SirScopeDefiningDeclaration -> when (other) {
        is SirFunction -> this.name == other.name && other.isInstance.not()
        is SirVariable -> this.name == other.name && other.isInstance.not()
        is SirScopeDefiningDeclaration -> this.name == other.name
        else -> false
    }
    else -> false
}
