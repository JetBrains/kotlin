/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.compiler.test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.test.Test
import kotlin.test.assertEquals

class JvmCompositionTests {
    private class TestReference(val invokeCount: Int = 0) : () -> Int {
        override fun invoke(): Int = invokeCount

        // overridden equals to test if remember compares this value correctly
        override fun equals(other: Any?): Boolean {
            return other is TestReference
        }
    }

    @Composable
    private fun rememberWFunctionReference(ref: () -> Int): Int {
        val remembered = remember(ref) { ref() }
        assertEquals(remembered, 0)
        return remembered
    }

    // regression test for b/319810819
    @Test
    fun remember_functionReference_key() = compositionTest {
        var state by mutableIntStateOf(0)
        compose {
            // use custom ref implementation to avoid strong skipping memoizing the instance
            rememberWFunctionReference(TestReference(state))
        }
        verifyConsistent()

        state++
        advance()
        verifyConsistent()
    }
}
