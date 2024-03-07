/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.kt.nodes

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildGetter
import org.jetbrains.kotlin.sir.builder.buildSetter
import org.jetbrains.kotlin.sir.kt.KotlinSource
import org.jetbrains.kotlin.sir.kt.SirSession
import org.jetbrains.kotlin.sir.kt.sirCallableKind
import org.jetbrains.kotlin.sir.visitors.SirTransformer
import org.jetbrains.kotlin.sir.visitors.SirVisitor

public class SirVariableFromSymbol(
    private val ktSymbol: KtKotlinPropertySymbol,
    public override val ktAnalysisSession: KtAnalysisSession,
    public override val sirSession: SirSession,
) : SirVariable(), SirDeclarationFromSymbol {
    override fun <R, D> acceptChildren(visitor: SirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: SirTransformer<D>, data: D) {}

    override val origin: SirOrigin by lazy {
        KotlinSource(ktSymbol)
    }
    override val visibility: SirVisibility by lazy {
        withTranslationContext {
            ktSymbol.sirVisibility()!!
        }
    }

    override var parent: SirDeclarationParent
        get() = withTranslationContext {
            ktSymbol.sirDeclarationParent()
        }
        set(value) {}
    override val name: String by lazy {
        withTranslationContext {
            ktSymbol.sirDeclarationName()
        }
    }
    override val type: SirType by lazy {
        withTranslationContext {
            ktSymbol.returnType.translateType()
        }
    }
    override val getter: SirGetter by lazy {
        buildGetter {
            origin = KotlinSource(ktSymbol.getter!!)
            kind = ktSymbol.sirCallableKind
            // TODO: Visibility of accessor
        }.also {
            it.parent = this
        }
    }
    override val setter: SirSetter? by lazy {
        if (ktSymbol.hasSetter) {
            buildSetter {
                origin = KotlinSource(ktSymbol.setter!!)
                kind = ktSymbol.sirCallableKind
                // TODO: Visibility of accessor
            }.also {
                it.parent = this
            }
        } else null
    }
}