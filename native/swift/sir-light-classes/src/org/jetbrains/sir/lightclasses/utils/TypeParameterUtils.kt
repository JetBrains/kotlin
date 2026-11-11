/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.utils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.scopes.declaredMemberScope
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildGetter
import org.jetbrains.kotlin.sir.builder.buildInit
import org.jetbrains.kotlin.sir.builder.buildVariable
import org.jetbrains.kotlin.sir.providers.*
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.BridgeFunctionBuilder
import org.jetbrains.kotlin.sir.providers.impl.SirTypeProviderImpl
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeSupportModule
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.sir.util.swiftIdentifier
import org.jetbrains.kotlin.types.Variance

import org.jetbrains.sir.lightclasses.nodes.SirFunctionFromKtSymbol

internal data class TranslatedTypeParameters(
    val typeParameters: List<SirTypeParameter>,
    val constraints: List<SirTypeConstraint>,
    val liftedValueParameterTypes: Map<Int, SirType> = emptyMap(),
    val liftedExtensionReceiverType: SirType? = null,
)

private object TypeTranslationAbortException : RuntimeException(null, null, false, false)

context(_: KaSession, sirSession: SirSession)
internal fun KaType.translateTypeOrNull(
    position: SirTypeVariance,
    processTypeImports: (List<SirImport>) -> Unit = {},
    typeParameterScope: Map<KaTypeParameterSymbol, SirTypeParameter> = emptyMap(),
    inGenericTypeArgument: Boolean = false,
): SirType? = try {
    translateType(
        position = position,
        reportErrorType = { throw TypeTranslationAbortException },
        reportUnsupportedType = { throw TypeTranslationAbortException },
        processTypeImports = processTypeImports,
        typeParameterScope = typeParameterScope,
        inGenericTypeArgument = inGenericTypeArgument,
    )
} catch (_: TypeTranslationAbortException) {
    null
}

private val translatingConstraintsGuard = ThreadLocal.withInitial { mutableSetOf<KaDeclarationSymbol>() }

internal fun SirDeclaration.enclosingTypeParameterScope(): Map<KaTypeParameterSymbol, SirTypeParameter> {
    if (this is SirScopeDefiningDeclaration) return emptyMap()
    val genericParent = parent as? SirGenericDeclaration ?: return emptyMap()
    return genericParent.typeParameters.mapNotNull { tp ->
        val sym = (tp.origin as? KotlinSource)?.symbol as? KaTypeParameterSymbol
        sym?.let { it to tp }
    }.toMap()
}

internal fun SirDeclaration.lexicalTypeParameterScope(): Map<KaTypeParameterSymbol, SirTypeParameter> {
    val enclosing = enclosingTypeParameterScope()
    val rawParams = when (this) {
        is SirFunctionFromKtSymbol -> this.rawOwnTypeParameters
        is SirGenericDeclaration -> this.typeParameters
        else -> emptyList()
    }
    val own = rawParams.mapNotNull { tp ->
        val sym = (tp.origin as? KotlinSource)?.symbol as? KaTypeParameterSymbol
        sym?.let { it to tp }
    }.toMap()
    return if (enclosing.isEmpty()) own else enclosing + own
}

