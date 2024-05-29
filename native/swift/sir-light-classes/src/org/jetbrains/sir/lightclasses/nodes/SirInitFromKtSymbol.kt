/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.utils.computeIsOverrideForDesignatedInit
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.documentation
import org.jetbrains.sir.lightclasses.extensions.lazyWithSessions
import org.jetbrains.sir.lightclasses.extensions.withSessions
import org.jetbrains.sir.lightclasses.utils.translateParameters

internal class SirInitFromKtSymbol(
    override val ktSymbol: KaConstructorSymbol,
    override val ktModule: KtModule,
    override val sirSession: SirSession,
) : SirInit(), SirFromKtSymbol<KaConstructorSymbol> {

    override val visibility: SirVisibility = SirVisibility.PUBLIC
    override val isFailable: Boolean = false
    override val initKind: SirInitializerKind = SirInitializerKind.ORDINARY

    override val origin: SirOrigin by lazy {
        KotlinSource(ktSymbol)
    }
    override val kind: SirCallableKind by lazy {
        SirCallableKind.CLASS_METHOD
    }
    override val parameters: List<SirParameter> by lazy {
        translateParameters()
    }
    override val documentation: String? by lazyWithSessions {
        ktSymbol.documentation()
    }

    override val isOverride: Boolean by lazy {
        computeIsOverrideForDesignatedInit(parent, parameters)
    }

    override var parent: SirDeclarationParent
        get() = withSessions {
            ktSymbol.getSirParent(analysisSession)
        }
        set(_) = Unit

    override var body: SirFunctionBody? = null
}