/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.components.combinedDeclaredMemberScope
import org.jetbrains.kotlin.analysis.api.components.containingModule
import org.jetbrains.kotlin.analysis.api.components.samConstructor
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildFunctionCopy
import org.jetbrains.kotlin.sir.builder.buildGetterCopy
import org.jetbrains.kotlin.sir.builder.buildSetterCopy
import org.jetbrains.kotlin.sir.builder.buildTypealias
import org.jetbrains.kotlin.sir.builder.buildVariableCopy
import org.jetbrains.kotlin.sir.providers.*
import org.jetbrains.kotlin.sir.providers.source.KotlinImplementationMarkerProtocol
import org.jetbrains.kotlin.sir.providers.source.KotlinMarkerProtocol
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.source.kaSymbolOrNull
import org.jetbrains.kotlin.sir.providers.utils.*
import org.jetbrains.kotlin.sir.util.SirSwiftConcurrencyModule
import org.jetbrains.kotlin.sir.util.isUnavailable
import org.jetbrains.kotlin.sir.util.swiftFqName
import org.jetbrains.kotlin.sir.util.unavailableTypes
import org.jetbrains.kotlin.sir.util.replaceOrAddPropagatedUnavailability
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.documentation
import org.jetbrains.sir.lightclasses.extensions.lazyWithSessions
import org.jetbrains.sir.lightclasses.extensions.withSessions
import org.jetbrains.sir.lightclasses.utils.decapitalizeNameSemantically
import org.jetbrains.sir.lightclasses.utils.objcClassSymbolName
import org.jetbrains.sir.lightclasses.utils.relocatedDeclarationNamePrefix
import org.jetbrains.sir.lightclasses.utils.translatedAttributes
import org.jetbrains.sir.lightclasses.utils.translatedOptInAttributes

