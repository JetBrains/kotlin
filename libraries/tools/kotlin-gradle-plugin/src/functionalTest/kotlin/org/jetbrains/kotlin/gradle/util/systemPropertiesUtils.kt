/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

/**
 * Executes a given [block] of code with a set of system properties temporarily modified.
 *
 * This function is primarily used in tests to simulate different environments.
 * For example, it can be used to emulate a different operating system by setting properties
 * like `os.name`.
 *
 * It guarantees that the original system property values are restored after the [block]
 * completes, even if the [block] throws an exception, by using a `finally` block.
 * If a property did not exist before, it will be cleared.
 *
 * This function is [Synchronized] to ensure thread-safety when running
 * tests in parallel, preventing different tests from overwriting
 * system properties concurrently.
 *
 * @param pairs The key-value pairs of system properties to set before executing the [block].
 * @param block The lambda to execute while the system properties are modified.
 */
@Synchronized
internal fun withModifiedSystemProperties(vararg pairs: Pair<String, String>, block: () -> Unit) {
    val backupValues = pairs.toMap().mapValues { (key, _) -> System.getProperty(key) }

    try {
        pairs.forEach { (key, value) -> System.setProperty(key, value) }
        block()
    } finally {
        backupValues.forEach { (key, value) ->
            if (value == null) {
                System.clearProperty(key)
            } else {
                System.setProperty(key, value)
            }
        }
    }
}