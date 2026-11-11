/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.scopes.combinedDeclaredMemberScope
import org.jetbrains.kotlin.analysis.api.scopes.combinedMemberScope
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildFunction
import org.jetbrains.kotlin.sir.builder.buildFunctionCopy
import org.jetbrains.kotlin.sir.builder.buildGetter
import org.jetbrains.kotlin.sir.builder.buildGetterCopy
import org.jetbrains.kotlin.sir.builder.buildInitCopy
import org.jetbrains.kotlin.sir.builder.buildSetter
import org.jetbrains.kotlin.sir.builder.buildSetterCopy
import org.jetbrains.kotlin.sir.builder.buildTypealias
import org.jetbrains.kotlin.sir.builder.buildVariable
import org.jetbrains.kotlin.sir.builder.buildVariableCopy
import org.jetbrains.kotlin.sir.providers.*
import org.jetbrains.kotlin.sir.providers.source.KotlinImplementationMarkerProtocol
import org.jetbrains.kotlin.sir.providers.source.KotlinMarkerProtocol
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.source.kaSymbolOrNull
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeSupportModule
import org.jetbrains.kotlin.sir.providers.utils.allRequiredOptIns
import org.jetbrains.kotlin.sir.providers.utils.throwsAnnotation
import org.jetbrains.kotlin.sir.providers.utils.updateImportFor
import org.jetbrains.kotlin.sir.providers.utils.updateImports
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.sir.util.isUnavailable
import org.jetbrains.kotlin.sir.util.replaceOrAddPropagatedUnavailability
import org.jetbrains.kotlin.sir.util.substituteTypeParameters
import org.jetbrains.kotlin.sir.util.swiftFqName
import org.jetbrains.kotlin.sir.util.swiftIdentifier
import org.jetbrains.kotlin.sir.util.unavailableTypes
import org.jetbrains.kotlin.utils.filterIsInstanceAnd
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.lazyWithSessions
import org.jetbrains.sir.lightclasses.extensions.withSessions
import org.jetbrains.sir.lightclasses.utils.*

internal fun createSirClassFromKtSymbol(
    ktSymbol: KaNamedClassSymbol,
    sirSession: SirSession,
): SirAbstractClassFromKtSymbol = SirClassFromKtSymbol(
    ktSymbol,
    sirSession
)

private class SirClassFromKtSymbol(
    ktSymbol: KaNamedClassSymbol,
    sirSession: SirSession,
) : SirAbstractClassFromKtSymbol(
    ktSymbol,
    sirSession
)

internal class SirStubClassFromKtSymbol(
    ktSymbol: KaNamedClassSymbol,
    sirSession: SirSession,
) : SirAbstractClassFromKtSymbol(
    ktSymbol,
    sirSession
) {
    override val declarations: List<SirDeclaration> = emptyList()
}