context(ka: KaSession, sirSession: SirSession)
internal fun translateTypeParametersAndConstraints(
    declarationSymbol: KaDeclarationSymbol,
    ownKtTypeParams: List<KaTypeParameterSymbol>,
    enclosingScope: Map<KaTypeParameterSymbol, SirTypeParameter> = emptyMap(),
    isProtocol: Boolean = false,
    selfClassSymbol: KaClassSymbol? = null,
    callableSymbol: KaCallableSymbol? = null,
    expandedType: KaType? = null,
    allowInputProjectionLifting: Boolean = false,
): TranslatedTypeParameters {
    // Without generics support no declaration has type parameters in Swift: a Kotlin type parameter is erased
    // to its upper bound where a type mentions it, see `SirTypeProviderImpl`.
    if (!sirSession.enableGenericsSupport) return TranslatedTypeParameters(emptyList(), emptyList())
    val guardSet = translatingConstraintsGuard.get()
    val firstVisit = guardSet.add(declarationSymbol)
    try {
        val effectiveOwnKtTypeParams = ownKtTypeParams.filter { it !in enclosingScope }
        val ownMap = LinkedHashMap<KaTypeParameterSymbol, SirTypeParameter>()
        val sirParams = effectiveOwnKtTypeParams.map { ktParam ->
            val sirVariance = when (ktParam.variance) {
                Variance.OUT_VARIANCE -> SirTypeVariance.COVARIANT
                Variance.IN_VARIANCE -> SirTypeVariance.CONTRAVARIANT
                Variance.INVARIANT -> SirTypeVariance.INVARIANT
            }
            SirTypeParameter(
                name = ktParam.name.asString(),
                variance = sirVariance,
                bounds = mutableListOf(),
                isPrimaryAssociatedType = isProtocol,
                origin = KotlinSource(ktParam),
            ).also { ownMap[ktParam] = it }
        }.toMutableList()

        if (!firstVisit) {
            sirParams.forEach { tp ->
                if (tp.bounds.isEmpty()) tp.bounds.add(SirNominalType(KotlinRuntimeSupportModule.kotlinBridgeable))
            }
            return TranslatedTypeParameters(sirParams, emptyList())
        }

        val localScope = enclosingScope + ownMap
        val constraints = mutableListOf<SirTypeConstraint>()

        // Detect Hashable requirements from Set<K> / Map<K, V> key positions.
        val hashableRequiredParams = mutableSetOf<KaTypeParameterSymbol>()
        if (effectiveOwnKtTypeParams.isNotEmpty()) {
            callableSymbol?.let { sym ->
                sym.contextParameters.forEach { it.returnType.collectHashableKeyTypeParameters(hashableRequiredParams) }
                sym.receiverParameter?.returnType?.collectHashableKeyTypeParameters(hashableRequiredParams)
                (sym as? KaFunctionSymbol)?.valueParameters?.forEach {
                    it.returnType.collectHashableKeyTypeParameters(hashableRequiredParams)
                }
                sym.returnType.collectHashableKeyTypeParameters(hashableRequiredParams)
            }
            expandedType?.collectHashableKeyTypeParameters(hashableRequiredParams)
            if (selfClassSymbol is KaNamedClassSymbol) {
                selfClassSymbol.superTypes.forEach { it.collectHashableKeyTypeParameters(hashableRequiredParams) }
                selfClassSymbol.declaredMemberScope.callables.forEach { member ->
                    if (member.sirAvailability() is SirAvailability.Available) {
                        member.receiverParameter?.returnType?.collectHashableKeyTypeParameters(hashableRequiredParams)
                        (member as? KaFunctionSymbol)?.valueParameters?.forEach {
                            it.returnType.collectHashableKeyTypeParameters(hashableRequiredParams)
                        }
                        member.returnType.collectHashableKeyTypeParameters(hashableRequiredParams)
                    }
                }
                selfClassSymbol.declaredMemberScope.constructors.forEach { ctor ->
                    if (ctor.sirAvailability() is SirAvailability.Available) {
                        ctor.valueParameters.forEach {
                            it.returnType.collectHashableKeyTypeParameters(hashableRequiredParams)
                        }
                    }
                }
            }
        }

        val inheritedProtocolAssocNames: Set<String> = if (isProtocol && selfClassSymbol != null) {
            collectInheritedProtocolAssocNames(selfClassSymbol)
        } else {
            emptySet()
        }

        for ([ktParam, sirParam] in ownMap) {
            val unwrappedBounds = mutableListOf<KaType>()
            fun unwrap(ub: KaType, visited: MutableSet<KaTypeParameterSymbol>) {
                if (ub is KaTypeParameterType) {
                    val sym = ub.symbol
                    if (visited.add(sym) && !ub.isMarkedNullable) {
                        sym.upperBounds.forEach { unwrap(it, visited) }
                    }
                } else {
                    unwrappedBounds.add(ub)
                }
            }
            ktParam.upperBounds.forEach { unwrap(it, mutableSetOf(ktParam)) }

            var hasClassBound = false
            val boundAssocTypes = mutableMapOf<String, SirType>()
            val tpSubjectName = sirParam.name.swiftIdentifier

            for (ub in unwrappedBounds) {
                if (ub.isMarkedNullable) continue
                val classType = ub.fullyExpandedType as? KaUsualClassType ?: continue
                if (classType.classId in SirTypeProviderImpl.STDLIB_RELAXED_BOUND_CLASS_IDS) continue
                if (classType.classId == StandardClassIds.Any) continue

                val ubSymbol = classType.symbol as? KaClassSymbol ?: continue
                if (ubSymbol.sirAvailability() !is SirAvailability.Available) continue

                if (isProtocol && selfClassSymbol != null && ubSymbol == selfClassSymbol) {
                    // Direct self-interface bound on the protocol itself relaxes to _KotlinBridgeable.
                    continue
                }

                if (ubSymbol.classKind == KaClassKind.INTERFACE) {
                    val proto = ubSymbol.toSir().primaryDeclaration as? SirProtocol ?: continue
                    val protoConstraintMap = LinkedHashMap<String, SirType>()
                    var conflict = false
                    if (proto.typeParameters.isNotEmpty() && classType.typeArguments.size == proto.typeParameters.size) {
                        for (idx in proto.typeParameters.indices) {
                            val protoTp = proto.typeParameters[idx]
                            val proj = classType.typeArguments[idx]
                            if (proj is KaTypeArgumentWithVariance) {
                                val translatedArg = proj.type.translateTypeOrNull(
                                    position = SirTypeVariance.INVARIANT,
                                    typeParameterScope = localScope,
                                    inGenericTypeArgument = true,
                                )
                                if (translatedArg != null && translatedArg !is SirErrorType && translatedArg !is SirUnsupportedType) {
                                    val assocKey = protoTp.name.swiftIdentifier
                                    val existing = boundAssocTypes[assocKey]
                                    if (existing != null && existing != translatedArg) {
                                        conflict = true
                                        break
                                    }
                                    protoConstraintMap[assocKey] = translatedArg
                                }
                            }
                        }
                    }
                    if (conflict) continue
                    val nominalProto = SirNominalType(proto)
                    if (nominalProto !in sirParam.bounds) {
                        sirParam.bounds.add(nominalProto)
                    }
                    for ([assocKey, translatedArg] in protoConstraintMap) {
                        if (assocKey !in boundAssocTypes) {
                            boundAssocTypes[assocKey] = translatedArg
                            constraints.add(
                                SirTypeConstraint.Equality(
                                    translatedArg,
                                    listOf(tpSubjectName, assocKey),
                                )
                            )
                        }
                    }
                } else if (ubSymbol.classKind == KaClassKind.CLASS && !hasClassBound) {
                    // Relax direct self-recursive class F-bounds (`SelfReferencing<T : SelfReferencing<T>>`).
                    if (classType.containsKtTypeParameterSymbol(ktParam)) continue
                    val translatedClassBound = classType.translateTypeOrNull(
                        position = SirTypeVariance.INVARIANT,
                        typeParameterScope = localScope,
                    )
                    if (translatedClassBound is SirNominalType && translatedClassBound.typeDeclaration is SirClass) {
                        hasClassBound = true
                        sirParam.bounds.add(0, translatedClassBound)
                    }
                }
            }

            if (sirParam.bounds.isEmpty()) {
                sirParam.bounds.add(SirNominalType(KotlinRuntimeSupportModule.kotlinBridgeable))
            }

            if (ktParam in hashableRequiredParams && !hasClassBound) {
                val hashableType = SirNominalType(SirSwiftModule.hashable)
                if (hashableType !in sirParam.bounds) {
                    sirParam.bounds.add(hashableType)
                }
            }

            // If this associatedtype is suppressed in protocol body because an ancestor protocol already declares it,
            // lift any non-trivial bounds onto the protocol's `where` clause (`where Self.T: Foo`).
            if (isProtocol && sirParam.name in inheritedProtocolAssocNames) {
                for (bound in sirParam.bounds) {
                    val decl = (bound as? SirNominalType)?.typeDeclaration
                    if (decl != KotlinRuntimeSupportModule.kotlinBridgeable) {
                        constraints.add(SirTypeConstraint.Conformance(bound, listOf("Self", tpSubjectName)))
                    }
                }
            }
        }

        // For interfaces, emit `where Self.Assoc == ConcreteOrOtherParam` when specializing super-protocols.
        if (isProtocol && selfClassSymbol != null) {
            val selfBoundAssoc = mutableMapOf<String, SirType>()
            for (supType in selfClassSymbol.superTypes) {
                val supClassType = supType.fullyExpandedType as? KaUsualClassType ?: continue
                val supSym = supClassType.symbol as? KaClassSymbol ?: continue
                if (supSym.classKind != KaClassKind.INTERFACE) continue
                if (supSym.sirAvailability() !is SirAvailability.Available) continue
                val supProto = supSym.toSir().primaryDeclaration as? SirProtocol ?: continue
                if (supProto.typeParameters.isNotEmpty() && supClassType.typeArguments.size == supProto.typeParameters.size) {
                    for (idx in supProto.typeParameters.indices) {
                        val assocName = supProto.typeParameters[idx].name.swiftIdentifier
                        val proj = supClassType.typeArguments[idx]
                        if (proj is KaTypeArgumentWithVariance) {
                            val sirArg = proj.type.translateTypeOrNull(
                                position = SirTypeVariance.INVARIANT,
                                typeParameterScope = localScope,
                                inGenericTypeArgument = true,
                            ) ?: continue
                            if (sirArg is SirErrorType || sirArg is SirUnsupportedType) continue
                            if (sirArg is SirTypeParameterType && sirArg.parameter.name.swiftIdentifier == assocName) {
                                continue
                            }
                            if (assocName !in selfBoundAssoc) {
                                selfBoundAssoc[assocName] = sirArg
                                constraints.add(SirTypeConstraint.Equality(sirArg, listOf("Self", assocName)))
                            }
                        }
                    }
                }
            }
        }

        // Lift input use-site projections (`Box<*>`, `Box<out Bound>`) on non-overriding final functions.
        val liftedValueParams = mutableMapOf<Int, SirType>()
        var liftedExtReceiver: SirType? = null
        if (allowInputProjectionLifting && callableSymbol is KaFunctionSymbol) {
            val usedNames = (localScope.values.map { it.name }).toMutableSet()
            var nextSynthIndex = 0
            fun freshSynthParam(bounds: List<SirType>): SirTypeParameter {
                var candidate: String
                do {
                    candidate = "_T$nextSynthIndex"
                    nextSynthIndex++
                } while (!usedNames.add(candidate))
                return SirTypeParameter(
                    name = candidate,
                    bounds = bounds.toMutableList(),
                ).also { sirParams.add(it) }
            }

            callableSymbol.receiverParameter?.returnType?.let { recvType ->
                liftInputProjectedClassType(recvType, localScope, ::freshSynthParam)?.let {
                    liftedExtReceiver = it
                }
            }
            callableSymbol.valueParameters.forEachIndexed { index, paramSym ->
                if (!paramSym.isVararg) {
                    liftInputProjectedClassType(paramSym.returnType, localScope, ::freshSynthParam)?.let {
                        liftedValueParams[index] = it
                    }
                }
            }
        }

        return TranslatedTypeParameters(
            typeParameters = sirParams,
            constraints = constraints,
            liftedValueParameterTypes = liftedValueParams,
            liftedExtensionReceiverType = liftedExtReceiver,
        )
    } finally {
        if (firstVisit) {
            guardSet.remove(declarationSymbol)
        }
    }
}

