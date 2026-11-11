/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.render
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildEnumCase
import org.jetbrains.kotlin.sir.builder.buildGetter
import org.jetbrains.kotlin.sir.builder.buildInit
import org.jetbrains.kotlin.sir.builder.buildVariable
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.generateFunctionBridge
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.BridgeFunctionProxy
import org.jetbrains.kotlin.sir.providers.sirAvailability
import org.jetbrains.kotlin.sir.providers.sirDeclarationName
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.toSir
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeSupportModule
import org.jetbrains.kotlin.sir.providers.utils.allRequiredOptIns
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.sir.util.isUnavailable
import org.jetbrains.kotlin.sir.util.substituteTypeParameters
import org.jetbrains.kotlin.sir.util.swiftFqName
import org.jetbrains.kotlin.sir.util.swiftIdentifier
import org.jetbrains.kotlin.sir.util.swiftName
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.lazyWithSessions
import org.jetbrains.sir.lightclasses.extensions.withSessions
import org.jetbrains.sir.lightclasses.utils.*

context(_: KaSession, sirSession: SirSession)
internal fun createSirSealedType(
    declaration: SirScopeDefiningDeclaration
): SirScopeDefiningDeclaration? {
    if (declaration !is SirAbstractClassFromKtSymbol && declaration !is SirProtocolFromKtSymbol) return null
    if (declaration.ktSymbol.isSealed) {
        SirSealedTypeEnum(declaration.ktSymbol, sirSession).takeUnless { it.isEmpty }?.let {
            it.parent = declaration.parent
            return it
        }
    }
    if (declaration.ktSymbol.superTypes.any { it.expandedSymbol.isSealed }) {
        return SirSealedTypeStruct(declaration.ktSymbol, sirSession, declaration).apply { parent = declaration.parent }
    }
    return null
}

private val SirScopeDefiningDeclaration.sealedType: SirScopeDefiningDeclaration?
    get() = when (this) {
        is SirAbstractClassFromKtSymbol -> sealedType
        is SirProtocolFromKtSymbol -> sealedType
        else -> null
    }

private fun SirScopeDefiningDeclaration.sealedProtocolTypeArguments(protocol: SirProtocolFromKtSymbol): List<SirType> {
    if (protocol.typeParameters.isEmpty()) return emptyList()
    return when (this) {
        is SirAbstractClassFromKtSymbol -> protocol.typeParameters.map { tp ->
            effectiveBoundAssocTypes[tp.name] ?: relaxSealedTypeArgument(tp)
        }
        is SirProtocolFromKtSymbol -> {
            val selfEqMap = constraints.filterIsInstance<SirTypeConstraint.Equality>()
                .filter { it.subjectPath.size == 2 && it.subjectPath[0] == "Self" }
                .associate { it.subjectPath[1] to it.constraint }
            protocol.typeParameters.map { tp ->
                selfEqMap[tp.name.swiftIdentifier]
                    ?: typeParameters.firstOrNull { it.name == tp.name }?.let(::SirTypeParameterType)
                    ?: relaxSealedTypeArgument(tp)
            }
        }
        else -> protocol.typeParameters.map(::relaxSealedTypeArgument)
    }
}

private fun relaxSealedTypeArgument(tp: SirTypeParameter): SirType {
    tp.bounds.firstOrNull { it is SirNominalType && it.typeDeclaration is SirClass }?.let { return it }
    return SirNominalType(KotlinRuntimeSupportModule.kotlinAny)
}

context(sirSession: SirSession)
internal fun createSirSealedTypeFunctions(
    declaration: SirScopeDefiningDeclaration,
): List<SirFunction> {
    require(declaration is SirAbstractClassFromKtSymbol || declaration is SirProtocolFromKtSymbol) {
        "Sealed type functions are only available for SirClasses and SirProtocols from KtSymbols"
    }
    val sealedType = declaration.sealedType ?: return emptyList()
    return buildList {
        val isSealedType = sealedType is SirSealedTypeEnum
        if (isSealedType) {
            add(SirSealedTypeFunction.Sealed(declaration.ktSymbol, sirSession, sealedType, declaration))
        }
        val superSealedType = declaration.superClassDeclaration?.sealedType as? SirSealedTypeEnum
        if (superSealedType != null) {
            val superArgs = if (superSealedType.typeParameters.isEmpty()) {
                emptyList()
            } else {
                (declaration as? SirAbstractClassFromKtSymbol)?.rawSuperClass?.typeArguments
                    ?.takeIf { it.size == superSealedType.typeParameters.size }
                    ?: superSealedType.typeParameters.map(::relaxSealedTypeArgument)
            }
            add(SirSealedTypeFunction.Leaf(declaration.ktSymbol, sirSession, superSealedType, isSealedType, true, superArgs))
        }
        for (protocol in declaration.protocols) {
            if (protocol !is SirProtocolFromKtSymbol) continue
            val protoSealedType = protocol.sealedType as? SirSealedTypeEnum ?: continue
            val protoArgs = declaration.sealedProtocolTypeArguments(protocol)
            add(SirSealedTypeFunction.Leaf(declaration.ktSymbol, sirSession, protoSealedType, isSealedType, false, protoArgs))
        }
    }
}