internal abstract class SirAbstractClassFromKtSymbol(
    override val ktSymbol: KaNamedClassSymbol,
    override val sirSession: SirSession,
) : SirClass(), SirFromKtSymbol<KaNamedClassSymbol> {

    override val origin: KotlinSource by lazy {
        KotlinSource(ktSymbol)
    }
    override val visibility: SirVisibility by lazy {
        SirVisibility.PUBLIC
    }
    override val modality: SirModality by lazy {
        when (ktSymbol.modality) {
            KaSymbolModality.OPEN -> SirModality.OPEN
            KaSymbolModality.FINAL -> SirModality.FINAL
            // In Swift, superclass of open class must be open.
            // Since Kotlin abstract or sealed class can be a superclass of Kotlin open class,
            // `open` modality should be used in Swift.
            KaSymbolModality.SEALED, KaSymbolModality.ABSTRACT -> SirModality.OPEN
        }
    }

    val kdocElements: KDocElements? by lazyWithSessions {
        KDocElements(this)
    }

    override val documentation: String? by lazyWithSessions {
        translateDocumentation(kdocElements)
    }

    override val name: String by lazyWithSessions {
        (this@SirAbstractClassFromKtSymbol.relocatedDeclarationNamePrefix() ?: "") + ktSymbol.sirDeclarationName()
    }

    override var parent: SirDeclarationParent
        get() = withSessions {
            ktSymbol.getSirParent()
        }
        set(_) = Unit

    private val translatedTypeParams: TranslatedTypeParameters by lazyWithSessions {
        translateTypeParametersAndConstraints(
            declarationSymbol = ktSymbol,
            ownKtTypeParams = ktSymbol.typeParameters,
            selfClassSymbol = ktSymbol,
        )
    }

    override val typeParameters: List<SirTypeParameter>
        get() = translatedTypeParams.typeParameters

    override val constraints: List<SirTypeConstraint>
        get() = translatedTypeParams.constraints

    internal val rawSuperClass: SirNominalType by lazyWithSessions {
        val supKtType = ktSymbol.superTypes.filterIsInstanceAnd<KaClassType> {
            it.isRegularClass && it.classId != KaStandardTypeClassIds.ANY
        }.firstOrNull()
        if (supKtType != null) {
            val supSirClass = supKtType.symbol.toSir().primaryDeclaration as? SirClass
            if (supSirClass != null) {
                ktSymbol.containingModule.sirModule().updateImportFor(supSirClass)
                val typeArgs = if (supSirClass.typeParameters.isNotEmpty() &&
                    supKtType is KaUsualClassType &&
                    supKtType.typeArguments.size == supSirClass.typeParameters.size
                ) {
                    val scope = lexicalTypeParameterScope()
                    fun KaType.referencesSelfSymbol(visited: MutableSet<KaTypeParameterSymbol> = HashSet()): Boolean = when (this) {
                        is KaClassType -> expandedSymbol == ktSymbol ||
                            typeArguments.any { (it as? KaTypeArgumentWithVariance)?.type?.referencesSelfSymbol(visited) == true }
                        is KaTypeParameterType -> visited.add(symbol) && symbol.upperBounds.any { it.referencesSelfSymbol(visited) }
                        is KaFunctionType -> contextParameterTypes.any { it.referencesSelfSymbol(visited) } ||
                            (receiverType?.referencesSelfSymbol(visited) == true) ||
                            parameterTypes.any { it.referencesSelfSymbol(visited) } ||
                            returnType.referencesSelfSymbol(visited)
                        else -> false
                    }
                    supKtType.typeArguments.mapIndexed { idx, proj ->
                        val supTp = supSirClass.typeParameters[idx]
                        val argKtType = (proj as? KaTypeArgumentWithVariance)?.type
                        if (argKtType == null || argKtType.referencesSelfSymbol()) {
                            relaxSuperTypeArgument(supTp)
                        } else {
                            val translated = argKtType.translateTypeOrNull(
                                position = SirTypeVariance.INVARIANT,
                                processTypeImports = ktSymbol.containingModule.sirModule()::updateImports,
                                typeParameterScope = scope,
                                inGenericTypeArgument = true,
                            )
                            if (translated == null || translated is SirErrorType || translated is SirUnsupportedType ||
                                translated.referencesDeclaration(this@SirAbstractClassFromKtSymbol)
                            ) {
                                relaxSuperTypeArgument(supTp)
                            } else {
                                translated
                            }
                        }
                    }
                } else {
                    emptyList()
                }
                return@lazyWithSessions SirNominalType(supSirClass, typeArgs)
            }
        }
        SirNominalType(KotlinRuntimeModule.kotlinBase)
    }

    internal val openGenericBaseClass: SirClass? by lazyWithSessions {
        if (typeParameters.isNotEmpty() && modality != SirModality.FINAL && rawSuperClass.typeArguments.isEmpty()) {
            SirOpenGenericBaseClass(this@SirAbstractClassFromKtSymbol)
        } else {
            null
        }
    }

    internal val openGenericReverseDispatchProtocol: SirProtocol? by lazyWithSessions {
        if (typeParameters.isNotEmpty() && modality == SirModality.OPEN && !isUnavailable) {
            SirOpenGenericReverseDispatchProtocol(this@SirAbstractClassFromKtSymbol)
        } else {
            null
        }
    }

    override val superClass: SirNominalType? by lazyWithSessions {
        openGenericBaseClass?.let(::SirNominalType) ?: rawSuperClass
    }

    override val declarations: List<SirDeclaration> by lazyWithSessions {
        inPlaceChildDeclarations +
            relocatedForwardingTypeAliases +
            patTypeAliases +
            intersectionOverrideDeclarations +
            syntheticDeclarations() +
            varianceDeclarations() +
            sealedTypeFunctions
    }

    @Volatile
    private var computingSuperUnavailability: Boolean = false

    override val attributes: List<SirAttribute> by lazy {
        buildList {
            addAll(this@SirAbstractClassFromKtSymbol.translatedAttributes)
            addDocumentationVisibility(kdocElements)
            replaceOrAddPropagatedUnavailability {
                if (computingSuperUnavailability) {
                    emptyList()
                } else {
                    computingSuperUnavailability = true
                    try {
                        rawSuperClass.unavailableTypes
                    } finally {
                        computingSuperUnavailability = false
                    }
                }
            }
        }
    }

    private val rawChildDeclarations: List<SirDeclaration> by lazyWithSessions {
        ktSymbol.combinedDeclaredMemberScope
            .extractDeclarations()
            .toList()
    }

    internal val inPlaceChildDeclarations: List<SirDeclaration> by lazyWithSessions {
        rawChildDeclarations.filter {
            it.parent == this@SirAbstractClassFromKtSymbol &&
                !(it is SirSubscriptTrampoline && it.getterFunction.isOverride)
        }
    }

    internal val relocatedNestedDeclarations: List<SirDeclaration> by lazyWithSessions {
        rawChildDeclarations.filter { it.parent != this@SirAbstractClassFromKtSymbol }
    }

    private val relocatedForwardingTypeAliases: List<SirDeclaration> by lazyWithSessions {
        relocatedNestedDeclarations
            .filterIsInstance<SirScopeDefiningDeclaration>()
            .filter { it.visibility == SirVisibility.PUBLIC && it.origin.javaClass == KotlinSource::class.java }
            .filter {
                val origParent = it.kaSymbolOrNull<KaDeclarationSymbol>()?.let { sym ->
                    with(sirSession) { sym.getOriginalSirParent() }
                }
                origParent == this@SirAbstractClassFromKtSymbol
            }
            .map { declaration ->
                val declTypeParams = (declaration as? SirGenericDeclaration)?.typeParameters.orEmpty()
                val tpSubst = LinkedHashMap<SirTypeParameter, SirType>()
                val copiedTps = declTypeParams.map { tp ->
                    SirTypeParameter(
                        name = tp.name,
                        variance = tp.variance,
                        bounds = mutableListOf(),
                        isPrimaryAssociatedType = false,
                        origin = tp.origin,
                    ).also { tpSubst[tp] = SirTypeParameterType(it) }
                }
                declTypeParams.zip(copiedTps).forEach { [origTp, copyTp] ->
                    copyTp.bounds.addAll(origTp.bounds.map { it.substituteTypeParameters(tpSubst) })
                }
                buildTypealias {
                    origin = SirOrigin.Trampoline(declaration)
                    visibility = SirVisibility.PUBLIC
                    name = declaration.kaSymbolOrNull<KaDeclarationSymbol>()?.sirDeclarationName() ?: declaration.name
                    typeParameters += copiedTps
                    if (declaration is SirConstrainedDeclaration) {
                        constraints += declaration.constraints.map { it.substituteConstraint(tpSubst) }
                    }
                    type = SirNominalType(declaration, copiedTps.map(::SirTypeParameterType))
                    attributes += declaration.attributes.filter { it is SirAttribute.Available || it is SirAttribute.SPI }
                }.also { it.parent = this@SirAbstractClassFromKtSymbol }
            }
    }

    internal val intersectionOverrideDeclarations: List<SirDeclaration> by lazyWithSessions {
        if (ktSymbol.modality != KaSymbolModality.ABSTRACT) return@lazyWithSessions emptyList()
        ktSymbol.combinedMemberScope.declarations
            .filterIsInstance<KaCallableSymbol>()
            .filter { it.origin == KaSymbolOrigin.INTERSECTION_OVERRIDE }
            .filter { it.intersectionOverriddenSymbols.all { it.modality == KaSymbolModality.ABSTRACT } }
            .extractDeclarations()
            .toList()
    }

    private fun kotlinBaseInitDeclaration(): SirDeclaration = buildInitCopy(KotlinRuntimeModule.kotlinBaseDesignatedInit) {
        origin = SirOrigin.KotlinBaseInitOverride(`for` = KotlinSource(ktSymbol))
        if (this@SirAbstractClassFromKtSymbol.typeParameters.isNotEmpty()) {
            visibility = SirVisibility.PUBLIC
            attributes += SirAttribute.Documentation(SirVisibility.INTERNAL)
        } else {
            visibility = SirVisibility.PACKAGE // Hide from users, but not from other Swift Export modules.
        }
        isOverride = true
        body = SirFunctionBody(listOf(
                "super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);"
            ))
    }.also { it.parent = this }

    private fun syntheticDeclarations(): List<SirDeclaration> = when (ktSymbol.classKind) {
        KaClassKind.OBJECT, KaClassKind.COMPANION_OBJECT -> listOf(
            kotlinBaseInitDeclaration(),
            SirObjectSyntheticInit(ktSymbol, sirSession),
            SirObjectAccessorVariableFromKtSymbol(ktSymbol, sirSession)
        ).onEach { it.parent = this }

        else -> listOf(
            kotlinBaseInitDeclaration()
        )
    }

    private fun varianceDeclarations(): List<SirDeclaration> {
        if (typeParameters.size != 1 || isUnavailable) return emptyList()
        val tp = typeParameters.single()
        val canInline = attributes.none { it is SirAttribute.SPI || it is SirAttribute.Available }
        val superAlreadyHasSameVarianceProp = generateSequence(rawSuperClass.typeDeclaration as? SirAbstractClassFromKtSymbol) {
            it.rawSuperClass.typeDeclaration as? SirAbstractClassFromKtSymbol
        }.any { !it.isUnavailable && it.typeParameters.singleOrNull()?.variance == tp.variance }
        return when (tp.variance) {
            SirTypeVariance.COVARIANT -> buildList {
                if (!superAlreadyHasSameVarianceProp) {
                    add(buildCovariantProperty(this@SirAbstractClassFromKtSymbol, tp, isInlinable = canInline))
                }
                add(buildCovariantConvenienceInit(this@SirAbstractClassFromKtSymbol, tp, isInlinable = canInline))
            }
            SirTypeVariance.CONTRAVARIANT -> buildList {
                if (!superAlreadyHasSameVarianceProp) {
                    add(buildContravariantProperty(this@SirAbstractClassFromKtSymbol, tp, isInlinable = canInline))
                }
                add(buildContravariantConvenienceInit(this@SirAbstractClassFromKtSymbol, tp, isInlinable = canInline))
            }
            SirTypeVariance.INVARIANT -> emptyList()
        }
    }

    private data class ComputedProtocolConformances(
        val protocols: List<SirProtocol>,
        val effectiveBoundAssocTypes: Map<String, SirType>,
        val ownNewAssocTypes: Map<String, SirType>,
    )

    private val computedProtocolConformances: ComputedProtocolConformances by lazyWithSessions {
        val isUnavailable = this.isUnavailable
        val errorConformance = SirSwiftModule.error.takeIf { ktSymbol.classId == StandardClassIds.Throwable }

        val superSirClass = rawSuperClass.typeDeclaration as? SirAbstractClassFromKtSymbol
        val inheritedAssocTypes: Map<String, SirType> = if (superSirClass != null) {
            val subMap = this.substitutionMapToAncestor(superSirClass)
            superSirClass.effectiveBoundAssocTypes.mapValues { [_, t] -> t.substituteTypeParameters(subMap) }
        } else {
            emptyMap()
        }

        val effectiveAssoc = LinkedHashMap<String, SirType>(inheritedAssocTypes)
        val ownNewAssoc = LinkedHashMap<String, SirType>()
        val ownParamMap = typeParameters.associateBy { it.name }
        ownParamMap.forEach { [tpName, tp] ->
            if (tpName !in effectiveAssoc) {
                effectiveAssoc[tpName] = SirTypeParameterType(tp)
            }
        }

        val scope = lexicalTypeParameterScope()
        val acceptedKotlinProtocols = mutableListOf<SirProtocolFromKtSymbol>()

        for (supType in ktSymbol.superTypes.filterIsInstance<KaClassType>()) {
            val supSym = supType.expandedSymbol ?: continue
            if (supSym.classKind != KaClassKind.INTERFACE) continue
            val avail = supSym.sirAvailability()
            if (avail !is SirAvailability.Available || avail.visibility <= SirVisibility.INTERNAL) continue
            val proto = supSym.toSir().primaryDeclaration as? SirProtocolFromKtSymbol ?: continue
            if (!isUnavailable && proto.isUnavailable) continue
            if (superClassDeclaration?.declaresConformance(proto) == true) continue

            if (proto.isPatProtocol) {
                val usType = supType.fullyExpandedType as? KaUsualClassType ?: continue
                if (usType.typeArguments.size != proto.typeParameters.size) continue
                val translatedArgs = mutableListOf<SirType>()
                var invalidArg = false
                for (proj in usType.typeArguments) {
                    if (proj !is KaTypeArgumentWithVariance) {
                        invalidArg = true
                        break
                    }
                    val tArg = proj.type.translateTypeOrNull(
                        position = SirTypeVariance.INVARIANT,
                        processTypeImports = ktSymbol.containingModule.sirModule()::updateImports,
                        typeParameterScope = scope,
                        inGenericTypeArgument = true,
                    )
                    if (tArg == null || tArg is SirErrorType || tArg is SirUnsupportedType) {
                        invalidArg = true
                        break
                    }
                    translatedArgs.add(tArg)
                }
                if (invalidArg) continue

                val bindings = proto.computeAssocBindingsForSubtypes(translatedArgs) ?: continue
                val conflicts = bindings.any { [assocKey, boundType] ->
                    val existing = effectiveAssoc[assocKey]
                    existing != null && existing != boundType
                }
                if (conflicts) continue

                for ([assocKey, boundType] in bindings) {
                    if (assocKey !in effectiveAssoc) {
                        effectiveAssoc[assocKey] = boundType
                        ownNewAssoc[assocKey] = boundType
                    } else if (assocKey !in inheritedAssocTypes && assocKey !in ownParamMap) {
                        ownNewAssoc[assocKey] = boundType
                    }
                }
            }

            acceptedKotlinProtocols.add(proto)
        }

        acceptedKotlinProtocols.forEach { ktSymbol.containingModule.sirModule().updateImportFor(it) }

        val hasGenericAncestor = generateSequence(rawSuperClass.typeDeclaration as? SirAbstractClassFromKtSymbol) {
            it.rawSuperClass.typeDeclaration as? SirAbstractClassFromKtSymbol
        }.any { it.typeParameters.isNotEmpty() }
        val genericBridgeableProtocols = buildList {
            if (typeParameters.isNotEmpty()) {
                if (!hasGenericAncestor) {
                    if (modality == SirModality.FINAL) {
                        add(KotlinRuntimeSupportModule.kotlinCustomPointerBridgeable)
                    } else {
                        add(KotlinRuntimeSupportModule.kotlinOpenGenericBridgeable)
                    }
                }
                openGenericReverseDispatchProtocol?.let(::add)
            }
        }

        val allProtos = listOfNotNull(errorConformance) +
            genericBridgeableProtocols +
            acceptedKotlinProtocols.flatMap { listOf(it, it.implementationMarker) }

        ComputedProtocolConformances(
            protocols = allProtos,
            effectiveBoundAssocTypes = effectiveAssoc,
            ownNewAssocTypes = ownNewAssoc,
        )
    }

    internal val effectiveBoundAssocTypes: Map<String, SirType>
        get() = computedProtocolConformances.effectiveBoundAssocTypes

    private val patTypeAliases: List<SirDeclaration> by lazyWithSessions {
        val ownParamNames = typeParameters.mapTo(mutableSetOf()) { it.name }
        computedProtocolConformances.ownNewAssocTypes
            .filterKeys { it !in ownParamNames }
            .map { [assocName, assocType] ->
                buildTypealias {
                    origin = SirOrigin.ExternallyDefined("patTypealias")
                    visibility = SirVisibility.PUBLIC
                    name = assocName
                    type = assocType
                }.also { it.parent = this@SirAbstractClassFromKtSymbol }
            }
    }

    override val protocols: List<SirProtocol>
        get() = computedProtocolConformances.protocols

    internal val sealedType: SirScopeDefiningDeclaration? by lazyWithSessions {
        createSirSealedType(this)
    }

    private val sealedTypeFunctions: List<SirDeclaration> by lazyWithSessions {
        createSirSealedTypeFunctions(this).onEach { it.parent = this }
    }

    override val bridges: List<SirBridge> by lazyWithSessions {
        if (typeParameters.isNotEmpty()) return@lazyWithSessions emptyList()
        listOfNotNull(
            sirSession.generateTypeBridge(
                ktSymbol.classId?.asSingleFqName(),
                kotlinOptIns = ktSymbol.allRequiredOptIns,
                swiftFqName = swiftFqName,
                swiftSymbolName = objcClassSymbolName,
            )
        )
    }

    private fun relaxSuperTypeArgument(supTp: SirTypeParameter): SirType {
        val singleBound = supTp.bounds.singleOrNull()
        val decl = (singleBound as? SirNominalType)?.typeDeclaration
        return when {
            decl is SirClass && !singleBound.referencesDeclaration(this) -> SirNominalType(decl)
            decl is SirProtocolFromKtSymbol -> {
                decl.anyWrapperDeclaration?.takeIf { it.typeParameters.isEmpty() }?.let(::SirNominalType)
                    ?: SirNominalType(
                        KotlinRuntimeSupportModule.kotlinExistential,
                        listOf(SirExistentialType(decl.existentialMarker)),
                    )
            }
            else -> SirNominalType(KotlinRuntimeModule.kotlinBase)
        }
    }
}