context(ka: KaSession, sirSession: SirSession)
private fun liftInputProjectedClassType(
    ktType: KaType,
    scope: Map<KaTypeParameterSymbol, SirTypeParameter>,
    freshParam: (List<SirType>) -> SirTypeParameter,
): SirType? {
    val expanded = ktType.fullyExpandedType as? KaUsualClassType ?: return null
    val sym = expanded.symbol as? KaClassSymbol ?: return null
    if (sym.classKind != KaClassKind.CLASS) return null
    if (sym.sirAvailability() !is SirAvailability.Available) return null
    if (expanded.typeArguments.isEmpty()) return null
    val hasLiftableProjection = expanded.typeArguments.any { proj ->
        proj is KaStarTypeProjection || (proj is KaTypeArgumentWithVariance && proj.variance == Variance.OUT_VARIANCE)
    }
    if (!hasLiftableProjection) return null

    val sirClass = sym.toSir().primaryDeclaration as? SirClass ?: return null
    if (sirClass.typeParameters.size != expanded.typeArguments.size) return null

    val newTypeArgs = mutableListOf<SirType>()
    var liftedAny = false
    for (idx in expanded.typeArguments.indices) {
        val proj = expanded.typeArguments[idx]
        val declSirTp = sirClass.typeParameters[idx]
        when {
            proj is KaStarTypeProjection -> {
                val copiedBounds = declSirTp.bounds.ifEmpty {
                    listOf(SirNominalType(KotlinRuntimeSupportModule.kotlinBridgeable))
                }
                // Only lift when bounds don't reference the class's other type parameters.
                val safeBounds = copiedBounds.filter { b -> !declSirTp.bounds.any { it is SirTypeParameterType } }
                    .ifEmpty { listOf(SirNominalType(KotlinRuntimeSupportModule.kotlinBridgeable)) }
                val synth = freshParam(safeBounds)
                newTypeArgs.add(SirTypeParameterType(synth))
                liftedAny = true
            }
            proj is KaTypeArgumentWithVariance && proj.variance == Variance.OUT_VARIANCE && !proj.type.isMarkedNullable -> {
                val boundSir = proj.type.translateTypeOrNull(
                    position = SirTypeVariance.INVARIANT,
                    typeParameterScope = scope,
                )
                val boundDecl = (boundSir as? SirNominalType)?.typeDeclaration
                if (boundSir != null && (boundDecl is SirClass || boundDecl is SirProtocol)) {
                    val synth = freshParam(listOf(boundSir))
                    newTypeArgs.add(SirTypeParameterType(synth))
                    liftedAny = true
                } else {
                    val argSir = proj.type.translateTypeOrNull(
                        position = SirTypeVariance.INVARIANT,
                        typeParameterScope = scope,
                        inGenericTypeArgument = true,
                    ) ?: return null
                    if (argSir is SirErrorType || argSir is SirUnsupportedType) return null
                    newTypeArgs.add(argSir)
                }
            }
            proj is KaTypeArgumentWithVariance -> {
                val argSir = proj.type.translateTypeOrNull(
                    position = SirTypeVariance.INVARIANT,
                    typeParameterScope = scope,
                    inGenericTypeArgument = true,
                ) ?: return null
                if (argSir is SirErrorType || argSir is SirUnsupportedType) return null
                newTypeArgs.add(argSir)
            }
            else -> return null
        }
    }
    if (!liftedAny) return null
    val baseNominal = SirNominalType(sirClass, newTypeArgs)
    return if (expanded.isMarkedNullable) SirOptionalType(baseNominal) else baseNominal
}