internal open class SirProtocolFromKtSymbol(
    override val ktSymbol: KaNamedClassSymbol,
    override val sirSession: SirSession,
) : SirProtocol(), SirFromKtSymbol<KaNamedClassSymbol> {
    override val origin: SirOrigin = KotlinSource(ktSymbol)
    override val visibility: SirVisibility = SirVisibility.PUBLIC
    override val documentation: String? by lazy {
        ktSymbol.documentation()
    }
    override val name: String by lazyWithSessions {
        (this.relocatedDeclarationNamePrefix() ?: "") + ktSymbol.sirDeclarationName()
    }
    override var parent: SirDeclarationParent
        get() = withSessions {
            ktSymbol.getSirParent()
        }
        set(_) = Unit

    override val superClass: SirNominalType? by lazy {
        SirNominalType(KotlinRuntimeModule.kotlinBase)
    }

    override val protocols: List<SirProtocol> by lazyWithSessions {
        val isUnavailable = this.isUnavailable
        val inherited = translatedProtocols.filter { isUnavailable || !it.isUnavailable }
        if (isUnavailable) inherited else inherited + existentialMarker
    }

    internal val translatedProtocols: List<SirProtocol> by lazyWithSessions {
        ktSymbol.superTypes
            .mapNotNull { it.symbol as? KaClassSymbol }
            .filter { it.classKind == KaClassKind.INTERFACE }
            .filter {
                it.sirAvailability().let {
                    it is SirAvailability.Available && it.visibility > SirVisibility.INTERNAL
                }
            }
            .mapNotNull {
                it.toSir().allDeclarations.firstIsInstanceOrNull<SirProtocol>()?.also {
                    ktSymbol.containingModule.sirModule().updateImportFor(it)
                }
            }
    }

    override val attributes: List<SirAttribute> by lazy { this.translatedAttributes }

    override val declarations: MutableList<SirDeclaration> by lazyWithSessions {
        mutableListOf<SirDeclaration>().apply {
            addAll(ktSymbol.combinedDeclaredMemberScope.extractDeclarations().filter {
                it !is SirOperatorAuxiliaryDeclaration // FIXME: rectify where auxiliary declarations should go.
            })
            addAll(sealedTypeFunctions)
        }
    }

    internal open val existentialExtension: SirExtension by lazy {
        SirExistentialProtocolImplementationFromKtSymbol(this)
    }

    internal val auxExtension: SirExtension by lazy {
        SirAuxiliaryProtocolDeclarationsFromKtSymbol(this)
    }

    internal val existentialMarker: SirProtocol by lazy {
        SirMarkerProtocolFromKtSymbol(this)
            .also { it.parent = this.parent }
    }

    /**
     * Per-interface "implementation marker" `__P` used to constrain the witness extension
     * ([SirBridgedProtocolImplementationFromKtSymbol]) that delegates protocol requirements to the
     * Kotlin counterpart. Only the wrappers we generate for Kotlin classes that conform to this
     * interface (and [KotlinRuntimeSupportModule.kotlinExistential]) declare conformance to it, so a
     * user's Swift subclass that conforms to this interface without its Kotlin supertype implementing
     * it does NOT inherit the (erroneous) delegating default. Unlike [existentialMarker], it is NOT
     * `@objc` and is NOT refined by the public protocol.
     */
    internal val implementationMarker: SirProtocol by lazy {
        SirImplementationMarkerProtocolFromKtSymbol(this)
            .also { it.parent = this.parent }
    }

    /**
     * Per-interface extension conforming [KotlinRuntimeSupportModule.kotlinExistentialPenBox] to this
     * protocol's [existentialMarker]. This is what lets `_KotlinExistential<Wrapped>` inherit
     * @objc marker conformances through its non-generic PenBox ancestor, sidestepping Swift's
     * rule against generic classes conforming to @objc protocols via extensions.
     */
    internal val penBoxMarkerConformance: SirExtension by lazy {
        SirPenBoxMarkerConformanceFromKtSymbol(this)
    }

    internal val samConverter: SirDeclaration? by lazyWithSessions {
        ktSymbol.samConstructor?.let {
            SirRelocatedFunction(SirFunctionFromKtSymbol(it, sirSession)).also {
                it.parent = this@SirProtocolFromKtSymbol.parent
                it.name = this@SirProtocolFromKtSymbol.name.let { name ->
                    val decapitalized = decapitalizeNameSemantically(name)
                    decapitalized.takeIf { it != name } ?: "${decapitalized}FromFunction"
                }
            }
        }
    }

    internal val sealedType: SirScopeDefiningDeclaration? by lazyWithSessions {
        createSirSealedType(this)
    }

    internal val sealedTypeFunctions by lazyWithSessions {
        createSirSealedTypeFunctions(this).onEach { it.parent = this }
    }

    override val bridges: List<SirBridge> = emptyList()
}

/**
 * Marker protocol declaration for protocol conformance to [target] of the universal existential type.
 * @property target target protocol to be implemented using this marker
 *
 * @see [KotlinRuntimeSupportModule.kotlinExistential]
 */
internal class SirMarkerProtocolFromKtSymbol(
    val target: SirProtocolFromKtSymbol
) : SirProtocol(), SirFromKtSymbol<KaNamedClassSymbol> {
    override val ktSymbol: KaNamedClassSymbol get() = target.ktSymbol
    override val sirSession: SirSession get() = target.sirSession

    override lateinit var parent: SirDeclarationParent
    override val origin: KotlinSource get() = KotlinMarkerProtocol(ktSymbol)
    override val visibility: SirVisibility = SirVisibility.PUBLIC
    override val documentation: String? = null
    override val attributes: List<SirAttribute> get() = listOf(SirAttribute.ObjC(this.name))
    override val name: String get() = "_${target.name}"
    override val declarations: MutableList<SirDeclaration> get() = mutableListOf()
    override val superClass: SirNominalType? get() = null
    override val protocols: List<SirProtocol>
        get() = target.translatedProtocols.filterIsInstance<SirProtocolFromKtSymbol>().map { it.existentialMarker }

    override val bridges: List<SirBridge> by lazyWithSessions {
        listOfNotNull(
            sirSession.generateTypeBridge(
                ktSymbol.classId?.asSingleFqName(),
                kotlinOptIns = ktSymbol.allRequiredOptIns,
                swiftFqName = swiftFqName,
                swiftSymbolName = objcClassSymbolName,
            ))
    }
}