internal class SirSealedTypeEnum(
    override val ktSymbol: KaNamedClassSymbol,
    override val sirSession: SirSession,
) : SirEnum(), SirFromKtSymbol<KaNamedClassSymbol> {
    override val name: String by lazyWithSessions {
        val hasGenericOuter = generateSequence(ktSymbol.containingDeclaration as? KaNamedClassSymbol) {
            it.containingDeclaration as? KaNamedClassSymbol
        }.any { it.typeParameters.isNotEmpty() }
        if (hasGenericOuter) sealedDeclaration.name + "_SealedType" else ktSymbol.name.asString() + "_SealedType"
    }
    override val origin: SirOrigin get() = SirOrigin.SealedType(KotlinSource(ktSymbol))
    override val visibility: SirVisibility = SirVisibility.PUBLIC
    override val documentation: String? = null
    override lateinit var parent: SirDeclarationParent

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

    override val attributes: List<SirAttribute> by lazyWithSessions {
        val baseAttrs = sealedDeclaration.propagatedAttributes
        if (typeParameters.isEmpty() || sealedDeclaration.isUnavailable) return@lazyWithSessions baseAttrs
        val enumSpiNames = baseAttrs.filterIsInstance<SirAttribute.SPI>().mapTo(mutableSetOf()) { it.name }
        val canBeFrozen = cases.all { [_, case] ->
            case == null || (
                !case.declaration.isUnavailable &&
                case.attributes.filterIsInstance<SirAttribute.SPI>().all { it.name in enumSpiNames }
            )
        }
        if (canBeFrozen) baseAttrs + SirAttribute.Frozen else baseAttrs
    }

    override val protocols: List<SirProtocol>
        get() = listOf(KotlinRuntimeSupportModule.sealedType)

    internal val sealedDeclaration: SirScopeDefiningDeclaration by lazyWithSessions {
        ktSymbol.toSir().primaryDeclaration as? SirScopeDefiningDeclaration
            ?: error("Failed to get declaration for sealed type ($ktSymbol)")
    }

    internal val sealedSubjectType: SirType by lazy {
        val tpArgs = typeParameters.map(::SirTypeParameterType)
        when (val decl = sealedDeclaration) {
            is SirProtocolFromKtSymbol -> if (decl.isPatProtocol) {
                SirNominalType(decl.anyWrapperDeclaration!!, tpArgs)
            } else {
                SirNominalType(decl)
            }
            else -> SirNominalType(decl, tpArgs)
        }
    }

    private val sealedInheritors: List<KaNamedClassSymbol> by lazyWithSessions {
        // Don't include enums, they don't conform to protocols (KT-88682 / KT-88716)
        ktSymbol.sealedClassInheritors.filter { it.classKind != KaClassKind.ENUM_CLASS }
    }

    val isEmpty: Boolean get() = sealedInheritors.isEmpty()

    val cases: List<Pair<ClassId, SirSealedTypeEnumCase?>> by lazyWithSessions {
        val names = createUniqueCaseNames(sealedInheritors)
        sealedInheritors.map { ktSymbol ->
            val classId = ktSymbol.classId ?: error("Sealed type ($ktSymbol) must have a classId")

            val visibility = ktSymbol.sirAvailability().visibility ?: SirVisibility.PRIVATE
            if (visibility <= SirVisibility.INTERNAL) return@map classId to null

            val declaration = ktSymbol.toSir().primaryDeclaration as? SirScopeDefiningDeclaration
                ?: error("Failed to get declaration for sealed type ($ktSymbol)")
            if (declaration.isUnavailable) return@map classId to null

            val name = names.getValue(ktSymbol).enumCaseName
            val sealedType = declaration.sealedType ?: error("Leaf declaration ($declaration) must have a SealedType")
            val case = SirSealedTypeEnumCase(ktSymbol, sirSession, declaration, sealedType, name).apply { parent = this@SirSealedTypeEnum }
            classId to case
        }
    }

    val unknownType: SirStruct? by lazyWithSessions {
        if (cases.none { it.second == null }) return@lazyWithSessions null
        SirSealedTypeStruct(ktSymbol, sirSession, sealedDeclaration, "Unknown").apply { parent = this@SirSealedTypeEnum }
    }

    val unknownCase: SirEnumCase? by lazyWithSessions {
        val unk = unknownType ?: return@lazyWithSessions null
        val parentNominal = if (typeParameters.isNotEmpty()) {
            SirNominalType(this@SirSealedTypeEnum, typeParameters.map(::SirTypeParameterType))
        } else {
            null
        }
        val type = SirNominalType(unk, parent = parentNominal)
        buildEnumCase {
            name = "unknown"
            associatedValueTypes.add(type)
        }.apply { parent = this@SirSealedTypeEnum }
    }

    private val valueVariable: SirVariable by lazy {
        buildVariable {
            name = "value"
            type = sealedSubjectType
            getter = buildGetter {
                val tpArgs = typeParameters.map(::SirTypeParameterType)
                val genericWrapperName = if (typeParameters.isNotEmpty()) {
                    when (val decl = sealedDeclaration) {
                        is SirProtocolFromKtSymbol -> SirNominalType(decl.anyWrapperDeclaration!!, tpArgs).swiftName
                        else -> SirNominalType(decl, tpArgs).swiftName
                    }
                } else {
                    null
                }
                val statement = buildList {
                    cases.forEach { addIfNotNull(it.second) }
                    addIfNotNull(unknownCase)
                }.joinToString(prefix = "switch self {\n", separator = "\n", postfix = "\n}") { case ->
                    val expr = if (genericWrapperName != null && case != unknownCase) {
                        "$genericWrapperName(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(type.value.__externalRCRef()), options: .asExistentialWrapper)"
                    } else {
                        "type.value"
                    }
                    "case let .${case.name.swiftIdentifier}(type): $expr"
                }
                body = SirFunctionBody(listOf(statement))
            }
        }.apply {
            parent = this@SirSealedTypeEnum
            getter?.parent = this
        }
    }

    override val declarations: List<SirDeclaration> by lazyWithSessions {
        buildList {
            cases.forEach { addIfNotNull(it.second) }
            addIfNotNull(unknownCase)
            addIfNotNull(unknownType)
            add(valueVariable)
            if (typeParameters.size == 1 && !sealedDeclaration.isUnavailable) {
                val tp = typeParameters.single()
                if (tp.variance == SirTypeVariance.COVARIANT) {
                    val canInline = attributes.none { it is SirAttribute.SPI || it is SirAttribute.Available }
                    add(buildCovariantProperty(this@SirSealedTypeEnum, tp, ownerExpr = "self.value", isInlinable = canInline))
                }
            }
        }
    }
}