context(ka: KaSession, sirSession: SirSession)
private fun collectInheritedProtocolAssocNames(classSymbol: KaClassSymbol): Set<String> {
    val result = mutableSetOf<String>()
    val visited = mutableSetOf<KaClassSymbol>()
    fun dfs(sym: KaClassSymbol) {
        for (sup in sym.superTypes) {
            val supSym = (sup.fullyExpandedType as? KaClassType)?.symbol as? KaClassSymbol ?: continue
            if (supSym.classKind == KaClassKind.INTERFACE && visited.add(supSym)) {
                if (supSym.sirAvailability() is SirAvailability.Available) {
                    supSym.typeParameters.forEach { result.add(it.name.asString()) }
                }
                dfs(supSym)
            }
        }
    }
    dfs(classSymbol)
    return result
}

context(ka: KaSession)
private fun KaType.collectHashableKeyTypeParameters(
    out: MutableSet<KaTypeParameterSymbol>,
    visited: MutableSet<KaType> = mutableSetOf(),
) {
    if (!visited.add(this)) return
    val expanded = fullyExpandedType
    when (expanded) {
        is KaClassType -> {
            val cid = expanded.classId
            if (cid == StandardClassIds.Set || cid == MUTABLE_SET_CLASS_ID) {
                (expanded.typeArguments.firstOrNull() as? KaTypeArgumentWithVariance)?.type?.collectConditionalHashableParams(out)
            } else if (cid == StandardClassIds.Map || cid == MUTABLE_MAP_CLASS_ID) {
                (expanded.typeArguments.firstOrNull() as? KaTypeArgumentWithVariance)?.type?.collectConditionalHashableParams(out)
            }
            expanded.typeArguments.forEach { proj ->
                (proj as? KaTypeArgumentWithVariance)?.type?.collectHashableKeyTypeParameters(out, visited)
            }
        }
        is KaFunctionType -> {
            expanded.contextParameterTypes.forEach { it.collectHashableKeyTypeParameters(out, visited) }
            expanded.receiverType?.collectHashableKeyTypeParameters(out, visited)
            expanded.parameterTypes.forEach { it.collectHashableKeyTypeParameters(out, visited) }
            expanded.returnType.collectHashableKeyTypeParameters(out, visited)
        }
        else -> {}
    }
}