/**
 * "Implementation marker" protocol declaration `__[target]`, used to constrain the witness extension
 * ([SirBridgedProtocolImplementationFromKtSymbol]) that satisfies [target]'s requirements by delegating
 * to the Kotlin counterpart. Only the wrappers we generate for Kotlin classes that conform to [target]
 * on the Kotlin side declare conformance to it, so it is never inherited by a user's Swift subclass that
 * conforms to [target] without its Kotlin supertype implementing it.
 *
 * Differs from the existential marker [SirMarkerProtocolFromKtSymbol] (`_[target]`): it is NOT `@objc`
 * (it is only a Swift generic constraint, never resolved by the ObjC runtime — which is also why the
 * generic `_KotlinExistential<Wrapped>` can conform to it directly, without the PenBox indirection), it
 * refines [KotlinRuntimeSupportModule.kotlinBridgeable] so witness bodies can call `__externalRCRef()`,
 * and it is NOT refined by the public protocol [target].
 */
internal class SirImplementationMarkerProtocolFromKtSymbol(
    val target: SirProtocolFromKtSymbol
) : SirProtocol(), SirFromKtSymbol<KaNamedClassSymbol> {
    override val ktSymbol: KaNamedClassSymbol get() = target.ktSymbol
    override val sirSession: SirSession get() = target.sirSession

    override lateinit var parent: SirDeclarationParent
    override val origin: KotlinSource get() = KotlinImplementationMarkerProtocol(ktSymbol)
    override val visibility: SirVisibility = SirVisibility.PUBLIC
    override val documentation: String? = null
    override val attributes: List<SirAttribute> by lazy { translatedOptInAttributes }
    override val name: String get() = "__${target.name}"
    override val declarations: MutableList<SirDeclaration> get() = mutableListOf()
    override val superClass: SirNominalType? get() = null
    override val protocols: List<SirProtocol> get() = listOf(KotlinRuntimeSupportModule.kotlinBridgeable)
    override val bridges: List<SirBridge> = emptyList()
}

/**
 * A supporting extension declaration providing bridges for interface/protocol requirements for classes exported from kotlin.
 * Exporting a Kotlin class to Swift can result in overridden members from an inherited interface not aligning correctly with their
 * counterparts in the exported Swift protocol due to differences in Swift’s subtyping rules compared to Kotlin.
 * In such cases, Swift will use definitions from this extension to satisfy the missing protocol requirements.
 *
 * @property targetProtocol Protocol declaration this extension belongs to.
 */
