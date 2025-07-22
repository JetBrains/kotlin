/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.components.DefaultTypeClassIds
import org.jetbrains.kotlin.analysis.api.components.combinedDeclaredMemberScope
import org.jetbrains.kotlin.analysis.api.components.containingModule
import org.jetbrains.kotlin.analysis.api.components.expandedSymbol
import org.jetbrains.kotlin.analysis.api.export.utilities.isCloneable
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildInitCopy
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.extractDeclarations
import org.jetbrains.kotlin.sir.providers.getSirParent
import org.jetbrains.kotlin.sir.providers.sirAvailability
import org.jetbrains.kotlin.sir.providers.sirDeclarationName
import org.jetbrains.kotlin.sir.providers.sirModule
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.toSir
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeSupportModule
import org.jetbrains.kotlin.sir.providers.utils.containingModule
import org.jetbrains.kotlin.sir.providers.utils.updateImport
import org.jetbrains.kotlin.sir.providers.utils.throwsAnnotation
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.sir.util.swiftFqName
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.filterIsInstanceAnd
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.documentation
import org.jetbrains.sir.lightclasses.extensions.lazyWithSessions
import org.jetbrains.sir.lightclasses.extensions.withSessions
import org.jetbrains.sir.lightclasses.utils.*
import org.jetbrains.sir.lightclasses.utils.OverrideStatus
import org.jetbrains.sir.lightclasses.utils.computeIsOverride
import org.jetbrains.sir.lightclasses.utils.superClassDeclaration
import org.jetbrains.sir.lightclasses.utils.translatedAttributes
import kotlin.lazy

internal fun createSirClassFromKtSymbol(
    ktSymbol: KaNamedClassSymbol,
    sirSession: SirSession,
): SirAbstractClassFromKtSymbol = SirClassFromKtSymbol(
    ktSymbol,
    sirSession
)

