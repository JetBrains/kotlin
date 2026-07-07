/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.allOverriddenSymbols
import org.jetbrains.kotlin.analysis.api.symbols.containingSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildGetterCopy
import org.jetbrains.kotlin.sir.builder.buildSetterCopy
import org.jetbrains.kotlin.sir.builder.buildVariableCopy
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.generateFunctionBridge
import org.jetbrains.kotlin.sir.providers.getSirParent
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.BridgeFunctionBuilder
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.BridgeFunctionProxy
import org.jetbrains.kotlin.sir.providers.sirDeclarationName
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.source.kaSymbolOrNull
import org.jetbrains.kotlin.sir.providers.utils.allRequiredOptIns
import org.jetbrains.kotlin.sir.providers.utils.throwsAnnotation
import org.jetbrains.kotlin.sir.providers.withSessions
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.sir.util.isUnavailable
import org.jetbrains.kotlin.sir.util.swiftFqName
import org.jetbrains.kotlin.sir.util.unavailableTypes
import org.jetbrains.kotlin.sir.util.replaceOrAddPropagatedUnavailability
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.*
import org.jetbrains.sir.lightclasses.extensions.documentation
import org.jetbrains.sir.lightclasses.utils.*
import org.jetbrains.sir.lightclasses.utils.translateReturnType
import org.jetbrains.sir.lightclasses.utils.translatedAttributes
import kotlin.lazy

internal abstract class SirAbstractVariableFromKtSymbol(
    override val ktSymbol: KaVariableSymbol,
    override val sirSession: SirSession,
) : SirVariable(), SirFromKtSymbol<KaVariableSymbol> {
    private class DefaultGetter(
        override val ktSymbol: KaVariableSymbol,
        sirSession: SirSession,
    ) : SirAbstractGetter(sirSession), SirFromKtSymbol<KaVariableSymbol> {
        override val origin: SirOrigin by lazy { KotlinSource(ktSymbol) }
        override val attributes: List<SirAttribute> by lazy { this.translatedAttributes }
        override val errorType: SirType get() = if (ktSymbol.throwsAnnotation != null) SirType.any else SirType.never
    }

    private class DefaultSetter(
        override val ktSymbol: KaVariableSymbol,
        sirSession: SirSession,
    ) : SirAbstractSetter(sirSession), SirFromKtSymbol<KaVariableSymbol> {
        override val origin: SirOrigin by lazy { KotlinSource(ktSymbol) }
        override val attributes: List<SirAttribute> by lazy { this.translatedAttributes }
        override val errorType: SirType get() = if (ktSymbol.throwsAnnotation != null) SirType.any else SirType.never
    }

    override val visibility: SirVisibility = SirVisibility.PUBLIC

    override val origin: SirOrigin by lazy {
        KotlinSource(ktSymbol)
    }
    override val name: String by lazyWithSessions {
        ktSymbol.sirDeclarationName()
    }
    override val type: SirType by lazy {
        if (ktSymbol.isVal) {
            translateReturnType()
        } else {
            translateInvariantType()
        }
    }
    override val getter: SirGetter by lazy {
        ((ktSymbol as? KaPropertySymbol)?.let {
            it.getter?.let {
                SirGetterFromKtSymbol(it, sirSession)
            }
        } ?: DefaultGetter(ktSymbol, sirSession)).also {
            it.parent = this@SirAbstractVariableFromKtSymbol
        }
    }
    override val setter: SirSetter? by lazy {
        (ktSymbol as? KaPropertySymbol)
            ?.takeIf { it.setter?.visibility == KaSymbolVisibility.PUBLIC }
            ?.let {
                it.setter?.let { SirSetterFromKtSymbol(it, sirSession) }
                    ?: if (!it.isVal) DefaultSetter(it, sirSession) else null
            }
            ?.apply { parent = this@SirAbstractVariableFromKtSymbol }
    }
    override val documentation: String? by lazy {
        ktSymbol.documentation()
    }

    override var parent: SirDeclarationParent
        get() = withSessions {
            ktSymbol.getSirParent()
        }
        set(_) = Unit

    override val attributes: List<SirAttribute> by lazy {
        buildList {
            addAll(this@SirAbstractVariableFromKtSymbol.translatedAttributes)
            if (overrideStatus is OverrideStatus.Conflicts) {
                add(SirAttribute.NonOverride)
            }
            replaceOrAddPropagatedUnavailability { type.unavailableTypes }
        }
    }

    override val isOverride: Boolean
        get() = overrideStatus is OverrideStatus.Overrides

    private val overrideStatus: OverrideStatus<SirVariable>? by lazy { computeIsOverride() }

    override val modality: SirModality
        get() = ktSymbol.modality.sirModality

    override val bridges: List<SirBridge> = emptyList()

    context(_: KaSession, _: SirSession)
    internal fun directDispatchProtocolWitnessOrNull(): SirVariable? {
        if (parent !is SirProtocol) return null
        if (!isInstance || isUnavailable || isAbstractKotlinProperty) return null
        if (!accessorNeedsReverseBridge()) return null
        val getterWitness = (getter as? SirAbstractGetter)?.directWitnessBridgeAndBody() ?: return null
        val setterWitness = (setter as? SirAbstractSetter)?.directWitnessBridgeAndBody()
        if (setter != null && setterWitness == null) return null
        val original = this
        return buildVariableCopy(original) {
            origin = SirOrigin.Trampoline(original)
            isOverride = false
            modality = SirModality.UNSPECIFIED
            bridges.clear()
            getter = buildGetterCopy(original.getter) {
                origin = SirOrigin.Trampoline(original.getter)
                bridges.clear()
                bridges.add(getterWitness.first)
                body = getterWitness.second
            }
            setter = original.setter?.let { originalSetter ->
                buildSetterCopy(originalSetter) {
                    origin = SirOrigin.Trampoline(originalSetter)
                    bridges.clear()
                    bridges.add(setterWitness!!.first)
                    body = setterWitness.second
                }
            }
        }.apply {
            getter?.parent = this
            setter?.parent = this
        }
    }
}

