/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir.providers.impl

import org.jetbrains.kotlin.kir.providers.KirDeclarationProvider
import org.jetbrains.kotlin.kir.providers.KirTranslationResult
import org.jetbrains.kotlin.sir.SirDeclaration

public class CachingKirDeclarationProvider(
    private val declarationsProvider: KirDeclarationProvider,
) : KirDeclarationProvider {

    private val visitedDeclarations: MutableMap<SirDeclaration, KirTranslationResult> = mutableMapOf()

    override fun SirDeclaration.toKir(): KirTranslationResult {
        return visitedDeclarations.getOrPut(this@toKir) {
            with(declarationsProvider) { this@toKir.toKir() }
        }
    }
}
