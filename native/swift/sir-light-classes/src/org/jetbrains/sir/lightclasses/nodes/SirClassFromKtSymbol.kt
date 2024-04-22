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
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule
import org.jetbrains.kotlin.sir.providers.utils.withSirAnalyse
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.documentation
import org.jetbrains.sir.lightclasses.extensions.lazyWithSessions

internal class SirClassFromKtSymbol(
    override val ktSymbol: KtNamedClassOrObjectSymbol,
    override val analysisApiSession: KtAnalysisSession,
    override val sirSession: SirSession,
) : SirClass(), SirFromKtSymbol {

    override val origin: SirOrigin by lazyWithSessions {
        KotlinSource(ktSymbol)
    }
    override val visibility: SirVisibility by lazyWithSessions {
        SirVisibility.PUBLIC
    }
    override val documentation: String? by lazyWithSessions {
        ktSymbol.documentation()
    }
    override val name: String by lazyWithSessions {
        ktSymbol.name.asString()
    }

    override var parent: SirDeclarationParent
        get() = withSirAnalyse(sirSession, analysisApiSession) {
            ktSymbol.getSirParent()
        }
        set(_) = Unit

    override val declarations: List<SirDeclaration> by lazyWithSessions {
        childDeclarations() + syntheticDeclarations()
    }

    override val superClass: SirType? by lazy {
        // For now, we support only `class C : Kotlin.Any()` class declarations, and
        // translate Kotlin.Any to KotlinRuntime.KotlinBase.
        SirNominalType(KotlinRuntimeModule.kotlinBase)
    }

    context(SirSession, KtAnalysisSession)
    private fun childDeclarations(): List<SirDeclaration> =
        ktSymbol.getCombinedDeclaredMemberScope()
            .extractDeclarations()
            .toList()

    private fun syntheticDeclarations(): List<SirDeclaration> = if (ktSymbol.classKind == KtClassKind.OBJECT)
        listOf<SirDeclaration>(
            buildInit {
                origin = SirOrigin.PrivateObjectInit(`for` = KotlinSource(ktSymbol))
                visibility = SirVisibility.PRIVATE
                kind = SirCallableKind.INSTANCE_METHOD
                isFailable = false
                initKind = SirInitializerKind.ORDINARY
            },
            buildVariable {
                origin = SirOrigin.ObjectAccessor(`for` = KotlinSource(ktSymbol))
                visibility = SirVisibility.PUBLIC
                type = SirNominalType(SirSwiftModule.int32) // todo: fixme when types become available - KT-65808
                name = "shared"
                getter = buildGetter {
                    kind = SirCallableKind.STATIC_METHOD
                }
            }.also {
                it.getter.parent = it
            }
        )
            .map { it.also { it.parent = this } }
    else
        emptyList()
}