internal class SirVariableFromKtSymbol(
    ktSymbol: KaVariableSymbol,
    sirSession: SirSession,
) : SirAbstractVariableFromKtSymbol(ktSymbol, sirSession) {
    override val isInstance: Boolean
        get() = !ktSymbol.isTopLevel && !(ktSymbol is KaPropertySymbol && ktSymbol.isStatic)
    override val isConstant: Boolean get() = false
}

internal abstract class SirAbstractGetter(
    val sirSession: SirSession,
) : SirGetter() {
    override lateinit var parent: SirDeclarationParent
    override val visibility: SirVisibility get() = SirVisibility.PUBLIC
    override val documentation: String? get() = null
    override val attributes: List<SirAttribute> get() = emptyList()
    override val errorType: SirType get() = SirType.never
    override val isAsync: Boolean get() = false
    private val variable get() = parent as? SirVariable

    open val fqName: FqName? by lazyWithSessions {
        variable?.kaSymbolOrNull<KaVariableSymbol>()
            ?.callableId?.asSingleFqName()
    }

    private val bridgeProxy: BridgeFunctionProxy? by lazyWithSessions {
        val suffix = "_get"
        val variable = variable?.takeUnless { it.isUnavailable } ?: return@lazyWithSessions null
        val fqName = fqName ?: return@lazyWithSessions null
        val baseName = fqName.baseBridgeName + suffix
        val getterSymbol = variable.kaSymbolOrNull<KaPropertySymbol>()?.getter ?: variable.kaSymbolOrNull<KaVariableSymbol>()

        generateFunctionBridge(
            baseBridgeName = baseName,
            explicitParameters = emptyList(),
            returnType = variable.type,
            kotlinFqName = fqName,
            kotlinOptIns = getterSymbol?.allRequiredOptIns ?: emptyList(),
            selfParameter = (variable.parent !is SirModule && variable.isInstance).ifTrue {
                val effectiveSelfType = variable.computeInterfaceSelfType() ?: selfType
                SirParameter(null, "self", effectiveSelfType ?: error("Only a member can have a self parameter"))
            },
            contextParameters = emptyList(),
            extensionReceiverParameter = null,
            errorParameter = errorType.takeIf { it != SirType.never }?.let {
                SirParameter(null, "_out_error", it)
            },
            isAsync = false,
        )
    }

    private val forwardCall: BridgeFunctionBuilder.() -> String = {
        val args = argNames
        val expectedParameters = if (extensionReceiverParameter != null) 1 else 0
        require(args.size == expectedParameters) { "Received an extension getter $name with ${args.size} parameters instead of a $expectedParameters, aborting" }
        buildCall("")
    }

    override val bridges: List<SirBridge> by lazyWithSessions {
        val proxy = bridgeProxy ?: return@lazyWithSessions emptyList()
        val variable = variable
        val propName = variable?.kotlinPropertyName
        buildList {
            addAll(proxy.createSirBridges(forwardCall))

            if (variable != null && propName != null && !isUnavailable) {
                if (variable.accessorNeedsNonVirtualForwardBridge() && !variable.isAbstractKotlinProperty) {
                    add(proxy.createDirectDispatchForwardBridge("<get-$propName>", forwardCall))
                }
                if (variable.accessorNeedsReverseBridge()) {
                    val tryPrefix = if (errorType != SirType.never) "try! " else ""
                    val swiftName = variable.name
                    addAll(
                        proxy.createReverseSirBridges(
                            targetClassFqName = variable.reverseBridgeTargetClassFqName(),
                            targetMethodName = "<get-$propName>",
                            swiftDynamicCall = { selfExpr, _ -> "$tryPrefix$selfExpr.$swiftName" },
                            swiftDeprecation = variable.effectiveReverseBridgeDeprecation(),
                        )
                    )
                }
            }
        }
    }

    override var body: SirFunctionBody?
        set(value) {}
        get() = sirSession.withSessions { buildAccessorBody(bridgeProxy ?: return@withSessions null, variable, isUnavailable) }

    // The `_direct` forward bridge + direct-dispatch getter body for an interface default-property witness.
    context(_: KaSession, _: SirSession)
    internal fun directWitnessBridgeAndBody(): Pair<SirBridge, SirFunctionBody>? {
        if (isUnavailable) return null
        val proxy = bridgeProxy ?: return null
        val propName = variable?.kotlinPropertyName ?: return null
        val bridge = proxy.createDirectDispatchForwardBridge("<get-$propName>", forwardCall)
        val body = SirFunctionBody(proxy.createSwiftInvocation(useDirectDispatch = true) { "return $it" })
        return bridge to body
    }

    private inline fun <R> lazyWithSessions(crossinline block: context(KaSession, SirSession) () -> R): Lazy<R> = lazy {
        sirSession.withSessions(block)
    }
}