private fun SirType.referencesDeclaration(target: SirDeclaration): Boolean = when (this) {
    is SirNominalType -> typeDeclaration == target || typeArguments.any { it.referencesDeclaration(target) }
    is SirOptionalType -> wrappedType.referencesDeclaration(target)
    is SirArrayType -> elementType.referencesDeclaration(target)
    is SirDictionaryType -> keyType.referencesDeclaration(target) || valueType.referencesDeclaration(target)
    is SirExistentialType -> protocols.any { [proto, args] -> proto == target || args.any { it.referencesDeclaration(target) } }
    is SirFunctionalType -> contextTypes.any { it.referencesDeclaration(target) } ||
        parameterTypes.any { it.referencesDeclaration(target) } ||
        returnType.referencesDeclaration(target)
    is SirTupleType -> types.any { it.second.referencesDeclaration(target) }
    else -> false
}

internal fun SirTypeConstraint.substituteConstraint(subst: Map<SirTypeParameter, SirType>): SirTypeConstraint = when (this) {
    is SirTypeConstraint.Conformance -> SirTypeConstraint.Conformance(constraint.substituteTypeParameters(subst), subjectPath)
    is SirTypeConstraint.Equality -> SirTypeConstraint.Equality(constraint.substituteTypeParameters(subst), subjectPath)
}

