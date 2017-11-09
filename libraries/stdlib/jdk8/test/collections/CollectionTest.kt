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
import java.util.function.Predicate
import java.util.stream.Collectors
import kotlin.streams.*

class CollectionTest {

    val data = listOf("abc", "fo", "baar")

    @Test fun stream() {
        assertEquals(
                data.flatMap { it.asIterable() },
                data.stream()
                        .flatMap { it.chars().boxed().map { it.toChar() } }
                        .collect(Collectors.toList()))

        assertEquals(data, data.parallelStream().toList())
    }


    @Test fun removeIf() {
        val coll: MutableCollection<String> = data.toMutableList()
        assertTrue(coll.removeIf { it.length < 3 })
        assertEquals(listOf("abc", "baar"), coll as Collection<String>)
        assertFalse(coll.removeIf(Predicate { it.length > 4 }))
    }


}
