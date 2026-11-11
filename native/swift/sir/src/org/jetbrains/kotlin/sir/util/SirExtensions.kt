/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.util

import org.jetbrains.kotlin.sir.*

val SirCallable.allParameters: List<SirParameter>
    get() = when (this) {
        is SirFunction -> listOfNotNull(this.contextParameter, this.extensionReceiverParameter) + this.parameters
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
        is SirExistentialType -> protocols.takeIf {
            it.isNotEmpty()
        }?.joinToString(prefix = "any ", separator = " & ") { [protocol, typeArguments] ->
            val typeArguments = typeArguments.takeIf { it.isNotEmpty() }
            "${protocol.swiftFqName}${typeArguments?.joinToString(prefix = "<", postfix = ">", separator = ",") { it.swiftName } ?: ""}"
        } ?: "Any"
        is SirNominalType -> listOfNotNull(
            parent?.swiftName?.let { "$it." },
            if (parent != null) typeDeclaration.name.swiftSanitizedName.swiftIdentifier else typeDeclaration.swiftFqName,
            typeArguments.takeIf { it.isNotEmpty() }?.let { it.joinToString(prefix = "<", postfix = ">", separator = ",") { it.swiftName } }
        ).joinToString("")
        is SirErrorType -> "ERROR_TYPE"
        is SirUnsupportedType -> "Swift.Never"
        is SirFunctionalType -> {
            val parameters = buildList {
                contextType?.let(::add)
                addAll(parameterTypes)
            }.joinToString { it.annotatedSwiftName }
            val async = " async".takeIf { isAsync } ?: ""
            val throws = when (errorType) {
                SirType.never -> ""
                SirType.any -> " throws"
                else -> " throws(${errorType.swiftName})"
            }
            val returnType = returnType.swiftName
            "($parameters)$async$throws -> $returnType"
        }
        is SirTupleType -> "(${types.joinToString { [name, type] -> "${name?.let { "$it: " } ?: ""}${type.swiftName}" }})"
        is SirType.Metatype -> when (type) {
            is SirNominalType, is SirType.Metatype, is SirTypeParameterType, is SirSelfType -> type.swiftName
            is SirExistentialType -> type.swiftName.removePrefix("any ").let { if (type.protocols.size == 1) it else "($it)" }
            else -> "(${type.swiftName})"
        }.let { "$it.Type" }
        is SirTypeParameterType -> parameter.name.swiftIdentifier
        is SirSelfType -> memberName?.let { "Self.${it.swiftIdentifier}" } ?: "Self"
    }

val SirType.annotatedSwiftName
    get(): String = (this.attributes.map {
        assert(it.arguments.isNullOrEmpty()) { "Rendering swift attributes with arguments is not supported" }
        "@${it.identifier.swiftIdentifier}${it.arguments?.let { "()" } ?: ""}"
    } + this.swiftName).joinToString(" ")

fun SirAttribute.renderAsSwiftSourceLine(): String {
    val rendered = arguments?.joinToString(prefix = "(", postfix = ")") { arg ->
        val value = when (val expr = arg.expression) {
            is SirExpression.Raw -> expr.raw
            is SirExpression.StringLiteral -> expr.value.swiftStringLiteral
        }
        arg.name?.let { "${it.swiftIdentifier}: $value" } ?: value
    }
    return "@${identifier.swiftIdentifier}${rendered.orEmpty()}"
}

val SirDeclaration.swiftParentNamePrefix: String?
    get() = this.parent.swiftFqNameOrNull

val SirDeclarationParent.swiftFqNameOrNull: String?
    get() = when {
        // Types of a cinterop re-export klib are referenced bare: every ObjC module the klib provides is
        // imported (and a single klib may bundle several), and the klib does not record which module each
        // type originates from. KT-82896 proposes storing the originating Clang module per declaration, which
        // would let us emit precise modular imports / qualifiers instead of relying on bare references.
        this is SirCinteropModule -> null
        else -> (this as? SirScopeDefiningDeclaration)?.swiftFqName
            ?: ((this as? SirScopeDefiningElement)?.name?.swiftSanitizedName)
            ?: ((this as? SirExtension)?.extendedType?.swiftName)
    }

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