internal class SirGetterFromKtSymbol(
    override val ktSymbol: KaPropertyGetterSymbol,
    sirSession: SirSession,
) : SirAbstractGetter(sirSession), SirFromKtSymbol<KaPropertyGetterSymbol> {
    override val origin: SirOrigin by lazy { KotlinSource(ktSymbol) }
    override val documentation: String? by lazy { ktSymbol.documentation() }
    override val attributes: List<SirAttribute> by lazy { this.translatedAttributes }
    override val errorType: SirType get() = if (ktSymbol.throwsAnnotation != null) SirType.any else SirType.never
}

internal abstract class SirAbstractSetter(
    val sirSession: SirSession,
) : SirSetter(), SirBridgedCallable {
    override lateinit var parent: SirDeclarationParent
    override val visibility: SirVisibility get() = SirVisibility.PUBLIC
    override val documentation: String? get() = null
    override val parameterName: String = "newValue"
    override val attributes: List<SirAttribute> get() = emptyList()
    override val errorType: SirType get() = SirType.never
    override val isAsync: Boolean get() = false
    private val variable get() = parent as? SirVariable

    open val fqName: FqName? by lazyWithSessions {
        variable?.kaSymbolOrNull<KaVariableSymbol>()
            ?.callableId?.asSingleFqName()
    }

    private val bridgeProxy: BridgeFunctionProxy? by lazyWithSessions {
        val suffix = "_set"
        val variable = variable?.takeUnless { it.isUnavailable } ?: return@lazyWithSessions null
        val fqName = fqName ?: return@lazyWithSessions null
        val baseName = fqName.baseBridgeName + suffix
        val setterSymbol = variable.kaSymbolOrNull<KaPropertySymbol>()?.setter ?: variable.kaSymbolOrNull<KaVariableSymbol>()

        generateFunctionBridge(
            baseBridgeName = baseName,
            explicitParameters = listOf(SirParameter(parameterName = parameterName, type = variable.type)),
            returnType = SirNominalType(SirSwiftModule.void),
            kotlinFqName = fqName,
            kotlinOptIns = setterSymbol?.allRequiredOptIns ?: emptyList(),
            selfParameter = (parent !is SirModule && variable.isInstance).ifTrue {
                val effectiveSelfType = variable.computeInterfaceSelfType() ?: selfType
                SirParameter(null, "self", effectiveSelfType ?: error("Only a member can have a self parameter"))
            },
            contextParameters = emptyList(),
            extensionReceiverParameter = null,
            errorParameter = errorType.takeIf { it != SirType.never }?.let {
                SirParameter(null, "_out_error", it)
            },
            isAsync = false,
        )
    }

    private val forwardCall: BridgeFunctionBuilder.() -> String = {
        val args = argNames
        val expectedParameters = if (extensionReceiverParameter != null) 2 else 1
        require(args.size == expectedParameters) { "Received an extension getter $name with ${args.size} parameters instead of a $expectedParameters, aborting" }
        buildCall(" = ${args.last()}")
    }

    override val bridges: List<SirBridge> by lazyWithSessions {
        val proxy = bridgeProxy ?: return@lazyWithSessions emptyList()
        val variable = variable
        val propName = variable?.kotlinPropertyName
        buildList {
            addAll(proxy.createSirBridges(forwardCall))

            if (variable != null && propName != null && !isUnavailable) {
                if (variable.accessorNeedsNonVirtualForwardBridge() && !variable.isAbstractKotlinProperty) {
                    add(proxy.createDirectDispatchForwardBridge("<set-$propName>", forwardCall))
                }
                if (variable.accessorNeedsReverseBridge()) {
                    val swiftName = variable.name
                    addAll(
                        proxy.createReverseSirBridges(
                            targetClassFqName = variable.reverseBridgeTargetClassFqName(),
                            targetMethodName = "<set-$propName>",
                            swiftDynamicCall = { selfExpr, paramExprs -> "{ $selfExpr.$swiftName = ${paramExprs.single()} }()" },
                            swiftDeprecation = variable.effectiveReverseBridgeDeprecation(),
                        )
                    )
                }
            }
        }
    }

    override var body: SirFunctionBody?
        set(value) {}
        get() = sirSession.withSessions { buildAccessorBody(bridgeProxy ?: return@withSessions null, variable, isUnavailable) }

    /** The `_direct` forward bridge + direct-dispatch setter body for an interface default-property witness. */
    context(_: KaSession, _: SirSession)
    internal fun directWitnessBridgeAndBody(): Pair<SirBridge, SirFunctionBody>? {
        if (isUnavailable) return null
        val proxy = bridgeProxy ?: return null
        val propName = variable?.kotlinPropertyName ?: return null
        val bridge = proxy.createDirectDispatchForwardBridge("<set-$propName>", forwardCall)
        val body = SirFunctionBody(proxy.createSwiftInvocation(useDirectDispatch = true) { "return $it" })
        return bridge to body
    }

    private inline fun <R> lazyWithSessions(crossinline block: context(KaSession, SirSession) () -> R): Lazy<R> = lazy {
        sirSession.withSessions(block)
    }
}

