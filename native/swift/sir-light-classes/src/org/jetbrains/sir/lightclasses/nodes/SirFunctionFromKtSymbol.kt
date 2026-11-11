/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.export.utilities.isSuspend
import org.jetbrains.kotlin.analysis.api.renderer.render
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KaTypeRendererForSource
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.api.types.builtinTypes
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildFunctionCopy
import org.jetbrains.kotlin.sir.providers.*
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.BridgeFunctionBuilder
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.BridgeFunctionProxy
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.utils.allRequiredOptIns
import org.jetbrains.kotlin.sir.providers.utils.resolveUpperBound
import org.jetbrains.kotlin.sir.providers.utils.throwsAnnotation
import org.jetbrains.kotlin.sir.util.isUnavailable
import org.jetbrains.kotlin.sir.util.replaceOrAddPropagatedUnavailability
import org.jetbrains.kotlin.sir.util.unavailableTypes
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.lazyWithSessions
import org.jetbrains.sir.lightclasses.extensions.sirModality
import org.jetbrains.sir.lightclasses.extensions.withSessions
import org.jetbrains.sir.lightclasses.utils.*

internal open class SirFunctionFromKtSymbol(
    override val ktSymbol: KaFunctionSymbol,
    override val sirSession: SirSession,
) : SirFunction(), SirFromKtSymbol<KaFunctionSymbol> {

    override val visibility: SirVisibility = SirVisibility.PUBLIC
    override val origin: SirOrigin by lazy {
        KotlinSource(ktSymbol)
    }
    override val name: String by lazyWithSessions {
        ktSymbol.sirDeclarationName()
    }

    private val translatedTypeParams: TranslatedTypeParameters by lazyWithSessions {
        val allowLifting = this@SirFunctionFromKtSymbol !is SirRenamedFunction &&
            parent !is SirProtocol &&
            modality == SirModality.FINAL &&
            ktSymbol.allOverriddenSymbols.none()
        translateTypeParametersAndConstraints(
            declarationSymbol = ktSymbol,
            ownKtTypeParams = ktSymbol.typeParameters,
            enclosingScope = enclosingTypeParameterScope(),
            callableSymbol = ktSymbol,
            allowInputProjectionLifting = allowLifting,
        )
    }

    private val usedTypeParamsAndConstraints: Pair<List<SirTypeParameter>, List<SirTypeConstraint>> by lazy {
        val rawParams = translatedTypeParams.typeParameters
        if (rawParams.isEmpty()) return@lazy emptyList<SirTypeParameter>() to emptyList()
        val ownSet = rawParams.toSet()
        fun SirType.collectOwnParams(out: MutableSet<SirTypeParameter>) {
            when (this) {
                is SirTypeParameterType -> if (parameter in ownSet) out.add(parameter)
                is SirNominalType -> {
                    parent?.collectOwnParams(out)
                    typeArguments.forEach { it.collectOwnParams(out) }
                }
                is SirExistentialType -> protocols.forEach { [_, args] -> args.forEach { it.collectOwnParams(out) } }
                is SirOptionalType -> wrappedType.collectOwnParams(out)
                is SirFunctionalType -> {
                    contextTypes.forEach { it.collectOwnParams(out) }
                    parameterTypes.forEach { it.collectOwnParams(out) }
                    errorType.collectOwnParams(out)
                    returnType.collectOwnParams(out)
                }
                is SirTupleType -> types.forEach { [_, ty] -> ty.collectOwnParams(out) }
                is SirType.Metatype -> type.collectOwnParams(out)
                else -> {}
            }
        }
        val used = HashSet<SirTypeParameter>()
        contextParameter?.type?.collectOwnParams(used)
        extensionReceiverParameter?.type?.collectOwnParams(used)
        parameters.forEach { it.type.collectOwnParams(used) }
        returnType.collectOwnParams(used)

        var changed = true
        while (changed) {
            changed = false
            val snapshot = used.toList()
            for (tp in snapshot) {
                val before = used.size
                tp.bounds.forEach { it.collectOwnParams(used) }
                translatedTypeParams.constraints.forEach { c ->
                    if (c.subjectPath.firstOrNull() == tp.name) {
                        c.constraint.collectOwnParams(used)
                    }
                }
                if (used.size != before) changed = true
            }
        }

        val keptParams = rawParams.filter { it in used }
        val keptNames = keptParams.mapTo(HashSet()) { it.name }
        val keptConstraints = translatedTypeParams.constraints.filter { c ->
            val head = c.subjectPath.firstOrNull()
            val cRefs = HashSet<SirTypeParameter>().also { c.constraint.collectOwnParams(it) }
            (head == null || head in keptNames) && cRefs.all { it in used }
        }
        keptParams to keptConstraints
    }

    internal val rawOwnTypeParameters: List<SirTypeParameter>
        get() = translatedTypeParams.typeParameters

    override val typeParameters: List<SirTypeParameter>
        get() = usedTypeParamsAndConstraints.first

    override val constraints: List<SirTypeConstraint>
        get() = usedTypeParamsAndConstraints.second

    internal val contextParameters: Pair<SirParameter, List<SirParameter>>? by lazy {
        translateContextParameters()
    }
    override val contextParameter: SirParameter? get() = contextParameters?.first
    override val extensionReceiverParameter: SirParameter? by lazy {
        translateExtensionParameter(liftedReceiverType = translatedTypeParams.liftedExtensionReceiverType)
    }
    override val parameters: List<SirParameter> by lazy {
        translateParameters(liftedParameterTypes = translatedTypeParams.liftedValueParameterTypes)
    }
    override val returnType: SirType by lazy {
        translateReturnType()
    }
    private val kdocElements: KDocElements? by lazyWithSessions {
        KDocElements(this)
    }
    override val documentation: String? by lazyWithSessions {
        translateDocumentation(kdocElements)
    }

    override var parent: SirDeclarationParent
        get() = withSessions {
            ktSymbol.getSirParent()
        }
        set(_) = Unit

    override val isOverride: Boolean get() = overrideStatus is OverrideStatus.Overrides

    private val overrideStatus: OverrideStatus<SirFunction>? by lazy { computeIsOverride() }

    override val isInstance: Boolean
        get() = !ktSymbol.isTopLevel && (ktSymbol as KaNamedFunctionSymbol).let { !it.isStatic }

    override val modality: SirModality
        get() = ktSymbol.modality.sirModality

    override val fixity: SirFixity?
        get() = null

    private val baseAttributes: List<SirAttribute> by lazy {
        buildList {
            addAll(this@SirFunctionFromKtSymbol.translatedAttributes)
            addIfNotNull(translatedDiscardableResultAttribute)
            if (overrideStatus is OverrideStatus.Conflicts) {
                add(SirAttribute.NonOverride)
            }
            replaceOrAddPropagatedUnavailability {
                buildList {
                    contextParameter?.type?.let(::add)
                    extensionReceiverParameter?.type?.let(::add)
                    addAll(parameters.map { it.type })
                    add(returnType)
                }.flatMap { it.unavailableTypes }
            }
            addDocumentationVisibility(kdocElements)
        }
    }

    private val isSelfUnavailable: Boolean
        get() = baseAttributes.any { it is SirAttribute.Available && it.unavailable }

    private val canRequestInlinable: Boolean by lazyWithSessions {
        val parentDecl = parent as? SirDeclaration
        val isGenericContext = typeParameters.isNotEmpty() ||
            (parent as? SirGenericDeclaration)?.typeParameters?.isNotEmpty() == true
        isGenericContext &&
            parent !is SirProtocol &&
            visibility == SirVisibility.PUBLIC &&
            modality == SirModality.FINAL &&
            !isAsync &&
            errorType == SirType.never &&
            contextParameter == null &&
            !isSelfUnavailable &&
            (parentDecl?.isUnavailable != true) &&
            (parentDecl?.attributes.orEmpty().none { it is SirAttribute.SPI || it is SirAttribute.Available }) &&
            baseAttributes.none { it is SirAttribute.SPI || it is SirAttribute.Available }
    }

    override val attributes: List<SirAttribute> by lazy {
        if (canRequestInlinable && bridgeProxy?.isSwiftInlinable == true) {
            baseAttributes + SirAttribute.Inlinable
        } else {
            baseAttributes
        }
    }

    override val errorType: SirType get() = if (ktSymbol.throwsAnnotation != null || isAsync) SirType.any else SirType.never

    override val isAsync: Boolean get() = ktSymbol.isSuspend

    private val bridgeProxy: BridgeFunctionProxy? by lazyWithSessions {
        if (isSelfUnavailable) return@lazyWithSessions null
        val fqName = bridgeFqName ?: return@lazyWithSessions null
        val suffix = ""
        val baseName = fqName.baseBridgeName + suffix

        val contextParameters = contextParameters?.second ?: emptyList()
        val extensionReceiverParameter = extensionReceiverParameter?.let {
            SirParameter(null, "receiver", it.type)
        }

        // For interface methods (and F-bounded class methods that override interfaces),
        // use the protocol existential as the self type.
        val effectiveSelfType = interfaceSelfType(ktSymbol) ?: selfType

        generateFunctionBridge(
            baseBridgeName = baseName,
            explicitParameters = listOfNotNull(extensionReceiverParameter) + parameters,
            returnType = returnType,
            kotlinFqName = fqName,
            kotlinOptIns = ktSymbol.allRequiredOptIns,
            selfParameter = (parent !is SirModule && isInstance).ifTrue {
                SirParameter(null, "self", effectiveSelfType ?: error("Only a member can have a self parameter"))
            },
            contextParameters = contextParameters,
            extensionReceiverParameter = extensionReceiverParameter,
            errorParameter = errorType.takeIf { it != SirType.never }?.let {
                SirParameter(null, "_out_error", it)
            },
            isAsync = isAsync,
            isInlinable = canRequestInlinable,
        )
    }

    context(_: KaSession, _: SirSession)
    private fun buildForwardKotlinCall(): BridgeFunctionBuilder.() -> String = {
        val enclosingClassSymbol = ktSymbol.containingDeclaration as? KaClassSymbol
        if (hasNonTrivialKotlinBridgeBounds(sirSession, enclosingClassSymbol, ktSymbol.typeParameters)) {
            buildLocalGenericBridgeCall(enclosingClassSymbol, ktSymbol, this@SirFunctionFromKtSymbol.returnType)
        } else {
            val typeArgs = ktSymbol.typeParameters.map { it.resolveUpperBound() ?: builtinTypes.nullableAny }
            val renderer = KaTypeRendererForSource.UPPER_BOUNDS_WITH_QUALIFIED_NAMES
            val typesAsString = typeArgs.takeIf { it.isNotEmpty() }?.joinToString(prefix = "<", postfix = ">") {
                it.render(renderer, position = Variance.INVARIANT)
            } ?: ""
            val actualArgs = argNames.drop(contextParameters.size + if (extensionReceiverParameter != null) 1 else 0)
            buildCall("$typesAsString(${actualArgs.joinToString()})")
        }
    }

    override val bridges: List<SirBridge> by lazyWithSessions {
        val forwardKotlinCall = buildForwardKotlinCall()

        val forwardBridges = bridgeProxy?.let { proxy ->
            buildList {
                addAll(proxy.createSirBridges(nonVirtualTargetMethod = null, forwardKotlinCall))
                val ktSymbol = this@SirFunctionFromKtSymbol.ktSymbol
                if (needsNonVirtualForwardBridge() && !isAbstractKotlinMethod && ktSymbol is KaNamedSymbol) {
                    add(proxy.createDirectDispatchForwardBridge(ktSymbol.name.asString(), forwardKotlinCall))
                }
            }
        }.orEmpty()

        val reverseBridges = if (needsReverseBridge()) {
            bridgeProxy?.createReverseSirBridges(
                targetClassFqName = ktSymbol.reverseBridgeTargetClassFqName,
                targetMethodName = ktSymbol.name?.asString() ?: "",
                swiftDynamicCall = this@SirFunctionFromKtSymbol::reverseBridgeSwiftCall,
                swiftDeprecation = effectiveReverseBridgeDeprecation(),
                openExistentialProtocol = openExistentialProtocolForReverseBridge(),
            ).orEmpty()
        } else {
            emptyList()
        }

        forwardBridges + reverseBridges
    }

    private val isAbstractKotlinMethod: Boolean
        get() = ktSymbol.modality == KaSymbolModality.ABSTRACT

    override var body: SirFunctionBody?
        set(_) {}
        get() = withSessions { bridgeProxy?.let { forwardBridgedBody(it, isAbstractKotlinMethod, memberKind = "member", memberName = name) } }

    internal fun directDispatchProtocolWitnessOrNull(): SirFunction? = withSessions {
        if (parent !is SirProtocol) return@withSessions null
        if (!isInstance || isUnavailable || isAbstractKotlinMethod) return@withSessions null
        if (!needsReverseBridge()) return@withSessions null
        val proxy = bridgeProxy ?: return@withSessions null
        val named = ktSymbol as? KaNamedSymbol ?: return@withSessions null

        val forwardKotlinCall = buildForwardKotlinCall()
        buildFunctionCopy(this@SirFunctionFromKtSymbol) {
            origin = SirOrigin.Trampoline(this@SirFunctionFromKtSymbol)
            isOverride = false
            modality = SirModality.UNSPECIFIED
            bridges.clear()
            bridges.add(proxy.createDirectDispatchForwardBridge(named.name.asString(), forwardKotlinCall))
            body = SirFunctionBody(proxy.createSwiftInvocation(useDirectDispatch = true) { "return $it" })
        }
    }
}