context(ka: KaSession)
private fun KaType.collectConditionalHashableParams(out: MutableSet<KaTypeParameterSymbol>) {
    val expanded = fullyExpandedType
    when (expanded) {
        is KaTypeParameterType -> out.add(expanded.symbol)
        is KaClassType -> {
            if (expanded.classId in CONDITIONAL_HASHABLE_CONTAINER_CLASS_IDS) {
                expanded.typeArguments.forEach { proj ->
                    (proj as? KaTypeArgumentWithVariance)?.type?.collectConditionalHashableParams(out)
                }
            }
        }
        else -> {}
    }
}

private val MUTABLE_SET_CLASS_ID = ClassId.topLevel(FqName("kotlin.collections.MutableSet"))
private val MUTABLE_MAP_CLASS_ID = ClassId.topLevel(FqName("kotlin.collections.MutableMap"))
private val CONDITIONAL_HASHABLE_CONTAINER_CLASS_IDS = setOf(
    StandardClassIds.List,
    ClassId.topLevel(FqName("kotlin.collections.MutableList")),
    StandardClassIds.Collection,
    ClassId.topLevel(FqName("kotlin.collections.MutableCollection")),
    StandardClassIds.Iterable,
    ClassId.topLevel(FqName("kotlin.collections.MutableIterable")),
    StandardClassIds.Set,
    MUTABLE_SET_CLASS_ID,
    StandardClassIds.Map,
    MUTABLE_MAP_CLASS_ID,
)

internal fun KaType.containsKtTypeParameterSymbol(
    target: KaTypeParameterSymbol,
    visited: MutableSet<KaTypeParameterSymbol> = mutableSetOf(),
): Boolean = when (this) {
    is KaTypeParameterType -> symbol == target || (visited.add(symbol) && symbol.upperBounds.any { it.containsKtTypeParameterSymbol(target, visited) })
    is KaClassType -> typeArguments.any { (it as? KaTypeArgumentWithVariance)?.type?.containsKtTypeParameterSymbol(target, visited) == true }
    is KaFunctionType -> contextParameterTypes.any { it.containsKtTypeParameterSymbol(target, visited) } ||
            (receiverType?.containsKtTypeParameterSymbol(target, visited) == true) ||
            parameterTypes.any { it.containsKtTypeParameterSymbol(target, visited) } ||
            returnType.containsKtTypeParameterSymbol(target, visited)
    else -> false
}

internal fun KaType.containsAnyKtTypeParameter(
    visited: MutableSet<KaTypeParameterSymbol> = mutableSetOf(),
): Boolean = when (this) {
    is KaTypeParameterType -> true
    is KaClassType -> typeArguments.any { (it as? KaTypeArgumentWithVariance)?.type?.containsAnyKtTypeParameter(visited) == true }
    is KaFunctionType -> contextParameterTypes.any { it.containsAnyKtTypeParameter(visited) } ||
            (receiverType?.containsAnyKtTypeParameter(visited) == true) ||
            parameterTypes.any { it.containsAnyKtTypeParameter(visited) } ||
            returnType.containsAnyKtTypeParameter(visited)
    else -> false
}

internal fun KaType.containsStarProjection(
    visited: MutableSet<KaTypeParameterSymbol> = mutableSetOf(),
): Boolean = when (this) {
    is KaTypeParameterType -> visited.add(symbol) && symbol.upperBounds.any { it.containsStarProjection(visited) }
    is KaClassType -> typeArguments.any {
        it is KaStarTypeProjection || ((it as? KaTypeArgumentWithVariance)?.type?.containsStarProjection(visited) == true)
    }
    is KaFunctionType -> contextParameterTypes.any { it.containsStarProjection(visited) } ||
            (receiverType?.containsStarProjection(visited) == true) ||
            parameterTypes.any { it.containsStarProjection(visited) } ||
            returnType.containsStarProjection(visited)
    else -> false
}

/**
 * Whether the Kotlin bridge of a member has to call it through a local generic function (see [buildLocalGenericBridgeCall]):
 * its type parameters have bounds that a cast to the erased type can't satisfy.
 *
 * Only with generics support: without it such members are not exported, or are called through the erased type as before.
 */
internal fun hasNonTrivialKotlinBridgeBounds(
    sirSession: SirSession,
    enclosingClassSymbol: KaClassSymbol?,
    callableTypeParams: List<KaTypeParameterSymbol>,
): Boolean {
    if (!sirSession.enableGenericsSupport) return false
    val classParams = enclosingClassSymbol?.typeParameters.orEmpty()
    if (classParams.any { it.variance == Variance.IN_VARIANCE }) return true
    val allParams = classParams + callableTypeParams
    return allParams.any { tp ->
        tp.upperBounds.size > 1 || tp.upperBounds.any { it.containsAnyKtTypeParameter() || it.containsStarProjection() }
    }
}

