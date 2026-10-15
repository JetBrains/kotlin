/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.symbols.KaPropertyAccessorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertyGetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.symbols.allOverriddenSymbols
import org.jetbrains.kotlin.analysis.api.symbols.containingSymbol
import org.jetbrains.kotlin.analysis.api.symbols.isTopLevel
import org.jetbrains.kotlin.name.render
import org.jetbrains.kotlin.sir.SirAttribute
import org.jetbrains.kotlin.sir.SirBridge
import org.jetbrains.kotlin.sir.SirClass
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirDeclarationParent
import org.jetbrains.kotlin.sir.SirExistentialType
import org.jetbrains.kotlin.sir.SirFixity
import org.jetbrains.kotlin.sir.SirFunction
import org.jetbrains.kotlin.sir.SirFunctionBody
import org.jetbrains.kotlin.sir.SirGetterFunction
import org.jetbrains.kotlin.sir.SirModality
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.SirOrigin
import org.jetbrains.kotlin.sir.SirParameter
import org.jetbrains.kotlin.sir.SirProtocol
import org.jetbrains.kotlin.sir.SirSetterFunction
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.builder.buildFunctionCopy
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.generateFunctionBridge
import org.jetbrains.kotlin.sir.providers.getSirParent
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.BridgeFunctionBuilder
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.BridgeFunctionProxy
import org.jetbrains.kotlin.sir.providers.sirDeclarationName
import org.jetbrains.kotlin.sir.providers.source.KotlinPropertyAccessorOrigin
import org.jetbrains.kotlin.sir.providers.source.kaSymbolOrNull
import org.jetbrains.kotlin.sir.providers.utils.allRequiredOptIns
import org.jetbrains.kotlin.sir.providers.utils.throwsAnnotation
import org.jetbrains.kotlin.sir.util.isUnavailable
import org.jetbrains.kotlin.sir.util.swiftFqName
import org.jetbrains.kotlin.sir.util.swiftIdentifier
import org.jetbrains.kotlin.sir.util.unavailableTypes
import org.jetbrains.kotlin.sir.util.replaceOrAddPropagatedUnavailability
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.lazyWithSessions
import org.jetbrains.sir.lightclasses.extensions.sirModality
import org.jetbrains.sir.lightclasses.extensions.withSessions
import org.jetbrains.sir.lightclasses.utils.KDocElements
import org.jetbrains.sir.lightclasses.utils.OverrideStatus
import org.jetbrains.sir.lightclasses.utils.baseBridgeName
import org.jetbrains.sir.lightclasses.utils.computeIsOverride
import org.jetbrains.sir.lightclasses.utils.hasFBoundedTypeParameters
import org.jetbrains.sir.lightclasses.utils.addDocumentationVisibility
import org.jetbrains.sir.lightclasses.utils.selfType
import org.jetbrains.sir.lightclasses.utils.translateContextParameters
import org.jetbrains.sir.lightclasses.utils.translateDocumentation
import org.jetbrains.sir.lightclasses.utils.translateExtensionParameter
import org.jetbrains.sir.lightclasses.utils.translateParameters
import org.jetbrains.sir.lightclasses.utils.translateReturnType
import org.jetbrains.sir.lightclasses.utils.translatedAttributes
import kotlin.getValue
import kotlin.lazy

internal class SirGetterFunctionFromKtSymbol(
    override val ktPropertySymbol: KaPropertySymbol,
    override val ktSymbol: KaPropertyGetterSymbol,
    override val sirSession: SirSession,
) : SirFunctionFromKtPropertySymbol(), SirGetterFunction {
    override val getter: SirGetterFunctionFromKtSymbol get() = this
    override var setter: SirSetterFunctionFromKtSymbol? = null
}

internal class SirSetterFunctionFromKtSymbol(
    override val ktPropertySymbol: KaPropertySymbol,
    override val ktSymbol: KaPropertySetterSymbol,
    override val sirSession: SirSession,
    override val getter: SirGetterFunctionFromKtSymbol,
) : SirFunctionFromKtPropertySymbol(), SirSetterFunction {
    override val setter: SirSetterFunctionFromKtSymbol get() = this
}

/**
 * Kotlin extension property accessors in Swift
 */
internal sealed class SirFunctionFromKtPropertySymbol : SirFunction(), SirFromKtSymbol<KaPropertyAccessorSymbol> {
    abstract val ktPropertySymbol: KaPropertySymbol

