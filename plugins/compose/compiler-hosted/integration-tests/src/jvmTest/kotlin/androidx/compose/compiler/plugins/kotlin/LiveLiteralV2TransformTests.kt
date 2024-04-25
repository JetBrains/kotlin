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

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Ignore
import org.junit.Test

class LiveLiteralV2TransformTests(useFir: Boolean) : AbstractLiveLiteralTransformTests(useFir) {
    override fun CompilerConfiguration.updateConfiguration() {
        put(ComposeConfiguration.LIVE_LITERALS_V2_ENABLED_KEY, true)
    }

    @Test
    fun testSiblingCallArgs() = assertNoDuplicateKeys(
        """
        fun Test() {
            print(1)
            print(1)
        }
        """
    )

    @Test
    fun testFunctionCallWithConstArg() = assertKeys(
        "Int%arg-0%call-print%fun-Test",
        "Int%arg-0%call-print-1%fun-Test"
    ) {
        """
        fun Test() {
            print(1)
            print(1)
        }
        """
    }

    @Test
    fun testDispatchReceiver() {
        assertKeys(
            "Int%%this%call-toString%arg-0%call-print%fun-Test",
            "Int%arg-0%call-print-1%fun-Test"
        ) {
            """
        fun Test() {
            print(1.toString())
            print(1)
        }
            """
        }
    }

    @Test
    fun testInsidePropertyGetter() = assertKeys(
        "Int%fun-%get-foo%%get%val-foo"
    ) {
        """
        val foo: Int get() = 1
        """
    }

    // NOTE(lmr): For static initializer expressions we can/should do more.
    @Test
    fun testInsidePropertyInitializer() = assertKeys {
        """
        val foo: Int = 1
        """
    }

    @Test
    fun testValueParameter() = assertKeys(
        "Int%param-x%fun-Foo"
    ) {
        """
        fun Foo(x: Int = 1) { print(x) }
        """
    }

    @Test
    fun testAnnotation() = assertKeys {
        """
        annotation class Foo(val value: Int = 1)
        @Foo fun Bar() {}
        @Foo(2) fun Bam() {}
        """
    }

    // NOTE(lmr): In the future we should try and get this to work
    @Test
    fun testForLoop() = assertKeys {
        """
        fun Foo() {
            for (x in 0..10) {
                print(x)
            }
        }
        """
    }

    @Test
    fun testWhileTrue() = assertKeys(
        "Double%arg-1%call-greater%cond%if%body%loop%fun-Foo",
        "Int%arg-0%call-print%body%loop%fun-Foo"
    ) {
        """
        fun Foo() {
            while (true) {
                print(1)
                if (Math.random() > 0.5) break
            }
        }
        """
    }

    @Test
    fun testWhileCondition() = assertKeys(
        "Int%arg-0%call-print%body%loop%fun-Foo"
    ) {
        """
        fun Foo() {
            while (Math.random() > 0.5) {
                print(1)
            }
        }
        """
    }

    @Test
    fun testForInCollection() = assertKeys(
        "Int%arg-0%call-print-1%body%loop%fun-Foo"
    ) {
        """
        fun Foo(items: List<Int>) {
            for (item in items) {
                print(item)
                print(1)
            }
        }
        """
    }

    // NOTE(lmr): we should deal with this in some cases, but leaving untouched for now
    @Test
    fun testConstantProperty() = assertKeys {
        """
        const val foo = 1
        """
    }

    @Test
    fun testSafeCall() = assertKeys(
        "Boolean%arg-1%call-EQEQ%fun-Foo",
        "String%arg-0%call-contains%else%when%arg-0%call-EQEQ%fun-Foo"
    ) {
        """
        fun Foo(bar: String?): Boolean {
            return bar?.contains("foo") == true
        }
        """
    }

    @Test
    fun testElvis() = assertKeys(
        "String%branch%when%fun-Foo"
    ) {
        """
        fun Foo(bar: String?): String {
            return bar ?: "Hello World"
        }
        """
    }

    @Test
    fun testTryCatch() = assertKeys(
        "Int%arg-0%call-invoke%catch%fun-Foo",
        "Int%arg-0%call-invoke%finally%fun-Foo",
        "Int%arg-0%call-invoke%try%fun-Foo"
    ) {
        """
        fun Foo(block: (Int) -> Unit) {
            try {
                block(1)
            } catch(e: Exception) {
                block(2)
            } finally {
                block(3)
            }
        }
        """
    }

    @Test
    fun testWhen() = assertKeys(
        "Double%arg-1%call-greater%cond%when%fun-Foo",
        "Double%arg-1%call-greater%cond-1%when%fun-Foo",
        "Int%arg-0%call-print%branch%when%fun-Foo",
        "Int%arg-0%call-print%branch-1%when%fun-Foo",
        "Int%arg-0%call-print%else%when%fun-Foo"
    ) {
        """
        fun Foo() {
            when {
                Math.random() > 0.5 -> print(1)
                Math.random() > 0.5 -> print(2)
                else -> print(3)
            }
        }
        """
    }

    @Test
    fun testWhenWithSubject() = assertKeys(
        "Double%%%this%call-rangeTo%%this%call-contains%cond%when%fun-Foo",
        "Double%%%this%call-rangeTo%%this%call-contains%cond-1%when%fun-Foo",
        "Double%arg-0%call-rangeTo%%this%call-contains%cond%when%fun-Foo",
        "Double%arg-0%call-rangeTo%%this%call-contains%cond-1%when%fun-Foo",
        "Int%arg-0%call-print%branch%when%fun-Foo",
        "Int%arg-0%call-print%branch-1%when%fun-Foo",
        "Int%arg-0%call-print%else%when%fun-Foo"
    ) {
        """
        fun Foo() {
            when (val x = Math.random()) {
                in 0.0..0.5 -> print(1)
                in 0.0..0.2 -> print(2)
                else -> print(3)
            }
        }
        """
    }

