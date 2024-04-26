/*
 * Copyright 2024 The Android Open Source Project
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

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

/* ktlint-disable max-line-length */
class ComposerParamSignatureTests(useFir: Boolean) : AbstractCodegenSignatureTest(useFir) {
    @Test
    fun testParameterlessChildrenLambdasReused() = checkApi(
        """
            @Composable fun Foo(content: @Composable () -> Unit) {
            }
            @Composable fun Bar() {
                Foo {}
            }
        """
    )

    @Test
    fun testNoComposerNullCheck() = validateBytecode(
        """
        @Composable fun Foo() {}
        """
    ) {
        assert(!it.contains("INVOKESTATIC kotlin/jvm/internal/Intrinsics.checkParameterIsNotNull"))
    }

    @Test
    fun testComposableLambdaCall() = validateBytecode(
        """
            @Composable
            fun Foo(f: @Composable () -> Unit) {
              f()
            }
        """
    ) {
        // Calls to a composable lambda needs to invoke the `Function2.invoke` interface method
        // taking two objects and *not* directly the `invoke` method that takes a Composer and
        // an unboxed int.
        assertTrue(it.contains("INVOKEINTERFACE kotlin/jvm/functions/Function2.invoke (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object; (itf)"))
    }

    @Test
    fun testStrangeReceiverIssue() = codegen(
        """
        import androidx.compose.runtime.ExplicitGroupsComposable
        import androidx.compose.runtime.NonRestartableComposable
        class Foo

        @Composable
        @ExplicitGroupsComposable
        fun A(foo: Foo) {
            foo.b()
        }

        @Composable
        @ExplicitGroupsComposable
        inline fun Foo.b(label: String = "") {
            c(this, label)
        }

        @Composable
        @ExplicitGroupsComposable
        inline fun c(foo: Foo, label: String) {
            used(label)
        }
        """
    )

    @Test
    fun testArrayListSizeOverride() = validateBytecode(
        """
        class CustomList : ArrayList<Any>() {
            override val size: Int
                get() = super.size
        }
        """
    ) {
        assertTrue(it.contains("INVOKESPECIAL java/util/ArrayList.size ()I"))
        assertFalse(it.contains("INVOKESPECIAL java/util/ArrayList.getSize ()I"))
    }

    @Test
    fun testForLoopIssue1() = codegen(
        """
            @Composable
            fun Test(text: String, callback: @Composable () -> Unit) {
                for (char in text) {
                    if (char == '}') {
                        callback()
                        continue
                    }
                }
            }
        """
    )

    @Test
    fun testConstantReturn() = validateBytecode(
        """
            @Composable
            fun Test(): Int {
                return 123 // line 12
            }
        """
    ) {
        val lines = it.split("\n").map { it.trim() }
        val lineNumberIndex = lines.indexOfFirst { it.startsWith("LINENUMBER 12") }
        // Line 12, which has the return statement, needs to be present in the bytecode
        assert(lineNumberIndex >= 0)
        // The return statement should be right after this
        assert(lines[lineNumberIndex + 1] == "IRETURN")
    }

    @Test
    fun testForLoopIssue2() = codegen(
        """
            @Composable
            fun Test(text: List<String>, callback: @Composable () -> Unit) {
                for ((i, value) in text.withIndex()) {
                    if (value == "" || i == 0) {
                        callback()
                        continue
                    }
                }
            }
        """
    )

    @Test
    fun testCaptureIssue23() = codegen(
        """
            import androidx.compose.animation.AnimatedContent
            import androidx.compose.animation.ExperimentalAnimationApi
            import androidx.compose.runtime.Composable

            @OptIn(ExperimentalAnimationApi::class)
            @Composable
            fun SimpleAnimatedContentSample() {
                @Composable fun Foo() {}

                AnimatedContent(1f) {
                    Foo()
                }
            }
        """
    )

    @Test
    fun test32Params() = codegen(
        """
        @Composable
        fun <T> TooVerbose(
            v00: T, v01: T, v02: T, v03: T, v04: T, v05: T, v06: T, v07: T, v08: T, v09: T,
            v10: T, v11: T, v12: T, v13: T, v14: T, v15: T, v16: T, v17: T, v18: T, v19: T,
            v20: T, v21: T, v22: T, v23: T, v24: T, v25: T, v26: T, v27: T, v28: T, v29: T,
            v30: T, v31: T,
        ) {
        }

        @Composable
        fun Test() {
            TooVerbose(
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1,
            )
        }

        """
    )

