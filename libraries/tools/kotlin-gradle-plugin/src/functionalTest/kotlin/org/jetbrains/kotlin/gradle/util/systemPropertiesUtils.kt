/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

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