private class SirOpenGenericBaseClass(
    val target: SirAbstractClassFromKtSymbol,
) : SirClass(), SirFromKtSymbol<KaNamedClassSymbol> {
    override val ktSymbol: KaNamedClassSymbol get() = target.ktSymbol
    override val sirSession: SirSession get() = target.sirSession

    override val origin: SirOrigin get() = SirOrigin.ExternallyDefined("openGenericBase")
    override val visibility: SirVisibility = SirVisibility.PUBLIC
    override val modality: SirModality = SirModality.OPEN
    override val documentation: String? = null
    override val name: String get() = "_${target.name}_Base"

    override var parent: SirDeclarationParent
        get() = target.parent
        set(_) = Unit

    override val superClass: SirNominalType get() = target.rawSuperClass
    override val protocols: List<SirProtocol> = emptyList()
    override val typeParameters: List<SirTypeParameter> = emptyList()
    override val constraints: List<SirTypeConstraint> = emptyList()

    override val attributes: List<SirAttribute> by lazy {
        buildList {
            addAll(target.attributes.filter { it is SirAttribute.Available || it is SirAttribute.SPI })
            add(SirAttribute.Documentation(SirVisibility.INTERNAL))
        }
    }

    override val declarations: List<SirDeclaration> by lazy {
        listOf(
            buildInitCopy(KotlinRuntimeModule.kotlinBaseDesignatedInit) {
                origin = SirOrigin.KotlinBaseInitOverride(`for` = KotlinSource(ktSymbol))
                visibility = SirVisibility.PUBLIC
                isOverride = true
                attributes += SirAttribute.Documentation(SirVisibility.INTERNAL)
                body = SirFunctionBody(
                    listOf("super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);")
                )
            }.also { it.parent = this@SirOpenGenericBaseClass }
        )
    }

    override val bridges: List<SirBridge> by lazyWithSessions {
        listOfNotNull(
            sirSession.generateTypeBridge(
                ktSymbol.classId?.asSingleFqName(),
                kotlinOptIns = ktSymbol.allRequiredOptIns,
                swiftFqName = swiftFqName,
                swiftSymbolName = objcClassSymbolName,
            )
        )
    }
}

