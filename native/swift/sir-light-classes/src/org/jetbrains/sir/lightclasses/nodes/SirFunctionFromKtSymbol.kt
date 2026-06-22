/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.components.allOverriddenSymbols
import org.jetbrains.kotlin.analysis.api.components.builtinTypes
import org.jetbrains.kotlin.analysis.api.components.containingSymbol
import org.jetbrains.kotlin.analysis.api.components.render
import org.jetbrains.kotlin.analysis.api.export.utilities.isSuspend
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KaTypeRendererForSource
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirTypeNamer.KotlinNameType
import org.jetbrains.kotlin.sir.providers.generateFunctionBridge
import org.jetbrains.kotlin.sir.providers.getSirParent
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.BridgeFunctionProxy
import org.jetbrains.kotlin.sir.providers.sirDeclarationName
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.source.kaSymbolOrNull
import org.jetbrains.kotlin.sir.providers.toSir
import org.jetbrains.kotlin.sir.providers.utils.allRequiredOptIns
import org.jetbrains.kotlin.sir.providers.utils.throwsAnnotation
import org.jetbrains.kotlin.sir.util.isUnavailable
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.sir.util.isUnavailable
import org.jetbrains.kotlin.sir.util.swiftFqName
import org.jetbrains.kotlin.sir.util.unavailableTypes
import org.jetbrains.kotlin.sir.util.replaceOrAddPropagatedUnavailability
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.BridgeFunctionBuilder
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.*
import org.jetbrains.sir.lightclasses.extensions.documentation
import org.jetbrains.sir.lightclasses.utils.*
import org.jetbrains.sir.lightclasses.utils.translateParameters
import org.jetbrains.sir.lightclasses.utils.translateReturnType
import kotlin.lazy

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
    private val contextParameters: Pair<SirParameter, List<SirParameter>>? by lazy {
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
    override val documentation: String? by lazyWithSessions {
        ktSymbol.documentation()
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
        val effectiveSelfType = computeInterfaceSelfType() ?: selfType

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

    override val bridges: List<SirBridge> by lazyWithSessions {
        val forwardKotlinCall: BridgeFunctionBuilder.() -> String = {
            val typeArgs = ktSymbol.typeParameters.map { it.upperBounds.singleOrNull() ?: builtinTypes.nullableAny }
            val renderer = KaTypeRendererForSource.UPPER_BOUNDS_WITH_QUALIFIED_NAMES
            val typesAsString = typeArgs.takeIf { it.isNotEmpty() }?.joinToString(prefix = "<", postfix = ">") {
                it.render(renderer, position = Variance.INVARIANT)
            } ?: ""
            val actualArgs = argNames.drop(if (extensionReceiverParameter != null) 1 else 0).dropLast(contextParameters.size)
            val argumentsString = actualArgs.joinToString()

            buildCall("$typesAsString($argumentsString)")
        }

        val forwardBridges = bridgeProxy?.let { proxy ->
            buildList {
                addAll(proxy.createSirBridges(forwardKotlinCall))
                val ktSymbol = this@SirFunctionFromKtSymbol.ktSymbol
                if (needsNonVirtualForwardBridge() && !isAbstractKotlinMethod && ktSymbol is KaNamedSymbol) {
                    add(proxy.createDirectDispatchForwardBridge(ktSymbol.name.asString(), forwardKotlinCall))
                }
            }
        }.orEmpty()

        val reverseBridges = if (needsReverseBridge()) {
            bridgeProxy?.createReverseSirBridges(
                targetClassFqName = (ktSymbol as? KaNamedFunctionSymbol)
                    ?.containingSymbol?.let { (it as? KaNamedClassSymbol)?.classId?.asSingleFqName()?.asString() }
                    ?: "",
                targetMethodName = ktSymbol.name?.asString() ?: "",
                swiftDynamicCall = { selfExpr, paramExprs ->
                    val methodName = this@SirFunctionFromKtSymbol.name
                    val args = this@SirFunctionFromKtSymbol.parameters
                        .zip(paramExprs)
                        .joinToString(", ") { [param, expr] ->
                            param.argumentName?.takeIf { it.isNotEmpty() }?.let { "$it: $expr" } ?: expr
                        }
                    val tryPrefix = if (errorType != SirType.never) "try! " else ""
                    "$tryPrefix$selfExpr.$methodName($args)"
                },
                swiftDeprecation = effectiveReverseBridgeDeprecation(),
            ).orEmpty()
        } else {
            emptyList()
        }

        forwardBridges + reverseBridges
    }

    private fun needsNonVirtualForwardBridge(): Boolean = withSessions {
        needsReverseBridge() && parent is SirClass
    }

    private val isAbstractKotlinMethod: Boolean
        get() = ktSymbol.modality == KaSymbolModality.ABSTRACT

    private fun needsReverseBridge(): Boolean = withSessions {
        if (!isInstance) return@withSessions false
        if (isUnavailable) return@withSessions false
        // TODO: Implement async reverse bridges with regular continuation machinery.
        if (isAsync) return@withSessions false
        when (val containingDecl = parent) {
            is SirClass -> {
                if (modality != SirModality.OPEN) return@withSessions false
                if (containingDecl.modality != SirModality.OPEN) return@withSessions false
                if (containingDecl.isUnavailable) return@withSessions false
                return@withSessions true
            }
            is SirProtocol -> {
                if (containingDecl.isUnavailable) return@withSessions false
                return@withSessions true
            }
            else -> return@withSessions false
        }
    }

    private fun effectiveReverseBridgeDeprecation(): SirAttribute.Available? {
        fun SirDeclaration.deprecatedAttr(): SirAttribute.Available? =
            attributes.firstOrNull { it is SirAttribute.Available && it.deprecated } as? SirAttribute.Available
        return this.deprecatedAttr()
            ?: (parent as? SirClass)?.deprecatedAttr()
            ?: (parent as? SirProtocol)?.deprecatedAttr()
    }

    /**
     * Computes the self SirType for interface methods (covering both direct protocol parents
     * and F-bounded class methods overriding interface methods). Returns `SirExistentialType(proto)`
     * so that the bridge uses `AsExistential` — whose kotlinToSwift conversion produces
     * `KotlinBase.__createProtocolWrapper(externalRCRef:) as! Foo`, required for reverse bridges
     * where the concrete Swift conformer is unknown at compile time.
     *
     * Returns null if not applicable (e.g., plain class method with no interface origin).
     */
    private fun computeInterfaceSelfType(): SirType? = withSessions {
        if (!isInstance) return@withSessions null

        (parent as? SirProtocol)?.let { return@withSessions SirExistentialType(it) }

        val containingClass = (parent as? SirClass)?.kaSymbolOrNull<KaClassSymbol>() ?: return@withSessions null
        if (!containingClass.hasFBoundedTypeParameters()) return@withSessions null

        val overriddenInterfaceMethod = ktSymbol.allOverriddenSymbols
            .filterIsInstance<KaNamedFunctionSymbol>()
            .firstOrNull { overridden ->
                val containingSymbol = overridden.containingSymbol
                containingSymbol is KaClassSymbol && containingSymbol.classKind == KaClassKind.INTERFACE
            } ?: return@withSessions null

        val interfaceSymbol = overriddenInterfaceMethod.containingSymbol as? KaNamedClassSymbol
            ?: return@withSessions null

        val sirProtocol = interfaceSymbol.toSir().allDeclarations.firstIsInstanceOrNull<SirProtocol>()
            ?: return@withSessions null

        SirExistentialType(sirProtocol)
    }

    override var body: SirFunctionBody?
        set(_) {}
        get() = withSessions {
            val proxy = bridgeProxy ?: return@withSessions null
            if (!needsNonVirtualForwardBridge()) {
                return@withSessions SirFunctionBody(proxy.createSwiftInvocation { "return $it" })
            }
            val wrapperFqName = (parent as SirClass).swiftFqName
            val virtualLines = proxy.createSwiftInvocation { "return $it" }
            val fallbackLines = if (isAbstractKotlinMethod) {
                listOf("fatalError(\"Cannot invoke the inherited implementation of abstract member '$wrapperFqName.$name': a Swift subclass must override it and must not call super.\")")
            } else {
                proxy.createSwiftInvocation(useDirectDispatch = true) { "return $it" }
            }
            SirFunctionBody(buildList {
                add("if Self.self == $wrapperFqName.self {")
                virtualLines.forEach { add("    $it") }
                add("} else {")
                fallbackLines.forEach { add("    $it") }
                add("}")
            })
        }
}