internal fun createSirEnumFromKtSymbol(
    ktSymbol: KaNamedClassSymbol,
    sirSession: SirSession,
): SirEnum = SirEnumFromKtSymbol(
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

internal class SirEnumFromKtSymbol(
    override val ktSymbol: KaNamedClassSymbol,
    override val sirSession: SirSession,
) : SirEnum(), SirFromKtSymbol<KaNamedClassSymbol> {
    override val origin: KotlinSource by lazy {
        KotlinSource(ktSymbol)
    }
    override val visibility: SirVisibility by lazy {
        SirVisibility.PUBLIC
    }
    override val documentation: String? by lazy {
        ktSymbol.documentation()
    }
    override val name: String by lazyWithSessions {
        (this@SirEnumFromKtSymbol.relocatedDeclarationNamePrefix() ?: "") + ktSymbol.sirDeclarationName()
    }
    override var parent: SirDeclarationParent
        get() = withSessions {
            ktSymbol.getSirParent()
        }
        set(_) = Unit
    override val declarations: MutableList<SirDeclaration> by lazyWithSessions {
        mutableListOf<SirDeclaration>().apply {
            addAll(childDeclarations)
            addAll(syntheticDeclarations())
        }
    }
    override val attributes: List<SirAttribute> by lazy { this.translatedAttributes }
    override val cases: List<SirEnumCase> get() = childDeclarations.filterIsInstance<SirEnumCase>()
    val childDeclarations: List<SirDeclaration> by lazyWithSessions {
        ktSymbol.combinedDeclaredMemberScope
            .extractDeclarations()
            .toList()
    }

    private fun syntheticDeclarations(): List<SirDeclaration> = listOf(
        kotlinBaseInitDeclaration()
    )

    private fun kotlinBaseInitDeclaration(): SirDeclaration = buildInitCopy(KotlinRuntimeModule.kotlinBaseDesignatedInit) {
        origin = SirOrigin.KotlinBaseInitOverride(`for` = KotlinSource(ktSymbol))
        visibility = SirVisibility.PACKAGE // Hide from users, but not from other Swift Export modules.
        isOverride = true
        body = SirFunctionBody(
            listOf("super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)")
        )
    }.also { it.parent = this }
}

internal class SirEnumClassFromKtSymbol(
    ktSymbol: KaNamedClassSymbol,
    sirSession: SirSession,
) : SirAbstractClassFromKtSymbol(
    ktSymbol,
    sirSession
) {
    override val superClass: SirNominalType? by lazyWithSessions {
        // TODO: this super class as default will become obsolete with the KT-66855
        SirNominalType(KotlinRuntimeModule.kotlinBase).also {
            ktSymbol.containingModule.sirModule()
        }
    }
    override val protocols: List<SirProtocol> = super.protocols + listOf(SirSwiftModule.caseIterable)
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

    override val documentation: String? by lazy {
        ktSymbol.documentation()
    }

    override val name: String by lazyWithSessions {
        (this@SirAbstractClassFromKtSymbol.relocatedDeclarationNamePrefix() ?: "") + ktSymbol.sirDeclarationName()
    }

    override var parent: SirDeclarationParent
        get() = withSessions {
            ktSymbol.getSirParent()
        }
        set(_) = Unit

    override val superClass: SirNominalType? by lazyWithSessions {
        ktSymbol.superTypes.filterIsInstanceAnd<KaClassType> {
            it.isRegularClass && it.classId != DefaultTypeClassIds.ANY
        }.firstOrNull()?.let {
            it.symbol.toSir().allDeclarations.firstIsInstanceOrNull<SirClass>()
                ?.also { ktSymbol.containingModule.sirModule().updateImport(SirImport(it.containingModule().name)) }
                ?.let { SirNominalType(it) }
        } ?: let {
            SirNominalType(KotlinRuntimeModule.kotlinBase)
        }
    }

    override val declarations: List<SirDeclaration> by lazyWithSessions {
        childDeclarations + syntheticDeclarations()
    }

    override val attributes: List<SirAttribute> by lazy { this.translatedAttributes }

    protected val childDeclarations: List<SirDeclaration> by lazyWithSessions {
        ktSymbol.combinedDeclaredMemberScope
            .extractDeclarations()
            .toList()
    }

    private fun kotlinBaseInitDeclaration(): SirDeclaration = buildInitCopy(KotlinRuntimeModule.kotlinBaseDesignatedInit) {
        origin = SirOrigin.KotlinBaseInitOverride(`for` = KotlinSource(ktSymbol))
        visibility = SirVisibility.PACKAGE // Hide from users, but not from other Swift Export modules.
        isOverride = true
        body = SirFunctionBody(listOf(
                "super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)"
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

    override val protocols: List<SirProtocol> by lazyWithSessions {
        translatedProtocols.filter { superClassDeclaration?.declaresConformance(it) != true }
    }

    private val translatedProtocols: List<SirProtocol> by lazyWithSessions {
        ktSymbol.superTypes
            .filterIsInstance<KaClassType>()
            .mapNotNull { it.expandedSymbol }
            .filter { it.classKind == KaClassKind.INTERFACE }
            .filter { it.typeParameters.isEmpty() } //Exclude generics
            .filter {
                it.sirAvailability().let {
                    it is SirAvailability.Available && it.visibility > SirVisibility.INTERNAL
                }
            }
            .filterNot { it.isCloneable }
            .flatMap {
                it.toSir().allDeclarations.filterIsInstance<SirProtocol>().also {
                    it.forEach {
                        ktSymbol.containingModule.sirModule().updateImport(SirImport(it.containingModule().name))
                    }
                }
            }
    }

    override val bridges: List<SirBridge> by lazyWithSessions {
        listOfNotNull(sirSession.generateTypeBridge(
            ktSymbol.classId?.asSingleFqName()?.pathSegments()?.map { it.toString() } ?: emptyList(),
            swiftFqName = swiftFqName,
            swiftSymbolName = objcClassSymbolName,
        ))
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
        override val documentation: String? by lazy { ktSymbol.documentation() }
        override val attributes: List<SirAttribute> by lazy { this.translatedAttributes }
        override val errorType: SirType get() = if (ktSymbol.throwsAnnotation != null) SirType.any else SirType.never

        override val fqName: List<String>? by lazyWithSessions {
            ktSymbol
                .classId?.asSingleFqName()
                ?.pathSegments()?.map { it.toString() }
                ?: return@lazyWithSessions null
        }
    }

    override lateinit var parent: SirDeclarationParent

    override val name: String get() = "shared"

    override val origin: SirOrigin = SirOrigin.ObjectAccessor(KotlinSource(ktSymbol))

    override val isInstance: Boolean get() = false

    override val visibility: SirVisibility get() = SirVisibility.PUBLIC

    override val type: SirType by lazyWithSessions {
        ktSymbol.toSir().primaryDeclaration?.let { it as? SirNamedDeclaration }?.let { SirNominalType(it) }
            ?: error("Failed to translate object accessor base type $ktSymbol")
    }

    override val getter: SirGetter by lazy {
        SirObjectAccessorGetterFromKtSymbol(ktSymbol, sirSession).also {
            it.parent = this@SirObjectAccessorVariableFromKtSymbol
        }
    }
    override val setter: SirSetter? get() = null

    override val documentation: String? by lazy {
        ktSymbol.documentation()
    }

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
