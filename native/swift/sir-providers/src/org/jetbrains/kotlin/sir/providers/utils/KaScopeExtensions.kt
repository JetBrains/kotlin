/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.utils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.providers.SirSession

public fun KaScope.extractDeclarations(ktAnalysisSession: KaSession, sirSession: SirSession): Sequence<SirDeclaration> = with(sirSession) {
    declarations.filter {
        when (it.sirVisibility(ktAnalysisSession)) {
            null, SirVisibility.PRIVATE, SirVisibility.FILEPRIVATE, SirVisibility.INTERNAL -> false
            SirVisibility.PUBLIC, SirVisibility.PACKAGE -> true
        }
    }
        .flatMap { it.toSir().allDeclarations }
        .flatMap { listOf(it) + it.trampolineDeclarations() }
}