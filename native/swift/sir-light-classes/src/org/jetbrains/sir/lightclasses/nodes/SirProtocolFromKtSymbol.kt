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
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildTypealias
import org.jetbrains.kotlin.sir.providers.*
import org.jetbrains.kotlin.sir.providers.source.KotlinMarkerProtocol
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.source.kaSymbolOrNull
import org.jetbrains.kotlin.sir.providers.utils.*
import org.jetbrains.kotlin.sir.util.SirSwiftConcurrencyModule
import org.jetbrains.kotlin.sir.util.swiftFqName
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.documentation
import org.jetbrains.sir.lightclasses.extensions.lazyWithSessions
import org.jetbrains.sir.lightclasses.extensions.withSessions
import org.jetbrains.sir.lightclasses.utils.decapitalizeNameSemantically
import org.jetbrains.sir.lightclasses.utils.objcClassSymbolName
import org.jetbrains.sir.lightclasses.utils.relocatedDeclarationNamePrefix
import org.jetbrains.sir.lightclasses.utils.translatedAttributes

internal open class SirProtocolFromKtSymbol(
    override val ktSymbol: KaNamedClassSymbol,
    override val sirSession: SirSession,
) : SirProtocol(), SirFromKtSymbol<KaNamedClassSymbol> {
    override val origin: SirOrigin = KotlinSource(ktSymbol)
    override val visibility: SirVisibility = SirVisibility.PUBLIC
    override val documentation: String? by lazy {
        ktSymbol.documentation()
    }
    override val name: String by lazy {
        (this.relocatedDeclarationNamePrefix() ?: "") + ktSymbol.name.asString()
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
                    ktSymbol.containingModule.sirModule().updateImport(SirImport(it.containingModule().name))
                }
            }

    }

    override val attributes: List<SirAttribute> by lazy { this.translatedAttributes }

    override val declarations: MutableList<SirDeclaration> by lazyWithSessions {
        ktSymbol.combinedDeclaredMemberScope.extractDeclarations()
            .filter { it !is SirOperatorAuxiliaryDeclaration } // FIXME: rectify where auxiliary declarations should go.
            .toMutableList()
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
    override val visibility: SirVisibility = SirVisibility.PACKAGE
    override val documentation: String? = null
    override val attributes: List<SirAttribute> get() = listOf(SirAttribute.ObjC(this.name))
    override val name: String get() = "_${target.name}"
    override val declarations: MutableList<SirDeclaration> get() = mutableListOf()
    override val superClass: SirNominalType? get() = null
    override val protocols: List<SirProtocol> get() = target.protocols.filterIsInstance<SirProtocolFromKtSymbol>().map { it.existentialMarker }

    override val bridges: List<SirBridge> by lazyWithSessions {
        listOfNotNull(
            sirSession.generateTypeBridge(
                ktSymbol.classId?.asSingleFqName(),
                swiftFqName = swiftFqName,
                swiftSymbolName = objcClassSymbolName,
            ))
    }
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
    val targetProtocol: SirProtocol,
) : SirExtension(), SirFromKtSymbol<KaNamedClassSymbol> {
    constructor(protocol: SirProtocolFromKtSymbol) : this(protocol.ktSymbol, protocol.sirSession, protocol)

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
            SirTypeConstraint.Conformance(SirNominalType(KotlinRuntimeSupportModule.kotlinBridgeable))
        )
    }

    override val attributes: List<SirAttribute> get() = emptyList()

    override val declarations: MutableList<SirDeclaration> by lazyWithSessions {
        ktSymbol.combinedDeclaredMemberScope
            .extractDeclarations()
            .mapNotNull {
                when (it) {
                    is SirFunction -> SirRelocatedFunction(it).also { it.parent = this@SirBridgedProtocolImplementationFromKtSymbol }
                    is SirVariable -> SirRelocatedVariable(it).also { it.parent = this@SirBridgedProtocolImplementationFromKtSymbol }
                    is SirSubscript -> SirRelocatedSubscript(it).also { it.parent = this@SirBridgedProtocolImplementationFromKtSymbol }
                    else -> null
                }
            }
            .toMutableList()
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
    override val contextParameters: List<SirParameter> get() = source.contextParameters
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
    override val modality: SirModality get() = SirModality.UNSPECIFIED
    override val attributes: List<SirAttribute> get() = source.attributes
    override val getter: SirGetter get() = source.getter
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

    override open val protocols: List<SirProtocol> get() = listOf(targetProtocol)

    override val constraints: List<SirTypeConstraint> by lazy {
        listOf(
            SirTypeConstraint.Conformance(SirExistentialType(targetProtocol.existentialMarker), listOf("Wrapped"))
        )
    }

    override val attributes: List<SirAttribute> get() = emptyList()

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

    override val attributes: List<SirAttribute> = emptyList()

    override val constraints: List<SirTypeConstraint> = emptyList()

    override val protocols: List<SirProtocol> = emptyList()

    override val extendedType: SirType = SirNominalType(targetProtocol)

    override val declarations: MutableList<SirDeclaration> by lazyWithSessions {
        ktSymbol.combinedDeclaredMemberScope
            .extractDeclarations()
            .filterIsInstance<SirScopeDefiningDeclaration>()
            .filter { it.visibility == SirVisibility.PUBLIC }
            .map { declaration ->
                buildTypealias {
                    origin = SirOrigin.Trampoline(declaration)
                    visibility = SirVisibility.INTERNAL // visibility modifiers are disallowed in protocols
                    // FIXME: we make here the best effort to restore the original name of a relocated declaration
                    name = declaration.kaSymbolOrNull<KaDeclarationSymbol>()?.sirDeclarationName() ?: declaration.name
                    type = SirNominalType(declaration) // Has to be nominal even for protocol declarations
                }.also { it.parent = this }
            }
            .toMutableList()
    }
}

/**
 * An ad-hoc translation for kotlinx.coroutines.Flow
 */
internal class SirFlowFromKtSymbol(
    ktSymbol: KaNamedClassSymbol,
    sirSession: SirSession,
) : SirProtocolFromKtSymbol(ktSymbol, sirSession), SirFromKtSymbol<KaNamedClassSymbol> {
    internal inner class SirExistentialProtocolImplementation : SirExistentialProtocolImplementationFromKtSymbol(this@SirFlowFromKtSymbol) {
        override val protocols: List<SirProtocol>
            get() = super.protocols + listOf(KotlinCoroutineSupportModule.kotlinFlow, SirSwiftConcurrencyModule.asyncSequence)
    }

    override val protocols: List<SirProtocol> by lazy {
        super.protocols + KotlinCoroutineSupportModule.kotlinFlow + SirSwiftConcurrencyModule.asyncSequence
    }

    override val existentialExtension: SirExtension by lazy {
        SirExistentialProtocolImplementation()
    }
}
