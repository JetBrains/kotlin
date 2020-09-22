/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.fir.findPsi
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.pointers.KtFirEnumEntrySymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.pointers.createSignature
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.firRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal class KtFirEnumEntrySymbol(
    fir: FirEnumEntry,
    resolveState: FirModuleResolveState,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder
) : KtEnumEntrySymbol(), KtFirSymbol<FirEnumEntry> {
    override val firRef = firRef(fir, resolveState)
    override val psi: PsiElement? by firRef.withFirAndCache { it.findPsi(fir.session) }

    override val name: Name get() = firRef.withFir { it.name }
    override val type: KtType by firRef.withFirAndCache(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE) { fir -> builder.buildKtType(fir.returnTypeRef) }

    override val containingEnumClassIdIfNonLocal: ClassId?
        get() = firRef.withFir { it.symbol.callableId.classId?.takeUnless { it.isLocal } }

    override fun createPointer(): KtSymbolPointer<KtEnumEntrySymbol> {
        KtPsiBasedSymbolPointer.createForSymbolFromSource(this)?.let { return it }
        return KtFirEnumEntrySymbolPointer(containingEnumClassIdIfNonLocal!!, firRef.withFir { it.createSignature() })
    }
}