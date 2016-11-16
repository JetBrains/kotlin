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

package test.collections.js

import org.junit.Test
import kotlin.test.*

class JsArrayTest {

    @Test fun arraySizeAndToList() {
        val a1 = arrayOf<String>()
        val a2 = arrayOf("foo")
        val a3 = arrayOf("foo", "bar")

        assertEquals(0, a1.size)
        assertEquals(1, a2.size)
        assertEquals(2, a3.size)

        assertEquals("[]", a1.toList().toString())
        assertEquals("[foo]", a2.toList().toString())
        assertEquals("[foo, bar]", a3.toList().toString())

    }

    @Test fun arrayListFromCollection() {
        var c: Collection<String>  = arrayOf("A", "B", "C").toList()
        var a = ArrayList(c)

        assertEquals(3, a.size)
        assertEquals("A", a[0])
        assertEquals("B", a[1])
        assertEquals("C", a[2])
    }
}
