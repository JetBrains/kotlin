/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildGetter
import org.jetbrains.kotlin.sir.builder.buildInit
import org.jetbrains.kotlin.sir.builder.buildVariable
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule
import org.jetbrains.kotlin.sir.providers.utils.computeIsOverrideForDesignatedInit
import org.jetbrains.kotlin.sir.providers.utils.updateImports
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.documentation
import org.jetbrains.sir.lightclasses.extensions.lazyWithSessions
import org.jetbrains.sir.lightclasses.extensions.withSessions

internal class SirClassFromKtSymbol(
    override val ktSymbol: KtNamedClassOrObjectSymbol,
    override val ktModule: KtModule,
    override val sirSession: SirSession,
) : SirClass(), SirFromKtSymbol<KtNamedClassOrObjectSymbol> {

    override val origin: SirOrigin by lazy {
        KotlinSource(ktSymbol)
    }
    override val visibility: SirVisibility by lazy {
        SirVisibility.PUBLIC
    }
    override val documentation: String? by lazy {
        ktSymbol.documentation()
    }
    override val name: String by lazy {
        ktSymbol.name.asString()
    }

    override var parent: SirDeclarationParent
        get() = withSessions {
            ktSymbol.getSirParent(analysisSession)
        }
        set(_) = Unit

    override val declarations: List<SirDeclaration> by lazyWithSessions {
        childDeclarations() + syntheticDeclarations()
    }

    override val superClass: SirType? by lazyWithSessions {
        // For now, we support only `class C : Kotlin.Any()` class declarations, and
        // translate Kotlin.Any to KotlinRuntime.KotlinBase.
        ktSymbol.getContainingModule().sirModule().updateImports(listOf(SirImport(KotlinRuntimeModule.name)))
        SirNominalType(KotlinRuntimeModule.kotlinBase)
    }

    private fun childDeclarations(): List<SirDeclaration> = withSessions {
        ktSymbol.getCombinedDeclaredMemberScope()
            .extractDeclarations(analysisSession)
            .toList()
    }

    private fun syntheticDeclarations(): List<SirDeclaration> = if (ktSymbol.classKind == KtClassKind.OBJECT)
        listOf<SirDeclaration>(
            buildInit {
                origin = SirOrigin.PrivateObjectInit(`for` = KotlinSource(ktSymbol))
                visibility = SirVisibility.PRIVATE
                kind = SirCallableKind.CLASS_METHOD
                isFailable = false
                initKind = SirInitializerKind.ORDINARY
                isOverride = computeIsOverrideForDesignatedInit(this@SirClassFromKtSymbol, emptyList())
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