internal class SirBridgedProtocolImplementationFromKtSymbol(
    override val ktSymbol: KaNamedClassSymbol,
    override val sirSession: SirSession,
    val targetProtocol: SirProtocolFromKtSymbol,
    private val implementationMarker: SirProtocol,
) : SirExtension(), SirFromKtSymbol<KaNamedClassSymbol> {
    constructor(protocol: SirProtocolFromKtSymbol) : this(protocol.ktSymbol, protocol.sirSession, protocol, protocol.implementationMarker)

    override val origin: SirOrigin = KotlinSource(ktSymbol)

    override val visibility: SirVisibility = SirVisibility.PUBLIC
    override val documentation: String? by lazy {
        ktSymbol.documentation()
    }
    override var parent: SirDeclarationParent
        get() = withSessions {
            ktSymbol.containingModule.sirModule()
        }
        set(_) = Unit

    override val extendedType: SirType
        get() = SirNominalType(targetProtocol)

    override val protocols: List<SirProtocol> get() = emptyList()

    override val constraints: List<SirTypeConstraint> by lazy {
        listOf(
            SirTypeConstraint.Conformance(SirNominalType(implementationMarker))
        )
    }

    override val attributes: List<SirAttribute> by lazy {
        buildList {
            replaceOrAddPropagatedUnavailability { extendedType.unavailableTypes }
        }
    }

    override val declarations: MutableList<SirDeclaration> by lazyWithSessions {
        mutableListOf<SirDeclaration>().apply {
            ktSymbol.combinedDeclaredMemberScope.extractDeclarations().forEach {
                when (it) {
                    is SirFunction -> add(SirRelocatedFunction(it))
                    is SirVariable -> add(SirRelocatedVariable(it))
                    is SirSubscript -> add(SirRelocatedSubscript(it))
                    else -> {}
                }
            }
            targetProtocol.sealedTypeFunctions.forEach { add(SirRelocatedFunction(it)) }
        }.onEach { it.parent = this@SirBridgedProtocolImplementationFromKtSymbol }
    }
}


/**
 * Relocated function
 * Mirrors the `source` declaration, but allows for changing parent.
 *
 * @property source The original declaration
 */
private class SirRelocatedFunction(
    val source: SirFunction,
) : SirFunction() {
    override lateinit var parent: SirDeclarationParent

    override val origin: SirOrigin get() = source.origin
    override val visibility: SirVisibility get() = source.visibility
    override val documentation: String? get() = source.documentation
    private var _name: String? = null
    override var name: String
        get() = _name ?: source.name
        set(newValue) { _name = newValue }
    override val returnType: SirType get() = source.returnType
    override val isOverride: Boolean get() = false
    override val isInstance: Boolean get() = source.isInstance
    override val modality: SirModality get() = SirModality.UNSPECIFIED
    override val fixity: SirFixity? get() = source.fixity
    override val attributes: List<SirAttribute> get() = source.attributes
    override val contextParameter: SirParameter? get() = source.contextParameter
    override val extensionReceiverParameter: SirParameter? get() = source.extensionReceiverParameter
    override val parameters: List<SirParameter> get() = source.parameters
    override val errorType: SirType get() = source.errorType
    override val isAsync: Boolean get() = source.isAsync
    override val bridges: List<SirBridge> get() {
            val result = source.bridges
            return result
        }

    override var body: SirFunctionBody?
        get() = source.body
        set(newValue) { source.body = newValue }
}

/**
 * Relocatied variable
 * Mirrors the `source` declaration, but allows for changing parent.
 *
 * @property source The original declaration
 */
private class SirRelocatedVariable(
    val source: SirVariable,
) : SirVariable() {
    override lateinit var parent: SirDeclarationParent

    override val origin: SirOrigin get() = source.origin
    override val visibility: SirVisibility get() = source.visibility
    override val documentation: String? get() = source.documentation
    override val name: String get() = source.name
    override val type: SirType get() = source.type
    override val isOverride: Boolean get() = false
    override val isInstance: Boolean get() = true
    override val isConstant: Boolean get() = source.isConstant
    override val modality: SirModality get() = SirModality.UNSPECIFIED
    override val attributes: List<SirAttribute> get() = source.attributes
    override val getter: SirGetter? get() = source.getter
    override val setter: SirSetter? get() = source.setter
    override val bridges: List<SirBridge> get() = source.bridges
}

/**
 * Relocatied subscript
 * Mirrors the `source` declaration, but allows for changing parent.
 *
 * @property source The original declaration
 */