private fun SirParameter.substituteTypeParameters(substitution: Map<SirTypeParameter, SirType>): SirParameter =
    if (substitution.isEmpty()) this else SirParameter(
        argumentName = argumentName,
        parameterName = parameterName,
        type = type.substituteTypeParameters(substitution),
        origin = origin,
        isVariadic = isVariadic,
    )

private fun SirFunction.hasAlphaEquivalentSignatureWith(other: SirFunction): Boolean {
    if (this.typeParameters.size != other.typeParameters.size) return false
    if (this.typeParameters.isEmpty()) {
        return this.contextParameter == other.contextParameter
                && this.extensionReceiverParameter == other.extensionReceiverParameter
                && this.errorType == other.errorType
                && this.parameters == other.parameters
    }
    val canonParams = this.typeParameters.indices.map { SirTypeParameter("_C$it") }
    val thisSub = this.typeParameters.mapIndexed { idx, tp -> tp to SirTypeParameterType(canonParams[idx]) }.toMap()
    val otherSub = other.typeParameters.mapIndexed { idx, tp -> tp to SirTypeParameterType(canonParams[idx]) }.toMap()
    val thisNameMap = this.typeParameters.mapIndexed { idx, tp -> tp.name to "_C$idx" }.toMap()
    val otherNameMap = other.typeParameters.mapIndexed { idx, tp -> tp.name to "_C$idx" }.toMap()

    for (i in canonParams.indices) {
        val b1 = this.typeParameters[i].bounds.map { it.substituteTypeParameters(thisSub) }.toSet()
        val b2 = other.typeParameters[i].bounds.map { it.substituteTypeParameters(otherSub) }.toSet()
        if (b1 != b2) return false
    }
    fun normalizeConstraint(c: SirTypeConstraint, sub: Map<SirTypeParameter, SirType>, names: Map<String, String>): SirTypeConstraint {
        val normPath = c.subjectPath.mapIndexed { idx, seg -> if (idx == 0) (names[seg] ?: seg) else seg }
        val normType = c.constraint.substituteTypeParameters(sub)
        return when (c) {
            is SirTypeConstraint.Conformance -> SirTypeConstraint.Conformance(normType, normPath)
            is SirTypeConstraint.Equality -> SirTypeConstraint.Equality(normType, normPath)
        }
    }
    val c1 = this.constraints.map { normalizeConstraint(it, thisSub, thisNameMap) }.toSet()
    val c2 = other.constraints.map { normalizeConstraint(it, otherSub, otherNameMap) }.toSet()
    if (c1 != c2) return false

    return this.contextParameter?.substituteTypeParameters(thisSub) == other.contextParameter?.substituteTypeParameters(otherSub)
            && this.extensionReceiverParameter?.substituteTypeParameters(thisSub) == other.extensionReceiverParameter?.substituteTypeParameters(otherSub)
            && this.errorType.substituteTypeParameters(thisSub) == other.errorType.substituteTypeParameters(otherSub)
            && this.parameters.map { it.substituteTypeParameters(thisSub) } == other.parameters.map { it.substituteTypeParameters(otherSub) }
}