internal class SirSealedTypeEnumCase(
    override val ktSymbol: KaNamedClassSymbol,
    override val sirSession: SirSession,
    internal val declaration: SirScopeDefiningDeclaration,
    private val sealedType: SirScopeDefiningDeclaration,
    override val name: String,
) : SirEnumCase(), SirFromKtSymbol<KaNamedClassSymbol> {
    override val origin: SirOrigin get() = SirOrigin.SealedType(KotlinSource(ktSymbol))
    override val visibility: SirVisibility = SirVisibility.PUBLIC
    override val documentation: String? = null
    override lateinit var parent: SirDeclarationParent
    override val attributes: List<SirAttribute> get() = declaration.propagatedAttributes
    override val bridges: List<SirBridge> get() = emptyList()

    internal val caseTypeArguments: List<SirType> by lazyWithSessions {
        val leafTypeParams = (sealedType as? SirGenericDeclaration)?.typeParameters.orEmpty()
        if (leafTypeParams.isEmpty()) return@lazyWithSessions emptyList()
        val parentEnum = parent as? SirSealedTypeEnum ?: return@lazyWithSessions leafTypeParams.map(::relaxSealedTypeArgument)
        if (parentEnum.typeParameters.isEmpty()) return@lazyWithSessions leafTypeParams.map(::relaxSealedTypeArgument)

        val leafParamMapping = HashMap<KaTypeParameterSymbol, SirType>()
        val matchingSuper = (ktSymbol.superTypes.asSequence() + ktSymbol.defaultType.allSupertypes)
            .mapNotNull { it.fullyExpandedType as? KaUsualClassType }
            .firstOrNull { it.symbol == parentEnum.ktSymbol && it.typeArguments.size == parentEnum.typeParameters.size }

        if (matchingSuper != null) {
            for (idx in parentEnum.typeParameters.indices) {
                val proj = matchingSuper.typeArguments[idx]
                val argType = (proj as? KaTypeArgumentWithVariance)?.type
                if (argType is KaTypeParameterType) {
                    leafParamMapping.putIfAbsent(argType.symbol, SirTypeParameterType(parentEnum.typeParameters[idx]))
                }
            }
        }

        leafTypeParams.mapIndexed { idx, leafSirTp ->
            val leafKtTp = ktSymbol.typeParameters.getOrNull(idx)
            (if (leafKtTp != null) leafParamMapping[leafKtTp] else null) ?: relaxSealedTypeArgument(leafSirTp)
        }
    }

    override val associatedValueTypes: List<SirType>
        get() = listOf(SirNominalType(sealedType, caseTypeArguments))
}