    @Test
    fun testInterfaceMethodWithComposableParameter() = validateBytecode(
        """
            @Composable
            fun test1(cc: ControlledComposition) {
                cc.setContent {}
            }
            fun test2(cc: ControlledComposition) {
                cc.setContent {}
            }
        """
    ) {
        assert(!it.contains("INVOKEINTERFACE androidx/compose/runtime/ControlledComposition.setContent (Lkotlin/jvm/functions/Function0;)V"))
    }

    @Test
    fun testFakeOverrideFromSameModuleButLaterTraversal() = validateBytecode(
        """
            class B : A() {
                fun test() {
                    show {}
                }
            }
            open class A {
                fun show(content: @Composable () -> Unit) {}
            }
        """
    ) {
        assert(!it.contains("INVOKEINTERFACE androidx/compose/runtime/ControlledComposition.setContent (Lkotlin/jvm/functions/Function0;)V"))
    }

    @Test
    fun testPrimitiveChangedCalls() = validateBytecode(
        """
        @Composable fun Foo(
            a: Boolean,
            b: Char,
            c: Byte,
            d: Short,
            e: Int,
            f: Float,
            g: Long,
            h: Double
        ) {
            used(a)
            used(b)
            used(c)
            used(d)
            used(e)
            used(f)
            used(g)
            used(h)
        }
        """
    ) {
        assert(it.contains("INVOKEINTERFACE androidx/compose/runtime/Composer.changed (Z)Z"))
        assert(it.contains("INVOKEINTERFACE androidx/compose/runtime/Composer.changed (C)Z"))
        assert(it.contains("INVOKEINTERFACE androidx/compose/runtime/Composer.changed (B)Z"))
        assert(it.contains("INVOKEINTERFACE androidx/compose/runtime/Composer.changed (S)Z"))
        assert(it.contains("INVOKEINTERFACE androidx/compose/runtime/Composer.changed (I)Z"))
        assert(it.contains("INVOKEINTERFACE androidx/compose/runtime/Composer.changed (F)Z"))
        assert(it.contains("INVOKEINTERFACE androidx/compose/runtime/Composer.changed (J)Z"))
        assert(it.contains("INVOKEINTERFACE androidx/compose/runtime/Composer.changed (D)Z"))
        assert(!it.contains("INVOKEINTERFACE androidx/compose/runtime/Composer.changed (Ljava/lang/Object;)Z"))
    }

    @Test
    fun testNonPrimitiveChangedCalls() = validateBytecode(
        """
        import androidx.compose.runtime.Stable

        @Stable class Bar
        @Composable fun Foo(a: Bar) {
            used(a)
        }
        """
    ) {
        assert(!it.contains("INVOKEINTERFACE androidx/compose/runtime/Composer.changed (Z)Z"))
        assert(!it.contains("INVOKEINTERFACE androidx/compose/runtime/Composer.changed (C)Z"))
        assert(!it.contains("INVOKEINTERFACE androidx/compose/runtime/Composer.changed (B)Z"))
        assert(!it.contains("INVOKEINTERFACE androidx/compose/runtime/Composer.changed (S)Z"))
        assert(!it.contains("INVOKEINTERFACE androidx/compose/runtime/Composer.changed (I)Z"))
        assert(!it.contains("INVOKEINTERFACE androidx/compose/runtime/Composer.changed (F)Z"))
        assert(!it.contains("INVOKEINTERFACE androidx/compose/runtime/Composer.changed (J)Z"))
        assert(!it.contains("INVOKEINTERFACE androidx/compose/runtime/Composer.changed (D)Z"))
        assert(it.contains("INVOKEINTERFACE androidx/compose/runtime/Composer.changed (Ljava/lang/Object;)Z"))
    }

    @Test
    fun testInlineClassChangedCalls() = validateBytecode(
        """
        inline class Bar(val value: Int)
        @Composable fun Foo(a: Bar) {
            used(a)
        }
        """
    ) {
        assert(!it.contains("INVOKESTATIC Bar.box-impl (I)LBar;"))
        assert(it.contains("INVOKEINTERFACE androidx/compose/runtime/Composer.changed (I)Z"))
        assert(
            !it.contains("INVOKEINTERFACE androidx/compose/runtime/Composer.changed (Ljava/lang/Object;)Z")
        )
    }

    @Test
    fun testNullableInlineClassChangedCalls() = validateBytecode(
        """
        inline class Bar(val value: Int)
        @Composable fun Foo(a: Bar?) {
            used(a)
        }
        """
    ) {
        val testClass = it.split("public final class ").single { it.startsWith("test/TestKt") }
        assert(
            !testClass.contains(
                "INVOKEVIRTUAL Bar.unbox-impl ()I"
            )
        )
        assert(
            !testClass.contains(
                "INVOKESTATIC java/lang/Integer.valueOf (I)Ljava/lang/Integer;"
            )
        )
        assert(
            testClass.contains(
                "INVOKEINTERFACE androidx/compose/runtime/Composer.changed (Ljava/lang/Object;)Z"
            )
        )
    }