fun SirDeclaration.conflictsWith(other: SirDeclaration): Boolean = when (this) {
    is SirFunction -> when (other) {
        is SirFunction -> this.name == other.name
                && this.isInstance == other.isInstance
                && this.hasAlphaEquivalentSignatureWith(other)
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

val SirDeclaration.isUnavailable: Boolean get() = attributes.any { it is SirAttribute.Available && it.unavailable }

val SirType.unavailableTypes: List<SirType>
    get() = collectUnavailableTypes(mutableSetOf(), mutableSetOf())

private fun SirType.collectUnavailableTypes(
    visitedParams: MutableSet<SirTypeParameter>,
    visitedDecls: MutableSet<SirDeclaration>,
): List<SirType> = when (this) {
    is SirNominalType -> listOfNotNull(this.takeIf { visitedDecls.add(typeDeclaration) && typeDeclaration.isUnavailable }) +
        (parent?.collectUnavailableTypes(visitedParams, visitedDecls).orEmpty()) +
        typeArguments.flatMap { it.collectUnavailableTypes(visitedParams, visitedDecls) }
    is SirExistentialType -> protocols.mapNotNull { [protocol, _] ->
        protocol.takeIf { visitedDecls.add(protocol) && it.isUnavailable }?.let(::SirNominalType)
    } + protocols.flatMap { [_, types] -> types.flatMap { it.collectUnavailableTypes(visitedParams, visitedDecls) } }
    is SirTupleType -> types.flatMap { it.second.collectUnavailableTypes(visitedParams, visitedDecls) }
    is SirFunctionalType -> (contextTypes + parameterTypes + errorType + returnType).flatMap { it.collectUnavailableTypes(visitedParams, visitedDecls) }
    is SirType.Metatype -> type.collectUnavailableTypes(visitedParams, visitedDecls)
    is SirTypeParameterType -> if (visitedParams.add(parameter)) {
        parameter.bounds.flatMap { it.collectUnavailableTypes(visitedParams, visitedDecls) }
    } else {
        emptyList()
    }
    is SirSelfType -> emptyList()
    is SirUnsupportedType -> listOf(this)
    is SirErrorType -> emptyList()
}

fun SirType.containsTypeParameter(): Boolean = when (this) {
    is SirTypeParameterType -> true
    is SirSelfType -> true
    is SirNominalType -> typeArguments.any { it.containsTypeParameter() } || (parent?.containsTypeParameter() == true)
    is SirExistentialType -> protocols.any { pair -> pair.second.any { it.containsTypeParameter() } }
    is SirFunctionalType -> contextTypes.any { it.containsTypeParameter() } ||
        parameterTypes.any { it.containsTypeParameter() } ||
        errorType.containsTypeParameter() ||
        returnType.containsTypeParameter()
    is SirTupleType -> types.any { it.second.containsTypeParameter() }
    is SirType.Metatype -> type.containsTypeParameter()
    is SirErrorType, is SirUnsupportedType -> false
}

fun SirType.substituteTypeParameters(substitution: Map<SirTypeParameter, SirType>): SirType {
    if (substitution.isEmpty()) return this
    return when (this) {
        is SirTypeParameterType -> substitution[parameter] ?: this
        is SirImplicitlyUnwrappedOptionalType -> wrappedType.substituteTypeParameters(substitution).implicitlyUnwrappedOptional()
        is SirOptionalType -> wrappedType.substituteTypeParameters(substitution).optional()
        is SirArrayType -> SirArrayType(elementType.substituteTypeParameters(substitution))
        is SirDictionaryType -> SirDictionaryType(
            keyType.substituteTypeParameters(substitution),
            valueType.substituteTypeParameters(substitution),
        )
        is SirNominalType -> if (typeArguments.isEmpty() && parent == null) {
            this
        } else {
            SirNominalType(
                typeDeclaration = typeDeclaration,
                typeArguments = typeArguments.map { it.substituteTypeParameters(substitution) },
                parent = parent?.substituteTypeParameters(substitution) as? SirNominalType,
                origin = origin,
                attributes = attributes,
            )
        }
        is SirExistentialType -> SirExistentialType(
            protocols = protocols.map { pair ->
                pair.first to pair.second.map { it.substituteTypeParameters(substitution) }
            },
            origin = origin,
        )
        is SirFunctionalType -> SirFunctionalType(
            contextTypes = contextTypes.map { it.substituteTypeParameters(substitution) },
            parameterTypes = parameterTypes.map { it.substituteTypeParameters(substitution) },
            isAsync = isAsync,
            errorType = errorType.substituteTypeParameters(substitution),
            returnType = returnType.substituteTypeParameters(substitution),
            origin = origin,
            attributes = attributes,
        )
        is SirTupleType -> SirTupleType(
            types = types.map { pair -> pair.first to pair.second.substituteTypeParameters(substitution) },
            origin = origin,
            attributes = attributes,
        )
        is SirType.Metatype -> SirType.Metatype(type.substituteTypeParameters(substitution), attributes)
        is SirSelfType, is SirErrorType, is SirUnsupportedType -> this
    }
}

inline fun MutableList<SirAttribute>.replaceOrAddPropagatedUnavailability(unavailableTypes: () -> List<SirType>) {
    if (this.any { it is SirAttribute.Available && it.unavailable }) return
    val unavailableTypes = unavailableTypes()
    if (unavailableTypes.isEmpty()) return
    val message = if (unavailableTypes.any { it is SirUnsupportedType }) {
        "Declaration uses unsupported types"
    } else {
        unavailableTypes.joinToString(prefix = "Unavailable type(s): ") { it.swiftName }
    }
    removeAll { it is SirAttribute.Available }
    add(SirAttribute.Available(message, unavailable = true))
}