internal class SirSealedTypeStruct(
    override val ktSymbol: KaNamedClassSymbol,
    override val sirSession: SirSession,
    private val declaration: SirScopeDefiningDeclaration,
    private val customName: String? = null,
) : SirStruct(), SirFromKtSymbol<KaNamedClassSymbol> {
    private val isUnknownStruct: Boolean get() = customName != null
    override val name: String = customName ?: (declaration.name + "_SealedType")
    override val origin: SirOrigin get() = SirOrigin.SealedType(KotlinSource(ktSymbol))
    override val visibility: SirVisibility = SirVisibility.PUBLIC
    override val documentation: String? = null
    override lateinit var parent: SirDeclarationParent
    override val attributes: List<SirAttribute> get() = declaration.propagatedAttributes
    override val protocols: List<SirProtocol>
        get() = listOf(KotlinRuntimeSupportModule.sealedType)

    private val copiedGenerics: Pair<List<SirTypeParameter>, List<SirTypeConstraint>> by lazy {
        if (isUnknownStruct) return@lazy emptyList<SirTypeParameter>() to emptyList()
        val srcParams = (declaration as? SirGenericDeclaration)?.typeParameters.orEmpty()
        if (srcParams.isEmpty()) return@lazy emptyList<SirTypeParameter>() to emptyList()
        val tpSubst = LinkedHashMap<SirTypeParameter, SirType>()
        val copiedParams = srcParams.map { tp ->
            SirTypeParameter(
                name = tp.name,
                variance = tp.variance,
                bounds = mutableListOf(),
                isPrimaryAssociatedType = false,
                origin = tp.origin,
            ).also { tpSubst[tp] = SirTypeParameterType(it) }
        }
        srcParams.zip(copiedParams).forEach { [origTp, copyTp] ->
            copyTp.bounds.addAll(origTp.bounds.map { it.substituteTypeParameters(tpSubst) })
        }
        val copiedConstraints = (declaration as? SirConstrainedDeclaration)?.constraints.orEmpty()
            .filter { it.subjectPath.firstOrNull() != "Self" }
            .map { constraint ->
                when (constraint) {
                    is SirTypeConstraint.Conformance -> SirTypeConstraint.Conformance(
                        constraint.constraint.substituteTypeParameters(tpSubst),
                        constraint.subjectPath,
                    )
                    is SirTypeConstraint.Equality -> SirTypeConstraint.Equality(
                        constraint.constraint.substituteTypeParameters(tpSubst),
                        constraint.subjectPath,
                    )
                }
            }
        copiedParams to copiedConstraints
    }

    override val typeParameters: List<SirTypeParameter>
        get() = copiedGenerics.first

    override val constraints: List<SirTypeConstraint>
        get() = copiedGenerics.second

    private val isPatProtocolSubject: Boolean
        get() = (declaration as? SirProtocolFromKtSymbol)?.isPatProtocol == true

    private val valueType: SirType by lazy {
        if (isUnknownStruct) {
            return@lazy (parent as? SirSealedTypeEnum)?.sealedSubjectType ?: SirNominalType(declaration)
        }
        val tpArgs = typeParameters.map(::SirTypeParameterType)
        when (declaration) {
            is SirProtocolFromKtSymbol -> if (declaration.isPatProtocol) {
                SirNominalType(declaration.anyWrapperDeclaration!!, tpArgs)
            } else {
                SirNominalType(declaration)
            }
            else -> SirNominalType(declaration, tpArgs)
        }
    }

    private val valueVariable: SirVariable by lazy {
        buildVariable {
            isConstant = true
            this.name = "value"
            type = valueType
        }.apply { parent = this@SirSealedTypeStruct }
    }

    private val init: SirInit by lazy {
        buildInit {
            visibility = SirVisibility.INTERNAL
            isFailable = false
            if (isPatProtocolSubject) {
                val wrapperName = valueType.swiftName
                parameters += SirParameter(null, "value", SirNominalType(KotlinRuntimeModule.kotlinBase))
                body = SirFunctionBody(
                    listOf("self.value = (value as? $wrapperName) ?? $wrapperName(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(value.__externalRCRef()), options: .asExistentialWrapper)")
                )
            } else {
                parameters += SirParameter(null, "value", valueType)
                body = SirFunctionBody(listOf("self.value = value"))
            }
        }.apply { parent = this@SirSealedTypeStruct }
    }

    override val declarations: List<SirDeclaration> by lazy {
        listOf(valueVariable, init)
    }
}

