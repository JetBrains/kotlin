/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

/**
 * Represents an empty set of domains.
 * An empty string instead represents an absent value (`null`).
 */
private const val NONE_DOMAINS_NOTATION = "<none>"

/**
 * Represents all domains.
 */
private const val ALL_DOMAINS_NOTATION = "*"

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
