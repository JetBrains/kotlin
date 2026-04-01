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

import org.junit.Test

class ComposeMultiPlatformTests : AbstractMultiPlatformIntegrationTest() {
    @Test
    fun testBasicMpp() {
        multiplatform(
            """
            expect val foo: String
        """,
            """
            actual val foo = ""
        """,
            """
            public final class JvmKt {
              private final static Ljava/lang/String; foo
              public final static getFoo()Ljava/lang/String;
              static <clinit>()V
            }
        """
        )
    }

    @Test
    fun testBasicComposable() {
        multiplatform(
            """
            import androidx.compose.runtime.Composable

            expect @Composable fun Test()
        """,
            """
            import androidx.compose.runtime.Composable

            actual @Composable fun Test() {}
        """,
            """
        public final class JvmKt {
          public final static Test(Landroidx/compose/runtime/Composer;I)V
          private final static Test%lambda%0(ILandroidx/compose/runtime/Composer;I)Lkotlin/Unit;
        }
        """
        )
    }

    @Test
    fun testComposableExpectDefaultParameter() {
        multiplatform(
            """
                import androidx.compose.runtime.Composable

                @Composable
                expect fun One(param: Int = 0)
            """,
            """
                import androidx.compose.runtime.Composable

                @Composable
                actual fun One(param: Int) { }
            """,
            """
                public final class JvmKt {
                  public final static One(ILandroidx/compose/runtime/Composer;II)V
                  private final static One%lambda%0(IIILandroidx/compose/runtime/Composer;I)Lkotlin/Unit;
                }
            """.trimIndent()
        )
    }
}