context(ka: KaSession)
internal fun renderKtTypeWithSubstitutedParams(
    ktType: KaType,
    substMap: Map<KaTypeParameterSymbol, String>,
): String {
    val expanded = ktType.fullyExpandedType
    if (expanded is KaFlexibleType) {
        return renderKtTypeWithSubstitutedParams(expanded.upperBound, substMap)
    }
    if (expanded is KaTypeParameterType) {
        val mapped = substMap[expanded.symbol]
        if (mapped != null) {
            return if (expanded.isMarkedNullable) "$mapped?" else mapped
        }
    }
    if (expanded is KaUsualClassType) {
        val fqName = expanded.symbol.classId?.asFqNameString()
        if (fqName != null) {
            val argsStr = if (expanded.typeArguments.isEmpty()) {
                ""
            } else {
                expanded.typeArguments.joinToString(prefix = "<", postfix = ">") { proj ->
                    when (proj) {
                        is KaStarTypeProjection -> "*"
                        is KaTypeArgumentWithVariance -> {
                            val vPrefix = when (proj.variance) {
                                Variance.OUT_VARIANCE -> "out "
                                Variance.IN_VARIANCE -> "in "
                                Variance.INVARIANT -> ""
                            }
                            "$vPrefix${renderKtTypeWithSubstitutedParams(proj.type, substMap)}"
                        }
                    }
                }
            }
            return if (expanded.isMarkedNullable) "$fqName$argsStr?" else "$fqName$argsStr"
        }
    }
    if (expanded is KaFunctionType) {
        val allParamTypes = buildList {
            addAll(expanded.contextParameterTypes)
            expanded.receiverType?.let(::add)
            addAll(expanded.parameterTypes)
        }
        val prefix = if (expanded.isSuspendFunctionType) "kotlin.coroutines.SuspendFunction" else "kotlin.Function"
        val renderedTypes = (allParamTypes + expanded.returnType).joinToString { renderKtTypeWithSubstitutedParams(it, substMap) }
        val fnStr = "$prefix${allParamTypes.size}<$renderedTypes>"
        return if (expanded.isMarkedNullable) "($fnStr)?" else fnStr
    }
    return if (expanded.isMarkedNullable) "kotlin.Any?" else "kotlin.Any"
}

private data class LocalHelperHeader(
    val typeParamsDecl: String,
    val whereClause: String,
    val nothingTypeArgs: String,
    val substMap: Map<KaTypeParameterSymbol, String>,
    val classTypeArgsStr: String,
    val callableTypeArgsStr: String,
)

context(ka: KaSession)
private fun buildLocalHelperHeader(
    enclosingClassSymbol: KaClassSymbol?,
    callableTypeParams: List<KaTypeParameterSymbol>,
): LocalHelperHeader {
    val classParams = enclosingClassSymbol?.typeParameters.orEmpty()
    val allParams = classParams + callableTypeParams
    val substMap = LinkedHashMap<KaTypeParameterSymbol, String>()
    allParams.forEachIndexed { index, sym -> substMap[sym] = "__T$index" }

    val whereParts = mutableListOf<String>()
    val declParts = allParams.map { sym ->
        val name = substMap.getValue(sym)
        val ubs = sym.upperBounds
        when {
            ubs.isEmpty() -> name
            ubs.size == 1 -> "$name : ${renderKtTypeWithSubstitutedParams(ubs.single(), substMap)}"
            else -> {
                ubs.forEach { ub ->
                    whereParts.add("$name : ${renderKtTypeWithSubstitutedParams(ub, substMap)}")
                }
                name
            }
        }
    }
    val whereClause = whereParts.takeIf { it.isNotEmpty() }?.joinToString(prefix = " where ") ?: ""
    val nothingTypeArgs = allParams.joinToString { "Nothing" }
    val classTypeArgsStr = classParams.takeIf { it.isNotEmpty() }
        ?.joinToString(prefix = "<", postfix = ">") { substMap.getValue(it) } ?: ""
    val callableTypeArgsStr = callableTypeParams.takeIf { it.isNotEmpty() }
        ?.joinToString(prefix = "<", postfix = ">") { substMap.getValue(it) } ?: ""

    return LocalHelperHeader(
        typeParamsDecl = declParts.joinToString(),
        whereClause = whereClause,
        nothingTypeArgs = nothingTypeArgs,
        substMap = substMap,
        classTypeArgsStr = classTypeArgsStr,
        callableTypeArgsStr = callableTypeArgsStr,
    )
}