private class SirOpenGenericReverseDispatchProtocol(
    val target: SirAbstractClassFromKtSymbol,
) : SirProtocol() {
    override val origin: SirOrigin get() = SirOrigin.ExternallyDefined("openGenericReverseDispatch")
    override val visibility: SirVisibility = SirVisibility.PUBLIC
    override val documentation: String? = null
    override val name: String get() = "_${target.name}_ReverseDispatch"

    override var parent: SirDeclarationParent
        get() = target.parent
        set(_) = Unit

    override val superClass: SirNominalType = SirNominalType(KotlinRuntimeModule.kotlinBase)
    override val protocols: List<SirProtocol> = emptyList()
    override val bridges: List<SirBridge> = emptyList()

    private val copiedHeader: Pair<List<SirTypeParameter>, Map<SirTypeParameter, SirType>> by lazy {
        val subst = LinkedHashMap<SirTypeParameter, SirType>()
        val params = target.typeParameters.map { clsTp ->
            SirTypeParameter(
                name = clsTp.name,
                variance = clsTp.variance,
                bounds = mutableListOf(),
                isPrimaryAssociatedType = true,
                origin = clsTp.origin,
            ).also { subst[clsTp] = SirTypeParameterType(it) }
        }
        target.typeParameters.zip(params).forEach { [clsTp, protoTp] ->
            protoTp.bounds.addAll(clsTp.bounds.map { it.substituteTypeParameters(subst) })
        }
        params to subst
    }

    override val typeParameters: List<SirTypeParameter>
        get() = copiedHeader.first

    override val constraints: List<SirTypeConstraint> by lazy {
        val subst = copiedHeader.second
        target.constraints.map { it.substituteConstraint(subst) }
    }

    override val attributes: List<SirAttribute> by lazy {
        buildList {
            addAll(target.attributes.filter { it is SirAttribute.Available || it is SirAttribute.SPI })
            add(SirAttribute.Documentation(SirVisibility.INTERNAL))
        }
    }

    override val declarations: MutableList<SirDeclaration> by lazy {
        val baseSubst = copiedHeader.second
        val members = (target.inPlaceChildDeclarations + target.intersectionOverrideDeclarations)
            .filter { (it as? SirClassMemberDeclaration)?.needsReverseBridge() == true }

        members.mapNotNull { member ->
            when (member) {
                is SirFunction -> {
                    val fnSubst = LinkedHashMap<SirTypeParameter, SirType>(baseSubst)
                    val copiedFnTps = member.typeParameters.map { fnTp ->
                        SirTypeParameter(
                            name = fnTp.name,
                            variance = fnTp.variance,
                            bounds = mutableListOf(),
                            isPrimaryAssociatedType = false,
                            origin = fnTp.origin,
                        ).also { fnSubst[fnTp] = SirTypeParameterType(it) }
                    }
                    member.typeParameters.zip(copiedFnTps).forEach { [fnTp, copiedTp] ->
                        copiedTp.bounds.addAll(fnTp.bounds.map { it.substituteTypeParameters(fnSubst) })
                    }
                    buildFunction {
                        origin = SirOrigin.ExternallyDefined("reverseDispatchRequirement")
                        visibility = SirVisibility.PUBLIC
                        name = member.name
                        isInstance = true
                        isOverride = false
                        modality = SirModality.UNSPECIFIED
                        fixity = member.fixity
                        isAsync = member.isAsync
                        errorType = member.errorType.substituteTypeParameters(fnSubst)
                        returnType = member.returnType.substituteTypeParameters(fnSubst)
                        typeParameters += copiedFnTps
                        constraints += member.constraints.map { it.substituteConstraint(fnSubst) }
                        contextParameter = member.contextParameter?.let { p ->
                            SirParameter(p.argumentName, p.parameterName, p.type.substituteTypeParameters(fnSubst), p.origin, p.isVariadic)
                        }
                        extensionReceiverParameter = member.extensionReceiverParameter?.let { p ->
                            SirParameter(p.argumentName, p.parameterName, p.type.substituteTypeParameters(fnSubst), p.origin, p.isVariadic)
                        }
                        parameters += member.parameters.map { p ->
                            SirParameter(p.argumentName, p.parameterName, p.type.substituteTypeParameters(fnSubst), p.origin, p.isVariadic)
                        }
                        attributes += member.attributes.filter { it is SirAttribute.Available || it is SirAttribute.SPI }
                    }.also { it.parent = this@SirOpenGenericReverseDispatchProtocol }
                }
                is SirVariable -> {
                    buildVariable {
                        origin = SirOrigin.ExternallyDefined("reverseDispatchRequirement")
                        visibility = SirVisibility.PUBLIC
                        name = member.name
                        isInstance = true
                        isConstant = false
                        isOverride = false
                        modality = SirModality.UNSPECIFIED
                        type = member.type.substituteTypeParameters(baseSubst)
                        attributes += member.attributes.filter { it is SirAttribute.Available || it is SirAttribute.SPI }
                        getter = buildGetter {
                            origin = SirOrigin.ExternallyDefined("reverseDispatchRequirement")
                            errorType = member.getter?.errorType ?: SirType.never
                        }
                        if (member.setter != null) {
                            setter = buildSetter {
                                origin = SirOrigin.ExternallyDefined("reverseDispatchRequirement")
                                errorType = member.setter?.errorType ?: SirType.never
                            }
                        }
                    }.also {
                        it.parent = this@SirOpenGenericReverseDispatchProtocol
                        it.getter?.parent = it
                        it.setter?.parent = it
                    }
                }
                else -> null
            }
        }.toMutableList()
    }
}

