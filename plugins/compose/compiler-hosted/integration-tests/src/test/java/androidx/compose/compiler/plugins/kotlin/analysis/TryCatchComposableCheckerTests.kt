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

/**
 * We're strongly considering supporting try-catch-finally blocks in the future.
 * If/when we do support them, these tests should be deleted.
 */
class TryCatchComposableCheckerTests : AbstractComposeDiagnosticsTest() {

    fun testTryCatchReporting001() {
        doTest(
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

    fun testTryCatchReporting002() {
        doTest(
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

    fun testTryCatchReporting003() {
        doTest(
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

    fun testTryCatchReporting004() {
        doTest(
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

    fun testTryCatchReporting005() {
        doTest(
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
}