    @Test
    fun testWhenWithSubject2() = assertKeys(
        "Int%arg-0%call-print%branch-1%when%fun-Foo",
        "Int%arg-0%call-print%else%when%fun-Foo",
        "String%arg-0%call-print%branch%when%fun-Foo"
    ) {
        """
        fun Foo(foo: Any) {
            when (foo) {
                is String -> print("Hello World")
                is Int -> print(2)
                else -> print(3)
            }
        }
        """
    }

    @Test
    fun testDelegatingCtor() = assertKeys(
        "Int%arg-0%call-%init%%class-Bar"
    ) {
        """
        open class Foo(val x: Int)
        class Bar() : Foo(123)
        """
    }

    @Test
    fun testLocalVal() = assertKeys(
        "Int%arg-0%call-plus%set-y%fun-Foo",
        "Int%val-x%fun-Foo",
        "Int%val-y%fun-Foo"
    ) {
        """
        fun Foo() {
            val x = 1
            var y = 2
            y += 10
        }
        """
    }

    @Test
    fun testCapturedVar() = assertKeys(
        "Int%val-a%fun-Example",
        "String%0%str%fun-Example",
        "String%2%str%fun-Example"
    ) {
        """
        fun Example(): String {
                val a = 123
                return "foo ${"$"}a bar"
            }
        """
    }

    @Test
    fun testStringTemplate(): Unit = assertKeys(
        "Int%val-a%fun-Example",
        "String%0%str%fun-Example",
        "String%2%str%fun-Example"
    ) {
        """
        fun Example(): String {
            val a = 123
            return "foo ${"$"}a bar"
        }
        """
    }

    @Test
    fun testEnumEntryMultipleArgs(): Unit = assertKeys(
        "Int%arg-0%call-%init%%entry-Bar%class-A",
        "Int%arg-0%call-%init%%entry-Baz%class-A",
        "Int%arg-0%call-%init%%entry-Foo%class-A",
        "Int%arg-1%call-%init%%entry-Bar%class-A",
        "Int%arg-1%call-%init%%entry-Baz%class-A",
        "Int%arg-1%call-%init%%entry-Foo%class-A"
    ) {
        """
        enum class A(val x: Int, val y: Int) {
            Foo(1, 2),
            Bar(2, 3),
            Baz(3, 4)
        }
        """
    }

    @Test
    fun testCommentsAbove() = assertDurableChange(
        """
            fun Test() {
                print(1)
            }
        """.trimIndent(),
        """
            fun Test() {
                // this is a comment
                print(1)
            }
        """.trimIndent()
    )

    @Test
    fun testValsAndStructureAbove() = assertDurableChange(
        """
            fun Test() {
                print(1)
            }
        """.trimIndent(),
        """
            fun Test() {
                val x = Math.random()
                println(x)
                print(1)
            }
        """.trimIndent()
    )

    @Test
    fun testAnonymousClass(): Unit = assertTransform(
        """
        """,
        """
            interface Foo { fun bar(): Int }
            fun a(): Foo {
                return object : Foo {
                    override fun bar(): Int { return 1 }
                }
            }
        """
    )

    @Test
    fun testBasicTransform() {
        assertTransform(
            """
            """,
            """
                fun A() {
                  print(1)
                  print("Hello World")
                  if (true) {
                    print(7)
                  }
                  if (true) {
                    print(1.0f)
                  }
                  print(3)
                }
            """
        )
    }

    @Test
    fun testBasicTransformConstantFoldingK1() {
        // K1 does not constant fold.
        assumeFalse(useFir)
        assertTransform(
            """
            """,
            """
                fun A() {
                    print(3 + 4)
                }
            """
        )
    }

    @Test
    fun testBasicTransformConstantFoldingK2() {
        // K2 constant folds.
        assumeTrue(useFir)
        assertTransform(
            """
            """,
            """
                fun A() {
                    print(3 + 4)
                }
            """
        )
    }

    @Ignore("ui/foundation dependency is not supported for now")
    @Test
    fun testComposeIrSkippingWithDefaultsRelease() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.ui.text.input.TextFieldValue
            import androidx.compose.runtime.*
            import androidx.compose.foundation.layout.*
            import androidx.compose.foundation.text.KeyboardActions

            object Ui {}

            @Composable
            fun Ui.UiTextField(
                isError: Boolean = false,
                keyboardActions2: Boolean = false,
            ) {
                println("t41 insideFunction ${'$'}isError")
                println("t41 insideFunction ${'$'}keyboardActions2")
                Column {
                    Text("${'$'}isError")
                    Text("${'$'}keyboardActions2")
                }
            }
        """.trimIndent(),
        extra = """
            import androidx.compose.runtime.Composable

            @Composable
            public fun Text(
                text: String,
                softWrap: Boolean = true,
                maxLines: Int = Int.MAX_VALUE,
                minLines: Int = 1,
            ) {}
        """.trimIndent()
    )

    @Test
    fun verifyInitInClass() {
        assertTransform(
            """
            """,
            """
                class ViewModel {
                    init {
                        1
                    }
                    init {
                        2
                    }
                }
            """
        )
    }
}