context(ka: KaSession, sirSession: SirSession)
internal fun BridgeFunctionBuilder.buildLocalGenericBridgeCall(
    enclosingClassSymbol: KaClassSymbol?,
    callableSymbol: KaCallableSymbol,
    sirReturnType: SirType,
    isSetter: Boolean = false,
    setterParamExpr: String? = null,
): String {
    val callableTypeParams = callableSymbol.typeParameters
    val header = buildLocalHelperHeader(enclosingClassSymbol, callableTypeParams)
    val substMap = header.substMap

    val selfCastType = if (selfParameter != null && enclosingClassSymbol != null) {
        val classFq = enclosingClassSymbol.classId?.asFqNameString()
        classFq?.let { "$it${header.classTypeArgsStr}" }
    } else {
        null
    }

    val innerExpr = if (isSetter) {
        val propTypeStr = renderKtTypeWithSubstitutedParams(callableSymbol.returnType, substMap)
        val rhs = "(${setterParamExpr ?: argNames.last()} as $propTypeStr)"
        val extRecv = (callableSymbol as? KaPropertySymbol)?.receiverParameter
        val baseSet = if (extRecv != null && extensionReceiverParameter != null) {
            val recvExpr = argNames[contextParameters.size]
            val recvTypeStr = renderKtTypeWithSubstitutedParams(extRecv.returnType, substMap)
            val memberName = kotlinFqName.shortName().asString()
            if (selfCastType != null) {
                "(__self as $selfCastType).run { ($recvExpr as $recvTypeStr).`$memberName` = $rhs }"
            } else if (!kotlinFqName.parent().isRoot) {
                "${buildCall("")} = $rhs"
            } else {
                "($recvExpr as $recvTypeStr).`$memberName` = $rhs"
            }
        } else {
            val memberCall = buildCall("", selfCastType = selfCastType)
            "$memberCall = $rhs"
        }
        if (contextParameters.isNotEmpty() && callableSymbol.contextParameters.size == contextParameters.size) {
            val castedCtx = callableSymbol.contextParameters.mapIndexed { idx, ctxSym ->
                val cType = renderKtTypeWithSubstitutedParams(ctxSym.returnType, substMap)
                "(${argNames[idx]} as $cType)"
            }.joinToString()
            val stripped = baseSet.removePrefix("context(${argNames.take(contextParameters.size).joinToString()}) { ").removeSuffix(" }")
            "context($castedCtx) { $stripped }"
        } else {
            baseSet
        }
    } else if (callableSymbol is KaPropertySymbol) {
        // Property getter (regular or extension)
        val extRecv = callableSymbol.receiverParameter
        val baseGet = if (extRecv != null && extensionReceiverParameter != null) {
            val recvExpr = argNames[contextParameters.size]
            val recvTypeStr = renderKtTypeWithSubstitutedParams(extRecv.returnType, substMap)
            val memberName = kotlinFqName.shortName().asString()
            if (selfCastType != null) {
                "(__self as $selfCastType).run { ($recvExpr as $recvTypeStr).`$memberName` }"
            } else if (!kotlinFqName.parent().isRoot) {
                buildCall("")
            } else {
                "($recvExpr as $recvTypeStr).`$memberName`"
            }
        } else {
            buildCall("", selfCastType = selfCastType)
        }
        if (contextParameters.isNotEmpty() && callableSymbol.contextParameters.size == contextParameters.size) {
            val castedCtx = callableSymbol.contextParameters.mapIndexed { idx, ctxSym ->
                val cType = renderKtTypeWithSubstitutedParams(ctxSym.returnType, substMap)
                "(${argNames[idx]} as $cType)"
            }.joinToString()
            val stripped = baseGet.removePrefix("context(${argNames.take(contextParameters.size).joinToString()}) { ").removeSuffix(" }")
            "context($castedCtx) { $stripped }"
        } else {
            baseGet
        }
    } else {
        val fnSymbol = callableSymbol as KaFunctionSymbol
        val dropCount = contextParameters.size + if (extensionReceiverParameter != null) 1 else 0
        val rawValueArgs = argNames.drop(dropCount)
        val castedValueArgs = rawValueArgs.mapIndexed { idx, argExpr ->
            val paramSym = fnSymbol.valueParameters.getOrNull(idx)
            if (paramSym != null && !paramSym.isVararg && !argExpr.startsWith("*") && !argExpr.contains(" = ")) {
                val pType = renderKtTypeWithSubstitutedParams(paramSym.returnType, substMap)
                "($argExpr as $pType)"
            } else {
                argExpr
            }
        }
        val argsSuffix = "${header.callableTypeArgsStr}(${castedValueArgs.joinToString()})"
        val extRecv = fnSymbol.receiverParameter
        val baseCall = if (extRecv != null && extensionReceiverParameter != null) {
            val recvTypeStr = renderKtTypeWithSubstitutedParams(extRecv.returnType, substMap)
            val recvExpr = argNames[contextParameters.size]
            val fnShort = kotlinFqName.shortName().asString()
            if (selfCastType != null) {
                "(__self as $selfCastType).run { ($recvExpr as $recvTypeStr).`$fnShort`$argsSuffix }"
            } else if (!kotlinFqName.parent().isRoot) {
                buildCall(argsSuffix).replaceFirst("$recvExpr.", "($recvExpr as $recvTypeStr).")
            } else {
                "($recvExpr as $recvTypeStr).`$fnShort`$argsSuffix"
            }
        } else {
            buildCall(argsSuffix, selfCastType = selfCastType)
        }
        if (contextParameters.isNotEmpty() && fnSymbol.contextParameters.size == contextParameters.size) {
            val castedCtx = fnSymbol.contextParameters.mapIndexed { idx, ctxSym ->
                val cType = renderKtTypeWithSubstitutedParams(ctxSym.returnType, substMap)
                "(${argNames[idx]} as $cType)"
            }.joinToString()
            val stripped = baseCall.removePrefix("context(${argNames.take(contextParameters.size).joinToString()}) { ").removeSuffix(" }")
            "context($castedCtx) { $stripped }"
        } else {
            baseCall
        }
    }

    val suspendMod = if (isAsync) "suspend " else ""
    if (isSetter || (sirReturnType == SirType.void && !isAsync)) {
        return "run {\n    @Suppress(\"UNCHECKED_CAST\") ${suspendMod}fun <${header.typeParamsDecl}> __bridge()${header.whereClause} { $innerExpr }\n    __bridge<${header.nothingTypeArgs}>()\n}"
    }
    val expectedKtReturn = typeNamer.kotlinFqName(sirReturnType, SirTypeNamer.KotlinNameType.PARAMETRIZED)
    if (expectedKtReturn == "Unit") {
        return "run {\n    @Suppress(\"UNCHECKED_CAST\") ${suspendMod}fun <${header.typeParamsDecl}> __bridge(): Unit${header.whereClause} { $innerExpr }\n    __bridge<${header.nothingTypeArgs}>()\n}"
    }
    return "run {\n    @Suppress(\"UNCHECKED_CAST\") ${suspendMod}fun <${header.typeParamsDecl}> __bridge(): $expectedKtReturn${header.whereClause} = ($innerExpr as kotlin.Any?) as $expectedKtReturn\n    __bridge<${header.nothingTypeArgs}>()\n}"
}

