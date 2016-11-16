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

package test.utils

import kotlin.*
import kotlin.test.*
import org.junit.Test

class TODOTest {
    private class PartiallyImplementedClass {
        public val prop: String get() = TODO()
        fun method1() = TODO() as String
        public fun method2(): Int = TODO()

        public fun method3(switch: Boolean, value: String): String {
            if (!switch)
                TODO("what if false")
            else {
                if (value.length < 3)
                    throw TODO("write message")
            }

            return value
        }

        public fun method4() {
            TODO()
        }
    }

    private fun assertNotImplemented(block: () -> Unit) {
        assertFailsWith<NotImplementedError>(block = block)
    }

    private fun assertNotImplementedWithMessage(message: String, block: () -> Unit) {
        val e = assertFailsWith<NotImplementedError>(block = block)
        assertTrue(message in e.message!!)
    }


    @Test fun usage() {
        val inst = PartiallyImplementedClass()

        assertNotImplemented { inst.prop }
        assertNotImplemented{ inst.method1() }
        assertNotImplemented { inst.method2() }
        assertNotImplemented { inst.method4() }
        assertNotImplementedWithMessage("what if false") { inst.method3(false, "test") }
        assertNotImplementedWithMessage("write message") { inst.method3(true, "t") }
        assertEquals("test", inst.method3(true, "test"))
    }
}
