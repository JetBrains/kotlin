/*
 * Copyright 2019 The Android Open Source Project
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

/**
 * We're strongly considering supporting try-catch-finally blocks in the future.
 * If/when we do support them, these tests should be deleted.
 */
class TryCatchComposableCheckerTests(useFir: Boolean) : AbstractComposeDiagnosticsTest(useFir) {
    @Test
    fun testTryCatchReporting001() {
        check(
            """
            import androidx.compose.runtime.*;

            @Composable fun foo() { }

            @Composable fun bar() {
                <!ILLEGAL_TRY_CATCH_AROUND_COMPOSABLE!>try<!> {
                    foo()
                } catch(e: Exception) {
                }
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
                try {
                    foo()
                } catch(e: Exception) {
                }
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
                try {
                } catch(e: Exception) {
                    foo()
                } finally {
                    foo()
                }
            }
        """
        )
    }

    @Test
    fun testTryCatchReporting004() {
        check(
            """
            import androidx.compose.runtime.*;

            @Composable fun foo() { }

            @Composable fun bar() {
                <!ILLEGAL_TRY_CATCH_AROUND_COMPOSABLE!>try<!> {
                    (1..10).forEach { foo() }
                } catch(e: Exception) {
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
            var globalContent = @Composable {}
            fun setContent(content: @Composable () -> Unit) {
                globalContent = content
            }
            @Composable fun A() {}

            fun test() {
                try {
                    setContent {
                        A()
                    }
                } finally {
                    print("done")
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
                <!ILLEGAL_TRY_CATCH_AROUND_COMPOSABLE!>try<!> {
                    object {
                        init { A() }
                    }
                } finally {}
            }
        """
        )
    }

    @Test
    fun testTryCatchReporting007() {
        check(
            """
            import androidx.compose.runtime.*
            @Composable fun A() {}

            @Composable
            fun test() {
                <!ILLEGAL_TRY_CATCH_AROUND_COMPOSABLE!>try<!> {
                    object {
                        val x = A()
                    }
                } finally {}
            }
        """
        )
    }

    @Test
    fun testTryCatchReporting008() {
        check(
            """
            import androidx.compose.runtime.*

            @Composable
            fun test() {
                <!ILLEGAL_TRY_CATCH_AROUND_COMPOSABLE!>try<!> {
                    val x by remember { lazy { 0 } }
                    print(x)
                } finally {}
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
                try {
                    object {
                        val x: Int
                            @Composable get() = remember { 0 }
                    }
                } finally {}
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
                try {
                    class C {
                        init { <!COMPOSABLE_INVOCATION!>A<!>() }
                    }
                } finally {}
            }
        """
        )
    }

    @Test
    fun testTryCatchReporting011() {
        check(
            """
            import androidx.compose.runtime.*
            @Composable fun A() {}

            @Composable
            fun test() {
                try {
                    @Composable fun B() {
                        A()
                    }
                } finally {}
            }
        """
        )
    }
}
