/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package collections

import kotlin.test.assertEquals
import org.junit.Test as test

/**
 * We need this test to run only on JVM because we have no mutability support by sub-list implementation for JavaScript
 */
class ReversedViewsJVMTest {

    @test fun testMutableSubList() {
        val original = arrayListOf(1, 2, 3, 4)
        val reversedSubList = original.asReversed().subList(1, 3)

        assertEquals(listOf(3, 2), reversedSubList)
        reversedSubList.clear()
        assertEquals(emptyList<Int>(), reversedSubList)
        assertEquals(listOf(1, 4), original)

        reversedSubList.add(100)
        assertEquals(listOf(100), reversedSubList)
        assertEquals(listOf(1, 100, 4), original)
    }

}
