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

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

/* ktlint-disable max-line-length */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(
    manifest = Config.NONE,
    minSdk = 23,
    maxSdk = 23
)
class ComposerParamSignatureTests(useFir: Boolean) : AbstractCodegenSignatureTest(useFir) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "useFir = {0}")
        fun data() = arrayOf<Any>(false, true)
    }

    @Test
    fun testParameterlessChildrenLambdasReused() = checkApi(
        """
            @Composable fun Foo(content: @Composable () -> Unit) {
            }
            @Composable fun Bar() {
                Foo {}
            }
        """,
        // We expect 3 lambda classes. One for Foo's restart group. One for Bar's restart group.
        // and one for the content lambda passed into Foo. Importantly, there is no lambda for
        // the content lambda's restart group because we are using the lambda itself.
        """
            public final class ComposableSingletons%TestKt {
              public <init>()V
              public final getLambda-1%test_module()Lkotlin/jvm/functions/Function2;
              static <clinit>()V
              public final static LComposableSingletons%TestKt; INSTANCE
              public static Lkotlin/jvm/functions/Function2; lambda-1
              final static INNERCLASS ComposableSingletons%TestKt%lambda-1%1 null null

            }
            final class ComposableSingletons%TestKt%lambda-1%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              <init>()V
              public final invoke(Landroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              static <clinit>()V
              public final static LComposableSingletons%TestKt%lambda-1%1; INSTANCE
              OUTERCLASS ComposableSingletons%TestKt null
              final static INNERCLASS ComposableSingletons%TestKt%lambda-1%1 null null
            }
            public final class TestKt {
              public final static Foo(Lkotlin/jvm/functions/Function2;Landroidx/compose/runtime/Composer;I)V
              public final static Bar(Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Bar%1 null null
              final static INNERCLASS TestKt%Foo%1 null null
            }
            final class TestKt%Foo%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              <init>(Lkotlin/jvm/functions/Function2;I)V
              public final invoke(Landroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final synthetic Lkotlin/jvm/functions/Function2; %content
              final synthetic I %%changed
              OUTERCLASS TestKt Foo (Lkotlin/jvm/functions/Function2;Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Foo%1 null null
            }
            final class TestKt%Bar%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              <init>(I)V
              public final invoke(Landroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final synthetic I %%changed
              OUTERCLASS TestKt Bar (Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Bar%1 null null
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
        """
            public final class Foo {
              public <init>()V
              static <clinit>()V
              public final static I %stable
            }
        """
    )

    @Test
    fun testLambdaReorderedParameter() = checkApi(
        """
            @Composable fun Foo(a: String, b: () -> Unit) { }
            @Composable fun Example() {
                Foo(b={}, a="Hello, world!")
            }
        """,
        """
            public final class TestKt {
              public final static Foo(Ljava/lang/String;Lkotlin/jvm/functions/Function0;Landroidx/compose/runtime/Composer;I)V
              public final static Example(Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Example%1 null null
              final static INNERCLASS TestKt%Example%2 null null
              final static INNERCLASS TestKt%Foo%1 null null
            }
            final class TestKt%Foo%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              <init>(Ljava/lang/String;Lkotlin/jvm/functions/Function0;I)V
              public final invoke(Landroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final synthetic Ljava/lang/String; %a
              final synthetic Lkotlin/jvm/functions/Function0; %b
              final synthetic I %%changed
              OUTERCLASS TestKt Foo (Ljava/lang/String;Lkotlin/jvm/functions/Function0;Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Foo%1 null null
            }
            final class TestKt%Example%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              <init>()V
              public final invoke()V
              public synthetic bridge invoke()Ljava/lang/Object;
              static <clinit>()V
              public final static LTestKt%Example%1; INSTANCE
              OUTERCLASS TestKt Example (Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Example%1 null null
            }
            final class TestKt%Example%2 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              <init>(I)V
              public final invoke(Landroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final synthetic I %%changed
              OUTERCLASS TestKt Example (Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Example%2 null null
            }
            """
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
        """
            public final class TestKt {
              public final static getA()Landroidx/compose/runtime/ProvidableCompositionLocal;
              public final static Foo(Landroidx/compose/runtime/Composer;I)V
              static <clinit>()V
              private final static Landroidx/compose/runtime/ProvidableCompositionLocal; a
              final static INNERCLASS TestKt%a%1 null null
              final static INNERCLASS TestKt%Foo%1 null null
            }
            final class TestKt%Foo%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              <init>(I)V
              public final invoke(Landroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final synthetic I %%changed
              OUTERCLASS TestKt Foo (Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Foo%1 null null
            }
            final class TestKt%a%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              <init>()V
              public final invoke()Ljava/lang/Integer;
              public synthetic bridge invoke()Ljava/lang/Object;
              static <clinit>()V
              public final static LTestKt%a%1; INSTANCE
              OUTERCLASS TestKt null
              final static INNERCLASS TestKt%a%1 null null
            }
        """
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
        """
            public final class A {
              public <init>()V
              public final makeA()LA;
              public final makeB()LA%B;
              static <clinit>()V
              public final static I %stable
              public final static INNERCLASS A%B A B
            }
            public final class A%B {
              public <init>()V
              static <clinit>()V
              public final static I %stable
              public final static INNERCLASS A%B A B
            }
            public final class C {
              public <init>()V
              public final useAB()V
              static <clinit>()V
              public final static I %stable
              public final static INNERCLASS A%B A B
            }
        """
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
    @Ignore("b/179279455")
    fun testCorrectComposerPassed1() = checkComposerParam(
        """
            var a: Composer? = null
            fun run() {
                a = makeComposer()
                invokeComposable(a) {
                    assertComposer(a)
                }
            }
        """
    )

    @Test
    @Ignore("b/179279455")
    fun testCorrectComposerPassed2() = checkComposerParam(
        """
            var a: Composer? = null
            @Composable fun Foo() {
                assertComposer(a)
            }
            fun run() {
                a = makeComposer()
                invokeComposable(a) {
                    Foo()
                }
            }
        """
    )

    @Test
    @Ignore("b/179279455")
    fun testCorrectComposerPassed3() = checkComposerParam(
        """
            var a: Composer? = null
            var b: Composer? = null
            @Composable fun Callback(fn: () -> Unit) {
                fn()
            }
            fun run() {
                a = makeComposer()
                b = makeComposer()
                invokeComposable(a) {
                    assertComposer(a)
                    Callback {
                        invokeComposable(b) {
                            assertComposer(b)
                        }
                    }
                }
            }
        """
    )

    @Test
    @Ignore("b/179279455")
    fun testCorrectComposerPassed4() = checkComposerParam(
        """
            var a: Composer? = null
            var b: Composer? = null
            @Composable fun makeInt(): Int {
                assertComposer(a)
                return 10
            }
            @Composable fun WithDefault(x: Int = makeInt()) {}
            fun run() {
                a = makeComposer()
                b = makeComposer()
                invokeComposable(a) {
                    assertComposer(a)
                    WithDefault()
                    WithDefault(10)
                }
                invokeComposable(b) {
                    assertComposer(b)
                    WithDefault(10)
                }
            }
        """
    )

    @Test
    @Ignore("b/179279455")
    fun testCorrectComposerPassed5() = checkComposerParam(
        """
            var a: Composer? = null
            @Composable fun Wrap(content: @Composable () -> Unit) {
                content()
            }
            fun run() {
                a = makeComposer()
                invokeComposable(a) {
                    assertComposer(a)
                    Wrap {
                        assertComposer(a)
                        Wrap {
                            assertComposer(a)
                            Wrap {
                                assertComposer(a)
                            }
                        }
                    }
                }
            }
        """
    )

    @Test
    fun testDefaultParameters() = checkApi(
        """
            @Composable fun Foo(x: Int = 0) {

            }
        """,
        """
            public final class TestKt {
              public final static Foo(ILandroidx/compose/runtime/Composer;II)V
              final static INNERCLASS TestKt%Foo%1 null null
            }
            final class TestKt%Foo%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              <init>(III)V
              public final invoke(Landroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final synthetic I %x
              final synthetic I %%changed
              final synthetic I %%default
              OUTERCLASS TestKt Foo (ILandroidx/compose/runtime/Composer;II)V
              final static INNERCLASS TestKt%Foo%1 null null
            }
        """
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
        """
            public final class TestKt {
              public final static identity(Ljava/lang/Object;Landroidx/compose/runtime/Composer;I)Ljava/lang/Object;
              public final static Foo(ILandroidx/compose/runtime/Composer;II)V
              public final static test(Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Foo%1 null null
              final static INNERCLASS TestKt%test%1 null null
            }
            final class TestKt%Foo%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              <init>(III)V
              public final invoke(Landroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final synthetic I %x
              final synthetic I %%changed
              final synthetic I %%default
              OUTERCLASS TestKt Foo (ILandroidx/compose/runtime/Composer;II)V
              final static INNERCLASS TestKt%Foo%1 null null
            }
            final class TestKt%test%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              <init>(I)V
              public final invoke(Landroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final synthetic I %%changed
              OUTERCLASS TestKt test (Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%test%1 null null
            }
        """
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
        """
            public final class TestKt {
              public final static Foo(ILjava/lang/String;Landroidx/compose/runtime/Composer;I)V
              public final static Bar(ILjava/lang/String;Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Bar%1 null null
              final static INNERCLASS TestKt%Foo%1 null null
            }
            final class TestKt%Foo%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              <init>(ILjava/lang/String;I)V
              public final invoke(Landroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final synthetic I %a
              final synthetic Ljava/lang/String; %b
              final synthetic I %%changed
              OUTERCLASS TestKt Foo (ILjava/lang/String;Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Foo%1 null null
            }
            final class TestKt%Bar%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              <init>(ILjava/lang/String;I)V
              public final invoke(Landroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final synthetic I %a
              final synthetic Ljava/lang/String; %b
              final synthetic I %%changed
              OUTERCLASS TestKt Bar (ILjava/lang/String;Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Bar%1 null null
            }
        """
    )

    @Test
    fun testCallFromInlinedLambda() = checkApi(
        """
            @Composable fun Foo() {
                listOf(1, 2, 3).forEach { Bar(it) }
            }

            @Composable fun Bar(a: Int) {}
        """,
        """
            public final class TestKt {
              public final static Foo(Landroidx/compose/runtime/Composer;I)V
              public final static Bar(ILandroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Bar%1 null null
              final static INNERCLASS TestKt%Foo%2 null null
            }
            final class TestKt%Foo%2 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              <init>(I)V
              public final invoke(Landroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final synthetic I %%changed
              OUTERCLASS TestKt Foo (Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Foo%2 null null
            }
            final class TestKt%Bar%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              <init>(II)V
              public final invoke(Landroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final synthetic I %a
              final synthetic I %%changed
              OUTERCLASS TestKt Bar (ILandroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Bar%1 null null
            }
        """
    )

    @Test
    fun testBasicLambda() = checkApi(
        """
            val foo = @Composable { x: Int -> print(x)  }
            @Composable fun Bar() {
              foo(123)
            }
        """,
        """
            public final class ComposableSingletons%TestKt {
              public <init>()V
              public final getLambda-1%test_module()Lkotlin/jvm/functions/Function3;
              static <clinit>()V
              public final static LComposableSingletons%TestKt; INSTANCE
              public static Lkotlin/jvm/functions/Function3; lambda-1
              final static INNERCLASS ComposableSingletons%TestKt%lambda-1%1 null null
            }
            final class ComposableSingletons%TestKt%lambda-1%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>()V
              public final invoke(ILandroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              static <clinit>()V
              public final static LComposableSingletons%TestKt%lambda-1%1; INSTANCE
              OUTERCLASS ComposableSingletons%TestKt null
              final static INNERCLASS ComposableSingletons%TestKt%lambda-1%1 null null
            }
            public final class TestKt {
              public final static getFoo()Lkotlin/jvm/functions/Function3;
              public final static Bar(Landroidx/compose/runtime/Composer;I)V
              static <clinit>()V
              private final static Lkotlin/jvm/functions/Function3; foo
              final static INNERCLASS TestKt%Bar%1 null null
            }
            final class TestKt%Bar%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              <init>(I)V
              public final invoke(Landroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final synthetic I %%changed
              OUTERCLASS TestKt Bar (Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Bar%1 null null
            }
        """
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
        """
            public final class ComposableSingletons%TestKt {
              public <init>()V
              public final getLambda-1%test_module()Lkotlin/jvm/functions/Function3;
              static <clinit>()V
              public final static LComposableSingletons%TestKt; INSTANCE
              public static Lkotlin/jvm/functions/Function3; lambda-1
              final static INNERCLASS ComposableSingletons%TestKt%lambda-1%1 null null
            }
            final class ComposableSingletons%TestKt%lambda-1%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>()V
              public final invoke(ILandroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              static <clinit>()V
              public final static LComposableSingletons%TestKt%lambda-1%1; INSTANCE
              OUTERCLASS ComposableSingletons%TestKt null
              final static INNERCLASS ComposableSingletons%TestKt%lambda-1%1 null null
            }
            public final class TestKt {
              public final static Bar(Lkotlin/jvm/functions/Function2;Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Bar%1 null null
            }
            final class TestKt%Bar%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              <init>(Lkotlin/jvm/functions/Function2;I)V
              public final invoke(Landroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final synthetic Lkotlin/jvm/functions/Function2; %content
              final synthetic I %%changed
              OUTERCLASS TestKt Bar (Lkotlin/jvm/functions/Function2;Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Bar%1 null null
            }
        """
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
        """
            public final class TestKt {
              public final static Wrap(Lkotlin/jvm/functions/Function3;Landroidx/compose/runtime/Composer;I)V
              public final static App(ILandroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%App%1 null null
              final static INNERCLASS TestKt%App%2 null null
              final static INNERCLASS TestKt%Wrap%1 null null
            }
            final class TestKt%Wrap%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              <init>(Lkotlin/jvm/functions/Function3;I)V
              public final invoke(Landroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final synthetic Lkotlin/jvm/functions/Function3; %content
              final synthetic I %%changed
              OUTERCLASS TestKt Wrap (Lkotlin/jvm/functions/Function3;Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Wrap%1 null null
            }
            final class TestKt%App%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>(I)V
              public final invoke(ILandroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final synthetic I %x
              OUTERCLASS TestKt App (ILandroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%App%1 null null
              final static INNERCLASS TestKt%App%1%1 null null
            }
            final class TestKt%App%1%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function3 {
              <init>(II)V
              public final invoke(ILandroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final synthetic I %x
              final synthetic I %a
              OUTERCLASS TestKt%App%1 invoke (ILandroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%App%1 null null
              final static INNERCLASS TestKt%App%1%1 null null
            }
            final class TestKt%App%2 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              <init>(II)V
              public final invoke(Landroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final synthetic I %x
              final synthetic I %%changed
              OUTERCLASS TestKt App (ILandroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%App%2 null null
            }
        """
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
        """
            public abstract interface Foo {
              public abstract bar(Landroidx/compose/runtime/Composer;I)V
            }
            public final class FooImpl implements Foo {
              public <init>()V
              public bar(Landroidx/compose/runtime/Composer;I)V
              static <clinit>()V
              public final static I %stable
              final static INNERCLASS FooImpl%bar%1 null null
            }
            final class FooImpl%bar%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              <init>(LFooImpl;I)V
              public final invoke(Landroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final synthetic LFooImpl; %tmp0_rcvr
              final synthetic I %%changed
              OUTERCLASS FooImpl bar (Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS FooImpl%bar%1 null null
            }
        """
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
        """
            public abstract class CompositionLocal2 {
              private <init>()V
              public final getCurrent(Landroidx/compose/runtime/Composer;I)Ljava/lang/Object;
              public final foo(Landroidx/compose/runtime/Composer;I)V
              public synthetic <init>(Lkotlin/jvm/internal/DefaultConstructorMarker;)V
              static <clinit>()V
              public final static I %stable
              final static INNERCLASS CompositionLocal2%foo%1 null null
            }
            final class CompositionLocal2%foo%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              <init>(LCompositionLocal2;I)V
              public final invoke(Landroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final synthetic LCompositionLocal2; %tmp0_rcvr
              final synthetic I %%changed
              OUTERCLASS CompositionLocal2 foo (Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS CompositionLocal2%foo%1 null null
            }
            public abstract class ProvidableCompositionLocal2 extends CompositionLocal2 {
              public <init>()V
              static <clinit>()V
              public final static I %stable
            }
            public final class DynamicProvidableCompositionLocal2 extends ProvidableCompositionLocal2 {
              public <init>()V
              static <clinit>()V
              public final static I %stable
            }
            public final class StaticProvidableCompositionLocal2 extends ProvidableCompositionLocal2 {
              public <init>()V
              static <clinit>()V
              public final static I %stable
            }
        """
    )

    @Test
    fun testComposableTopLevelProperty() = checkApi(
        """
            val foo: Int @Composable get() { return 123 }
        """,
        """
            public final class TestKt {
              public final static getFoo(Landroidx/compose/runtime/Composer;I)I
            }
        """
    )

    @Test
    fun testComposableProperty() = checkApi(
        """
            class Foo {
                val foo: Int @Composable get() { return 123 }
            }
        """,
        """
            public final class Foo {
              public <init>()V
              public final getFoo(Landroidx/compose/runtime/Composer;I)I
              static <clinit>()V
              public final static I %stable
            }
        """
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
        """
            public final class TestKt {
              public final static getBar(Landroidx/compose/runtime/Composer;I)I
              public final static Example(Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Example%1 null null
            }
            final class TestKt%Example%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              <init>(I)V
              public final invoke(Landroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final synthetic I %%changed
              OUTERCLASS TestKt Example (Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Example%1 null null
            }
        """
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
        """
            public abstract class BaseFoo {
              public <init>()V
              public abstract bar(Landroidx/compose/runtime/Composer;I)V
              static <clinit>()V
              public final static I %stable
            }
            public final class FooImpl extends BaseFoo {
              public <init>()V
              public bar(Landroidx/compose/runtime/Composer;I)V
              static <clinit>()V
              public final static I %stable
              final static INNERCLASS FooImpl%bar%1 null null
            }
            final class FooImpl%bar%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              <init>(LFooImpl;I)V
              public final invoke(Landroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final synthetic LFooImpl; %tmp0_rcvr
              final synthetic I %%changed
              OUTERCLASS FooImpl bar (Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS FooImpl%bar%1 null null
            }
        """
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
        """
            public final class TestKt {
              public final static Wat(Landroidx/compose/runtime/Composer;I)V
              public final static Foo(ILandroidx/compose/runtime/Composer;I)V
              private final static Foo%goo(Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Foo%1 null null
              public final static INNERCLASS TestKt%Foo%Bar null Bar
              final static INNERCLASS TestKt%Wat%1 null null
            }
            final class TestKt%Wat%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              <init>(I)V
              public final invoke(Landroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final synthetic I %%changed
              OUTERCLASS TestKt Wat (Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Wat%1 null null
            }
            public final class TestKt%Foo%Bar {
              public <init>()V
              public final baz(Landroidx/compose/runtime/Composer;I)V
              OUTERCLASS TestKt Foo (ILandroidx/compose/runtime/Composer;I)V
              public final static INNERCLASS TestKt%Foo%Bar null Bar
            }
            final class TestKt%Foo%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              <init>(II)V
              public final invoke(Landroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final synthetic I %x
              final synthetic I %%changed
              OUTERCLASS TestKt Foo (ILandroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Foo%1 null null
            }
        """
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
        """
            public final class TestKt {
              public final static A()V
              public final static getB()I
              public final static C(I)V
              public final static I(Lkotlin/jvm/functions/Function0;)V
              public final static J()V
              public final static INNERCLASS TestKt%C%g%1 null null
              public final static INNERCLASS TestKt%C%D null D
              final static INNERCLASS TestKt%J%1 null null
            }
            public final class TestKt%C%D {
              public <init>()V
              public final E()V
              public final getF()I
              OUTERCLASS TestKt C (I)V
              public final static INNERCLASS TestKt%C%D null D
            }
            public final class TestKt%C%g%1 {
              <init>()V
              public final H()V
              OUTERCLASS TestKt C (I)V
              public final static INNERCLASS TestKt%C%g%1 null null
            }
            final class TestKt%J%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              <init>()V
              public final invoke()V
              public synthetic bridge invoke()Ljava/lang/Object;
              static <clinit>()V
              public final static LTestKt%J%1; INSTANCE
              OUTERCLASS TestKt J ()V
              final static INNERCLASS TestKt%J%1 null null
              final static INNERCLASS TestKt%J%1%1 null null
            }
            final class TestKt%J%1%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              <init>()V
              public final invoke()V
              public synthetic bridge invoke()Ljava/lang/Object;
              static <clinit>()V
              public final static LTestKt%J%1%1; INSTANCE
              OUTERCLASS TestKt%J%1 invoke ()V
              final static INNERCLASS TestKt%J%1 null null
              final static INNERCLASS TestKt%J%1%1 null null
            }
        """
    )

    @Test
    fun testCircularCall() = checkApi(
        """
            @Composable fun Example() {
                Example()
            }
        """,
        """
            public final class TestKt {
              public final static Example(Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Example%1 null null
            }
            final class TestKt%Example%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              <init>(I)V
              public final invoke(Landroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final synthetic I %%changed
              OUTERCLASS TestKt Example (Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Example%1 null null
            }
        """
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
        """
            public final class TestKt {
              public final static Example(Lkotlin/jvm/functions/Function2;Landroidx/compose/runtime/Composer;I)V
              public final static Test(Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Test%2 null null
            }
            final class TestKt%Test%2 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              <init>(I)V
              public final invoke(Landroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final synthetic I %%changed
              OUTERCLASS TestKt Test (Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Test%2 null null
            }
        """
    )

    @Test
    fun testDexNaming() = checkApi(
        """
            val myProperty: () -> Unit @Composable get() {
                return {  }
            }
        """,
        """
            public final class TestKt {
              public final static getMyProperty(Landroidx/compose/runtime/Composer;I)Lkotlin/jvm/functions/Function0;
              final static INNERCLASS TestKt%myProperty%1 null null
            }
            final class TestKt%myProperty%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function0 {
              <init>()V
              public final invoke()V
              public synthetic bridge invoke()Ljava/lang/Object;
              static <clinit>()V
              public final static LTestKt%myProperty%1; INSTANCE
              OUTERCLASS TestKt getMyProperty (Landroidx/compose/runtime/Composer;I)Lkotlin/jvm/functions/Function0;
              final static INNERCLASS TestKt%myProperty%1 null null
            }
        """
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
        """
            public abstract interface A {
              public abstract b()V
              public final static INNERCLASS A%DefaultImpls A DefaultImpls
            }
            public final class A%DefaultImpls {
              public static b(LA;)V
              public final static INNERCLASS A%DefaultImpls A DefaultImpls
            }
            public final class C {
              public <init>()V
              public final getFoo()I
              static <clinit>()V
              private final I foo
              public final static I %stable
              public final INNERCLASS C%D C D
            }
            public final class C%D implements A {
              public <init>(LC;)V
              public b()V
              final synthetic LC; this%0
              public final INNERCLASS C%D C D
            }
        """
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
        """
            public abstract interface A {
              public abstract compute(I)V
            }
            public final class TestKt {
              public final static Example(LA;)V
              public final static Usage()V
              private final static Usage%lambda%0(I)V
            }
        """
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
        """
            public abstract interface A {
              public abstract compute(ILandroidx/compose/runtime/Composer;I)V
            }
            public final class TestKt {
              public final static Example(LA;)V
              final static INNERCLASS TestKt%Example%1 null null
            }
            final class TestKt%Example%1 implements A {
              <init>(LA;)V
              public final compute(ILandroidx/compose/runtime/Composer;I)V
              final synthetic LA; %a
              OUTERCLASS TestKt Example (LA;)V
              final static INNERCLASS TestKt%Example%1 null null
              final static INNERCLASS TestKt%Example%1%compute%1 null null
            }
            final class TestKt%Example%1%compute%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              <init>(LTestKt%Example%1;II)V
              public final invoke(Landroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final synthetic LTestKt%Example%1; %tmp0_rcvr
              final synthetic I %it
              final synthetic I %%changed
              OUTERCLASS TestKt%Example%1 compute (ILandroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Example%1 null null
              final static INNERCLASS TestKt%Example%1%compute%1 null null
            }
        """
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
        """
            public abstract interface MeasurePolicy {
              public abstract compute(I)V
            }
            public final class TestKt {
              public final static Text(Landroidx/compose/runtime/Composer;I)V
              public final static Layout(LMeasurePolicy;Landroidx/compose/runtime/Composer;I)V
              private final static Text%lambda%0(I)V
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
        """
            public abstract interface Consumer {
              public abstract consume(Ljava/lang/Object;Landroidx/compose/runtime/Composer;I)V
            }
            public final class Repro {
              public <init>()V
              public final test(LConsumer;)V
              static <clinit>()V
              public final static I %stable
            }
            public final class TestKt {
              public final static test()V
              final static INNERCLASS TestKt%test%1 null null
            }
            final class TestKt%test%1 implements Consumer {
              <init>()V
              public final consume(Ljava/lang/String;Landroidx/compose/runtime/Composer;I)V
              public synthetic bridge consume(Ljava/lang/Object;Landroidx/compose/runtime/Composer;I)V
              static <clinit>()V
              public final static LTestKt%test%1; INSTANCE
              OUTERCLASS TestKt test ()V
              final static INNERCLASS TestKt%test%1 null null
              final static INNERCLASS TestKt%test%1%consume%1 null null
            }
            final class TestKt%test%1%consume%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              <init>(LTestKt%test%1;Ljava/lang/String;I)V
              public final invoke(Landroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final synthetic LTestKt%test%1; %tmp0_rcvr
              final synthetic Ljava/lang/String; %string
              final synthetic I %%changed
              OUTERCLASS TestKt%test%1 consume (Ljava/lang/String;Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%test%1 null null
              final static INNERCLASS TestKt%test%1%consume%1 null null
            }
        """
    )

    val hashCodeEqualsAndToString = if (useFir) {
        """
              public static toString-impl(I)Ljava/lang/String;
              public toString()Ljava/lang/String;
              public static hashCode-impl(I)I
              public hashCode()I
              public static equals-impl(ILjava/lang/Object;)Z
              public equals(Ljava/lang/Object;)Z
        """
    } else {
        """
              public static toString-impl(I)Ljava/lang/String;
              public toString()Ljava/lang/String;
              public static hashCode-impl(I)I
              public hashCode()I
              public static equals-impl(ILjava/lang/Object;)Z
              public equals(Ljava/lang/Object;)Z
        """
    }

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
        """
            public final class Color {
              public final getValue()I
              $hashCodeEqualsAndToString
              private synthetic <init>(I)V
              public static constructor-impl(I)I
              public final static synthetic box-impl(I)LColor;
              public final synthetic unbox-impl()I
              public final static equals-impl0(II)Z
              private final I value
            }
            public abstract interface A {
              public abstract compute-dZQu5ag(I)I
            }
            public final class TestKt {
              public final static Example(LA;)V
              final static INNERCLASS TestKt%Example%1 null null
            }
            final class TestKt%Example%1 implements A {
              <init>()V
              public final compute-dZQu5ag(I)I
              static <clinit>()V
              public final static LTestKt%Example%1; INSTANCE
              OUTERCLASS TestKt Example (LA;)V
              final static INNERCLASS TestKt%Example%1 null null
            }
        """
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
        """
            public final class Color {
              public final getValue()I
              $hashCodeEqualsAndToString
              private synthetic <init>(I)V
              public static constructor-impl(I)I
              public final static synthetic box-impl(I)LColor;
              public final synthetic unbox-impl()I
              public final static equals-impl0(II)Z
              private final I value
            }
            public abstract interface A {
              public abstract compute-WWBqCfo(ILandroidx/compose/runtime/Composer;I)I
            }
            public final class TestKt {
              public final static Example(LA;)V
              final static INNERCLASS TestKt%Example%1 null null
            }
            final class TestKt%Example%1 implements A {
              <init>()V
              public final compute-WWBqCfo(ILandroidx/compose/runtime/Composer;I)I
              static <clinit>()V
              public final static LTestKt%Example%1; INSTANCE
              OUTERCLASS TestKt Example (LA;)V
              final static INNERCLASS TestKt%Example%1 null null
            }
        """
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
        """,
        """
            public final class Color {
              public final getValue-s-VKNKU()J
              public static toString-impl(J)Ljava/lang/String;
              public toString()Ljava/lang/String;
              public static hashCode-impl(J)I
              public hashCode()I
              public static equals-impl(JLjava/lang/Object;)Z
              public equals(Ljava/lang/Object;)Z
              private synthetic <init>(J)V
              public static constructor-impl(J)J
              public final static synthetic box-impl(J)LColor;
              public final synthetic unbox-impl()J
              public final static equals-impl0(JJ)Z
              public final static synthetic access%getRed%cp()J
              public final static synthetic access%getBlue%cp()J
              static <clinit>()V
              public final static LColor%Companion; Companion
              private final J value
              private final static J Red
              private final static J Blue
              public final static INNERCLASS Color%Companion Color Companion
            }
            public final class Color%Companion {
              private <init>()V
              public final getRed-0d7_KjU()J
              public static synthetic getRed-0d7_KjU%annotations()V
              public final getBlue-0d7_KjU()J
              public static synthetic getBlue-0d7_KjU%annotations()V
              public synthetic <init>(Lkotlin/jvm/internal/DefaultConstructorMarker;)V
              public final static INNERCLASS Color%Companion Color Companion
            }
            public abstract interface ButtonColors {
              public abstract getColor-WaAFU9c(Landroidx/compose/runtime/Composer;I)J
            }
            public final class TestKt {
              public final static Text-iJQMabo(Ljava/lang/String;JLandroidx/compose/runtime/Composer;II)V
              public final static condition(Landroidx/compose/runtime/Composer;I)Z
              public final static Button(LButtonColors;Landroidx/compose/runtime/Composer;I)V
              public final static Test(Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Button%1 null null
              final static INNERCLASS TestKt%Test%1 null null
              final static INNERCLASS TestKt%Test%2 null null
              final static INNERCLASS TestKt%Text%1 null null
              public final static INNERCLASS androidx/compose/ui/graphics/Color%Companion androidx/compose/ui/graphics/Color Companion
            }
            final class TestKt%Text%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              <init>(Ljava/lang/String;JII)V
              public final invoke(Landroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final synthetic Ljava/lang/String; %text
              final synthetic J %color
              final synthetic I %%changed
              final synthetic I %%default
              OUTERCLASS TestKt Text-iJQMabo (Ljava/lang/String;JLandroidx/compose/runtime/Composer;II)V
              final static INNERCLASS TestKt%Text%1 null null
            }
            final class TestKt%Button%1 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              <init>(LButtonColors;I)V
              public final invoke(Landroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final synthetic LButtonColors; %colors
              final synthetic I %%changed
              OUTERCLASS TestKt Button (LButtonColors;Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Button%1 null null
            }
            final class TestKt%Test%1 implements ButtonColors {
              <init>()V
              public final getColor-WaAFU9c(Landroidx/compose/runtime/Composer;I)J
              static <clinit>()V
              public final static LTestKt%Test%1; INSTANCE
              OUTERCLASS TestKt Test (Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Test%1 null null
              public final static INNERCLASS androidx/compose/ui/graphics/Color%Companion androidx/compose/ui/graphics/Color Companion
            }
            final class TestKt%Test%2 extends kotlin/jvm/internal/Lambda implements kotlin/jvm/functions/Function2 {
              <init>(I)V
              public final invoke(Landroidx/compose/runtime/Composer;I)V
              public synthetic bridge invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              final synthetic I %%changed
              OUTERCLASS TestKt Test (Landroidx/compose/runtime/Composer;I)V
              final static INNERCLASS TestKt%Test%2 null null
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
