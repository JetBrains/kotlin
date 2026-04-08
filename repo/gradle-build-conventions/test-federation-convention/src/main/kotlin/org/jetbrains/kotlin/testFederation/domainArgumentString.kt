/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

/**
 * Denotes that indeed not a single domain is affected
 * Note: Passing an empty string is considered as the property being absent (null)
 */
private const val NONE_DOMAINS_NOTATION = "<none>"

/**
 * Denotes that *all* domains are affected
 */
private const val ALL_DOMAINS_NOTATION = "*"

fun Iterable<Domain>.toArgumentString(): String {
    val set = sorted().toSet()
    if (set.isEmpty()) return NONE_DOMAINS_NOTATION
    if (set.containsAll(Domain.entries)) return ALL_DOMAINS_NOTATION

    return set.joinToString(";") { it.name }
}

fun Domain.Companion.fromArgumentString(value: String): Set<Domain>? {
    val trimmed = value.trim()
    when {
        trimmed.isBlank() -> return null
        trimmed == NONE_DOMAINS_NOTATION -> return emptySet()
        trimmed == ALL_DOMAINS_NOTATION -> return Domain.entries.toSet()
    }

    val values = trimmed.split(";")
    return buildSet {
        values.forEach { raw ->
            if (raw == "*") addAll(Domain.entries)
            else add(Domain.valueOf(raw))
        }
    }
}