private class SirRelocatedSubscript(
    val source: SirSubscript,
) : SirSubscript() {
    override lateinit var parent: SirDeclarationParent

    override val origin: SirOrigin get() = source.origin
    override val visibility: SirVisibility get() = source.visibility
    override val documentation: String? get() = source.documentation
    override val attributes: List<SirAttribute> get() = source.attributes
    override val isOverride: Boolean get() = source.isOverride
    override val isInstance: Boolean get() = source.isInstance
    override val modality: SirModality get() = source.modality
    override val parameters: List<SirParameter> get() = source.parameters
    override val returnType: SirType get() = source.returnType
    override val getter: SirGetter get() = source.getter
    override val setter: SirSetter? get() = source.setter
}

/**
 * A supporting extension ensuring conformance of the universal existential wrapper type to [targetProtocol].
 * @see KotlinRuntimeSupportModule.kotlinExistential
 *
 * @property targetProtocol Protocol declaration this extension belongs to.
 */
internal open class SirExistentialProtocolImplementationFromKtSymbol(
    override val ktSymbol: KaNamedClassSymbol,
    override val sirSession: SirSession,
    private val targetProtocol: SirProtocolFromKtSymbol,
) : SirExtension(), SirFromKtSymbol<KaNamedClassSymbol> {
    constructor(protocol: SirProtocolFromKtSymbol) : this(
        protocol.ktSymbol,
        protocol.sirSession,
        protocol
    )

    override val origin: SirOrigin = KotlinSource(ktSymbol)

    override val visibility: SirVisibility = SirVisibility.PUBLIC
    override val documentation: String? by lazy {
        ktSymbol.documentation()
    }
    override var parent: SirDeclarationParent
        get() = withSessions {
            ktSymbol.containingModule.sirModule()
        }
        set(_) = Unit

    override val extendedType: SirType
        get() = SirNominalType(KotlinRuntimeSupportModule.kotlinExistential)

    override open val protocols: List<SirProtocol>
        get() = listOf(targetProtocol, targetProtocol.implementationMarker)

    override val constraints: List<SirTypeConstraint> by lazy {
        listOf(
            SirTypeConstraint.Conformance(SirExistentialType(targetProtocol.existentialMarker), listOf("Wrapped"))
        )
    }

    override val attributes: List<SirAttribute> by lazy {
        buildList {
            addAll(this@SirExistentialProtocolImplementationFromKtSymbol.translatedOptInAttributes)
            replaceOrAddPropagatedUnavailability { SirNominalType(targetProtocol).unavailableTypes }
        }
    }

    override val declarations: MutableList<SirDeclaration> = mutableListOf()
}

/**
 * Extension declaring that [KotlinRuntimeSupportModule.kotlinExistentialPenBox] conforms to
 * [targetProtocol.existentialMarker]. Emitted once per exported Kotlin interface in the module that
 * declares it, so the @objc marker metadata is registered alongside the marker protocol itself.
 *
 * Because PenBox is non-generic, it can legally conform to an @objc protocol in an extension —
 * which a generic class like `_KotlinExistential<Wrapped>` cannot do directly. `_KotlinExistential`
 * then inherits the conformance through its PenBox superclass.
 */
internal class SirPenBoxMarkerConformanceFromKtSymbol(
    override val ktSymbol: KaNamedClassSymbol,
    override val sirSession: SirSession,
    private val targetProtocol: SirProtocolFromKtSymbol,
) : SirExtension(), SirFromKtSymbol<KaNamedClassSymbol> {
    constructor(protocol: SirProtocolFromKtSymbol) : this(
        protocol.ktSymbol,
        protocol.sirSession,
        protocol,
    )

    override val origin: SirOrigin = KotlinSource(ktSymbol)

    override val visibility: SirVisibility = SirVisibility.PACKAGE

    override val documentation: String? = null

    override var parent: SirDeclarationParent
        get() = withSessions { ktSymbol.containingModule.sirModule() }
        set(_) = Unit

    override val extendedType: SirType
        get() = SirNominalType(KotlinRuntimeSupportModule.kotlinExistentialPenBox)

    override val protocols: List<SirProtocol>
        get() = if (targetProtocol.isUnavailable) emptyList() else listOf(targetProtocol.existentialMarker)

    override val constraints: List<SirTypeConstraint> = emptyList()

    override val attributes: List<SirAttribute> by lazy {
        buildList {
            replaceOrAddPropagatedUnavailability { SirNominalType(targetProtocol).unavailableTypes }
        }
    }

    override val declarations: MutableList<SirDeclaration> = mutableListOf()
}

