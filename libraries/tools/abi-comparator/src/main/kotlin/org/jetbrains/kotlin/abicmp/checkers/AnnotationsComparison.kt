/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.checkers

import org.jetbrains.kotlin.abicmp.reports.ListEntryDiff
import org.jetbrains.org.objectweb.asm.Type

val IGNORED_ANNOTATIONS = listOf("Lkotlin/Metadata;", "Lkotlin/coroutines/jvm/internal/DebugMetadata;")

fun compareAnnotations(
    annotations1: List<AnnotationEntry>,
    annotations2: List<AnnotationEntry>,
): List<ListEntryDiff>? {
    val anns1Sorted = annotations1.preprocessAnnotations()
    val anns2Sorted = annotations2.preprocessAnnotations()

    val result = ArrayList<ListEntryDiff>()

    var i1 = 0
    var i2 = 0
    val size1 = anns1Sorted.size
    val size2 = anns2Sorted.size
    while (i1 < size1 || i2 < size2) {
        if (i1 < size1 && i2 < size2) {
            val ann1 = anns1Sorted[i1]
            val ann2 = anns2Sorted[i2]

            // TODO proper comparison for annotation argument values?
            if (ann1.fullString() == ann2.fullString()) {
                ++i1
                ++i2
            } else {
                when {
                    ann1.desc == ann2.desc -> {
                        ++i1
                        ++i2
                        result.add(ListEntryDiff(ann1.fullString(), ann2.fullString()))
                    }
                    ann1.desc < ann2.desc -> {
                        ++i1
                        result.add(ListEntryDiff(ann1.fullString(), null))
                    }
                    else -> {
                        ++i2
                        result.add(ListEntryDiff(null, ann2.fullString()))
                    }
                }
            }
        } else {
            if (i1 < size1) {
                val ann1 = anns1Sorted[i1]
                ++i1
                result.add(ListEntryDiff(ann1.fullString(), null))
            } else {
                val ann2 = anns2Sorted[i2]
                ++i2
                result.add(ListEntryDiff(null, ann2.fullString()))
            }
        }
    }

    return if (result.isEmpty()) null else result
}

private fun List<AnnotationEntry>.preprocessAnnotations() =
    filter { it.desc !in IGNORED_ANNOTATIONS }.sortedBy { it.fullString() }

private fun AnnotationEntry.shortString(): String =
    if (values.isEmpty())
        "@$desc"
    else
        "@$desc(...)"

private fun AnnotationEntry.fullString(): String =
    if (values.isEmpty())
        "@$desc"
    else
        "@$desc( ${values.joinToString { it.toValueString() }} )"

private fun Pair<String, Any?>.toValueString(): String =
    "$first: ${second.toValueString()}"

private fun Any?.toValueString(): String =
    when (this) {
        null -> "NULL"
        is Type -> "<$descriptor>"
        is List<*> -> joinToString(separator = ", ", prefix = "#{ ", postfix = " }") { it.toValueString() }
        is Array<*> -> toList().toValueString()
        else -> toString()
    }