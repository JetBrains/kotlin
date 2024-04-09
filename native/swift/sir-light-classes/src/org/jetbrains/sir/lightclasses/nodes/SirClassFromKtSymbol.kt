/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildGetter
import org.jetbrains.kotlin.sir.builder.buildInit
import org.jetbrains.kotlin.sir.builder.buildVariable
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.utils.withSirAnalyse
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.documentation

internal class SirClassFromKtSymbol(
    override val ktSymbol: KtNamedClassOrObjectSymbol,
    override val analysisApiSession: KtAnalysisSession,
    override val sirSession: SirSession,
) : SirClass(), SirFromKtSymbol {

    override val origin: SirOrigin
    override val visibility: SirVisibility = SirVisibility.PUBLIC
    override var documentation: String?
    override val name: String

    override var parent: SirDeclarationParent
        get() = withSirAnalyse(sirSession, analysisApiSession) {
            ktSymbol.getSirParent()
        }
        set(_) = Unit

    init {
        name = ktSymbol.name.asString()
        origin = KotlinSource(ktSymbol)
        documentation = ktSymbol.documentation()
    }

    override val declarations: List<SirDeclaration> by lazy {
        childDeclarations() + syntheticDeclarations()
    }

    private fun childDeclarations(): List<SirDeclaration> = withSirAnalyse(sirSession, analysisApiSession) {
        ktSymbol.getCombinedDeclaredMemberScope()
            .extractDeclarations()
            .toList()
    }

    private fun syntheticDeclarations(): List<SirDeclaration> = if (ktSymbol.classKind == KtClassKind.OBJECT)
        listOf<SirDeclaration>(
            buildInit {
                origin = SirOrigin.PrivateObjectInit(`for` = KotlinSource(ktSymbol))
                visibility = SirVisibility.PRIVATE
                kind = SirCallableKind.INSTANCE_METHOD
                isFailable = false
                initKind = SirInitializerKind.ORDINARY
                parent = this@SirClassFromKtSymbol
            },
            buildVariable {
                origin = SirOrigin.ObjectAccessor(`for` = KotlinSource(ktSymbol))
                visibility = SirVisibility.PUBLIC
                type = SirNominalType(SirSwiftModule.int32) // todo: fixme when types become available - KT-65808
                name = "shared"
                getter = buildGetter {
                    kind = SirCallableKind.STATIC_METHOD
                }
                parent = this@SirClassFromKtSymbol
            }.also { it.getter.parent = it }
        )
    else
        emptyList()
}