internal sealed class SirSealedTypeFunction : SirFunction(), SirFromKtSymbol<KaNamedClassSymbol> {

    protected abstract val sealedType: SirSealedTypeEnum
    override val typeParameters: List<SirTypeParameter> get() = emptyList()
    override val constraints: List<SirTypeConstraint> get() = emptyList()

    override val visibility: SirVisibility = SirVisibility.PUBLIC
    override val origin: SirOrigin get() = SirOrigin.SealedType(KotlinSource(ktSymbol))
    override val name: String = "sealedType"
    override val contextParameter: SirParameter? = null
    override val extensionReceiverParameter: SirParameter? = null
    override val parameters: List<SirParameter> get() = emptyList()
    override val documentation: String? = null
    override lateinit var parent: SirDeclarationParent
    override val isInstance: Boolean = true
    override val fixity: SirFixity? = null
    override val errorType: SirType get() = SirType.never
    override val isAsync: Boolean = false

    class Sealed(
        override val ktSymbol: KaNamedClassSymbol,
        override val sirSession: SirSession,
        override val sealedType: SirSealedTypeEnum,
        private val ownerDeclaration: SirScopeDefiningDeclaration,
    ) : SirSealedTypeFunction() {
        override val isOverride: Boolean = false
        override val modality: SirModality = SirModality.OPEN
        override val attributes: List<SirAttribute> get() = sealedType.propagatedAttributes

        private val ownerTypeArguments: List<SirType>
            get() = (ownerDeclaration as? SirGenericDeclaration)?.typeParameters.orEmpty().map(::SirTypeParameterType)

        override val returnType: SirType
            get() = SirNominalType(sealedType, ownerTypeArguments)

        private val orderedVisibleCases: List<SirSealedTypeEnumCase> by lazyWithSessions {
            val visible = sealedType.cases.mapNotNull { it.second }
            if (visible.size <= 1) return@lazyWithSessions visible
            val remaining = visible.toMutableList()
            val sorted = ArrayList<SirSealedTypeEnumCase>(visible.size)
            while (remaining.isNotEmpty()) {
                val pickIdx = remaining.indexOfFirst { cand ->
                    remaining.none { other -> other !== cand && other.ktSymbol.isSubClassOf(cand.ktSymbol) }
                }.let { if (it >= 0) it else 0 }
                sorted.add(remaining.removeAt(pickIdx))
            }
            sorted
        }

        private val needsTagBridge: Boolean by lazyWithSessions {
            sealedType.typeParameters.isNotEmpty() || orderedVisibleCases.any { it.caseTypeArguments.isNotEmpty() }
        }

        private val tagBridgeProxy: BridgeFunctionProxy? by lazyWithSessions {
            if (!needsTagBridge || isUnavailable || orderedVisibleCases.isEmpty()) {
                return@lazyWithSessions null
            }
            val fqName = ktSymbol.classId?.asSingleFqName() ?: return@lazyWithSessions null
            val allOptIns = (ktSymbol.allRequiredOptIns + orderedVisibleCases.flatMap { it.ktSymbol.allRequiredOptIns }).distinct()
            generateFunctionBridge(
                baseBridgeName = fqName.baseBridgeName + "_sealedType_tag",
                explicitParameters = emptyList(),
                returnType = SirNominalType(SirSwiftModule.int32),
                kotlinFqName = fqName,
                kotlinOptIns = allOptIns,
                selfParameter = SirParameter(null, "self", SirNominalType(KotlinRuntimeModule.kotlinBase)),
                contextParameters = emptyList(),
                extensionReceiverParameter = null,
                errorParameter = null,
                isAsync = false,
            )
        }

        override val bridges: List<SirBridge> by lazyWithSessions {
            val proxy = tagBridgeProxy ?: return@lazyWithSessions emptyList()
            proxy.createSirBridges(nonVirtualTargetMethod = null) {
                val branches = orderedVisibleCases.mapIndexed { idx, case ->
                    val leafFq = case.ktSymbol.classId!!.asSingleFqName().render()
                    val starArgs = case.ktSymbol.typeParameters.takeIf { it.isNotEmpty() }
                        ?.joinToString(prefix = "<", postfix = ">") { "*" } ?: ""
                    "is $leafFq$starArgs -> $idx"
                }.joinToString(separator = "; ")
                "when (__self) { $branches; else -> -1 }"
            }
        }

        override var body: SirFunctionBody?
            set(_) = Unit
            get() = withSessions {
                val defaultCase = "default: " + when (val unknownCase = sealedType.unknownCase) {
                    null -> """fatalError("missing sealedType for \(self)")"""
                    else -> ".${unknownCase.name.swiftIdentifier}(.init(self))"
                }
                if (!needsTagBridge) {
                    val statement = sealedType.cases.mapNotNull {
                        it.second
                    }.joinToString(prefix = "switch self {\n", separator = "", postfix = "$defaultCase\n}") { case ->
                        val valueType = SirNominalType(case.declaration).swiftName
                        val isSealedType = case.declaration.sealedType is SirSealedTypeEnum
                        val type = if (isSealedType) "value.${name.swiftIdentifier}()" else ".init(value)"
                        "case let value as $valueType: .${case.name.swiftIdentifier}($type)\n"
                    }
                    return@withSessions SirFunctionBody(listOf(statement))
                }

                val proxy = tagBridgeProxy
                if (proxy == null) {
                    return@withSessions SirFunctionBody(listOf("switch 0 {\n$defaultCase\n}"))
                }

                val ownerParamSub: Map<SirTypeParameter, SirType> =
                    sealedType.typeParameters.zip(ownerTypeArguments).toMap()

                val tagExpr = proxy.createSwiftInvocation { it }.single()
                val casesStr = orderedVisibleCases.mapIndexed { idx, case ->
                    val substitutedCaseArgs = case.caseTypeArguments.map { it.substituteTypeParameters(ownerParamSub) }
                    val leafExpr = when (val decl = case.declaration) {
                        is SirProtocolFromKtSymbol -> if (decl.isPatProtocol) {
                            val anyWrapper = decl.anyWrapperDeclaration!!
                            "${SirNominalType(anyWrapper, substitutedCaseArgs).swiftName}.__fromKotlinPointer(KotlinRuntimeSupport._cloneBridgeableRef(self.__externalRCRef()))"
                        } else {
                            "KotlinRuntimeSupport._KotlinExistential<any ${decl.existentialMarker.swiftFqName}>(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(self.__externalRCRef()), options: .asExistentialWrapper)"
                        }
                        else -> "${SirNominalType(decl, substitutedCaseArgs).swiftName}.__fromKotlinPointer(KotlinRuntimeSupport._cloneBridgeableRef(self.__externalRCRef()))"
                    }
                    val isSubSealed = case.declaration.sealedType is SirSealedTypeEnum
                    val wrapped = if (isSubSealed) "$leafExpr.${name.swiftIdentifier}()" else ".init($leafExpr)"
                    "case $idx: .${case.name.swiftIdentifier}($wrapped)\n"
                }.joinToString(separator = "")

                SirFunctionBody(listOf("switch $tagExpr {\n$casesStr$defaultCase\n}"))
            }
    }

