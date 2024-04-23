/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeAliasSymbol
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirTypeProvider
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.utils.withSirAnalyse
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.documentation
import org.jetbrains.sir.lightclasses.extensions.lazyWithSessions
import org.jetbrains.sir.lightclasses.extensions.withSessions

internal class SirTypealiasFromKtSymbol(
    override val ktSymbol: KtTypeAliasSymbol,
    override val ktModule: KtModule,
    override val sirSession: SirSession,
) : SirTypealias(), SirFromKtSymbol<KtTypeAliasSymbol> {

    override val origin: SirOrigin = KotlinSource(ktSymbol)
    override val visibility: SirVisibility = SirVisibility.PUBLIC
    override val documentation: String? by lazy {
        ktSymbol.documentation()
    }
    override val name: String by lazy {
        ktSymbol.name.asString()
    }
    override val type: SirType by lazyWithSessions {
        ktSymbol.expandedType.translateType(analysisSession)
    }

    override var parent: SirDeclarationParent
        get() = withSessions {
            ktSymbol.getSirParent(analysisSession)
        }
        set(_) = Unit

    init {
        withSirAnalyse(sirSession, analysisApiSession) {
            name = ktSymbol.name.asString()
            val typeRequest = SirTypeProvider.TranslationRequest(ktSymbol.expandedType)
            type = when (val response = translateType(typeRequest)) {
                is SirTypeProvider.TranslationResponse.Success -> {
                    ktSymbol.getContainingModule().sirModule().updateImports(response.containingModules)
                    response.sirType
                }
                is SirTypeProvider.TranslationResponse.Unknown ->
                    error("Underlying type of typealias ${ktSymbol.render()} is not declared")
                is SirTypeProvider.TranslationResponse.Unsupported ->
                    error("Underlying type of typealias ${ktSymbol.render()} is not supported yet.")
            }
            origin = KotlinSource(ktSymbol)
            documentation = ktSymbol.documentation()
        }
    }
}