internal class SirObjectSyntheticInit(
    override val ktSymbol: KaNamedClassSymbol,
    override val sirSession: SirSession,
) : SirInit(), SirFromKtSymbol<KaNamedClassSymbol> {
    override val origin: SirOrigin = SirOrigin.PrivateObjectInit(`for` = KotlinSource(ktSymbol))
    override val visibility: SirVisibility = SirVisibility.PRIVATE
    override val isFailable: Boolean = false
    override val parameters: List<SirParameter> = emptyList()
    override val documentation: String? = null
    override val isRequired: Boolean = false
    override val isConvenience: Boolean = false
    override val isOverride: Boolean get() = overrideStatus is OverrideStatus.Overrides
    private val overrideStatus: OverrideStatus<SirInit>? by lazy { computeIsOverride() }
    override lateinit var parent: SirDeclarationParent
    override val attributes: List<SirAttribute> by lazy {
        listOfNotNull(
            SirAttribute.NonOverride.takeIf { overrideStatus is OverrideStatus.Conflicts }
        )
    }
    override val errorType: SirType get() = SirType.never
    override val isAsync: Boolean get() = false
    override val typeParameters: List<SirTypeParameter> get() = emptyList()
    override val constraints: List<SirTypeConstraint> get() = emptyList()
    override val bridges: List<SirBridge> get() = emptyList()
    override var body: SirFunctionBody?
        get() = null
        set(_) = Unit
}

