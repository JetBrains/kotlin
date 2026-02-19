/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import org.jetbrains.kotlin.test.Directives
import org.jetbrains.kotlin.test.directives.model.*
import org.jetbrains.kotlin.test.util.joinToArrayString

/**
 * The same as [RegisteredDirectives.contains], but for [Directives].
 */
internal operator fun Directives.contains(directive: Directive): Boolean = contains(directive.name)

/**
 * The same as [RegisteredDirectives.get], but for [Directives].
 */
internal operator fun <T : Any> Directives.get(directive: ValueDirective<T>): List<T> =
    listValues(directive.name)?.map {
        directive.parser(it) ?: error("$it is not a valid value for $directive")
    }.orEmpty()

/**
 * The same as [RegisteredDirectives.get], but for [Directives].
 */
internal operator fun Directives.get(directive: StringDirective): List<String> =
    listValues(directive.name).orEmpty()

/**
 * The same as [RegisteredDirectives.singleOrZeroValue], but for [Directives].
 */
fun <T : Any> Directives.singleOrZeroValue(directive: ValueDirective<T>): T? {
    val values = this[directive]
    return when (values.size) {
        0 -> null
        1 -> values.single()
        else -> error("Too many values passed to $directive: ${values.joinToArrayString()}")
    }
}