internal class SirSetterFromKtSymbol(
    override val ktSymbol: KaPropertySetterSymbol,
    sirSession: SirSession,
) : SirAbstractSetter(sirSession), SirFromKtSymbol<KaPropertySetterSymbol> {
    override val origin: SirOrigin by lazy { KotlinSource(ktSymbol) }
    override val documentation: String? by lazy { ktSymbol.documentation() }
    override val attributes: List<SirAttribute> by lazy { this.translatedAttributes }
}

private fun SirVariable.accessorNeedsReverseBridge(): Boolean {
    if (!isInstance) return false
    if (isUnavailable) return false
    return when (val container = parent) {
        is SirClass -> modality == SirModality.OPEN && container.modality == SirModality.OPEN && !container.isUnavailable
        is SirProtocol -> !container.isUnavailable
        else -> false
    }
}

private fun SirVariable.accessorNeedsNonVirtualForwardBridge(): Boolean =
    accessorNeedsReverseBridge() && parent is SirClass

private val SirVariable.isAbstractKotlinProperty: Boolean
    get() = kaSymbolOrNull<KaVariableSymbol>()?.modality == KaSymbolModality.ABSTRACT

private val SirVariable.kotlinPropertyName: String?
    get() = kaSymbolOrNull<KaVariableSymbol>()?.name?.asString()

