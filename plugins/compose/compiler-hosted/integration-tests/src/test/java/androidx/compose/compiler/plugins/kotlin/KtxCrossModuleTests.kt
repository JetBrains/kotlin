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

package androidx.compose.compiler.plugins.kotlin

import android.widget.TextView
import androidx.compose.runtime.Composer
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import java.net.URLClassLoader
import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(
    manifest = Config.NONE,
    minSdk = 23,
    maxSdk = 23
)
class KtxCrossModuleTests(useFir: Boolean) : AbstractCodegenTest(useFir) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "useFir = {0}")
        fun data() = arrayOf<Any>(false, true)
    }

    @Test
    fun testInlineFunctionDefaultArgument() {
        compile(
            mapOf(
                "library module" to mapOf(
                    "x/library.kt" to """
                    package x

                    import androidx.compose.runtime.Composable

                    @Composable
                    inline fun f(x: () -> Unit = { println("default") }) {
                      x()
                    }
                    """.trimIndent()
                ),
                "Main" to mapOf(
                    "y/User.kt" to """
                    package y

                    import x.f
                    import androidx.compose.runtime.Composable

                    @Composable
                    fun g() {
                      f {
                        println("non-default")
                      }
                    }
                    """.trimIndent()
                )
            )
        )
    }

    @Test
    fun testInlineFunctionDefaultArgument2() {
        compile(
            mapOf(
                "library module" to mapOf(
                    "x/library.kt" to """
                    package x

                    import androidx.compose.runtime.Composable

                    @Composable
                    inline fun f(x: () -> Unit = { println("default") }) {
                      x()
                    }
                    """.trimIndent()
                ),
                "Main" to mapOf(
                    "y/User.kt" to """
                    package y

                    import x.f
                    import androidx.compose.runtime.Composable

                    @Composable
                    fun g() {
                      f()
                    }
                    """.trimIndent()
                )
            )
        )
    }

    @Test
    fun testAccessibilityBridgeGeneration() {
        compile(
            mapOf(
                "library module" to mapOf(
                    "x/I.kt" to """
                      package x

                      import androidx.compose.runtime.Composable

                      @Composable fun bar(arg: @Composable () -> Unit) {
                          arg()
                      }
                    """.trimIndent()
                ),
                "Main" to mapOf(
                    "y/User.kt" to """
                      package y

                      import x.bar
                      import androidx.compose.runtime.Composable

                      @Composable fun baz() {
                          bar {
                            foo()
                          }
                      }
                      @Composable private fun foo() { }
                    """.trimIndent()
                )
            )
        ) {
            // Check that there is only one method declaration for access$foo.
            // We used to introduce more symbols for the same function leading
            // to multiple identical methods in the output.
            // In the dump, $ is mapped to %.
            val declaration = "synthetic access%foo"
            val occurrences = it.windowed(declaration.length) { candidate ->
                if (candidate == declaration)
                    1
                else
                    0
            }.sum()
            assert(occurrences == 1)
        }
    }

    @Test
    fun testInlineClassCrossModule() {
        compile(
            mapOf(
                "library module" to mapOf(
                    "x/I.kt" to """
                      package x
                      inline class I(val i: Int) {
                        val prop
                          get() = i + 1
                      }
                    """.trimIndent()
                ),
                "Main" to mapOf(
                    "y/User.kt" to """
                      package y
                      import x.I
                      inline class J(val j: Int)
                      fun foo(): Int = I(42).i + J(23).j + I(1).prop
                    """.trimIndent()
                )
            )
        ) {
            // If the output contains getI-impl, the cross-module inline class
            // was incorrectly compiled and the getter was not removed. This
            // happens if the relationship between the getter and the corresponding
            // property is broken by the compiler.
            assert(!it.contains("getI-impl"))
            // Check that inline classes where optimized to integers.
            assert(it.contains("INVOKESTATIC x/I.constructor-impl (I)I"))
            assert(it.contains("INVOKESTATIC y/J.constructor-impl (I)I"))
            // Check that the inline class prop getter is correctly mangled.
            assert(it.contains("INVOKESTATIC x/I.getProp-impl (I)I"))
        }
    }

    @Test // see: b/255983530
    fun testNonComposableWithComposableReturnTypeCrossModule() {
        compile(
            mapOf(
                "library module" to mapOf(
                    "x/MakeComposable.kt" to """
                      package x
                      import androidx.compose.runtime.Composable

                      fun makeComposable(): @Composable () -> Unit = @Composable {}
                    """.trimIndent()
                ),
                "Main" to mapOf(
                    "y/User.kt" to """
                      package y
                      import x.makeComposable
                      import androidx.compose.runtime.Composable

                      fun acceptComposable(composable: @Composable () -> Unit) {

                      }

                      fun test() {
                        acceptComposable(makeComposable())
                      }
                    """.trimIndent()
                )
            ),
        ) {
            assert(
                it.contains("public final static makeComposable()Lkotlin/jvm/functions/Function2;")
            )
            assert(
                !it.contains(
                "INVOKESTATIC x/MakeComposableKt.makeComposable ()Lkotlin/jvm/functions/Function0;"
                )
            )
            assert(
                it.contains(
                "INVOKESTATIC x/MakeComposableKt.makeComposable ()Lkotlin/jvm/functions/Function2;"
                )
            )
        }
    }

    @Test // see: b/255983530
    fun testNonComposableWithNestedComposableReturnTypeCrossModule() {
        compile(
            mapOf(
                "library module" to mapOf(
                    "x/MakeComposable.kt" to """
                      package x
                      import androidx.compose.runtime.Composable

                      fun makeComposable(): List<@Composable () -> Unit> = listOf(@Composable {})
                    """.trimIndent()
                ),
                "Main" to mapOf(
                    "y/User.kt" to """
                      package y
                      import x.makeComposable
                      import androidx.compose.runtime.Composable

                      fun acceptComposable(composable: @Composable () -> Unit) {

                      }

                      fun test() {
                        acceptComposable(makeComposable().single())
                      }
                    """.trimIndent()
                )
            ),
        ) {
            assert(
                it.contains("INVOKESTATIC x/MakeComposableKt.makeComposable ()Ljava/util/List;")
            )
            assert(!it.contains("CHECKCAST kotlin/jvm/functions/Function0"))
            assert(it.contains("CHECKCAST kotlin/jvm/functions/Function2"))
        }
    }

    @Test
    fun testInlineClassOverloading() {
        compile(
            mapOf(
                "library module" to mapOf(
                    "x/A.kt" to """
                        package x

                        import androidx.compose.runtime.Composable

                        inline class I(val i: Int)
                        inline class J(val j: Int)

                        @Composable fun foo(i: I) { }
                        @Composable fun foo(j: J) { }
                    """.trimIndent()
                ),
                "Main" to mapOf(
                    "y/B.kt" to """
                        package y

                        import androidx.compose.runtime.Composable
                        import x.*

                        @Composable fun bar(k: Int) {
                            foo(I(k))
                            foo(J(k))
                        }
                    """
                )
            )
        ) {
            // Check that the composable functions were properly mangled
            assert(
                it.contains(
                    "public final static foo-4e73Vzs(ILandroidx/compose/runtime/Composer;I)V"
                )
            )
            assert(
                it.contains(
                    "public final static foo-YK1ovzU(ILandroidx/compose/runtime/Composer;I)V"
                )
            )
            // Check that we didn't leave any references to the original name, which probably
            // leads to a compile error.
            assert(!it.contains("foo("))
        }
    }

    @Test
    fun testFunInterfaceWithInlineClass() {
        compile(
            mapOf(
                "library module" to mapOf(
                    "x/A.kt" to """
                        package x

                        inline class A(val value: Int)
                        fun interface B {
                          fun method(a: A)
                        }
                    """.trimIndent()
                ),
                "Main" to mapOf(
                    "y/B.kt" to """
                        package y

                        import x.*

                        val b = B { }
                    """
                )
            )
        ) {
            assert(it.contains("public abstract method-C8LvVsQ(I)V"))
            assert(it.contains("public final method-C8LvVsQ(I)V"))
            assert(!it.contains("public final method(I)V"))
        }
    }

    @Test
    fun testParentNotInitializedBug() {
        compile(
            mapOf(
                "library module" to mapOf(
                    "x/Base.kt" to """
                    package x

                    import androidx.compose.runtime.Composable

                    class Foo

                    abstract class Base {
                        @Composable abstract fun content(a: Foo)
                    }
                 """
                ),
                "Main" to mapOf(
                    "b/Extends.kt" to """
                    package b

                    import androidx.compose.runtime.Composable
                    import x.Base
                    import x.Foo

                    abstract class Extends : Base() {
                        @Composable
                        override fun content(a: Foo) {
                        }
                    }
                """
                )
            )
        )
    }

    @Test
    fun testConstCrossModule() {
        compile(
            mapOf(
                "library module" to mapOf(
                    "x/A.kt" to """
                    package x

                    const val MyConstant: String = ""
                 """
                ),
                "Main" to mapOf(
                    "b/B.kt" to """
                    package b

                    import x.MyConstant

                    fun Test(foo: String = MyConstant) {
                        print(foo)
                    }
                """
                )
            )
        ) {
            assert(it.contains("LDC \"\""))
            assert(!it.contains("INVOKESTATIC x/AKt.getMyConstant"))
        }
    }

    @Test
    fun testNonCrossinlineComposable() {
        compile(
            mapOf(
                "library module" to mapOf(
                    "x/A.kt" to """
                    package x

                    import androidx.compose.runtime.Composable

                    @Composable
                    inline fun <T> key(
                        block: @Composable () -> T
                    ): T = block()
                 """
                ),
                "Main" to mapOf(
                    "b/B.kt" to """
                    package b

                    import androidx.compose.runtime.Composable
                    import x.key

                    @Composable fun Test() {
                        key { }
                    }
                """
                )
            )
        )
    }

    @Test
    fun testNonCrossinlineComposableNoGenerics() {
        compile(
            mapOf(
                "library module" to mapOf(
                    "x/A.kt" to """
                    package x

                    import androidx.compose.runtime.Composable

                    @Composable
                    inline fun key(
                        @Suppress("UNUSED_PARAMETER")
                        v1: Int,
                        block: @Composable () -> Int
                    ): Int = block()
                 """
                ),
                "Main" to mapOf(
                    "b/B.kt" to """
                    package b

                    import androidx.compose.runtime.Composable
                    import x.key

                    @Composable fun Test() {
                        key(123) { 456 }
                    }
                """
                )
            )
        )
    }

    @Test
    fun testRemappedTypes() {
        compile(
            mapOf(
                "library module" to mapOf(
                    "x/A.kt" to """
                    package x

                    class A {
                        fun makeA(): A { return A() }
                        fun makeB(): B { return B() }
                        class B() {
                        }
                    }
                 """
                ),
                "Main" to mapOf(
                    "b/B.kt" to """
                    package b

                    import x.A

                    class C {
                        fun useAB() {
                            val a = A()
                            a.makeA()
                            a.makeB()
                            val b = A.B()
                        }
                    }
                """
                )
            )
        )
    }

    @Test
    fun testInlineIssue() {
        compile(
            mapOf(
                "library module" to mapOf(
                    "x/C.kt" to """
                    fun ghi() {
                        abc {}
                    }
                    """,
                    "x/A.kt" to """
                    inline fun abc(fn: () -> Unit) {
                        fn()
                    }
                    """,
                    "x/B.kt" to """
                    fun def() {
                        abc {}
                    }
                    """
                ),
                "Main" to mapOf(
                    "b/B.kt" to """
                """
                )
            )
        )
    }

    @Test
    fun testInlineComposableProperty() {
        compile(
            mapOf(
                "library module" to mapOf(
                    "x/A.kt" to """
                    package x

                    import androidx.compose.runtime.Composable

                    class Foo {
                      val value: Int @Composable get() = 123
                    }
                 """
                ),
                "Main" to mapOf(
                    "b/B.kt" to """
                    package b

                    import androidx.compose.runtime.Composable
                    import x.Foo

                    val foo = Foo()

                    @Composable fun Test() {
                        print(foo.value)
                    }
                """
                )
            )
        )
    }

    @Test
    fun testNestedInlineIssue() {
        compile(
            mapOf(
                "library module" to mapOf(
                    "x/C.kt" to """
                    fun ghi() {
                        abc {
                            abc {

                            }
                        }
                    }
                    """,
                    "x/A.kt" to """
                    inline fun abc(fn: () -> Unit) {
                        fn()
                    }
                    """,
                    "x/B.kt" to """
                    fun def() {
                        abc {
                            abc {

                            }
                        }
                    }
                    """
                ),
                "Main" to mapOf(
                    "b/B.kt" to """
                """
                )
            )
        )
    }

    @Test
    fun testComposerIntrinsicInline() {
        compile(
            mapOf(
                "library module" to mapOf(
                    "x/C.kt" to """
                    import androidx.compose.runtime.Composable

                    @Composable
                    fun ghi() {
                        val x = abc()
                        print(x)
                    }
                    """,
                    "x/A.kt" to """
                    import androidx.compose.runtime.Composable
                    import androidx.compose.runtime.currentComposer

                    @Composable
                    inline fun abc(): Any {
                        return currentComposer
                    }
                    """,
                    "x/B.kt" to """
                    import androidx.compose.runtime.Composable

                    @Composable
                    fun def() {
                        val x = abc()
                        print(x)
                    }
                    """
                ),
                "Main" to mapOf(
                    "b/B.kt" to """
                """
                )
            )
        )
    }

    @Test
    fun testComposableOrderIssue() {
        compile(
            mapOf(
                "library module" to mapOf(
                    "C.kt" to """
                    import androidx.compose.runtime.*

                    @Composable
                    fun b() {
                        a()
                    }
                    """,
                    "A.kt" to """
                    import androidx.compose.runtime.*

                    @Composable
                    fun a() {

                    }
                    """,
                    "B.kt" to """
                    import androidx.compose.runtime.*

                    @Composable
                    fun c() {
                        a()
                    }

                    """
                ),
                "Main" to mapOf(
                    "b/B.kt" to """
                """
                )
            )
        )
    }

    @Test
    fun testSimpleXModuleCall() {
        compile(
            mapOf(
                "library module" to mapOf(
                    "a/A.kt" to """
                    package a

                    import androidx.compose.runtime.*

                    @Composable
                    fun FromA() {}
                 """
                ),
                "Main" to mapOf(
                    "b/B.kt" to """
                    package b

                    import a.FromA
                    import androidx.compose.runtime.*

                    @Composable
                    fun FromB() {
                        FromA()
                    }
                """
                )
            )
        )
    }

    @Test
    fun testJvmFieldIssue() {
        compile(
            mapOf(
                "library module" to mapOf(
                    "x/C.kt" to """
                    fun Test2() {
                      bar = 10
                      print(bar)
                    }
                    """,
                    "x/A.kt" to """
                      @JvmField var bar: Int = 0
                    """,
                    "x/B.kt" to """
                    fun Test() {
                      bar = 10
                      print(bar)
                    }
                    """
                ),
                "Main" to mapOf(
                    "b/B.kt" to """
                """
                )
            )
        )
    }

    @Test
    fun testInstanceXModuleCall() {
        compile(
            mapOf(
                "library module" to mapOf(
                    "a/Foo.kt" to """
                    package a

                    import androidx.compose.runtime.*

                    class Foo {
                        @Composable
                        fun FromA() {}
                    }
                 """
                ),
                "Main" to mapOf(
                    "B.kt" to """
                    import a.Foo
                    import androidx.compose.runtime.*

                    @Composable
                    fun FromB() {
                        Foo().FromA()
                    }
                """
                )
            )
        )
    }

    @Test
    fun testXModuleProperty() {
        compile(
            mapOf(
                "library module" to mapOf(
                    "a/Foo.kt" to """
                    package a

                    import androidx.compose.runtime.*

                    val foo: Int @Composable get() { return 123 }
                 """
                ),
                "Main" to mapOf(
                    "B.kt" to """
                    import a.foo
                    import androidx.compose.runtime.*

                    @Composable
                    fun FromB() {
                        foo
                    }
                """
                )
            )
        )
    }

    @Test
    fun testXModuleInterface() {
        compile(
            mapOf(
                "library module" to mapOf(
                    "a/Foo.kt" to """
                    package a

                    import androidx.compose.runtime.*

                    interface Foo {
                        @Composable fun foo()
                    }
                 """
                ),
                "Main" to mapOf(
                    "B.kt" to """
                    import a.Foo
                    import androidx.compose.runtime.*

                    class B : Foo {
                        @Composable override fun foo() {}
                    }

                    @Composable fun Example(inst: Foo) {
                        B().foo()
                        inst.foo()
                    }
                """
                )
            )
        )
    }

    @Test
    fun testXModuleComposableProperty() {
        compile(
            mapOf(
                "library module" to mapOf(
                    "a/Foo.kt" to """
                    package a

                    import androidx.compose.runtime.*

                    val foo: () -> Unit
                        @Composable get() = {}
                 """
                ),
                "Main" to mapOf(
                    "B.kt" to """
                    import a.foo
                    import androidx.compose.runtime.*

                    @Composable fun Example() {
                        val bar = foo
                        bar()
                    }
                """
                )
            )
        )
    }

    @Test
    fun testXModuleCtorComposableParam() {
        compile(
            mapOf(
                "library module" to mapOf(
                    "a/Foo.kt" to """
                    package a

                    import androidx.compose.runtime.*

                    class Foo(val bar: @Composable () -> Unit)
                 """
                ),
                "Main" to mapOf(
                    "B.kt" to """
                    import a.Foo
                    import androidx.compose.runtime.*

                    @Composable fun Example(bar: @Composable () -> Unit) {
                        val foo = Foo(bar)
                    }
                """
                )
            )
        )
    }

    @Ignore("b/171801506")
    @Test
    fun testCrossModule_SimpleComposition() {
        val tvId = 29

        compose(
            "TestF",
            mapOf(
                "library module" to mapOf(
                    "my/test/lib/InternalComp.kt" to """
                    package my.test.lib

                    import androidx.compose.runtime.*

                    @Composable fun InternalComp(block: @Composable () -> Unit) {
                        block()
                    }
                 """
                ),
                "Main" to mapOf(
                    "my/test/app/Main.kt" to """
                   package my.test.app

                   import android.widget.*
                   import androidx.compose.runtime.*
                   import androidx.compose.ui.viewinterop.emitView
                   import my.test.lib.*

                   var bar = 0
                   var scope: RecomposeScope? = null

                   class TestF {
                       @Composable
                       fun compose() {
                         scope = currentRecomposeScope
                         Foo(bar)
                       }

                       fun advance() {
                         bar++
                         scope?.invalidate()
                       }
                   }

                   @Composable
                   fun Foo(bar: Int) {
                     InternalComp {
                       emitView(::TextView) { 
                         it.text="${'$'}bar" 
                         it.id=$tvId
                       }
                     }
                   }
                """
                )
            )
        ).then { activity ->
            val tv = activity.findViewById(tvId) as TextView
            assertEquals("0", tv.text)
        }.then { activity ->
            val tv = activity.findViewById(tvId) as TextView
            assertEquals("1", tv.text)
        }
    }

    /**
     * Test for b/169071070
     */
    @Test
    fun testCrossModule_ComposableInterfaceFunctionWithInlineClasses() {
        compile(
            mapOf(
                "library module" to mapOf(
                    "x/Library.kt" to """
                    package x

                    import androidx.compose.runtime.Composable

                    inline class InlineClass(val value: Float)

                    interface Foo {
                        @Composable
                        fun apply(value: InlineClass)
                    }
                    """.trimIndent()
                ),
                "Main" to mapOf(
                    "y/Impl.kt" to """
                    package y

                    import androidx.compose.runtime.Composable
                    import x.Foo
                    import x.InlineClass

                    object Bar : Foo {
                        @Composable
                        override fun apply(value: InlineClass) {}
                    }
                    """.trimIndent()
                )
            )
        )
    }

    @Test
    fun testAnnotationInferenceAcrossModules() {
        compile(
            mapOf(
                "Base" to mapOf(
                    "base/Library.kt" to """
                    package base

                    import androidx.compose.runtime.*

                    @Composable
                    @ComposableTarget("UI")
                    fun Text(text: String) { }

                    @Composable
                    @ComposableTarget("UI")
                    fun Row(content: @Composable @ComposableTarget("UI") () -> Unit) {
                      content()
                    }
                    """
                ),
                "Client" to mapOf(
                    "client/Library.kt" to """
                    package client

                    import androidx.compose.runtime.*
                    import base.*

                    @Composable
                    fun Labeled(text: String, content: @Composable () -> Unit) {
                      Text(text)
                      Row { content() }
                    }
                    """
                ),
                "Main" to mapOf(
                    "Main.kt" to """
                    package main

                    import androidx.compose.runtime.*
                    import client.*

                    @Composable
                    fun Main() {
                      Labeled("test") { }
                    }
                    """
                )
            )
        ) {
            assert(it.contains("[UI[UI]]", false)) {
                "Layered composable didn't store the inferred composable target"
            }
        }
    }

    /**
     * Test for b/221280935
     */
    @Test
    fun testOverriddenSymbolParentsInDefaultParameters() {
        compile(
            mapOf(
                "Base" to mapOf(
                    "base/Base.kt" to """
                    package base

                    import androidx.compose.runtime.Composable

                    open class Base {
                      fun f(block: (@Composable () -> Unit)? = null) {}
                    }
                    """
                ),
                "Main" to mapOf(
                    "Main.kt" to """
                    package main

                    import androidx.compose.runtime.Composable
                    import base.Base

                    class Child : Base() {
                      init { f {} }
                    }
                    """
                )
            )
        )
    }

    @Test
    fun testFunctionInterfaceReturningComposable() {
        compile(
            mapOf(
                "Base" to mapOf(
                    "base/Base.kt" to """
                    package base

                    import androidx.compose.runtime.Composable

                    fun interface Base {
                        fun getContent(b: @Composable () -> Unit): @Composable () -> Unit
                    }
                    """
                ),
                "Main" to mapOf(
                    "Main.kt" to """
                    package main

                    import base.Base

                    val funInterfaceReturnComposable = Base {
                        it
                    }

                    fun main() {
                       funInterfaceReturnComposable.getContent {}
                    }
                    """
                )
            ),
            validate = {
                val indyExpr = Regex("INVOKEDYNAMIC.*?\\[([\\w\\W]*?)]").find(it)
                val indyParams = indyExpr?.groupValues?.first()

                assertTrue(
                    "Could not find INVOKEDYNAMIC call",
                    indyParams != null
                )
                assertEquals(
                    indyParams!!.lines().joinToString("\n") { it.trimEnd() },
                    """
                        INVOKEDYNAMIC getContent()Lbase/Base; [
                              // handle kind 0x6 : INVOKESTATIC
                              java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles%Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;
                              // arguments:
                              (Lkotlin/jvm/functions/Function2;)Lkotlin/jvm/functions/Function2;,
                              // handle kind 0x6 : INVOKESTATIC
                              main/MainKt.funInterfaceReturnComposable%lambda%0(Lkotlin/jvm/functions/Function2;)Lkotlin/jvm/functions/Function2;,
                              (Lkotlin/jvm/functions/Function2;)Lkotlin/jvm/functions/Function2;
                            ]
                    """.trimIndent()
                )
            },
        )
    }

    private fun compile(
        modules: Map<String, Map<String, String>>,
        dumpClasses: Boolean = false,
        validate: ((String) -> Unit)? = null
    ): List<OutputFile> {
        val libraryClasses = modules.filter { it.key != "Main" }.flatMap {
            classLoader(
                it.value,
                listOf(classesDirectory.root),
                dumpClasses
            ).allGeneratedFiles.also { outputFiles ->
                // Write the files to the class directory so they can be used by the next module
                // and the application
                outputFiles.writeToDir(classesDirectory.root)
            }
        }

        // compile the next one
        val appClasses = classLoader(
            modules["Main"]
                ?: error("No Main module specified"),
            listOf(classesDirectory.root),
            dumpClasses
        ).allGeneratedFiles

        // Load the files looking for mainClassName
        val outputFiles = (libraryClasses + appClasses).filter {
            it.relativePath.endsWith(".class")
        }

        if (validate != null) {
            validate(outputFiles.joinToString("\n") { it.asText().replace('$', '%') })
        }

        return outputFiles
    }

    private fun compose(
        mainClassName: String,
        modules: Map<String, Map<String, String>>,
        dumpClasses: Boolean = false
    ): RobolectricComposeTester {
        val allClasses = compile(modules, dumpClasses)
        val loader = URLClassLoader(emptyArray(), this.javaClass.classLoader)
        val instanceClass = run {
            var instanceClass: Class<*>? = null
            var loadedOne = false
            for (outFile in allClasses) {
                val bytes = outFile.asByteArray()
                val loadedClass = loadClass(loader, null, bytes)
                if (loadedClass.name.endsWith(mainClassName)) instanceClass = loadedClass
                loadedOne = true
            }
            if (!loadedOne) error("No classes loaded")
            instanceClass ?: error("Could not find class $mainClassName in loaded classes")
        }

        val instanceOfClass = instanceClass.getDeclaredConstructor().newInstance()
        val advanceMethod = instanceClass.getMethod("advance")
        val composeMethod = instanceClass.getMethod(
            "compose",
            Composer::class.java,
            Int::class.java
        )

        return composeMulti({ composer, _ ->
            composeMethod.invoke(instanceOfClass, composer, 1)
        }) {
            advanceMethod.invoke(instanceOfClass)
        }
    }

    @JvmField
    @Rule
    val classesDirectory = TemporaryFolder()
}

fun OutputFile.writeToDir(directory: File) =
    FileUtil.writeToFile(File(directory, relativePath), asByteArray())

fun Collection<OutputFile>.writeToDir(directory: File) = forEach { it.writeToDir(directory) }
