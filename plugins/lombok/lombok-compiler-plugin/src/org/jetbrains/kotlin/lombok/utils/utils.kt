/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.utils

@Suppress("UNCHECKED_CAST")
fun <E, R> Collection<E>.collectWithNotNull(f: (E) -> R?): List<Pair<E, R>> =
    map { it to f(it) }.filter { it.second != null } as List<Pair<E, R>>

fun String?.trimToNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