    class Leaf(
        override val ktSymbol: KaNamedClassSymbol,
        override val sirSession: SirSession,
        override val sealedType: SirSealedTypeEnum,
        private val isSealedType: Boolean,
        override val isOverride: Boolean,
        private val returnTypeArguments: List<SirType> = emptyList(),
    ) : SirSealedTypeFunction() {
        override val modality: SirModality = SirModality.FINAL
        override val bridges: List<SirBridge> get() = emptyList()
        override val returnType: SirType
            get() = SirNominalType(sealedType, returnTypeArguments)
        private val enumCase: SirEnumCase by lazy {
            val classId = ktSymbol.classId
            sealedType.cases.firstOrNull { it.first == classId }?.let {
                it.second ?: sealedType.unknownCase
            } ?: error("Leaf class ($classId) must have a SealedType case")
        }
        override val attributes: List<SirAttribute> by lazy {
            buildList {
                addAll(sealedType.propagatedAttributes)
                if (isSealedType) add(SirAttribute.DisfavoredOverload)
            }
        }
        override var body: SirFunctionBody?
            set(_) = Unit
            get() {
                val case = enumCase as? SirSealedTypeEnumCase
                val ownTpArgs = (case?.declaration as? SirGenericDeclaration)?.typeParameters?.map(::SirTypeParameterType).orEmpty()
                val expectedCaseArgs = if (case != null && returnTypeArguments.size == sealedType.typeParameters.size) {
                    val ownerParamSub = sealedType.typeParameters.zip(returnTypeArguments).toMap()
                    case.caseTypeArguments.map { it.substituteTypeParameters(ownerParamSub) }
                } else {
                    ownTpArgs
                }
                val selfExpr = if (case != null && expectedCaseArgs.isNotEmpty() && expectedCaseArgs != ownTpArgs) {
                    val decl = case.declaration
                    val targetType = if (decl is SirProtocolFromKtSymbol && decl.isPatProtocol) {
                        SirNominalType(decl.anyWrapperDeclaration!!, expectedCaseArgs)
                    } else {
                        SirNominalType(decl, expectedCaseArgs)
                    }
                    "${targetType.swiftName}.__fromKotlinPointer(KotlinRuntimeSupport._cloneBridgeableRef(self.__externalRCRef()))"
                } else {
                    "self"
                }
                val type = if (isSealedType && enumCase != sealedType.unknownCase) {
                    if (selfExpr == "self") "${name.swiftIdentifier}()" else "$selfExpr.${name.swiftIdentifier}()"
                } else {
                    ".init($selfExpr)"
                }
                val statement = ".${enumCase.name.swiftIdentifier}($type)"
                return SirFunctionBody(listOf(statement))
            }
    }
}

