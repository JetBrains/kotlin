/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.idea.fir.findPsi
import org.jetbrains.kotlin.idea.fir.low.level.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.firRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal class KtFirConstructorValueParameterSymbol(
    fir: FirValueParameterImpl,
    resolveState: FirModuleResolveState,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder
) : KtConstructorParameterSymbol(), KtFirSymbol<FirValueParameterImpl> {
    override val firRef = firRef(fir, resolveState)
    override val psi: PsiElement? by firRef.withFirAndCache { it.findPsi(fir.session) }

    override val name: Name get() = firRef.withFir { it.name }
    override val type: KtType by firRef.withFirAndCache(FirResolvePhase.TYPES) { builder.buildKtType(it.returnTypeRef) }
    override val symbolKind: KtSymbolKind
        get() = firRef.withFir { fir ->
            when {
                fir.isVal || fir.isVal -> KtSymbolKind.MEMBER
                else -> KtSymbolKind.NON_PROPERTY_PARAMETER
            }
        }

    override val constructorParameterKind: KtConstructorParameterSymbolKind
        get() = firRef.withFir { fir ->
            when {
                fir.isVal -> KtConstructorParameterSymbolKind.VAL_PROPERTY
                fir.isVar -> KtConstructorParameterSymbolKind.VAR_PROPERTY
                else -> KtConstructorParameterSymbolKind.NON_PROPERTY
            }
        }

    override val hasDefaultValue: Boolean get() = firRef.withFir { it.defaultValue != null }

    override fun createPointer(): KtSymbolPointer<KtConstructorParameterSymbol> {
        KtPsiBasedSymbolPointer.createForSymbolFromSource(this)?.let { return it }
        TODO("Creating symbols for library constructor parameters is not supported yet")
    }
}