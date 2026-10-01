/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.util

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess

/** FIR declarations indexed by source range for metadata-less generated IR declarations. */
class FirDeclarationsBySource private constructor(private val entries: List<Entry>) {
    private class Entry(val startOffset: Int, val endOffset: Int, val metadata: FirMetadataSource)

    /** Smallest declaration whose source range contains [startOffset]..[endOffset]. */
    fun findInnermostContaining(startOffset: Int, endOffset: Int): FirMetadataSource? {
        if (startOffset < 0 || endOffset < 0) return null

        var best: Entry? = null
        for (entry in entries) {
            if (entry.startOffset > startOffset || entry.endOffset < endOffset) continue
            if (best == null || (entry.endOffset - entry.startOffset) < (best.endOffset - best.startOffset)) {
                best = entry
            }
        }
        return best?.metadata
    }

    companion object {
        @OptIn(DirectDeclarationsAccess::class)
        fun of(file: FirFile): FirDeclarationsBySource {
            val entries = mutableListOf<Entry>()
            file.declarations.forEach { collect(it, entries) }
            return FirDeclarationsBySource(entries)
        }

        @OptIn(DirectDeclarationsAccess::class)
        private fun collect(declaration: FirDeclaration, entries: MutableList<Entry>) {
            val source = declaration.source
            if (source != null && source.kind == KtRealSourceElementKind) {
                metadataSourceOf(declaration)?.let { entries += Entry(source.startOffset, source.endOffset, it) }
            }

            when (declaration) {
                is FirClass -> declaration.declarations.forEach { collect(it, entries) }
                is FirProperty -> {
                    declaration.getter?.let { collect(it, entries) }
                    declaration.setter?.let { collect(it, entries) }
                    declaration.backingField?.let { collect(it, entries) }
                }
                else -> {}
            }
        }

        private fun metadataSourceOf(declaration: FirDeclaration): FirMetadataSource? = when (declaration) {
            is FirClass -> FirMetadataSource.Class(declaration)
            is FirFunction -> FirMetadataSource.Function(declaration)
            is FirProperty -> FirMetadataSource.Property(declaration)
            is FirField -> FirMetadataSource.Field(declaration)
            else -> null
        }
    }
}
