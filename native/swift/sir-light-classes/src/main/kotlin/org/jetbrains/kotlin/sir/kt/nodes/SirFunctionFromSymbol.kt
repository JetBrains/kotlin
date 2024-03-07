/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.kt.nodes

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.analysis.api.symbols.psiSafe
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.kt.KotlinSource
import org.jetbrains.kotlin.sir.kt.SirSession
import org.jetbrains.kotlin.sir.kt.sirCallableKind
import org.jetbrains.kotlin.sir.visitors.SirTransformer
import org.jetbrains.kotlin.sir.visitors.SirVisitor

public class SirFunctionFromSymbol(
    private val ktSymbol: KtFunctionLikeSymbol,
    public override val ktAnalysisSession: KtAnalysisSession,
    public override val sirSession: SirSession,
) : SirFunction(), SirDeclarationFromSymbol {
    override fun <R, D> acceptChildren(visitor: SirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: SirTransformer<D>, data: D) {}

    override val origin: SirOrigin by lazy {
        KotlinSource(ktSymbol)
    }
    override val visibility: SirVisibility by lazy {
        withTranslationContext {
            if (ktSymbol is KtSymbolWithVisibility) {
                ktSymbol.sirVisibility()!!
            } else {
                SirVisibility.PRIVATE
            }
        }
    }

    override var parent: SirDeclarationParent
        get() {
            return withTranslationContext {
                ktSymbol.sirDeclarationParent()
            }
        }
        set(value) {}

    override val kind: SirCallableKind by lazy {
        ktSymbol.sirCallableKind
    }
    override var body: SirFunctionBody? = null
    override val name: String by lazy {
        with (sirSession) {
            ktSymbol.sirDeclarationName()
        }
    }

    override val parameters: List<SirParameter> by lazy {
        ktSymbol.valueParameters.map {
            SirParameterFromSymbol(it, ktAnalysisSession, sirSession)
        }
    }
    override val returnType: SirType by lazy {
        withTranslationContext {
            ktSymbol.returnType.translateType()
        }
    }
    override var documentation: String?
        get() = ktSymbol.psiSafe<KtFunction>()?.docComment?.text
        set(value) {}
}