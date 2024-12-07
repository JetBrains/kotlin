/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.checkers

import org.jetbrains.kotlin.abicmp.reports.ListEntryDiff

fun compareLists(list1: List<String>, list2: List<String>): List<ListEntryDiff>? {
    val result = ArrayList<ListEntryDiff>()
    var i1 = 0
    var i2 = 0
    while (i1 < list1.size || i2 < list2.size) {
        val s1 = list1.getOrNull(i1)
        val s2 = list2.getOrNull(i2)

        if (s1 == s2) {
            ++i1
            ++i2
            continue
        }

        when {
            s1 == null && s2 == null ->
                break // really should not happen
            s1 == null -> {
                result.add(ListEntryDiff(null, s2))
                ++i2
            }
            s2 == null -> {
                result.add(ListEntryDiff(s1, null))
                ++i1
            }
            s1 < s2 -> {
                result.add(ListEntryDiff(s1, null))
                ++i1
            }
            s1 > s2 -> {
                result.add(ListEntryDiff(null, s2))
                ++i2
            }
        }
    }

    return if (result.isEmpty()) null else result
}