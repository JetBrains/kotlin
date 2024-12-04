/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text

import kotlin.test.*

class RegexJsTest {
    @Test
    fun replace() {
        // js capturing group name can contain Unicode letters, $, _, and digits (0-9), but may not start with a digit.
        // jvm capturing group name can contain a-z, A-Z, and 0-9, but may not start with a digit.
        // make sure reference to capturing group name in K/JS Regex.replace(input, replacement) obeys K/JVM rules
        val input = "123-456"
        Regex("(?<first_part>\\d+)-(?<second_part>\\d+)").let { regex ->
            assertEquals("123/456", regex.replace(input, "$1/$2"))
            assertEquals("123/456", regex.replaceFirst(input, "$1/$2"))

            assertEquals("123/456", regex.replace(input, "\${first_part}/\${second_part}"))
            assertEquals("123/456", regex.replaceFirst(input, "\${first_part}/\${second_part}"))
        }
        Regex("(?<\$first>\\d+)-(?<\$second>\\d+)").let { regex ->
            assertEquals("123/456", regex.replace(input, "\${\$first}/\${\$second}"))
            assertEquals("123/456", regex.replaceFirst(input, "\${\$first}/\${\$second}"))

            assertFailsWith<IllegalArgumentException> { regex.replace(input, "\${first}/\${second}") }
            assertFailsWith<IllegalArgumentException> { regex.replaceFirst(input, "\${first}/\${second}") }
        }
        Regex("(?<first>\\d+)-(?<second>\\d+)").let { regex ->
            assertFailsWith<IllegalArgumentException> { regex.replace(input, "\${\$first}/\${\$second}") }
            assertFailsWith<IllegalArgumentException> { regex.replaceFirst(input, "\${\$first}/\${\$second}") }
        }
    }
}