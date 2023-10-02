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
import org.junit.Test

class LiveLiteralTransformTests(useFir: Boolean) : AbstractLiveLiteralTransformTests(useFir) {
    override fun CompilerConfiguration.updateConfiguration() {
        put(ComposeConfiguration.LIVE_LITERALS_ENABLED_KEY, true)
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
        // K2 constant folds the toString call.
        val printOneToStringKey = if (useFir) {
            "String%arg-0%call-print%fun-Test"
        } else {
            "Int%%this%call-toString%arg-0%call-print%fun-Test"
        }
        assertKeys(
            printOneToStringKey,
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
        """,
        """
            interface Foo {
              abstract fun bar(): Int
            }
            fun a(): Foo {
              return object : Foo {
                override fun bar(): Int {
                  return LiveLiterals%TestKt.Int%fun-bar%class-%no-name-provided%%fun-a()
                }
              }
            }
            @LiveLiteralFileInfo(file = "/Test.kt")
            internal object LiveLiterals%TestKt {
              val Int%fun-bar%class-%no-name-provided%%fun-a: Int = 1
              var State%Int%fun-bar%class-%no-name-provided%%fun-a: State<Int>?
              @LiveLiteralInfo(key = "Int%fun-bar%class-%no-name-provided%%fun-a", offset = 159)
              fun Int%fun-bar%class-%no-name-provided%%fun-a(): Int {
                if (!isLiveLiteralsEnabled) {
                  return Int%fun-bar%class-%no-name-provided%%fun-a
                }
                val tmp0 = State%Int%fun-bar%class-%no-name-provided%%fun-a
                return if (tmp0 == null) {
                  val tmp1 = liveLiteral("Int%fun-bar%class-%no-name-provided%%fun-a", Int%fun-bar%class-%no-name-provided%%fun-a)
                  State%Int%fun-bar%class-%no-name-provided%%fun-a = tmp1
                  tmp1
                } else {
                  tmp0
                }
                .value
              }
            }
        """
    )

    @Test
    fun testBasicTransform() {
        // String constant start offsets are off by one in K2.
        // TODO: Inline the non-K2 offset once fixed.
        val stringConstantOffset = if (useFir) 85 else 86
        assertTransform(
            """
            """,
            """
                fun A() {
                  print(1)
                  print("Hello World")
                  if (true) {
                    print(4)
                  }
                  if (true) {
                    print(1.0f)
                  }
                  print(3)
                }
            """,
            """
                fun A() {
                  print(LiveLiterals%TestKt.Int%arg-0%call-print%fun-A())
                  print(LiveLiterals%TestKt.String%arg-0%call-print-1%fun-A())
                  if (LiveLiterals%TestKt.Boolean%cond%if%fun-A()) {
                    print(LiveLiterals%TestKt.Int%arg-0%call-print%branch%if%fun-A())
                  }
                  if (LiveLiterals%TestKt.Boolean%cond%if-1%fun-A()) {
                    print(LiveLiterals%TestKt.Float%arg-0%call-print%branch%if-1%fun-A())
                  }
                  print(LiveLiterals%TestKt.Int%arg-0%call-print-2%fun-A())
                }
                @LiveLiteralFileInfo(file = "/Test.kt")
                internal object LiveLiterals%TestKt {
                  val Int%arg-0%call-print%fun-A: Int = 1
                  var State%Int%arg-0%call-print%fun-A: State<Int>?
                  @LiveLiteralInfo(key = "Int%arg-0%call-print%fun-A", offset = 70)
                  fun Int%arg-0%call-print%fun-A(): Int {
                    if (!isLiveLiteralsEnabled) {
                      return Int%arg-0%call-print%fun-A
                    }
                    val tmp0 = State%Int%arg-0%call-print%fun-A
                    return if (tmp0 == null) {
                      val tmp1 = liveLiteral("Int%arg-0%call-print%fun-A", Int%arg-0%call-print%fun-A)
                      State%Int%arg-0%call-print%fun-A = tmp1
                      tmp1
                    } else {
                      tmp0
                    }
                    .value
                  }
                  val String%arg-0%call-print-1%fun-A: String = "Hello World"
                  var State%String%arg-0%call-print-1%fun-A: State<String>?
                  @LiveLiteralInfo(key = "String%arg-0%call-print-1%fun-A", offset = $stringConstantOffset)
                  fun String%arg-0%call-print-1%fun-A(): String {
                    if (!isLiveLiteralsEnabled) {
                      return String%arg-0%call-print-1%fun-A
                    }
                    val tmp0 = State%String%arg-0%call-print-1%fun-A
                    return if (tmp0 == null) {
                      val tmp1 = liveLiteral("String%arg-0%call-print-1%fun-A", String%arg-0%call-print-1%fun-A)
                      State%String%arg-0%call-print-1%fun-A = tmp1
                      tmp1
                    } else {
                      tmp0
                    }
                    .value
                  }
                  val Boolean%cond%if%fun-A: Boolean = true
                  var State%Boolean%cond%if%fun-A: State<Boolean>?
                  @LiveLiteralInfo(key = "Boolean%cond%if%fun-A", offset = 110)
                  fun Boolean%cond%if%fun-A(): Boolean {
                    if (!isLiveLiteralsEnabled) {
                      return Boolean%cond%if%fun-A
                    }
                    val tmp0 = State%Boolean%cond%if%fun-A
                    return if (tmp0 == null) {
                      val tmp1 = liveLiteral("Boolean%cond%if%fun-A", Boolean%cond%if%fun-A)
                      State%Boolean%cond%if%fun-A = tmp1
                      tmp1
                    } else {
                      tmp0
                    }
                    .value
                  }
                  val Int%arg-0%call-print%branch%if%fun-A: Int = 4
                  var State%Int%arg-0%call-print%branch%if%fun-A: State<Int>?
                  @LiveLiteralInfo(key = "Int%arg-0%call-print%branch%if%fun-A", offset = 132)
                  fun Int%arg-0%call-print%branch%if%fun-A(): Int {
                    if (!isLiveLiteralsEnabled) {
                      return Int%arg-0%call-print%branch%if%fun-A
                    }
                    val tmp0 = State%Int%arg-0%call-print%branch%if%fun-A
                    return if (tmp0 == null) {
                      val tmp1 = liveLiteral("Int%arg-0%call-print%branch%if%fun-A", Int%arg-0%call-print%branch%if%fun-A)
                      State%Int%arg-0%call-print%branch%if%fun-A = tmp1
                      tmp1
                    } else {
                      tmp0
                    }
                    .value
                  }
                  val Boolean%cond%if-1%fun-A: Boolean = true
                  var State%Boolean%cond%if-1%fun-A: State<Boolean>?
                  @LiveLiteralInfo(key = "Boolean%cond%if-1%fun-A", offset = 153)
                  fun Boolean%cond%if-1%fun-A(): Boolean {
                    if (!isLiveLiteralsEnabled) {
                      return Boolean%cond%if-1%fun-A
                    }
                    val tmp0 = State%Boolean%cond%if-1%fun-A
                    return if (tmp0 == null) {
                      val tmp1 = liveLiteral("Boolean%cond%if-1%fun-A", Boolean%cond%if-1%fun-A)
                      State%Boolean%cond%if-1%fun-A = tmp1
                      tmp1
                    } else {
                      tmp0
                    }
                    .value
                  }
                  val Float%arg-0%call-print%branch%if-1%fun-A: Float = 1.0f
                  var State%Float%arg-0%call-print%branch%if-1%fun-A: State<Float>?
                  @LiveLiteralInfo(key = "Float%arg-0%call-print%branch%if-1%fun-A", offset = 175)
                  fun Float%arg-0%call-print%branch%if-1%fun-A(): Float {
                    if (!isLiveLiteralsEnabled) {
                      return Float%arg-0%call-print%branch%if-1%fun-A
                    }
                    val tmp0 = State%Float%arg-0%call-print%branch%if-1%fun-A
                    return if (tmp0 == null) {
                      val tmp1 = liveLiteral("Float%arg-0%call-print%branch%if-1%fun-A", Float%arg-0%call-print%branch%if-1%fun-A)
                      State%Float%arg-0%call-print%branch%if-1%fun-A = tmp1
                      tmp1
                    } else {
                      tmp0
                    }
                    .value
                  }
                  val Int%arg-0%call-print-2%fun-A: Int = 3
                  var State%Int%arg-0%call-print-2%fun-A: State<Int>?
                  @LiveLiteralInfo(key = "Int%arg-0%call-print-2%fun-A", offset = 201)
                  fun Int%arg-0%call-print-2%fun-A(): Int {
                    if (!isLiveLiteralsEnabled) {
                      return Int%arg-0%call-print-2%fun-A
                    }
                    val tmp0 = State%Int%arg-0%call-print-2%fun-A
                    return if (tmp0 == null) {
                      val tmp1 = liveLiteral("Int%arg-0%call-print-2%fun-A", Int%arg-0%call-print-2%fun-A)
                      State%Int%arg-0%call-print-2%fun-A = tmp1
                      tmp1
                    } else {
                      tmp0
                    }
                    .value
                  }
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
            """,
            """
                fun A() {
                  print(LiveLiterals%TestKt.Int%%this%call-plus%arg-0%call-print%fun-A() + LiveLiterals%TestKt.Int%arg-0%call-plus%arg-0%call-print%fun-A())
                }
                @LiveLiteralFileInfo(file = "/Test.kt")
                internal object LiveLiterals%TestKt {
                  val Int%%this%call-plus%arg-0%call-print%fun-A: Int = 3
                  var State%Int%%this%call-plus%arg-0%call-print%fun-A: State<Int>?
                  @LiveLiteralInfo(key = "Int%%this%call-plus%arg-0%call-print%fun-A", offset = 72)
                  fun Int%%this%call-plus%arg-0%call-print%fun-A(): Int {
                    if (!isLiveLiteralsEnabled) {
                      return Int%%this%call-plus%arg-0%call-print%fun-A
                    }
                    val tmp0 = State%Int%%this%call-plus%arg-0%call-print%fun-A
                    return if (tmp0 == null) {
                      val tmp1 = liveLiteral("Int%%this%call-plus%arg-0%call-print%fun-A", Int%%this%call-plus%arg-0%call-print%fun-A)
                      State%Int%%this%call-plus%arg-0%call-print%fun-A = tmp1
                      tmp1
                    } else {
                      tmp0
                    }
                    .value
                  }
                  val Int%arg-0%call-plus%arg-0%call-print%fun-A: Int = 4
                  var State%Int%arg-0%call-plus%arg-0%call-print%fun-A: State<Int>?
                  @LiveLiteralInfo(key = "Int%arg-0%call-plus%arg-0%call-print%fun-A", offset = 76)
                  fun Int%arg-0%call-plus%arg-0%call-print%fun-A(): Int {
                    if (!isLiveLiteralsEnabled) {
                      return Int%arg-0%call-plus%arg-0%call-print%fun-A
                    }
                    val tmp0 = State%Int%arg-0%call-plus%arg-0%call-print%fun-A
                    return if (tmp0 == null) {
                      val tmp1 = liveLiteral("Int%arg-0%call-plus%arg-0%call-print%fun-A", Int%arg-0%call-plus%arg-0%call-print%fun-A)
                      State%Int%arg-0%call-plus%arg-0%call-print%fun-A = tmp1
                      tmp1
                    } else {
                      tmp0
                    }
                    .value
                  }
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
            """,
            """
                fun A() {
                  print(LiveLiterals%TestKt.Int%arg-0%call-print%fun-A())
                }
                @LiveLiteralFileInfo(file = "/Test.kt")
                internal object LiveLiterals%TestKt {
                  val Int%arg-0%call-print%fun-A: Int = 7
                  var State%Int%arg-0%call-print%fun-A: State<Int>?
                  @LiveLiteralInfo(key = "Int%arg-0%call-print%fun-A", offset = 74)
                  fun Int%arg-0%call-print%fun-A(): Int {
                    if (!isLiveLiteralsEnabled) {
                      return Int%arg-0%call-print%fun-A
                    }
                    val tmp0 = State%Int%arg-0%call-print%fun-A
                    return if (tmp0 == null) {
                      val tmp1 = liveLiteral("Int%arg-0%call-print%fun-A", Int%arg-0%call-print%fun-A)
                      State%Int%arg-0%call-print%fun-A = tmp1
                      tmp1
                    } else {
                      tmp0
                    }
                    .value
                  }
                }
            """
        )
    }

    @Test
    fun testComposeIrSkippingWithDefaultsRelease() = verifyComposeIrTransform(
        """
            import androidx.compose.ui.text.input.TextFieldValue
            import androidx.compose.runtime.*
            import androidx.compose.foundation.layout.*
            import androidx.compose.foundation.text.KeyboardActions
            import androidx.compose.material.*

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
        """
            @StabilityInferred(parameters = 0)
            object Ui {
              static val %stable: Int = LiveLiterals%TestKt.Int%class-Ui()
            }
            @Composable
            @ComposableTarget(applier = "androidx.compose.ui.UiComposable")
            fun Ui.UiTextField(isError: Boolean, keyboardActions2: Boolean, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(UiTextField)")
              val %dirty = %changed
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%default and 0b0001 === 0 && %composer.changed(isError)) 0b00100000 else 0b00010000
              }
              if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%default and 0b0010 === 0 && %composer.changed(keyboardActions2)) 0b000100000000 else 0b10000000
              }
              if (%dirty and 0b001011010001 !== 0b10010000 || !%composer.skipping) {
                %composer.startDefaults()
                if (%changed and 0b0001 === 0 || %composer.defaultsInvalid) {
                  if (%default and 0b0001 !== 0) {
                    isError = LiveLiterals%TestKt.Boolean%param-isError%fun-UiTextField()
                    %dirty = %dirty and 0b01110000.inv()
                  }
                  if (%default and 0b0010 !== 0) {
                    keyboardActions2 = LiveLiterals%TestKt.Boolean%param-keyboardActions2%fun-UiTextField()
                    %dirty = %dirty and 0b001110000000.inv()
                  }
                } else {
                  %composer.skipToGroupEnd()
                  if (%default and 0b0001 !== 0) {
                    %dirty = %dirty and 0b01110000.inv()
                  }
                  if (%default and 0b0010 !== 0) {
                    %dirty = %dirty and 0b001110000000.inv()
                  }
                }
                %composer.endDefaults()
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                println("%{LiveLiterals%TestKt.String%0%str%arg-0%call-println%fun-UiTextField()}%isError")
                println("%{LiveLiterals%TestKt.String%0%str%arg-0%call-println-1%fun-UiTextField()}%keyboardActions2")
                Column(null, null, null, { %composer: Composer?, %changed: Int ->
                  Text("%isError", null, <unsafe-coerce>(0L), <unsafe-coerce>(0L), null, null, null, <unsafe-coerce>(0L), null, null, <unsafe-coerce>(0L), <unsafe-coerce>(0), false, 0, 0, null, null, %composer, 0, 0, 0b00011111111111111110)
                  Text("%keyboardActions2", null, <unsafe-coerce>(0L), <unsafe-coerce>(0L), null, null, null, <unsafe-coerce>(0L), null, null, <unsafe-coerce>(0L), <unsafe-coerce>(0), false, 0, 0, null, null, %composer, 0, 0, 0b00011111111111111110)
                }, %composer, 0, 0b0111)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                UiTextField(isError, keyboardActions2, %composer, updateChangedFlags(%changed or 0b0001), %default)
              }
            }
            @LiveLiteralFileInfo(file = "/Test.kt")
            internal object LiveLiterals%TestKt {
              val Int%class-Ui: Int = 0
              var State%Int%class-Ui: State<Int>?
              @LiveLiteralInfo(key = "Int%class-Ui", offset = -1)
              fun Int%class-Ui(): Int {
                if (!isLiveLiteralsEnabled) {
                  return Int%class-Ui
                }
                val tmp0 = State%Int%class-Ui
                return if (tmp0 == null) {
                  val tmp1 = liveLiteral("Int%class-Ui", Int%class-Ui)
                  State%Int%class-Ui = tmp1
                  tmp1
                } else {
                  tmp0
                }
                .value
              }
              val Boolean%param-isError%fun-UiTextField: Boolean = false
              var State%Boolean%param-isError%fun-UiTextField: State<Boolean>?
              @LiveLiteralInfo(key = "Boolean%param-isError%fun-UiTextField", offset = 292)
              fun Boolean%param-isError%fun-UiTextField(): Boolean {
                if (!isLiveLiteralsEnabled) {
                  return Boolean%param-isError%fun-UiTextField
                }
                val tmp0 = State%Boolean%param-isError%fun-UiTextField
                return if (tmp0 == null) {
                  val tmp1 = liveLiteral("Boolean%param-isError%fun-UiTextField", Boolean%param-isError%fun-UiTextField)
                  State%Boolean%param-isError%fun-UiTextField = tmp1
                  tmp1
                } else {
                  tmp0
                }
                .value
              }
              val Boolean%param-keyboardActions2%fun-UiTextField: Boolean = false
              var State%Boolean%param-keyboardActions2%fun-UiTextField: State<Boolean>?
              @LiveLiteralInfo(key = "Boolean%param-keyboardActions2%fun-UiTextField", offset = 331)
              fun Boolean%param-keyboardActions2%fun-UiTextField(): Boolean {
                if (!isLiveLiteralsEnabled) {
                  return Boolean%param-keyboardActions2%fun-UiTextField
                }
                val tmp0 = State%Boolean%param-keyboardActions2%fun-UiTextField
                return if (tmp0 == null) {
                  val tmp1 = liveLiteral("Boolean%param-keyboardActions2%fun-UiTextField", Boolean%param-keyboardActions2%fun-UiTextField)
                  State%Boolean%param-keyboardActions2%fun-UiTextField = tmp1
                  tmp1
                } else {
                  tmp0
                }
                .value
              }
              val String%0%str%arg-0%call-println%fun-UiTextField: String = "t41 insideFunction "
              var State%String%0%str%arg-0%call-println%fun-UiTextField: State<String>?
              @LiveLiteralInfo(key = "String%0%str%arg-0%call-println%fun-UiTextField", offset = 355)
              fun String%0%str%arg-0%call-println%fun-UiTextField(): String {
                if (!isLiveLiteralsEnabled) {
                  return String%0%str%arg-0%call-println%fun-UiTextField
                }
                val tmp0 = State%String%0%str%arg-0%call-println%fun-UiTextField
                return if (tmp0 == null) {
                  val tmp1 = liveLiteral("String%0%str%arg-0%call-println%fun-UiTextField", String%0%str%arg-0%call-println%fun-UiTextField)
                  State%String%0%str%arg-0%call-println%fun-UiTextField = tmp1
                  tmp1
                } else {
                  tmp0
                }
                .value
              }
              val String%0%str%arg-0%call-println-1%fun-UiTextField: String = "t41 insideFunction "
              var State%String%0%str%arg-0%call-println-1%fun-UiTextField: State<String>?
              @LiveLiteralInfo(key = "String%0%str%arg-0%call-println-1%fun-UiTextField", offset = 398)
              fun String%0%str%arg-0%call-println-1%fun-UiTextField(): String {
                if (!isLiveLiteralsEnabled) {
                  return String%0%str%arg-0%call-println-1%fun-UiTextField
                }
                val tmp0 = State%String%0%str%arg-0%call-println-1%fun-UiTextField
                return if (tmp0 == null) {
                  val tmp1 = liveLiteral("String%0%str%arg-0%call-println-1%fun-UiTextField", String%0%str%arg-0%call-println-1%fun-UiTextField)
                  State%String%0%str%arg-0%call-println-1%fun-UiTextField = tmp1
                  tmp1
                } else {
                  tmp0
                }
                .value
              }
            }
        """
    )
}