    @Test
    fun testNoNullCheckForPassedParameters() = validateBytecode(
        """
        inline class Bar(val value: Int)
        fun nonNull(bar: Bar) {}
        @NonRestartableComposable @Composable fun Foo(bar: Bar = Bar(123)) {
            nonNull(bar)
        }
        """
    ) {
        assert(it.contains("public final static Foo-9N9I_pQ(ILandroidx/compose/runtime/Composer;II)V"))
    }

    @Test
    fun testNoComposerNullCheck2() = validateBytecode(
        """
        val foo = @Composable {}
        val bar = @Composable { x: Int -> }
        """
    ) {
        assert(!it.contains("INVOKESTATIC kotlin/jvm/internal/Intrinsics.checkParameterIsNotNull"))
    }

    @Test
    fun testComposableLambdaInvoke() = validateBytecode(
        """
        @Composable fun NonNull(content: @Composable() () -> Unit) {
            content.invoke()
        }
        @Composable fun Nullable(content: (@Composable() () -> Unit)?) {
            content?.invoke()
        }
        """
    ) {
        assert(
            !it.contains(
                "INVOKEINTERFACE kotlin/jvm/functions/Function0.invoke ()Ljava/lang/Object; (itf)"
            )
        )
    }

    @Test
    fun testAnonymousParamNaming() = validateBytecode(
        """
        @Composable
        fun Foo(content: @Composable (a: Int, b: Int) -> Unit) {}
        @Composable
        fun test() {
            Foo { _, _ -> }
        }
        """
    ) {
        assert(!it.contains("%anonymous parameter 0%"))
    }

    @Test
    fun testBasicClassStaticTransform() = checkApi(
        """
            class Foo
        """,
    )

    @Test
    fun testLambdaReorderedParameter() = checkApi(
        """
            @Composable fun Foo(a: String, b: () -> Unit) { }
            @Composable fun Example() {
                Foo(b={}, a="Hello, world!")
            }
        """,
    )

    @Test
    fun testCompositionLocalCurrent() = checkApi(
        """
            val a = compositionLocalOf { 123 }
            @Composable fun Foo() {
                val b = a.current
                print(b)
            }
        """,
    )

    @Test
    fun testRemappedTypes() = checkApi(
        """
            class A {
                fun makeA(): A { return A() }
                fun makeB(): B { return B() }
                class B() {
                }
            }
            class C {
                fun useAB() {
                    val a = A()
                    a.makeA()
                    a.makeB()
                    val b = A.B()
                }
            }
        """,
    )

    @Test
    fun testDataClassHashCode() = validateBytecode(
        """
        data class Foo(
            val bar: @Composable () -> Unit
        )
        """
    ) {
        assert(!it.contains("CHECKCAST kotlin/jvm/functions/Function0"))
    }

    @Test
    fun testDefaultParameters() = checkApi(
        """
            @Composable fun Foo(x: Int = 0) {

            }
        """,
    )

    @Test
    fun testDefaultExpressionsWithComposableCall() = checkApi(
        """
            @Composable fun <T> identity(value: T): T = value
            @Composable fun Foo(x: Int = identity(20)) {

            }
            @Composable fun test() {
                Foo()
                Foo(10)
            }
        """,
    )

    @Test
    fun testBasicCallAndParameterUsage() = checkApi(
        """
            @Composable fun Foo(a: Int, b: String) {
                print(a)
                print(b)
                Bar(a, b)
            }

            @Composable fun Bar(a: Int, b: String) {
                print(a)
                print(b)
            }
        """,
    )

    @Test
    fun testCallFromInlinedLambda() = checkApi(
        """
            @Composable fun Foo() {
                listOf(1, 2, 3).forEach { Bar(it) }
            }

            @Composable fun Bar(a: Int) {}
        """,
    )

    @Test
    fun testBasicLambda() = checkApi(
        """
            val foo = @Composable { x: Int -> print(x)  }
            @Composable fun Bar() {
              foo(123)
            }
        """,
    )

    @Test
    fun testLocalLambda() = checkApi(
        """
            @Composable fun Bar(content: @Composable () -> Unit) {
                val foo = @Composable { x: Int -> print(x)  }
                foo(123)
                content()
            }
        """,
    )

