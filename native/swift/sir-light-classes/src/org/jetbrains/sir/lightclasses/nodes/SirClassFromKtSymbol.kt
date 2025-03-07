/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.components.DefaultTypeClassIds
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildGetter
import org.jetbrains.kotlin.sir.builder.buildInit
import org.jetbrains.kotlin.sir.builder.buildVariable
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeSupportModule
import org.jetbrains.kotlin.sir.providers.utils.containingModule
import org.jetbrains.kotlin.sir.providers.utils.extractDeclarations
import org.jetbrains.kotlin.sir.providers.utils.updateImport
import org.jetbrains.kotlin.sir.util.SirSwiftModule
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

internal fun createSirClassFromKtSymbol(
    ktSymbol: KaNamedClassSymbol,
    ktModule: KaModule,
    sirSession: SirSession,
): SirAbstractClassFromKtSymbol = when (ktSymbol.classKind) {
    KaClassKind.ENUM_CLASS ->
        SirEnumClassFromKtSymbol(
            ktSymbol,
            ktModule,
            sirSession
        )
    else -> SirClassFromKtSymbol(
        ktSymbol,
        ktModule,
        sirSession
    )
}

private class SirClassFromKtSymbol(
    ktSymbol: KaNamedClassSymbol,
    ktModule: KaModule,
    sirSession: SirSession,
) : SirAbstractClassFromKtSymbol(
    ktSymbol,
    ktModule,
    sirSession
) {
    override val superClass: SirType? by lazyWithSessions {
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
}

internal class SirEnumClassFromKtSymbol(
    ktSymbol: KaNamedClassSymbol,
    ktModule: KaModule,
    sirSession: SirSession,
) : SirAbstractClassFromKtSymbol(
    ktSymbol,
    ktModule,
    sirSession
) {
    override val superClass: SirType? by lazyWithSessions {
        // TODO: this super class as default will become obsolete with the KT-66855
        SirNominalType(KotlinRuntimeModule.kotlinBase).also {
            ktSymbol.containingModule.sirModule()
        }
    }
    override val protocols: List<SirProtocol> = super.protocols + listOf(SirSwiftModule.caseIterable)
}

internal abstract class SirAbstractClassFromKtSymbol(
    override val ktSymbol: KaNamedClassSymbol,
    override val ktModule: KaModule,
    override val sirSession: SirSession,
) : SirClass(), SirFromKtSymbol<KaNamedClassSymbol> {

    override val origin: SirOrigin by lazy {
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
    override val name: String by lazy {
        ktSymbol.name.asString()
    }

    override var parent: SirDeclarationParent
        get() = withSessions {
            ktSymbol.getSirParent(useSiteSession)
        }
        set(_) = Unit

    override val declarations: List<SirDeclaration> by lazyWithSessions {
        childDeclarations + syntheticDeclarations()
    }

    override val attributes: List<SirAttribute> by lazy { this.translatedAttributes }

    protected val childDeclarations: List<SirDeclaration> by lazyWithSessions {
        ktSymbol.combinedDeclaredMemberScope
            .extractDeclarations(useSiteSession, sirSession)
            .toList()
    }

    private fun kotlinBaseInitDeclaration(): SirDeclaration = buildInit {
        origin = SirOrigin.KotlinBaseInitOverride(`for` = KotlinSource(ktSymbol))
        visibility = SirVisibility.PACKAGE // Hide from users, but not from other Swift Export modules.
        isFailable = false
        isOverride = true
        parameters.add(
            SirParameter(
                argumentName = "__externalRCRef",
                type = SirNominalType(SirSwiftModule.uint)
            )
        )
    }.also { it.parent = this }

    private fun syntheticDeclarations(): List<SirDeclaration> = when (ktSymbol.classKind) {
        KaClassKind.OBJECT, KaClassKind.COMPANION_OBJECT -> listOf(
            kotlinBaseInitDeclaration(),
            SirObjectSyntheticInit(ktSymbol),
            buildVariable {
                origin = SirOrigin.ObjectAccessor(`for` = KotlinSource(ktSymbol))
                visibility = SirVisibility.PUBLIC
                type = SirNominalType(this@SirAbstractClassFromKtSymbol)
                name = "shared"
                isInstance = false
                modality = SirModality.FINAL
                getter = buildGetter {}
            }.also {
                it.getter.parent = it
            }
        ).onEach { it.parent = this }

        else -> listOf(
            kotlinBaseInitDeclaration()
        )
    }

    override val protocols: List<SirProtocol> by lazyWithSessions {
        (translatedProtocols + listOf(KotlinRuntimeSupportModule.kotlinBridged))
            .filter { superClassDeclaration?.declaresConformance(it) != true }
    }

    private val translatedProtocols: List<SirProtocol> by lazyWithSessions {
        ktSymbol.superTypes
            .filterIsInstance<KaClassType>().mapNotNull { it.expandedSymbol }.filter {
                it.classKind == KaClassKind.INTERFACE
            }.flatMap {
                it.toSir().allDeclarations.filterIsInstance<SirProtocol>().also {
                    it.forEach {
                        ktSymbol.containingModule.sirModule().updateImport(SirImport(it.containingModule().name))
                    }
                }
            }
    }
}

internal class SirObjectSyntheticInit(ktSymbol: KaNamedClassSymbol) : SirInit() {
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
    override var body: SirFunctionBody? = null
}

private val KaClassType.isRegularClass: Boolean
    get() = (symbol as? KaClassSymbol)?.let { it.classKind == KaClassKind.CLASS } ?: false
