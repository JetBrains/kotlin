/*
 * Copyright 2022 The Android Open Source Project
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
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.config.languageVersionSettings
import org.junit.Test

class ContextReceiversTransformTests(useFir: Boolean) : AbstractIrTransformTest(useFir) {
    override fun CompilerConfiguration.updateConfiguration() {
        put(ComposeConfiguration.SOURCE_INFORMATION_ENABLED_KEY, true)
        languageVersionSettings = LanguageVersionSettingsImpl(
            languageVersion = languageVersionSettings.languageVersion,
            apiVersion = languageVersionSettings.apiVersion,
            specificFeatures = mapOf(
                LanguageFeature.ContextReceivers to LanguageFeature.State.ENABLED
            )
        )
    }

    private fun contextReceivers(
        @Language("kotlin")
        unchecked: String,
        @Language("kotlin")
        checked: String,
    ) = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.Composable

            $checked
        """.trimIndent(),
        extra = """
            import androidx.compose.runtime.Composable

            $unchecked

            fun used(x: Any?) {}
        """.trimIndent(),
    )

    @Test
    fun testTrivialContextReceivers(): Unit = contextReceivers(
        """
            class Foo { }
        """,
        """
            context(Foo)
            @Composable
            fun Test() { }
        """
    )

    @Test
    fun testMultipleContextReceivers(): Unit = contextReceivers(
        """
            class Foo { }
            class Bar { }
            class FooBar { }
        """,
        """
            context(Foo, Bar)
            @Composable
            fun A() { }

            context(Foo, Bar, FooBar)
            @Composable
            fun B() { }
        """
    )

    @Test
    fun testContextReceiversAndExtensionReceiver(): Unit = contextReceivers(
        """
            class Foo { }
            class Bar { }
            class FooBar { }
        """,
        """
            context(Foo, Bar)
            @Composable
            fun String.A() { }

            context(Foo, Bar, FooBar)
            @Composable
            fun String.B() { }
        """
    )

    @Test
    fun testContextReceiversAndDefaultParams(): Unit = contextReceivers(
        """
            class Foo { }
            class Bar { }
            class FooBar { }
        """,
        """
            context(Foo, Bar)
            @Composable
            fun A(a: Int = 1) { }

            context(Foo, Bar, FooBar)
            @Composable
            fun B(a: Int, b: String = "", c: Int = 1) { }

            context(Foo)
            @Composable
            fun C(a: Int, bar: Bar = Bar()) { }
        """
    )

    @Test
    fun testContextReceiversAndExtensionReceiverAndDefaultParams(): Unit = contextReceivers(
        """
            class Foo { }
            class Bar { }
            class FooBar { }
        """,
        """
            context(Foo, Bar, FooBar)
            @Composable
            fun String.B(a: Int, b: String = "", c: Int = 1) { }
        """
    )

    @Test
    fun testContextReceiversWith(): Unit = contextReceivers(
        """
            context(Foo)
            @Composable
            fun A() { }

            class Foo { }
        """,
        """

            @Composable
            fun Test(foo: Foo) {
                with(foo) {
                  A()
                }
            }
        """
    )

    @Test
    fun testContextReceiversNestedWith(): Unit = contextReceivers(
        """
            context(Foo)
            @Composable
            fun A() { }

            context(Foo, Bar)
            @Composable
            fun B() { }

            class Foo { }
            class Bar { }
        """,
        """
            @Composable
            fun Test(foo: Foo) {
                with(foo) {
                    A()
                    with(Bar()) {
                        B()
                    }
                }
            }
        """
    )

    @Test
    fun testContextReceiversWithAndDefaultParam(): Unit = contextReceivers(
        """
            context(Foo)
            @Composable
            fun String.A(param1: Int, param2: String = "") { }

            class Foo { }
        """,
        """
            @Composable
            fun Test(foo: Foo) {
                with(foo) {
                  "Hello".A(2)
                }
            }
        """
    )

    @Test
    fun testLotsOfContextReceivers(): Unit = contextReceivers(
        """
            class A { }
            class B { }
            class C { }
            class D { }
            class E { }
            class F { }
            class G { }
            class H { }
            class I { }
            class J { }
            class K { }
            class L { }
        """,
        """
            context(A, B, C, D, E, F, G, H, I, J, K, L)
            @Composable
            fun Test() {
            }
        """
    )

    @Test
    fun testContextReceiverAndComposableLambdaParam() {
        contextReceivers(
            """
                class Foo { }
            """,
            """
                context(Foo)
                @Composable
                fun Test(a: String, b: @Composable (String) -> Unit) {
                    b("yay")
                }
            """
        )
    }

    @Test
    fun testContextReceiverAndDefaultParamsUsage(): Unit = contextReceivers(
        """
            class Foo {
                val someString = "Some String"
            }
        """,
        """
            @Composable
            fun Parent() {
                with(Foo()) {
                    Test()
                    Test(a = "a")
                    Test(b = 101)
                    Test(a = "Yes", b = 10)
                }
            }

            context(Foo)
            @Composable
            fun Test(a: String = "A", b: Int = 2) {
                val combineParams = a + b
                if (someString == combineParams) {
                    println("Same same")
                }
            }
        """
    )
}