internal class SirStubProtocol(
    ktSymbol: KaNamedClassSymbol,
    sirSession: SirSession
) : SirProtocolFromKtSymbol(
    ktSymbol,
    sirSession
) {
    override val declarations: MutableList<SirDeclaration> = mutableListOf()
}

/**
 * An extension for miscellaneous supporting declarations for [targetProtocol], like convenience typealiases or default implementations.
 *
 * @property targetProtocol Protocol declaration this extension belongs to.
 */
internal class SirAuxiliaryProtocolDeclarationsFromKtSymbol(
    override val ktSymbol: KaNamedClassSymbol,
    override val sirSession: SirSession,
    private val targetProtocol: SirProtocolFromKtSymbol,
) : SirExtension(), SirFromKtSymbol<KaNamedClassSymbol> {
    constructor(protocol: SirProtocolFromKtSymbol) : this(
        protocol.ktSymbol,
        protocol.sirSession,
        protocol
    )

    override var parent: SirDeclarationParent
        get() = withSessions {
            ktSymbol.containingModule.sirModule()
        }
        set(_) = Unit

    override val origin: SirOrigin get() = SirOrigin.AdditionalDeclaration(KotlinSource(ktSymbol))

    override val visibility: SirVisibility get() = SirVisibility.PUBLIC

    override val documentation: String? get() = null

    override val attributes: List<SirAttribute> by lazy {
        buildList {
            replaceOrAddPropagatedUnavailability { extendedType.unavailableTypes }
        }
    }

    override val constraints: List<SirTypeConstraint> = emptyList()

    override val protocols: List<SirProtocol> = emptyList()

    override val extendedType: SirType = SirNominalType(targetProtocol)

    override val declarations: MutableList<SirDeclaration> by lazyWithSessions {
        val members = ktSymbol.combinedDeclaredMemberScope.extractDeclarations().toList()

        val typeAliases = members
            .filterIsInstance<SirScopeDefiningDeclaration>()
            .filter { it.visibility == SirVisibility.PUBLIC }
            .filter { it.origin !is KotlinMarkerProtocol && it.origin !is KotlinImplementationMarkerProtocol }
            .map { declaration ->
                buildTypealias {
                    origin = SirOrigin.Trampoline(declaration)
                    visibility = SirVisibility.INTERNAL // visibility modifiers are disallowed in protocols
                    // FIXME: we make here the best effort to restore the original name of a relocated declaration
                    name = declaration.kaSymbolOrNull<KaDeclarationSymbol>()?.sirDeclarationName() ?: declaration.name
                    type = SirNominalType(declaration) // Has to be nominal even for protocol declarations
                }.also { it.parent = this }
            }

        // Per swift rules, @_spi-requirements in non-@_spi protocols require default implementations.
        val protocolSpiGroups = targetProtocol.attributes
            .filterIsInstance<SirAttribute.SPI>()
            .mapTo(mutableSetOf()) { it.name }
        val spiMembers = members.filter { function ->
            function.attributes.any { it is SirAttribute.SPI && it.name !in protocolSpiGroups }
        }

        fun createSpiTrap(name: String) =
            SirFunctionBody(listOf("fatalError(\"'${name}' is an @_spi requirement that must be implemented by Swift conformers\")"))

        val spiFunctionTraps = spiMembers.filterIsInstance<SirFunction>().map { function ->
            buildFunctionCopy(function) {
                origin = SirOrigin.Trampoline(function)
                isOverride = false
                modality = SirModality.UNSPECIFIED
                bridges.clear() // pure Swift trap, no Kotlin bridge (avoids duplicating the witness's bridge)
                body = createSpiTrap(function.name)
            }.also { it.parent = this }
        }
        val spiVariableTraps = spiMembers.filterIsInstance<SirVariable>().map { variable ->
            buildVariableCopy(variable) {
                origin = SirOrigin.Trampoline(variable)
                isOverride = false
                modality = SirModality.UNSPECIFIED
                bridges.clear()
                getter = variable.getter?.let { getter ->
                    buildGetterCopy(getter) {
                        origin = SirOrigin.Trampoline(getter)
                        bridges.clear()
                        body = createSpiTrap(variable.name)
                    }
                }
                setter = variable.setter?.let { setter ->
                    buildSetterCopy(setter) {
                        origin = SirOrigin.Trampoline(setter)
                        bridges.clear()
                        body = createSpiTrap(variable.name)
                    }
                }
            }.apply {
                parent = this@SirAuxiliaryProtocolDeclarationsFromKtSymbol
                getter?.parent = this
                setter?.parent = this
            }
        }

        (typeAliases + spiFunctionTraps + spiVariableTraps).toMutableList()
    }
}

