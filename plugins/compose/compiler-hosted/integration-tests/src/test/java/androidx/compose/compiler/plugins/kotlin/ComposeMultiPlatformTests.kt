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

@Suppress("unused")
class ComposeMultiPlatformTests : AbstractMultiPlatformIntegrationTest() {

    @Test
    fun testBasicMpp() = ensureSetup {
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
    fun testBasicComposable() = ensureSetup {
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
        final class JvmKt%Test%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
          OUTERCLASS JvmKt Test (Landroidx/compose/runtime/Composer;I)V
          final static INNERCLASS JvmKt%Test%1 null null
          final synthetic I %%changed
          <init>(I)V
          public final invoke(Landroidx/compose/runtime/Composer;I)V
          public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
        }
        public final class JvmKt {
          final static INNERCLASS JvmKt%Test%1 null null
          public final static Test(Landroidx/compose/runtime/Composer;I)V
        }
        """
        )
    }

    @Test
    fun testComposableExpectDefaultParameter() = ensureSetup {
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
                final class JvmKt%One%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
                  OUTERCLASS JvmKt One (ILandroidx/compose/runtime/Composer;II)V
                  final static INNERCLASS JvmKt%One%1 null null
                  final synthetic I %param
                  final synthetic I %%changed
                  final synthetic I %%default
                  <init>(III)V
                  public final invoke(Landroidx/compose/runtime/Composer;I)V
                  public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                }
                public final class JvmKt {
                  final static INNERCLASS JvmKt%One%1 null null
                  public final static One(ILandroidx/compose/runtime/Composer;II)V
                }
            """.trimIndent()
        )
    }
}