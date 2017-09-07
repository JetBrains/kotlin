/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package kotlin.jdk8.collections.test

import org.junit.Test
import kotlin.test.*
import java.util.function.UnaryOperator

class ListTest {

    @Test fun replaceAll() {
        val list = mutableListOf("ab", "cde", "x")
        list.replaceAll { it.length.toString() }

        val expected = listOf("2", "3", "1")
        assertEquals(expected, list)

        list.replaceAll(UnaryOperator.identity())
        assertEquals(expected, list)
    }
}
