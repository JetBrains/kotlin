/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.test

import kotlin.reflect.KClass

/**
 * Takes the given [block] of test code and _doesn't_ execute it.
 *
 * This keeps the code under test referenced, but doesn't actually test it until it is implemented.
 */
expect fun todo(block: () -> Unit)

/**
 * Asserts that a [block] fails with a specific exception of type [exceptionClass] being thrown.
 *
 * If the assertion fails, the specified [message] is used unless it is null as a prefix for the failure message.
 *
 * @return An exception of the expected exception type [T] that successfully caught.
 * The returned exception can be inspected further, for example by asserting its property values.
 */
expect fun <T : Throwable> assertFailsWith(exceptionClass: KClass<T>, message: String?, block: () -> Unit): T
