/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.utilities

import org.jetbrains.dokka.InternalDokkaApi

/**
 * This utility method was previously imported from `org.jetbrains.kotlin.utils.addToStdlib`,
 * and there were a lot of usages. Since no replacement exists in stdlib, it was implemented
 * locally for convenience.
 */
@InternalDokkaApi
public inline fun <reified T : Any> Iterable<*>.firstIsInstanceOrNull(): T? {
    for (element in this) if (element is T) return element
    return null
}

/**
 * This utility method was previously imported from `org.jetbrains.kotlin.utils.addToStdlib`,
 * and there were a lot of usages. Since no replacement exists in stdlib, it was implemented
 * locally for convenience.
 */
@InternalDokkaApi
public inline fun <reified T : Any> Sequence<*>.firstIsInstanceOrNull(): T? {
    for (element in this) if (element is T) return element
    return null
}
