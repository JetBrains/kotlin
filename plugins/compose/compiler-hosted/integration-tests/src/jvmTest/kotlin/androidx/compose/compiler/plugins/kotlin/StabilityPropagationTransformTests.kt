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

import org.intellij.lang.annotations.Language
import org.junit.Test

class StabilityPropagationTransformTests(useFir: Boolean) : AbstractIrTransformTest(useFir) {
    private fun stabilityPropagation(
        @Language("kotlin")
        unchecked: String,
        @Language("kotlin")
        checked: String,
        dumpTree: Boolean = false
    ) = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            $checked
        """.trimIndent(),
        """
            import androidx.compose.runtime.Composable

            $unchecked
        """.trimIndent(),
        dumpTree = dumpTree
    )

    @Test
    fun testPassingLocalKnownStable(): Unit = stabilityPropagation(
        """
            class Foo(val foo: Int)
            @Composable fun A(x: Any) {}
        """,
        """
            import androidx.compose.runtime.remember

            @Composable
            fun Test(x: Foo) {
                A(x)
                A(Foo(0))
                A(remember { Foo(0) })
            }
        """
    )

    @Test
    fun testPassingLocalKnownUnstable(): Unit = stabilityPropagation(
        """
            class Foo(var foo: Int)
            @Composable fun A(x: Any) {}
        """,
        """
            import androidx.compose.runtime.remember

            @Composable
            fun Test(x: Foo) {
                A(x)
                A(Foo(0))
                A(remember { Foo(0) })
            }
        """
    )

    @Test
    fun testListOfMarkedAsStable(): Unit = stabilityPropagation(
        """
            @Composable fun A(x: Any) {}
        """,
        """
            @Composable
            fun Example() {
                A(listOf("a"))
            }
        """
    )
}
