/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildGetter
import org.jetbrains.kotlin.sir.builder.buildSetter
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.impl.sirDeclarationName
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.utils.throwsAnnotation
import org.jetbrains.kotlin.sir.providers.utils.updateImports
import org.jetbrains.kotlin.utils.addToStdlib.ifFalse
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.*
import org.jetbrains.sir.lightclasses.extensions.documentation
import org.jetbrains.sir.lightclasses.extensions.lazyWithSessions
import org.jetbrains.sir.lightclasses.extensions.withSessions
import org.jetbrains.sir.lightclasses.utils.*
import org.jetbrains.sir.lightclasses.utils.translateReturnType
import org.jetbrains.sir.lightclasses.utils.translatedAttributes

internal abstract class SirAbstractVariableFromKtSymbol(
    override val ktSymbol: KaVariableSymbol,
    override val ktModule: KaModule,
    override val sirSession: SirSession,
) : SirVariable(), SirFromKtSymbol<KaVariableSymbol> {

    override val visibility: SirVisibility = SirVisibility.PUBLIC

    override val origin: SirOrigin by lazy {
        KotlinSource(ktSymbol)
    }
    override val name: String by lazyWithSessions {
        ktSymbol.sirDeclarationName()
    }
    override val type: SirType by lazy {
        translateReturnType()
    }
    override val getter: SirGetter by lazy {
        ((ktSymbol as? KaPropertySymbol)?.let {
            it.getter?.let {
                SirGetterFromKtSymbol(it, ktModule, sirSession)
            }
        } ?: buildGetter()).also {
            it.parent = this@SirAbstractVariableFromKtSymbol
        }
    }
    override val setter: SirSetter? by lazy {
        ((ktSymbol as? KaPropertySymbol)?.let {
            it.setter?.let {
                SirSetterFromKtSymbol(it, ktModule, sirSession)
            }
        } ?: ktSymbol.isVal.ifFalse { buildSetter() })?.also {
            it.parent = this@SirAbstractVariableFromKtSymbol
        }
    }
    override val documentation: String? by lazy {
        ktSymbol.documentation()
    }

    override var parent: SirDeclarationParent
        get() = withSessions {
            ktSymbol.getSirParent(useSiteSession)
        }
        set(_) = Unit

    override val attributes: List<SirAttribute> by lazy {
        this.translatedAttributes + listOfNotNull(SirAttribute.NonOverride.takeIf { overrideStatus is OverrideStatus.Conflicts })
    }

    override val isOverride: Boolean
        get() = overrideStatus is OverrideStatus.Overrides

    private val overrideStatus: OverrideStatus<SirVariable>? by lazy { computeIsOverride() }

    override val modality: SirModality
        get() = ktSymbol.modality.sirModality
}

internal class SirVariableFromKtSymbol(
    ktSymbol: KaVariableSymbol,
    ktModule: KaModule,
    sirSession: SirSession,
) : SirAbstractVariableFromKtSymbol(ktSymbol, ktModule, sirSession) {
    override val isInstance: Boolean
        get() = !ktSymbol.isTopLevel && !(ktSymbol is KaPropertySymbol && ktSymbol.isStatic)
}

@OptIn(KaExperimentalApi::class)
internal class SirEnumEntriesStaticPropertyFromKtSymbol(
    ktSymbol: KaPropertySymbol,
    ktModule: KaModule,
    sirSession: SirSession,
) : SirAbstractVariableFromKtSymbol(ktSymbol, ktModule, sirSession) {
    override val isInstance: Boolean
        get() = false

    override val name: String
        get() = "allCases"

    override val type: SirType by lazyWithSessions {
        SirArrayType(
            (ktSymbol.returnType as KaClassType)
                .typeArguments.first().type!!
                .translateType(
                    useSiteSession,
                    reportErrorType = { error("Can't translate return type in ${ktSymbol.render()}: ${it}") },
                    reportUnsupportedType = { error("Can't translate return type in ${ktSymbol.render()}: type is not supported") },
                    processTypeImports = ktSymbol.containingModule.sirModule()::updateImports
                )
        )
    }
}

internal class SirEnumCaseFromKtSymbol(
    ktSymbol: KaEnumEntrySymbol,
    ktModule: KaModule,
    sirSession: SirSession,
) : SirAbstractVariableFromKtSymbol(ktSymbol, ktModule, sirSession) {
    override val isInstance: Boolean = false
}

internal class SirGetterFromKtSymbol(
    override val ktSymbol: KaPropertyGetterSymbol,
    override val ktModule: KaModule,
    override val sirSession: SirSession,
) : SirGetter(), SirFromKtSymbol<KaPropertyGetterSymbol> {
    override val origin: SirOrigin by lazy { KotlinSource(ktSymbol) }
    override val visibility: SirVisibility get() = SirVisibility.PUBLIC
    override val documentation: String? by lazy { ktSymbol.documentation() }
    override lateinit var parent: SirDeclarationParent
    override val attributes: List<SirAttribute> by lazy { this.translatedAttributes }
    override val errorType: SirType get() = if (ktSymbol.throwsAnnotation != null) SirType.any else SirType.never
    override var body: SirFunctionBody? = null
}

internal class SirSetterFromKtSymbol(
    override val ktSymbol: KaPropertySetterSymbol,
    override val ktModule: KaModule,
    override val sirSession: SirSession,
) : SirSetter(), SirFromKtSymbol<KaPropertySetterSymbol> {
    override val origin: SirOrigin by lazy { KotlinSource(ktSymbol) }
    override val visibility: SirVisibility get() = SirVisibility.PUBLIC
    override val documentation: String? by lazy { ktSymbol.documentation() }
    override lateinit var parent: SirDeclarationParent
    override val attributes: List<SirAttribute> by lazy { this.translatedAttributes }
    override val errorType: SirType get() = SirType.never
    override var body: SirFunctionBody? = null
    override val parameterName: String = "newValue"
}
