/*
 * Copyright 2021 The Android Open Source Project
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

import org.junit.Test

class ComposeModuleMetricsTests : AbstractMetricsTransformTest() {
    @Test
    fun testStableAndUnstableClassesTxt() = assertClasses(
        """
            class Foo { var x: Int = 0 }
            class Bar(val x: Int = 0)
        """,
        """
            unstable class Foo {
              stable var x: Int
              <runtime stability> = Unstable
            }
            stable class Bar {
              stable val x: Int
              <runtime stability> = Stable
            }
        """
    )

    @Test
    fun testComposablesTxt() = assertComposables(
        """
            import androidx.compose.runtime.*

            fun makeInt(): Int = 0
            fun used(x: Any?) {}
            class Unstable { var x = 0 }

            @Composable fun A() {}
            @Composable fun B(b: Int) { used(b) }
            @Composable fun C(c: Int = 0) { used(c) }
            @Composable fun D(d: Int = makeInt()) { used(d) }
            @Composable fun E(e: Unstable) { used(e)}
            @Composable fun F(f: Unstable? = null) { used(f) }
        """,
        """
            restartable skippable fun A()
            restartable skippable fun B(
              stable b: Int
            )
            restartable skippable fun C(
              stable c: Int = @static 0
            )
            restartable skippable fun D(
              stable d: Int = @dynamic makeInt()
            )
            restartable fun E(
              unstable e: Unstable
            )
            restartable skippable fun F(
              unstable f: Unstable? = @static null
            )
        """
    )

    @Test
    fun testComposablesCsv() = assertComposablesCsv(
        """
            import androidx.compose.runtime.*

            fun makeInt(): Int = 0
            fun used(x: Any?) {}
            class Unstable { var x = 0 }

            @Composable fun A() {}
            @Composable fun B(b: Int) { used(b) }
            @Composable fun C(c: Int = 0) { used(c) }
            @Composable fun D(d: Int = makeInt()) { used(d) }
            @Composable fun E(e: Unstable) { used(e)}
            @Composable fun F(f: Unstable? = null) { used(f) }
        """,
        """
            package,name,composable,skippable,restartable,readonly,inline,isLambda,hasDefaults,defaultsGroup,groups,calls,
            A,A,1,1,1,0,0,0,0,0,1,0,
            B,B,1,1,1,0,0,0,0,0,1,0,
            C,C,1,1,1,0,0,0,0,0,1,0,
            D,D,1,1,1,0,0,0,1,0,2,0,
            E,E,1,0,1,0,0,0,0,0,1,0,
            F,F,1,1,1,0,0,0,0,0,1,0,
        """
    )

    @Test
    fun testModuleJson() = assertModuleJson(
        """
            import androidx.compose.runtime.*

            @Composable fun A() {}
            @Composable fun B(b: Int) {
                B(b)
                A()
                if (b > 0) {
                    B(b - 1)
                }
            }
        """,
        """
            {
             "skippableComposables": 2,
             "restartableComposables": 2,
             "readonlyComposables": 0,
             "totalComposables": 2,
             "restartGroups": 2,
             "totalGroups": 2,
             "staticArguments": 0,
             "certainArguments": 1,
             "knownStableArguments": 2,
             "knownUnstableArguments": 0,
             "unknownStableArguments": 0,
             "totalArguments": 2,
             "markedStableClasses": 0,
             "inferredStableClasses": 0,
             "inferredUnstableClasses": 0,
             "inferredUncertainClasses": 0,
             "effectivelyStableClasses": 0,
             "totalClasses": 0,
             "memoizedLambdas": 0,
             "singletonLambdas": 0,
             "singletonComposableLambdas": 0,
             "composableLambdas": 0,
             "totalLambdas": 0
            }
        """
    )
}