internal class SirObjectAccessorVariableFromKtSymbol(
    override val ktSymbol: KaNamedClassSymbol,
    override val sirSession: SirSession,
) : SirVariable(), SirFromKtSymbol<KaNamedClassSymbol> {
    private class SirObjectAccessorGetterFromKtSymbol(
        override val ktSymbol: KaNamedClassSymbol,
        sirSession: SirSession,
    ) : SirAbstractGetter(sirSession), SirFromKtSymbol<KaNamedClassSymbol> {
        override val origin: SirOrigin by lazy { KotlinSource(ktSymbol) }
        override val documentation: String? get() = null
        override val attributes: List<SirAttribute> by lazy { this.translatedAttributes }
        override val errorType: SirType get() = if (ktSymbol.throwsAnnotation != null) SirType.any else SirType.never
        override val isAsync: Boolean get() = false
        override val fqName: FqName? by lazyWithSessions {
            ktSymbol.classId?.asSingleFqName()
                ?: return@lazyWithSessions null
        }
    }

    override lateinit var parent: SirDeclarationParent

    override val name: String get() = "shared"

    override val origin: SirOrigin = SirOrigin.ObjectAccessor(KotlinSource(ktSymbol))

    override val isInstance: Boolean get() = false
    override val isConstant: Boolean get() = false

    override val visibility: SirVisibility get() = SirVisibility.PUBLIC

    override val type: SirType by lazyWithSessions {
        ktSymbol.toSir().primaryDeclaration?.let { it as? SirScopeDefiningDeclaration }?.let { SirNominalType(it) }
            ?: error("Failed to translate object accessor base type $ktSymbol")
    }

    override val getter: SirGetter by lazy {
        SirObjectAccessorGetterFromKtSymbol(ktSymbol, sirSession).also {
            it.parent = this@SirObjectAccessorVariableFromKtSymbol
        }
    }
    override val setter: SirSetter? get() = null

    override val documentation: String? get() = null

    override val attributes: List<SirAttribute> by lazy {
        this.translatedAttributes + listOfNotNull(SirAttribute.NonOverride.takeIf { overrideStatus is OverrideStatus.Conflicts })
    }

    override val isOverride: Boolean
        get() = overrideStatus is OverrideStatus.Overrides

    private val overrideStatus: OverrideStatus<SirVariable>? by lazy { computeIsOverride() }

    override val modality: SirModality = SirModality.FINAL

    override val bridges: List<SirBridge> = emptyList()
}

private val KaClassType.isRegularClass: Boolean
    get() = (symbol as? KaClassSymbol)?.let { it.classKind == KaClassKind.CLASS } ?: false
