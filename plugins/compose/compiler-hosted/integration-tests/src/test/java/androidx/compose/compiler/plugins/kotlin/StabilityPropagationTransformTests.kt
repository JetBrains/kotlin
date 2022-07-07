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

class StabilityPropagationTransformTests : ComposeIrTransformTest() {
    private fun stabilityPropagation(
        @Language("kotlin")
        unchecked: String,
        @Language("kotlin")
        checked: String,
        expectedTransformed: String,
        dumpTree: Boolean = false
    ) = verifyComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            $checked
        """.trimIndent(),
        expectedTransformed,
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
        """,
        """
        @Composable
        fun Test(x: Foo, %composer: Composer?, %changed: Int) {
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(Test)<A(x)>,<A(Foo(...>,<rememb...>,<A(reme...>:Test.kt")
          val %dirty = %changed
          if (%changed and 0b1110 === 0) {
            %dirty = %dirty or if (%composer.changed(x)) 0b0100 else 0b0010
          }
          if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
            if (isTraceInProgress()) {
              traceEventStart(<>, %dirty, -1, <>)
            }
            A(x, %composer, 0b1110 and %dirty)
            A(Foo(0), %composer, 0)
            A(remember({
              Foo(0)
            }, %composer, 0), %composer, 0b0110)
            if (isTraceInProgress()) {
              traceEventEnd()
            }
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            Test(x, %composer, %changed or 0b0001)
          }
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
        """,
        """
            @Composable
            fun Test(x: Foo, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<A(x)>,<A(Foo(...>,<rememb...>,<A(reme...>:Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              A(x, %composer, 0b1000)
              A(Foo(0), %composer, 0b1000)
              A(remember({
                Foo(0)
              }, %composer, 0), %composer, 0b1000)
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(x, %composer, %changed or 0b0001)
              }
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
        """,
        """
            @Composable
            fun Example(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Example)<A(list...>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                A(listOf("a"), %composer, 0)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Example(%composer, %changed or 0b0001)
              }
            }
        """
    )
}
