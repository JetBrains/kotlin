/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin

import androidx.compose.compiler.plugins.kotlin.lower.DurableKeyVisitor
import junit.framework.TestCase

class DurableKeyTests : TestCase() {
    private fun visit(block: DurableKeyVisitor.() -> Unit) = DurableKeyVisitor().block()

    private fun DurableKeyVisitor.assertKey(prefix: String, expected: String) {
        val (key, success) = buildPath(prefix)
        assertEquals(expected, key)
        assert(success) { "Duplicate key found: $key" }
    }

    private fun DurableKeyVisitor.assertDuplicate(prefix: String, expected: String) {
        val (key, success) = buildPath(prefix)
        assertEquals(key, expected)
        assert(!success) { "Expected duplicate, but wasn't: $key" }
    }

    fun testBasic() = visit {
        enter("a") {
            assertKey("b", "b/a")
        }
    }

    fun testBasicNonSiblings() = visit {
        enter("a") {
            assertKey("b", "b/a")
            assertKey("c", "c/a")
        }
    }

    fun testSiblings() = visit {
        siblings("a") {
            enter("b") { assertKey("i", "i/b/a") }
            enter("c") { assertKey("i", "i/c/a") }
            enter("d") { assertKey("i", "i/d/a") }
            enter("d") { assertKey("i", "i/d:1/a") }
        }
    }

    fun testNestedSiblings() = visit {
        siblings("a") {
            siblings {
                enter("b") { assertKey("i", "i/b/a") }
            }
            enter("b") { assertKey("i", "i/b:1/a") }
        }
    }

    fun testDuplicateFailure() = visit {
        siblings("a") {
            enter("b") { assertKey("i", "i/b/a") }
            enter("b") { assertKey("i", "i/b:1/a") }
        }
        enter("b") {
            assertKey("c", "c/b")
            assertDuplicate("c", "c/b")
        }
    }
}
