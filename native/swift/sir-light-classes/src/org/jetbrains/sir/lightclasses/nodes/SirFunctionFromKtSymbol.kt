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
import org.jetbrains.kotlin.analysis.api.symbols.*
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
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.sir.util.unavailableTypes
import org.jetbrains.kotlin.sir.util.replaceOrAddPropagatedUnavailability
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
        val fqName = bridgeFqName ?: return@lazyWithSessions null
        val suffix = ""
        val baseName = fqName.baseBridgeName + suffix

        val contextParameters = contextParameters?.second ?: emptyList()
        val extensionReceiverParameter = extensionReceiverParameter?.let {
            SirParameter("", "receiver", it.type)
        }

        // For interface methods (and F-bounded class methods that override interfaces),
        // use the protocol existential as the self type. This routes both forward and reverse
        // bridges through `AsExistential`, whose kotlinToSwift expansion produces
        // `KotlinBase.__createProtocolWrapper(externalRCRef:) as! Foo` — required for the reverse
        // bridge, where the concrete Swift class is unknown at compile time.
        val effectiveSelfType = computeInterfaceSelfType() ?: selfType

        generateFunctionBridge(
            baseBridgeName = baseName,
            explicitParameters = listOfNotNull(extensionReceiverParameter) + parameters,
            returnType = returnType,
            kotlinFqName = fqName,
            kotlinOptIns = ktSymbol.allRequiredOptIns,
            selfParameter = (parent !is SirModule && isInstance).ifTrue {
                SirParameter("", "self", effectiveSelfType ?: error("Only a member can have a self parameter"))
            },
            contextParameters = contextParameters,
            extensionReceiverParameter = extensionReceiverParameter,
            errorParameter = errorType.takeIf { it != SirType.never }?.let {
                SirParameter("", "_out_error", it)
            },
            isAsync = isAsync,
        )
    }

    override val bridges: List<SirBridge> by lazyWithSessions {
        val forwardBridges = bridgeProxy?.createSirBridges {
            val typeArgs = ktSymbol.typeParameters.map { it.upperBounds.singleOrNull() ?: builtinTypes.nullableAny }
            val typesAsString = typeArgs.takeIf { it.isNotEmpty() }?.joinToString(prefix = "<", postfix = ">") {
                it.render(position = Variance.INVARIANT)
            } ?: ""
            val actualArgs = argNames.drop(if (extensionReceiverParameter != null) 1 else 0).dropLast(contextParameters.size)
            val argumentsString = actualArgs.joinToString()

            buildCall("$typesAsString($argumentsString)")
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
                        .joinToString(", ") { (param, expr) ->
                            // A missing/empty argumentName maps to Swift's `_` label — call
                            // positionally without a prefix. A non-empty one uses `label: value`.
                            val argLabel = param.argumentName?.takeIf { it.isNotEmpty() }
                            if (argLabel != null) "$argLabel: $expr" else expr
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

    private fun needsReverseBridge(): Boolean = withSessions {
        if (!isInstance) return@withSessions false
        if (attributes.any { it is SirAttribute.Available && it.unavailable }) return@withSessions false
        // Suspend/async methods bridge to Swift `async throws`, which can't be invoked from the
        // non-async `@_cdecl` reverse trampoline without full continuation machinery — out of scope.
        if (isAsync) return@withSessions false
        when (val containingDecl = parent) {
            is SirClass -> {
                if (modality != SirModality.OPEN) return@withSessions false
                if (containingDecl.modality != SirModality.OPEN) return@withSessions false
                if (containingDecl.attributes.any { it is SirAttribute.Available && it.unavailable }) return@withSessions false
                return@withSessions true
            }
            is SirProtocol -> {
                // Every method of an exported Kotlin interface gets a reverse-bridge trampoline
                // so that Swift classes conforming to the protocol can override it. Modality check
                // doesn't apply — interface methods are open by definition.
                if (containingDecl.attributes.any { it is SirAttribute.Available && it.unavailable }) return@withSessions false
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

        // Case 1: function is declared directly on an exported interface.
        (parent as? SirProtocol)?.let { return@withSessions SirExistentialType(it) }

        // Case 2: F-bounded class method that overrides an interface method. Resolve the interface
        // and use its existential — avoids a double cast (class-then-interface) in the bridge.
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
        get() = withSessions { bridgeProxy?.createSwiftInvocation { "return $it" }?.let(::SirFunctionBody) }
}
