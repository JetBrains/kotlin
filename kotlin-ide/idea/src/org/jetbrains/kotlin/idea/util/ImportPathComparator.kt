/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util

import org.jetbrains.kotlin.idea.core.formatter.KotlinPackageEntry
import org.jetbrains.kotlin.idea.core.formatter.KotlinPackageEntryTable
import org.jetbrains.kotlin.resolve.ImportPath
import java.util.Comparator

class ImportPathComparator(
    private val packageTable: KotlinPackageEntryTable
) : Comparator<ImportPath> {
    private val comparator = compareBy<ImportPath>(
        { import -> bestMatchIndex(import) },
        { import -> import.toString() }
    )

    override fun compare(import1: ImportPath, import2: ImportPath): Int = comparator.compare(import1, import2)

    private fun bestMatchIndex(path: ImportPath): Int {
        var bestIndex: Int? = null
        var bestEntryMatch: KotlinPackageEntry? = null

        for ((index, entry) in packageTable.getEntries().withIndex()) {
            if (entry.isBetterMatchForPackageThan(bestEntryMatch, path)) {
                bestEntryMatch = entry
                bestIndex = index
            }
        }

        return bestIndex!!
    }
}
