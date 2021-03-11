/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.trasformers

import org.jetbrains.kotlin.fir.declarations.FirDeclaration

internal class PhaseReplaceOracle(private val targetDeclaration: FirDeclaration) {
    private var isInsideCurrentDeclaration = false

    fun needReplacePhase(firDeclaration: FirDeclaration): Boolean =
        isInsideCurrentDeclaration

    inline fun <T> transformDeclarationInside(declaration: FirDeclaration, transform: (FirDeclaration) -> T): T {
        if (declaration == targetDeclaration) isInsideCurrentDeclaration = true
        return try {
            transform(declaration)
        } finally {
            if (declaration == targetDeclaration) isInsideCurrentDeclaration = false
        }
    }
}