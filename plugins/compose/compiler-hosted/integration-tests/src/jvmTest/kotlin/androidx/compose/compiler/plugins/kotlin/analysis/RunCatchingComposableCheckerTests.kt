/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.compiler.plugins.kotlin.analysis

import androidx.compose.compiler.plugins.kotlin.AbstractComposeDiagnosticsTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RunCatchingComposableCheckerTests : AbstractComposeDiagnosticsTest(useFir = true) {
    @Test
    fun testTryCatchReporting001() {
        check(
            """
            import androidx.compose.runtime.*;

            @Composable fun foo() { }

            @Composable fun bar() {
                <!ILLEGAL_RUN_CATCHING_AROUND_COMPOSABLE!>runCatching<!> { foo() }
            }
        """
        )
    }

    @Test
    fun testTryCatchReporting002() {
        check(
            """
            import androidx.compose.runtime.*;

            fun foo() { }

            @Composable fun bar() {
                runCatching { foo() }
            }
        """
        )
    }

    @Test
    fun testTryCatchReporting003() {
        check(
            """
            import androidx.compose.runtime.*;

            @Composable fun foo() { }

            @Composable fun bar() {
                <!ILLEGAL_RUN_CATCHING_AROUND_COMPOSABLE!>runCatching<!> {
                    (1..10).forEach { foo() }
                }
            }
        """
        )
    }

    @Test
    fun testTryCatchReporting004() {
        check(
            """
            import androidx.compose.runtime.*
            var globalContent = @Composable {}
            fun setContent(content: @Composable () -> Unit) {
                globalContent = content
            }
            @Composable fun A() {}

            fun test() {
                runCatching {
                    setContent {
                        A()
                    }
                }
            }
        """
        )
    }

    @Test
    fun testTryCatchReporting005() {
        check(
            """
            import androidx.compose.runtime.*
            @Composable fun A() {}

            @Composable
            fun test() {
                <!ILLEGAL_RUN_CATCHING_AROUND_COMPOSABLE!>runCatching<!> {
                    object {
                        init { A() }
                    }
                }
            }
        """
        )
    }

    @Test
    fun testTryCatchReporting006() {
        check(
            """
            import androidx.compose.runtime.*
            @Composable fun A() {}

            @Composable
            fun test() {
                <!ILLEGAL_RUN_CATCHING_AROUND_COMPOSABLE!>runCatching<!> {
                    object {
                        val x = A()
                    }
                }
            }
        """
        )
    }

    @Test
    fun testTryCatchReporting007() {
        check(
            """
            import androidx.compose.runtime.*

            @Composable
            fun test() {
                <!ILLEGAL_RUN_CATCHING_AROUND_COMPOSABLE!>runCatching<!> {
                    val x by remember { lazy { 0 } }
                    print(x)
                }
            }
        """
        )
    }

    @Test
    fun testTryCatchReporting008() {
        check(
            """
            import androidx.compose.runtime.*
            @Composable fun A() {}

            @Composable
            fun test() {
                runCatching {
                    object {
                        val x: Int
                            @Composable get() = remember { 0 }
                    }
                }
            }
        """
        )
    }

    @Test
    fun testTryCatchReporting009() {
        check(
            """
            import androidx.compose.runtime.*
            @Composable fun A() {}

            @Composable
            fun test() {
                runCatching {
                    class C {
                        init { <!COMPOSABLE_INVOCATION!>A<!>() }
                    }
                }
            }
        """
        )
    }

    @Test
    fun testTryCatchReporting010() {
        check(
            """
            import androidx.compose.runtime.*
            @Composable fun A() {}

            @Composable
            fun test() {
                runCatching {
                    @Composable fun B() {
                        A()
                    }
                }
            }
        """
        )
    }
}
