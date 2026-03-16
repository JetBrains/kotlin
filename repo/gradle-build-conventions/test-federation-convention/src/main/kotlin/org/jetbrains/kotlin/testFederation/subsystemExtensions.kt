/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

fun Iterable<Subsystem>.asArgumentString(): String = joinToString(";") { it.name }

fun Subsystem.Companion.fromArgumentString(value: String): List<Subsystem> {
    val values = value.split(";")
    return buildList {
        values.forEach { raw ->
            if (raw == "*") addAll(Subsystem.entries)
            else add(Subsystem.valueOf(raw))
        }
    }
}