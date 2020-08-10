/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.utils

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.fir.low.level.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.assertIsValid
import java.lang.ref.WeakReference

internal class FirRef<D : FirDeclaration>(fir: D, resolveState: FirModuleResolveState, val token: ValidityToken) {
    private val firWeakRef = WeakReference(fir)
    private val resolveStateWeakRef = WeakReference(resolveState)

    inline fun <R> withFir(phase: FirResolvePhase = FirResolvePhase.RAW_FIR, action: (fir: D) -> R): R {
        token.assertIsValid()
        val fir = firWeakRef.get() ?: error("FirElement was garbage collected while analysis session is still valid")
        val resolveState =
            resolveStateWeakRef.get() ?: error("FirModuleResolveState was garbage collected while analysis session is still valid")
        return action(resolveState.resolvedFirToPhase(fir, phase))
    }

    inline fun <R> withFirAndCache(phase: FirResolvePhase = FirResolvePhase.RAW_FIR, crossinline createValue: (fir: D) -> R) =
        ValidityAwareCachedValue(token) {
            withFir(phase) { fir -> createValue(fir) }
        }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <D : FirDeclaration> ValidityTokenOwner.firRef(fir: D, resolveState: FirModuleResolveState) =
    FirRef(fir, resolveState, token)