/**
 * An ad-hoc translation for kotlinx.coroutines.Flow/StateFlow/MutableStateFlow
 */
internal class SirFlowFromKtSymbol(
    ktSymbol: KaNamedClassSymbol,
    sirSession: SirSession,
) : SirProtocolFromKtSymbol(ktSymbol, sirSession), SirFromKtSymbol<KaNamedClassSymbol> {

    internal companion object {
        val FLOW_CLASS_ID = ClassId.fromString("kotlinx/coroutines/flow/Flow")
        val SHARED_FLOW_CLASS_ID = ClassId.fromString("kotlinx/coroutines/flow/SharedFlow")
        val MUTABLE_SHARED_FLOW_CLASS_ID = ClassId.fromString("kotlinx/coroutines/flow/MutableSharedFlow")
        val STATE_FLOW_CLASS_ID = ClassId.fromString("kotlinx/coroutines/flow/StateFlow")
        val MUTABLE_STATE_FLOW_CLASS_ID = ClassId.fromString("kotlinx/coroutines/flow/MutableStateFlow")

        val CLASS_IDS = listOf(
            FLOW_CLASS_ID,
            SHARED_FLOW_CLASS_ID, MUTABLE_SHARED_FLOW_CLASS_ID,
            STATE_FLOW_CLASS_ID, MUTABLE_STATE_FLOW_CLASS_ID,
        )
    }

    private val supportProtocol = when (ktSymbol.classId) {
        FLOW_CLASS_ID -> KotlinCoroutineSupportModule.kotlinFlow
        SHARED_FLOW_CLASS_ID -> KotlinCoroutineSupportModule.kotlinSharedFlow
        MUTABLE_SHARED_FLOW_CLASS_ID -> KotlinCoroutineSupportModule.kotlinMutableSharedFlow
        STATE_FLOW_CLASS_ID -> KotlinCoroutineSupportModule.kotlinStateFlow
        MUTABLE_STATE_FLOW_CLASS_ID -> KotlinCoroutineSupportModule.kotlinMutableStateFlow
        else -> throw IllegalArgumentException("Unsupported flow kind: ${ktSymbol.classId}")
    }

    internal inner class SirExistentialProtocolImplementation : SirExistentialProtocolImplementationFromKtSymbol(this@SirFlowFromKtSymbol) {
        override val protocols: List<SirProtocol>
            get() = super.protocols + supportProtocol
    }

    override val protocols: List<SirProtocol> by lazy {
        super.protocols + supportProtocol
    }

    override val existentialExtension: SirExtension by lazy {
        SirExistentialProtocolImplementation()
    }
}