private val KaClassSymbol?.isSealed: Boolean
    get() = this?.modality == KaSymbolModality.SEALED

private val SirDeclaration.propagatedAttributes: List<SirAttribute>
    get() = attributes.filter { it is SirAttribute.Available || it is SirAttribute.SPI || it is SirAttribute.Documentation }

context(_: SirSession, _: KaSession)
private fun createUniqueCaseNames(
    symbols: List<KaNamedClassSymbol>
): Map<KaNamedClassSymbol, String> {
    val fqNames = symbols.associateWith { symbol ->
        buildList {
            var symbol: KaDeclarationSymbol? = symbol
            while (symbol != null) {
                add(symbol.sirDeclarationName())
                symbol = symbol.containingDeclaration
            }
        }.asReversed()
    }
    val names = mutableMapOf("" to symbols.toMutableList())
    var index = 1
    do {
        val conflicts = names.filter { it.key.isEmpty() || it.value.size > 1 }
        for (conflict in conflicts) {
            for (symbol in conflict.value) {
                val fqName = fqNames.getValue(symbol).takeLast(index).joinToString(".")
                if (fqName == conflict.key) error("Failed to create unique SealedType enum case name for $symbol")
                names.getOrPut(fqName) { mutableListOf() }.add(symbol)
            }
            names.remove(conflict.key)
        }
        index++
    } while (conflicts.isNotEmpty())
    return buildMap { names.forEach { put(it.value.single(), it.key) } }
}
