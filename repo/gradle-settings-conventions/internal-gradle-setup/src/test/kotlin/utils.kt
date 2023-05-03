/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build

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