    @Test
    fun testNesting() = checkApi(
        """
            @Composable fun Wrap(content: @Composable (x: Int) -> Unit) {
                content(123)
            }
            @Composable fun App(x: Int) {
                print(x)
                Wrap { a ->
                    print(a)
                    print(x)
                    Wrap { b ->
                        print(x)
                        print(a)
                        print(b)
                    }
                }
            }
        """,
    )

    @Test
    fun testComposableInterface() = checkApi(
        """
            interface Foo {
                @Composable fun bar()
            }

            class FooImpl : Foo {
                @Composable override fun bar() {}
            }
        """,
    )

    @Test
    fun testSealedClassEtc() = checkApi(
        """
            sealed class CompositionLocal2<T> {
                inline val current: T
                    @Composable
                    get() = error("")
                @Composable fun foo() {}
            }

            abstract class ProvidableCompositionLocal2<T> : CompositionLocal2<T>() {}
            class DynamicProvidableCompositionLocal2<T> : ProvidableCompositionLocal2<T>() {}
            class StaticProvidableCompositionLocal2<T> : ProvidableCompositionLocal2<T>() {}
        """,
    )

    @Test
    fun testComposableTopLevelProperty() = checkApi(
        """
            val foo: Int @Composable get() { return 123 }
        """,
    )

    @Test
    fun testComposableProperty() = checkApi(
        """
            class Foo {
                val foo: Int @Composable get() { return 123 }
            }
        """,
    )

    @Test
    fun testTableLambdaThing() = validateBytecode(
        """
        @Composable
        fun Foo() {
            val c: @Composable () -> Unit = with(123) {
                val x = @Composable {}
                x
            }
        }
        """
    ) {
        // TODO(lmr): test
    }

    @Test
    fun testDefaultArgs() = validateBytecode(
        """
        @Composable
        fun Scaffold(
            topAppBar: @Composable (() -> Unit)? = null
        ) {}
        """
    ) {
        // TODO(lmr): test
    }

    @Test
    fun testSyntheticAccessFunctions() = validateBytecode(
        """
        class Foo {
            @Composable private fun Bar() {}
        }
        """
    ) {
        // TODO(lmr): test
    }

    @Test
    fun testLambdaMemoization() = validateBytecode(
        """
        fun subcompose(block: @Composable () -> Unit) {}
        private class Foo {
            var content: @Composable (Double) -> Unit = {}
            fun subcompose() {
                val constraints = Math.random()
                subcompose {
                    content(constraints)
                }
            }
        }
        """
    ) {
        // TODO(lmr): test
    }

    @Test
    fun testCallingProperties() = checkApi(
        """
            val bar: Int @Composable get() { return 123 }

            @Composable fun Example() {
                bar
            }
        """,
    )

    @Test
    fun testAbstractComposable() = checkApi(
        """
            abstract class BaseFoo {
                @Composable abstract fun bar()
            }

            class FooImpl : BaseFoo() {
                @Composable override fun bar() {}
            }
        """,
    )

    @Test
    fun testLocalClassAndObjectLiterals() = checkApi(
        """
            @Composable
            fun Wat() {}

            @Composable
            fun Foo(x: Int) {
                Wat()
                @Composable fun goo() { Wat() }
                class Bar {
                    @Composable fun baz() { Wat() }
                }
                goo()
                Bar().baz()
            }
        """,
    )

    @Test
    fun testNonComposableCode() = checkApi(
        """
            fun A() {}
            val b: Int get() = 123
            fun C(x: Int) {
                var x = 0
                x++

                class D {
                    fun E() { A() }
                    val F: Int get() = 123
                }
                val g = object { fun H() {} }
            }
            fun I(block: () -> Unit) { block() }
            fun J() {
                I {
                    I {
                        A()
                    }
                }
            }
        """,
    )

    @Test
    fun testCircularCall() = checkApi(
        """
            @Composable fun Example() {
                Example()
            }
        """,
    )

    @Test
    fun testInlineCall() = checkApi(
        """
            @Composable inline fun Example(content: @Composable () -> Unit) {
                content()
            }

            @Composable fun Test() {
                Example {}
            }
        """,
    )

    @Test
    fun testDexNaming() = checkApi(
        """
            val myProperty: () -> Unit @Composable get() {
                return {  }
            }
        """,
    )

    @Test
    fun testInnerClass() = checkApi(
        """
            interface A {
                fun b() {}
            }
            class C {
                val foo = 1
                inner class D : A {
                    override fun b() {
                        print(foo)
                    }
                }
            }
        """,
    )

    @Test
    fun testFunInterfaces() = checkApi(
        """
            fun interface A {
                fun compute(value: Int): Unit
            }
            fun Example(a: A) {
                a.compute(123)
            }
            fun Usage() {
                Example { it -> it + 1 }
            }
        """,
    )