context(ka: KaSession)
internal fun BridgeFunctionBuilder.buildLocalGenericInitBridgeCall(
    enclosingClassSymbol: KaClassSymbol,
    ctorSymbol: KaConstructorSymbol,
): String {
    val header = buildLocalHelperHeader(enclosingClassSymbol, emptyList())
    val substMap = header.substMap
    val classFq = enclosingClassSymbol.classId?.asFqNameString() ?: error("Missing classId for $enclosingClassSymbol")
    val rawArgs = argNames.drop(1)
    val castedArgs = rawArgs.mapIndexed { idx, argExpr ->
        val paramSym = ctorSymbol.valueParameters.getOrNull(idx)
        if (paramSym != null && !paramSym.isVararg && !argExpr.startsWith("*") && !argExpr.contains(" = ")) {
            val pType = renderKtTypeWithSubstitutedParams(paramSym.returnType, substMap)
            "($argExpr as $pType)"
        } else {
            argExpr
        }
    }
    val ctorCall = "$classFq${header.classTypeArgsStr}(${castedArgs.joinToString()})"
    return "run {\n    @Suppress(\"UNCHECKED_CAST\") fun <${header.typeParamsDecl}> __init()${header.whereClause} { kotlin.native.internal.initInstance(${argNames.first()}, $ctorCall) }\n    __init<${header.nothingTypeArgs}>()\n}"
}

context(ka: KaSession)
internal fun BridgeFunctionBuilder.buildLocalGenericAllocBridgeCall(
    enclosingClassSymbol: KaNamedClassSymbol,
): String {
    val header = buildLocalHelperHeader(enclosingClassSymbol, emptyList())
    val classFq = enclosingClassSymbol.classId?.asFqNameString() ?: error("Missing classId for $enclosingClassSymbol")
    val inlineSuffix = if (enclosingClassSymbol.isInline) " as kotlin.Any?" else ""
    val allocCall = "kotlin.native.internal.createUninitializedInstance<$classFq${header.classTypeArgsStr}>()$inlineSuffix"
    return "run {\n    @Suppress(\"UNCHECKED_CAST\") fun <${header.typeParamsDecl}> __alloc(): kotlin.Any?${header.whereClause} = $allocCall\n    __alloc<${header.nothingTypeArgs}>()\n}"
}

internal fun buildCovariantProperty(
    parentDecl: SirDeclarationParent,
    typeParam: SirTypeParameter,
    ownerExpr: String = "self",
    isInlinable: Boolean = true,
): SirVariable = buildVariable {
    origin = SirOrigin.ExternallyDefined("variance")
    visibility = SirVisibility.PUBLIC
    name = "covariant"
    type = SirNominalType(KotlinRuntimeSupportModule.kotlinCovariant, listOf(SirTypeParameterType(typeParam)))
    if (isInlinable) attributes += SirAttribute.Inlinable
    getter = buildGetter {
        origin = SirOrigin.ExternallyDefined("variance")
        body = SirFunctionBody(
            listOf("return (_owner: $ownerExpr, _variance: KotlinRuntimeSupport._absurd)")
        )
    }
}.also {
    it.parent = parentDecl
    it.getter?.parent = it
}

internal fun buildContravariantProperty(
    parentDecl: SirDeclarationParent,
    typeParam: SirTypeParameter,
    ownerExpr: String = "self",
    isInlinable: Boolean = true,
): SirVariable = buildVariable {
    origin = SirOrigin.ExternallyDefined("variance")
    visibility = SirVisibility.PUBLIC
    name = "contravariant"
    type = SirNominalType(KotlinRuntimeSupportModule.kotlinContravariant, listOf(SirTypeParameterType(typeParam)))
    if (isInlinable) attributes += SirAttribute.Inlinable
    getter = buildGetter {
        origin = SirOrigin.ExternallyDefined("variance")
        body = SirFunctionBody(
            listOf("return (_owner: $ownerExpr, _variance: KotlinRuntimeSupport._noop)")
        )
    }
}.also {
    it.parent = parentDecl
    it.getter?.parent = it
}

internal fun buildCovariantConvenienceInit(
    parentClass: SirClass,
    typeParam: SirTypeParameter,
    isInlinable: Boolean = true,
): SirInit = buildInit {
    origin = SirOrigin.ExternallyDefined("variance")
    visibility = SirVisibility.PUBLIC
    isConvenience = true
    isFailable = false
    isOverride = false
    if (isInlinable) attributes += SirAttribute.Inlinable
    parameters += SirParameter(
        argumentName = null,
        parameterName = "projection",
        type = SirNominalType(KotlinRuntimeSupportModule.kotlinCovariant, listOf(SirTypeParameterType(typeParam))),
    )
    body = SirFunctionBody(
        listOf("self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)")
    )
}.also { it.parent = parentClass }

internal fun buildContravariantConvenienceInit(
    parentClass: SirClass,
    typeParam: SirTypeParameter,
    isInlinable: Boolean = true,
): SirInit = buildInit {
    origin = SirOrigin.ExternallyDefined("variance")
    visibility = SirVisibility.PUBLIC
    isConvenience = true
    isFailable = false
    isOverride = false
    if (isInlinable) attributes += SirAttribute.Inlinable
    parameters += SirParameter(
        argumentName = null,
        parameterName = "projection",
        type = SirNominalType(KotlinRuntimeSupportModule.kotlinContravariant, listOf(SirTypeParameterType(typeParam))),
    )
    body = SirFunctionBody(
        listOf("self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)")
    )
}.also { it.parent = parentClass }