    override val visibility: SirVisibility = SirVisibility.PUBLIC
    override val origin: SirOrigin by lazy {
        KotlinPropertyAccessorOrigin(ktSymbol, ktPropertySymbol)
    }
    val variableName: String by lazyWithSessions {
        ktPropertySymbol.sirDeclarationName()
    }
    override val name: String by lazy {
        val prefix = when (ktSymbol) {
            is KaPropertyGetterSymbol -> "get"
            is KaPropertySetterSymbol -> "set"
        }
        prefix + variableName.replaceFirstChar { it.titlecase() }
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
            ktPropertySymbol.getSirParent()
        }
        set(_) = Unit

    override val isOverride: Boolean get() = overrideStatus is OverrideStatus.Overrides

    private val overrideStatus: OverrideStatus<SirFunction>? by lazy { computeIsOverride() }

    override val isInstance: Boolean
        get() = !ktPropertySymbol.isTopLevel && !ktPropertySymbol.isStatic

    override val modality: SirModality
        get() = ktPropertySymbol.modality.sirModality

    override val fixity: SirFixity?
        get() = null

    override val attributes: List<SirAttribute> by lazy {
        buildList {
            addAll(this@SirFunctionFromKtPropertySymbol.translatedAttributes)
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

    override val errorType: SirType get() = if (ktPropertySymbol.throwsAnnotation != null) SirType.any else SirType.never

    override val isAsync: Boolean get() = false

    private val bridgeProxy: BridgeFunctionProxy? by lazyWithSessions {
        if (isUnavailable) return@lazyWithSessions null
        val fqName = ktPropertySymbol
            .callableId?.asSingleFqName() ?: return@lazyWithSessions null

        val suffix = when (ktSymbol) {
            is KaPropertyGetterSymbol -> "_get"
            is KaPropertySetterSymbol -> "_set"
        }

        val baseName = fqName.baseBridgeName + suffix

        val contextParameters = contextParameters?.second ?: emptyList()
        val extensionReceiverParameter = extensionReceiverParameter?.let {
            SirParameter(null, "receiver", it.type)
        }

        generateFunctionBridge(
            baseBridgeName = baseName,
            explicitParameters = listOfNotNull(extensionReceiverParameter) + parameters,
            returnType = returnType,
            kotlinFqName = fqName,
            kotlinOptIns = ktSymbol.allRequiredOptIns,
            selfParameter = (parent !is SirModule && isInstance).ifTrue {
                val effectiveSelfType = computeInterfaceSelfType() ?: selfType
                SirParameter(null, "self", effectiveSelfType ?: error("Only a member can have a self parameter"))
            },
            contextParameters = contextParameters,
            extensionReceiverParameter = extensionReceiverParameter,
            errorParameter = errorType.takeIf { it != SirType.never }?.let {
                SirParameter(null, "_out_error", it)
            },
            isAsync = false,
        )
    }

    private val forwardCall: BridgeFunctionBuilder.() -> String = {
        val args = argNames
        when (ktSymbol) {
            is KaPropertyGetterSymbol -> {
                val expectedParameters = (if (extensionReceiverParameter != null) 1 else 0) + contextParameters.size
                require(args.size == expectedParameters) { "Received an extension getter $name with ${args.size} parameters instead of a $expectedParameters, aborting" }
                buildCall("")
            }
            is KaPropertySetterSymbol -> {
                val contextParameterCount = contextParameters.size
                val expectedParameters = (if (extensionReceiverParameter != null) 2 else 1) + contextParameterCount
                require(args.size == expectedParameters) { "Received an extension getter $name with ${args.size} parameters instead of a $expectedParameters, aborting" }
                buildCall(" = ${args.last()}")
            }
        }
    }

    /** The name of the Kotlin accessor, e.g. `<get-foo>`. */
    private val kotlinAccessorName: String
        get() = when (ktSymbol) {
            is KaPropertyGetterSymbol -> "<get-${ktPropertySymbol.name.asString()}>"
            is KaPropertySetterSymbol -> "<set-${ktPropertySymbol.name.asString()}>"
        }

    private val isAbstractKotlinProperty: Boolean
        get() = ktPropertySymbol.modality == KaSymbolModality.ABSTRACT

    override val bridges: List<SirBridge> by lazyWithSessions {
        val proxy = bridgeProxy ?: return@lazyWithSessions emptyList()
        buildList {
            addAll(proxy.createSirBridges(nonVirtualTargetMethod = null, forwardCall))
            if (needsNonVirtualForwardBridge() && !isAbstractKotlinProperty) {
                add(proxy.createDirectDispatchForwardBridge(kotlinAccessorName, forwardCall))
            }
            if (needsReverseBridge()) {
                val params = listOfNotNull(contextParameter, extensionReceiverParameter) + parameters
                addAll(
                    proxy.createReverseSirBridges(
                        targetClassFqName = (ktPropertySymbol.containingSymbol as? KaNamedClassSymbol)
                            ?.classId?.asSingleFqName()?.render() ?: "",
                        targetMethodName = kotlinAccessorName,
                        swiftDynamicCall = { selfExpr, paramExprs ->
                            check(paramExprs.size == params.size) { "Parameter expression count doesn't match parameter count" }
                            val tryPrefix = if (errorType != SirType.never) "try " else ""
                            val args = params.zip(paramExprs).joinToString(", ") { [param, expr] ->
                                param.argumentName?.takeIf { it.isNotEmpty() }?.let { "${it.swiftIdentifier}: $expr" } ?: expr
                            }
                            "$tryPrefix$selfExpr.${name.swiftIdentifier}($args)"
                        },
                        swiftDeprecation = effectiveReverseBridgeDeprecation(),
                    )
                )
            }
        }
    }

    override var body: SirFunctionBody?
        set(value) {}
        get() = withSessions {
            val proxy = bridgeProxy ?: return@withSessions null
            if (!needsNonVirtualForwardBridge()) {
                return@withSessions SirFunctionBody(proxy.createSwiftInvocation { "return $it" })
            }
            val wrapperFqName = (parent as SirClass).swiftFqName
            val virtualLines = proxy.createSwiftInvocation { "return $it" }
            val fallbackLines = if (isAbstractKotlinProperty) {
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

    internal fun directDispatchProtocolWitnessOrNull(): SirFunction? = withSessions {
        if (parent !is SirProtocol) return@withSessions null
        if (!isInstance || isUnavailable || isAbstractKotlinProperty) return@withSessions null
        if (!needsReverseBridge()) return@withSessions null
        val proxy = bridgeProxy ?: return@withSessions null

        buildFunctionCopy(this@SirFunctionFromKtPropertySymbol) {
            origin = SirOrigin.Trampoline(this@SirFunctionFromKtPropertySymbol)
            isOverride = false
            modality = SirModality.UNSPECIFIED
            bridges.clear()
            bridges.add(proxy.createDirectDispatchForwardBridge(kotlinAccessorName, forwardCall))
            body = SirFunctionBody(proxy.createSwiftInvocation(useDirectDispatch = true) { "return $it" })
        }
    }

    private fun needsReverseBridge(): Boolean {
        if (!isInstance || isUnavailable) return false
        return when (val container = parent) {
            is SirClass -> modality == SirModality.OPEN && container.modality == SirModality.OPEN && !container.isUnavailable
            is SirProtocol -> !container.isUnavailable
            else -> false
        }
    }

    private fun needsNonVirtualForwardBridge(): Boolean = needsReverseBridge() && parent is SirClass

    private fun effectiveReverseBridgeDeprecation(): SirAttribute.Available? {
        fun SirDeclaration.deprecatedAttr(): SirAttribute.Available? =
            attributes.firstOrNull { it is SirAttribute.Available && it.deprecated } as? SirAttribute.Available
        return this.deprecatedAttr()
            ?: (parent as? SirClass)?.deprecatedAttr()
            ?: (parent as? SirProtocol)?.deprecatedAttr()
    }

    private fun computeInterfaceSelfType(): SirType? = withSessions {
        if (!isInstance) return@withSessions null
        (parent as? SirProtocol)?.let { return@withSessions SirExistentialType(it) }

        val containingClass = (parent as? SirClass)?.kaSymbolOrNull<KaClassSymbol>() ?: return@withSessions null
        if (!containingClass.hasFBoundedTypeParameters()) return@withSessions null

        val interfaceSymbol = ktPropertySymbol.allOverriddenSymbols
            .firstOrNull { (it.containingSymbol as? KaClassSymbol)?.classKind == KaClassKind.INTERFACE }
            ?.containingSymbol as? KaNamedClassSymbol ?: return@withSessions null

        with(sirSession) { interfaceSymbol.toSir().allDeclarations }.firstIsInstanceOrNull<SirProtocol>()?.let(::SirExistentialType)
    }
}