    @Test
    fun testComposableFunInterfaces() = checkApi(
        """
            fun interface A {
                @Composable fun compute(value: Int): Unit
            }
            fun Example(a: A) {
                Example { it -> a.compute(it) }
            }
        """,
    )

    @Test
    fun testFunInterfacesInComposableCall() = checkApi(
        """
            fun interface MeasurePolicy {
                fun compute(value: Int): Unit
            }

            @NonRestartableComposable
            @Composable fun Text() {
                Layout { value ->
                    println(value)
                }
            }

            @Composable inline fun Layout(policy: MeasurePolicy) {
                policy.compute(0)
            }
        """,
    )

    @Test
        fun testComposableFunInterfacesInVariance() = checkApi(
        """
           import androidx.compose.runtime.*

            fun interface Consumer<T> {
                @Composable fun consume(t: T)
            }

            class Repro<T : Any>() {
                fun test(consumer: Consumer<in T>) {}
            }

            fun test() {
                Repro<String>().test { string ->
                    println(string)
                }
            }
        """,
    )

    @Test
    fun testFunInterfaceWithInlineReturnType() = checkApi(
        """
            inline class Color(val value: Int)
            fun interface A {
                fun compute(value: Int): Color
            }
            fun Example(a: A) {
                Example { it -> Color(it) }
            }
        """,
    )

    @Test
    fun testComposableFunInterfaceWithInlineReturnType() = checkApi(
        """
            inline class Color(val value: Int)
            fun interface A {
                @Composable fun compute(value: Int): Color
            }
            fun Example(a: A) {
                Example { it -> Color(it) }
            }
        """,
    )

    @Test
    fun testComposableMap() = codegen(
        """
            class Repro {
                private val composables = linkedMapOf<String, @Composable () -> Unit>()

                fun doSomething() {
                    composables[""]
                }
            }
        """
    )

    @Test
    fun testComposableColorFunInterfaceExample() = checkApi(
        """
            import androidx.compose.ui.graphics.Color
            import java.lang.UnsupportedOperationException

            @Composable
            public fun Text(text: String, color: Color = Color.Unspecified) {}

            @Immutable
            @kotlin.jvm.JvmInline
            value class Color(val value: ULong) {
                companion object {
                    @Stable
                    val Red = Color(0xFFFF0000)

                    @Stable
                    val Blue = Color(0xFF0000FF)
                }
            }

            @Composable fun condition(): Boolean = true

            fun interface ButtonColors {
                @Composable fun getColor(): Color
            }
            @Composable
            fun Button(colors: ButtonColors) {
                Text("hello world", color = colors.getColor())
            }
            @Composable
            fun Test() {
                Button {
                    if (condition()) Color.Red else Color.Blue
                }
            }
        """
    )

    @Test
    fun testComposableInlineFieldDelegate_noPropertyRefInit() = validateBytecode(
        """
            import kotlin.reflect.KProperty

            class FooInline

            @Composable
            inline operator fun FooInline.getValue(thisRef: Any?, property: KProperty<*>) = 0

            @Composable fun Test(foo: FooInline): Int {
                val value by foo
                return value
            }
        """,
    ) {
        assertFalse(it.contains("INVOKESTATIC kotlin/jvm/internal/Reflection.property0 (Lkotlin/jvm/internal/PropertyReference0;)Lkotlin/reflect/KProperty0;"))
    }

    @Test
    fun testComposableAdaptedFunctionReference() = validateBytecode(
        """
            class ScrollState {
                fun test(index: Int, default: Int = 0): Int = 0
                fun testExact(index: Int): Int = 0
            }
            fun scrollState(): ScrollState = TODO()

            @Composable fun rememberFooInline() = fooInline(scrollState()::test)
            @Composable fun rememberFoo() = foo(scrollState()::test)
            @Composable fun rememberFooExactInline() = fooInline(scrollState()::testExact)
            @Composable fun rememberFooExact() = foo(scrollState()::testExact)

            @Composable
            inline fun fooInline(block: (Int) -> Int) = block(0)

            @Composable
            fun foo(block: (Int) -> Int) = block(0)
        """,
        validate = {
            // Validate that function references in inline calls are actually getting inlined
            assertFalse(
                it.contains("""INVOKESPECIAL Test_0Kt${'$'}rememberFooInline$1$1.<init> (Ljava/lang/Object;)V""")
            )
            assertFalse(
                it.contains("""INVOKESPECIAL Test_0Kt${'$'}rememberFooExactInline$1$1.<init> (Ljava/lang/Object;)V""")
            )
        }
    )
}
