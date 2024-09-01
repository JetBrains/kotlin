/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build

import kotlin.test.junit5.JUnit5Asserter.fail

internal fun assertContainsExactTimes(content: String, substring: String, expectedCount: Int) {
    var currentOffset = 0
    var count = 0
    var nextIndex = content.indexOf(substring, currentOffset)

    while (nextIndex != -1 && count < expectedCount + 1) {
        count++
        currentOffset = nextIndex + substring.length
        nextIndex = content.indexOf(substring, currentOffset)
    }
    assert(expectedCount == count) {
        """
            |The content is expected to contain '$substring' exactly $expectedCount times, but was found $count times:
            |File content:
            |${content.prependIndent()}
        """.trimMargin()
    }
}

internal fun withSystemProperty(key: String, value: String, action: () -> Unit) {
    val oldValue = System.getProperty(key)
    try {
        System.setProperty(key, value)
        action()
    } finally {
        if (oldValue != null) {
            System.setProperty(key, oldValue)
        } else {
            System.clearProperty(key)
        }
    }
}

internal fun assertMapIsEmpty(map: Map<*, *>) {
    if (map.isNotEmpty()) {
        fail("Map is expected to be empty, but contains entries: $map")
    }
}

internal fun <K, V> assertContainsKey(map: Map<K, V>, key: K) {
    if (!map.containsKey(key)) {
        fail("Map should contain key '$key'")
    }
}