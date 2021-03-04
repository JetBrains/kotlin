/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.klib.metadata

import kotlinx.metadata.*

private fun render(element: Any?): String = when (element) {
    is KmTypeAlias -> "typealias ${element.name}"
    is KmFunction -> "function ${element.name}"
    is KmProperty -> "property ${element.name}"
    is KmClass -> "class ${element.name}"
    is KmType -> "`type ${element.classifier}`"
    else -> element.toString()
}

internal fun <T> serialComparator(
        vararg comparators: Pair<(T, T) -> MetadataCompareResult, String>?
): (T, T) -> MetadataCompareResult = { o1, o2 ->
    comparators.filterNotNull().map { (comparator, message) ->
        comparator(o1, o2).let { result ->
            if (result is Fail) Fail(message, result) else result
        }
    }.wrap()
}

internal fun <T> serialComparator(
        vararg comparators: (T, T) -> MetadataCompareResult
): (T, T) -> MetadataCompareResult = { o1, o2 ->
    comparators
            .map { comparator -> comparator(o1, o2) }
            .wrap()
}

internal fun Collection<MetadataCompareResult>.wrap(): MetadataCompareResult =
        filterIsInstance<Fail>().let { fails ->
            when (fails.size) {
                0 -> Ok
                else -> Fail(fails)
            }
        }

internal fun <T> compareNullable(
        comparator: (T, T) -> MetadataCompareResult
): (T?, T?) -> MetadataCompareResult = { a, b ->
    when {
        a != null && b != null -> comparator(a, b)
        a == null && b == null -> Ok
        else -> Fail("${render(a)} ${render(b)}")
    }
}

internal fun <T> compareLists(elementComparator: (T, T) -> MetadataCompareResult, sortBy: T.() -> String? = { null }) =
        { list1: List<T>, list2: List<T> -> compareLists(list1.sortedBy(sortBy), list2.sortedBy(sortBy), elementComparator) }

private fun <T> compareLists(l1: List<T>, l2: List<T>, comparator: (T, T) -> MetadataCompareResult) = when {
    l1.size != l2.size -> Fail("${l1.size} != ${l2.size}")
    else -> l1.zip(l2).map { comparator(it.first, it.second) }.wrap()
}