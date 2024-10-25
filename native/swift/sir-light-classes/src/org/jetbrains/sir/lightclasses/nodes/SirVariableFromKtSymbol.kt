/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildGetter
import org.jetbrains.kotlin.sir.builder.buildSetter
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.utils.addToStdlib.ifFalse
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.*
import org.jetbrains.sir.lightclasses.extensions.documentation
import org.jetbrains.sir.lightclasses.extensions.lazyWithSessions
import org.jetbrains.sir.lightclasses.extensions.withSessions
import org.jetbrains.sir.lightclasses.utils.isSubtypeOf
import org.jetbrains.sir.lightclasses.utils.overridableCandidates
import org.jetbrains.sir.lightclasses.utils.translateReturnType
import org.jetbrains.sir.lightclasses.utils.translatedAttributes

internal class SirVariableFromKtSymbol(
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
            it.parent = this@SirVariableFromKtSymbol
        }
    }
    override val setter: SirSetter? by lazy {
        ((ktSymbol as? KaPropertySymbol)?.let {
            it.setter?.let {
                SirSetterFromKtSymbol(it, ktModule, sirSession)
            }
        } ?: ktSymbol.isVal.ifFalse { buildSetter() })?.also {
            it.parent = this@SirVariableFromKtSymbol
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

    override val attributes: List<SirAttribute> by lazy { this.translatedAttributes }

    override val isOverride: Boolean
        get() = isInstance && overridableCandidates.any {
            this.name == it.name &&
            (it.setter == null && this.type.isSubtypeOf(it.type) || this.type == it.type) &&
            this.isInstance == it.isInstance
        }

    override val isInstance: Boolean
        get() = !ktSymbol.isTopLevel

    override val modality: SirModality
        get() = ktSymbol.modality.sirModality
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
    override var body: SirFunctionBody? = null
    override val parameterName: String = "newValue"
}