private fun SirVariable.effectiveReverseBridgeDeprecation(): SirAttribute.Available? {
    fun SirDeclaration.deprecatedAttr(): SirAttribute.Available? =
        attributes.firstOrNull { it is SirAttribute.Available && it.deprecated } as? SirAttribute.Available
    return this.deprecatedAttr()
        ?: (parent as? SirClass)?.deprecatedAttr()
        ?: (parent as? SirProtocol)?.deprecatedAttr()
}

context(_: KaSession, _: SirSession)
private fun SirVariable.reverseBridgeTargetClassFqName(): String =
    kaSymbolOrNull<KaVariableSymbol>()
        ?.containingSymbol
        ?.let { (it as? KaNamedClassSymbol)?.classId?.asSingleFqName()?.asString() }
        ?: ""

context(_: KaSession, sirSession: SirSession)
private fun SirVariable.computeInterfaceSelfType(): SirType? {
    if (!isInstance) return null
    (parent as? SirProtocol)?.let { return SirExistentialType(it) }
    val containingClass = (parent as? SirClass)?.kaSymbolOrNull<KaClassSymbol>() ?: return null
    if (!containingClass.hasFBoundedTypeParameters()) return null
    val propertySymbol = kaSymbolOrNull<KaVariableSymbol>() ?: return null
    val overriddenInterfaceProperty = propertySymbol.allOverriddenSymbols
        .filterIsInstance<KaVariableSymbol>()
        .firstOrNull { overridden ->
            val containingSymbol = overridden.containingSymbol
            containingSymbol is KaClassSymbol && containingSymbol.classKind == KaClassKind.INTERFACE
        } ?: return null
    val interfaceSymbol = overriddenInterfaceProperty.containingSymbol as? KaNamedClassSymbol ?: return null
    // `toSir` is a member-extension of SirSession, so it needs SirSession as a dispatch receiver.
    val sirDeclarations = with(sirSession) { interfaceSymbol.toSir().allDeclarations }
    val sirProtocol = sirDeclarations.firstIsInstanceOrNull<SirProtocol>() ?: return null
    return SirExistentialType(sirProtocol)
}

context(_: KaSession, _: SirSession)
private fun buildAccessorBody(proxy: BridgeFunctionProxy, variable: SirVariable?, accessorUnavailable: Boolean): SirFunctionBody? {
    if (accessorUnavailable || variable == null || !variable.accessorNeedsNonVirtualForwardBridge()) {
        return SirFunctionBody(proxy.createSwiftInvocation { "return $it" })
    }
    val wrapperFqName = (variable.parent as SirClass).swiftFqName
    val virtualLines = proxy.createSwiftInvocation { "return $it" }
    val fallbackLines = if (variable.isAbstractKotlinProperty) {
        listOf("fatalError(\"Cannot invoke the inherited implementation of abstract property '$wrapperFqName.${variable.name}': a Swift subclass must override it and must not call super.\")")
    } else {
        proxy.createSwiftInvocation(useDirectDispatch = true) { "return $it" }
    }
    return SirFunctionBody(buildList {
        add("if Self.self == $wrapperFqName.self {")
        virtualLines.forEach { add("    $it") }
        add("} else {")
        fallbackLines.forEach { add("    $it") }
        add("}")
    })
}
