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
    internal val contextParameters: Pair<SirParameter, List<SirParameter>>? by lazy {
        translateContextParameters()
    }
    override val contextParameter: SirParameter? get() = contextParameters?.first
    override val extensionReceiverParameter: SirParameter? by lazy {
        translateExtensionParameter()
    }
    override val parameters: List<SirParameter> by lazy {
        translateParameters()
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

    override val attributes: List<SirAttribute> by lazy {
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

    override val errorType: SirType get() = if (ktSymbol.throwsAnnotation != null || isAsync) SirType.any else SirType.never

    override val isAsync: Boolean get() = ktSymbol.isSuspend

    private val bridgeProxy: BridgeFunctionProxy? by lazyWithSessions {
        if (isUnavailable) return@lazyWithSessions null
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
        )
    }

    context(_: KaSession, _: SirSession)
    private fun buildForwardKotlinCall(): BridgeFunctionBuilder.() -> String = {
        val typeArgs = ktSymbol.typeParameters.map { it.resolveUpperBound() ?: builtinTypes.nullableAny }
        val renderer = KaTypeRendererForSource.UPPER_BOUNDS_WITH_QUALIFIED_NAMES
        val typesAsString = typeArgs.takeIf { it.isNotEmpty() }?.joinToString(prefix = "<", postfix = ">") {
            it.render(renderer, position = Variance.INVARIANT)
        } ?: ""
        val actualArgs = argNames.drop(contextParameters.size + if (extensionReceiverParameter != null) 1 else 0)
        buildCall("$typesAsString(${actualArgs.joinToString()})")
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
