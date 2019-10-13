// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.stats.completion

data class LookupState(val ids: List<Int>,
                       val newItems: List<LookupEntryInfo>,
                       val itemsDiff: List<LookupEntryDiff>,
                       val selectedPosition: Int,
                       val commonSessionFactors: Map<String, String>)

fun LookupState.withSelected(position: Int): LookupState {
    return LookupState(ids, newItems, itemsDiff, position, commonSessionFactors)
}

fun LookupState.withoutNewItems(): LookupState {
    return LookupState(ids, emptyList(), itemsDiff, selectedPosition, commonSessionFactors)
}