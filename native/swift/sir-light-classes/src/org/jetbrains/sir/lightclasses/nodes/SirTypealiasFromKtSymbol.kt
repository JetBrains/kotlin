/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeAliasSymbol
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.utils.updateImports
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.documentation
import org.jetbrains.sir.lightclasses.extensions.lazyWithSessions
import org.jetbrains.sir.lightclasses.extensions.withSessions

internal class SirTypealiasFromKtSymbol(
    override val ktSymbol: KaTypeAliasSymbol,
    override val ktModule: KtModule,
    override val sirSession: SirSession,
) : SirTypealias(), SirFromKtSymbol<KaTypeAliasSymbol> {

    override val origin: SirOrigin = KotlinSource(ktSymbol)
    override val visibility: SirVisibility = SirVisibility.PUBLIC
    override val documentation: String? by lazy {
        ktSymbol.documentation()
    }
    override val name: String by lazy {
        ktSymbol.name.asString()
    }

    @OptIn(KaExperimentalApi::class)
    override val type: SirType by lazyWithSessions {
        ktSymbol.expandedType.translateType(
            useSiteSession,
            reportErrorType = { error("Can't translate ${ktSymbol.render()} type: $it") },
            reportUnsupportedType = { error("Can't translate ${ktSymbol.render()} type: it is not supported") },
            processTypeImports = ktSymbol.containingModule.sirModule()::updateImports
        )
    }

    override var parent: SirDeclarationParent
        get() = withSessions {
            ktSymbol.getSirParent(useSiteSession)
        }
        set(_) = Unit
}