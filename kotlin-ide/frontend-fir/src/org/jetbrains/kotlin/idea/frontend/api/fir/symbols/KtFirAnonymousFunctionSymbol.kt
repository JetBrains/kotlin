/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.idea.fir.findPsi
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.firRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer

internal class KtFirAnonymousFunctionSymbol(
    fir: FirAnonymousFunction,
    resolveState: FirModuleResolveState,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder
) : KtAnonymousFunctionSymbol(), KtFirSymbol<FirAnonymousFunction> {
    override val firRef = firRef(fir, resolveState)
    override val psi: PsiElement? by firRef.withFirAndCache { fir -> fir.findPsi(fir.session) }

    override val type: KtType by firRef.withFirAndCache(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE) { builder.buildKtType(it.returnTypeRef) }
    override val valueParameters: List<KtParameterSymbol> by firRef.withFirAndCache { fir ->
        fir.valueParameters.map { valueParameter ->
            check(valueParameter is FirValueParameterImpl)
            builder.buildParameterSymbol(valueParameter)
        }
    }

    override fun createPointer(): KtSymbolPointer<KtAnonymousFunctionSymbol> {
        KtPsiBasedSymbolPointer.createForSymbolFromSource(this)?.let { return it }
        error("Could not create a pointer for anonymous function from library")
    }
}