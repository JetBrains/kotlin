/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.utils

import org.junit.Test
import kotlin.test.assertEquals

class GradleVersionTest {
    @Test
    fun testParse() {
        fun String.parseToPair() = ParsedGradleVersion.parse(this)?.let { it.major to it.minor }

        assertEquals(2 to 1, "2.1".parseToPair())
        assertEquals(2 to 10, "2.10".parseToPair())
        assertEquals(2 to 14, "2.14.1".parseToPair())
        assertEquals(3 to 2, "3.2-rc-1".parseToPair())
        assertEquals(3 to 2, "3.2".parseToPair())
    }

    @Test
    fun testCompare() {
        assert(ParsedGradleVersion(3, 2) == ParsedGradleVersion(3, 2))
        assert(ParsedGradleVersion(3, 2) > ParsedGradleVersion(2, 14))
        assert(ParsedGradleVersion(3, 2) > ParsedGradleVersion(3, 1))
        assert(ParsedGradleVersion(3, 2) < ParsedGradleVersion(3, 3))
    }
}