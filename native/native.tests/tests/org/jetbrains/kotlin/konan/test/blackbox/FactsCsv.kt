/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

private val FACTS_CSV_HEADER = ["relation", "path", "kind", "symbolKey", "hash", "representation"]

/**
 * Parses a facts dump written by the compiler's `DebugFactsSerializer` (RFC 4180 CSV with a header row)
 * and returns its rows without the header: `relation, path, kind, symbolKey, hash, representation`.
 */
internal fun parseFactsCsv(text: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val field = StringBuilder()
    var inQuotes = false
    var quoted = false

    fun endField() {
        row.add(field.toString())
        field.clear()
        quoted = false
    }

    fun endRow() {
        endField()
        if (row.size > 1 || row.single().isNotEmpty()) rows.add(row)
        row = mutableListOf()
    }

    var i = 0
    while (i < text.length) {
        val c = text[i]
        when {
            inQuotes && c == '"' && text.getOrNull(i + 1) == '"' -> {
                field.append('"')
                i++
            }
            inQuotes && c == '"' -> inQuotes = false
            inQuotes -> field.append(c)
            c == '"' && field.isEmpty() && !quoted -> {
                inQuotes = true
                quoted = true
            }
            c == ',' -> endField()
            c == '\n' -> endRow()
            c == '\r' -> {}
            else -> field.append(c)
        }
        i++
    }
    check(!inQuotes) { "Unterminated quoted CSV field" }
    if (field.isNotEmpty() || row.isNotEmpty() || quoted) endRow()

    if (rows.isEmpty()) return rows
    check(rows.first() == FACTS_CSV_HEADER) { "Unexpected facts header: ${rows.first()}" }
    return rows.drop(1).onEach { check(it.size == FACTS_CSV_HEADER.size) { "Malformed facts row: $it" } }
}
