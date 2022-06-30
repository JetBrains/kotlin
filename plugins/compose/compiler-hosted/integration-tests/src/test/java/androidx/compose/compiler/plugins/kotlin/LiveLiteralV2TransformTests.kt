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

import androidx.compose.compiler.plugins.kotlin.lower.DurableKeyVisitor
import androidx.compose.compiler.plugins.kotlin.lower.LiveLiteralTransformer
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.junit.Test

class LiveLiteralV2TransformTests : AbstractIrTransformTest() {

    fun testSiblingCallArgs() = assertNoDuplicateKeys(
        """
        fun Test() {
            print(1)
            print(1)
        }
        """
    )

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

    fun testDispatchReceiver() = assertKeys(
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

    fun testInsidePropertyGetter() = assertKeys(
        "Int%fun-%get-foo%%get%val-foo"
    ) {
        """
        val foo: Int get() = 1
        """
    }

    // NOTE(lmr): For static initializer expressions we can/should do more.
    fun testInsidePropertyInitializer() = assertKeys {
        """
        val foo: Int = 1
        """
    }

    fun testValueParameter() = assertKeys(
        "Int%param-x%fun-Foo"
    ) {
        """
        fun Foo(x: Int = 1) { print(x) }
        """
    }

    fun testAnnotation() = assertKeys {
        """
        annotation class Foo(val value: Int = 1)
        @Foo fun Bar() {}
        @Foo(2) fun Bam() {}
        """
    }

    // NOTE(lmr): In the future we should try and get this to work
    fun testForLoop() = assertKeys {
        """
        fun Foo() {
            for (x in 0..10) {
                print(x)
            }
        }
        """
    }

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
    fun testConstantProperty() = assertKeys {
        """
        const val foo = 1
        """
    }

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

    fun testElvis() = assertKeys(
        "String%branch%when%fun-Foo"
    ) {
        """
        fun Foo(bar: String?): String {
            return bar ?: "Hello World"
        }
        """
    }

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

    fun testDelegatingCtor() = assertKeys(
        "Int%arg-0%call-%init%%class-Bar"
    ) {
        """
        open class Foo(val x: Int)
        class Bar() : Foo(123)
        """
    }

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
              val enabled: Boolean = false
              val Int%fun-bar%class-%no-name-provided%%fun-a: Int = 1
              var State%Int%fun-bar%class-%no-name-provided%%fun-a: State<Int>?
              @LiveLiteralInfo(key = "Int%fun-bar%class-%no-name-provided%%fun-a", offset = 159)
              fun Int%fun-bar%class-%no-name-provided%%fun-a(): Int {
                if (!enabled) {
                  return Int%fun-bar%class-%no-name-provided%%fun-a
                }
                val tmp0 = State%Int%fun-bar%class-%no-name-provided%%fun-a
                return if (tmp0 == null) {
                  val tmp1 = liveLiteral("Int%fun-bar%class-%no-name-provided%%fun-a", Int%fun-bar%class-%no-name-provided%%fun-a)
                  <set-State%Int%fun-bar%class-%no-name-provided%%fun-a>(tmp1)
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
    fun testBasicTransform(): Unit = assertTransform(
        """
        """,
        """
            fun A() {
              print(1)
              print("Hello World")
              if (true) {
                print(3 + 4)
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
                print(LiveLiterals%TestKt.Int%%this%call-plus%arg-0%call-print%branch%if%fun-A() + LiveLiterals%TestKt.Int%arg-0%call-plus%arg-0%call-print%branch%if%fun-A())
              }
              if (LiveLiterals%TestKt.Boolean%cond%if-1%fun-A()) {
                print(LiveLiterals%TestKt.Float%arg-0%call-print%branch%if-1%fun-A())
              }
              print(LiveLiterals%TestKt.Int%arg-0%call-print-2%fun-A())
            }
            @LiveLiteralFileInfo(file = "/Test.kt")
            internal object LiveLiterals%TestKt {
              val enabled: Boolean = false
              val Int%arg-0%call-print%fun-A: Int = 1
              var State%Int%arg-0%call-print%fun-A: State<Int>?
              @LiveLiteralInfo(key = "Int%arg-0%call-print%fun-A", offset = 62)
              fun Int%arg-0%call-print%fun-A(): Int {
                if (!enabled) {
                  return Int%arg-0%call-print%fun-A
                }
                val tmp0 = State%Int%arg-0%call-print%fun-A
                return if (tmp0 == null) {
                  val tmp1 = liveLiteral("Int%arg-0%call-print%fun-A", Int%arg-0%call-print%fun-A)
                  <set-State%Int%arg-0%call-print%fun-A>(tmp1)
                  tmp1
                } else {
                  tmp0
                }
                .value
              }
              val String%arg-0%call-print-1%fun-A: String = "Hello World"
              var State%String%arg-0%call-print-1%fun-A: State<String>?
              @LiveLiteralInfo(key = "String%arg-0%call-print-1%fun-A", offset = 74)
              fun String%arg-0%call-print-1%fun-A(): String {
                if (!enabled) {
                  return String%arg-0%call-print-1%fun-A
                }
                val tmp0 = State%String%arg-0%call-print-1%fun-A
                return if (tmp0 == null) {
                  val tmp1 = liveLiteral("String%arg-0%call-print-1%fun-A", String%arg-0%call-print-1%fun-A)
                  <set-State%String%arg-0%call-print-1%fun-A>(tmp1)
                  tmp1
                } else {
                  tmp0
                }
                .value
              }
              val Boolean%cond%if%fun-A: Boolean = true
              var State%Boolean%cond%if%fun-A: State<Boolean>?
              @LiveLiteralInfo(key = "Boolean%cond%if%fun-A", offset = 94)
              fun Boolean%cond%if%fun-A(): Boolean {
                if (!enabled) {
                  return Boolean%cond%if%fun-A
                }
                val tmp0 = State%Boolean%cond%if%fun-A
                return if (tmp0 == null) {
                  val tmp1 = liveLiteral("Boolean%cond%if%fun-A", Boolean%cond%if%fun-A)
                  <set-State%Boolean%cond%if%fun-A>(tmp1)
                  tmp1
                } else {
                  tmp0
                }
                .value
              }
              val Int%%this%call-plus%arg-0%call-print%branch%if%fun-A: Int = 3
              var State%Int%%this%call-plus%arg-0%call-print%branch%if%fun-A: State<Int>?
              @LiveLiteralInfo(key = "Int%%this%call-plus%arg-0%call-print%branch%if%fun-A", offset = 112)
              fun Int%%this%call-plus%arg-0%call-print%branch%if%fun-A(): Int {
                if (!enabled) {
                  return Int%%this%call-plus%arg-0%call-print%branch%if%fun-A
                }
                val tmp0 = State%Int%%this%call-plus%arg-0%call-print%branch%if%fun-A
                return if (tmp0 == null) {
                  val tmp1 = liveLiteral("Int%%this%call-plus%arg-0%call-print%branch%if%fun-A", Int%%this%call-plus%arg-0%call-print%branch%if%fun-A)
                  <set-State%Int%%this%call-plus%arg-0%call-print%branch%if%fun-A>(tmp1)
                  tmp1
                } else {
                  tmp0
                }
                .value
              }
              val Int%arg-0%call-plus%arg-0%call-print%branch%if%fun-A: Int = 4
              var State%Int%arg-0%call-plus%arg-0%call-print%branch%if%fun-A: State<Int>?
              @LiveLiteralInfo(key = "Int%arg-0%call-plus%arg-0%call-print%branch%if%fun-A", offset = 116)
              fun Int%arg-0%call-plus%arg-0%call-print%branch%if%fun-A(): Int {
                if (!enabled) {
                  return Int%arg-0%call-plus%arg-0%call-print%branch%if%fun-A
                }
                val tmp0 = State%Int%arg-0%call-plus%arg-0%call-print%branch%if%fun-A
                return if (tmp0 == null) {
                  val tmp1 = liveLiteral("Int%arg-0%call-plus%arg-0%call-print%branch%if%fun-A", Int%arg-0%call-plus%arg-0%call-print%branch%if%fun-A)
                  <set-State%Int%arg-0%call-plus%arg-0%call-print%branch%if%fun-A>(tmp1)
                  tmp1
                } else {
                  tmp0
                }
                .value
              }
              val Boolean%cond%if-1%fun-A: Boolean = true
              var State%Boolean%cond%if-1%fun-A: State<Boolean>?
              @LiveLiteralInfo(key = "Boolean%cond%if-1%fun-A", offset = 129)
              fun Boolean%cond%if-1%fun-A(): Boolean {
                if (!enabled) {
                  return Boolean%cond%if-1%fun-A
                }
                val tmp0 = State%Boolean%cond%if-1%fun-A
                return if (tmp0 == null) {
                  val tmp1 = liveLiteral("Boolean%cond%if-1%fun-A", Boolean%cond%if-1%fun-A)
                  <set-State%Boolean%cond%if-1%fun-A>(tmp1)
                  tmp1
                } else {
                  tmp0
                }
                .value
              }
              val Float%arg-0%call-print%branch%if-1%fun-A: Float = 1.0f
              var State%Float%arg-0%call-print%branch%if-1%fun-A: State<Float>?
              @LiveLiteralInfo(key = "Float%arg-0%call-print%branch%if-1%fun-A", offset = 147)
              fun Float%arg-0%call-print%branch%if-1%fun-A(): Float {
                if (!enabled) {
                  return Float%arg-0%call-print%branch%if-1%fun-A
                }
                val tmp0 = State%Float%arg-0%call-print%branch%if-1%fun-A
                return if (tmp0 == null) {
                  val tmp1 = liveLiteral("Float%arg-0%call-print%branch%if-1%fun-A", Float%arg-0%call-print%branch%if-1%fun-A)
                  <set-State%Float%arg-0%call-print%branch%if-1%fun-A>(tmp1)
                  tmp1
                } else {
                  tmp0
                }
                .value
              }
              val Int%arg-0%call-print-2%fun-A: Int = 3
              var State%Int%arg-0%call-print-2%fun-A: State<Int>?
              @LiveLiteralInfo(key = "Int%arg-0%call-print-2%fun-A", offset = 165)
              fun Int%arg-0%call-print-2%fun-A(): Int {
                if (!enabled) {
                  return Int%arg-0%call-print-2%fun-A
                }
                val tmp0 = State%Int%arg-0%call-print-2%fun-A
                return if (tmp0 == null) {
                  val tmp1 = liveLiteral("Int%arg-0%call-print-2%fun-A", Int%arg-0%call-print-2%fun-A)
                  <set-State%Int%arg-0%call-print-2%fun-A>(tmp1)
                  tmp1
                } else {
                  tmp0
                }
                .value
              }
            }
        """
    )

    private var builtKeys = mutableSetOf<String>()

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun postProcessingStep(
        module: IrModuleFragment,
        context: IrPluginContext
    ) {
        val symbolRemapper = DeepCopySymbolRemapper()
        val keyVisitor = DurableKeyVisitor(builtKeys)
        val transformer = object : LiveLiteralTransformer(
            true,
            true,
            keyVisitor,
            context,
            symbolRemapper,
            ModuleMetricsImpl("temp", context)
        ) {
            override fun makeKeySet(): MutableSet<String> {
                return super.makeKeySet().also { builtKeys = it }
            }
        }
        transformer.lower(module)
    }

    // since the lowering will throw an exception if duplicate keys are found, all we have to do
    // is run the lowering
    private fun assertNoDuplicateKeys(@Language("kotlin") src: String) {
        JvmCompilation().compile(
            listOf(
                sourceFile("Test.kt", src.replace('%', '$'))
            )
        )
    }

    // For a given src string, a
    private fun assertKeys(vararg keys: String, makeSrc: () -> String) {
        builtKeys = mutableSetOf()
        JvmCompilation().compile(
            listOf(
                sourceFile("Test.kt", makeSrc().replace('%', '$'))
            )
        )
        assertEquals(
            keys.toList().sorted().joinToString(separator = ",\n") {
                "\"${it.replace('$', '%')}\""
            },
            builtKeys.toList().sorted().joinToString(separator = ",\n") {
                "\"${it.replace('$', '%')}\""
            }
        )
    }

    // test: have two src strings (before/after) and assert that the keys of the params didn't change
    private fun assertDurableChange(before: String, after: String) {
        JvmCompilation().compile(
            listOf(
                sourceFile("Test.kt", before.replace('%', '$'))
            )
        )
        val beforeKeys = builtKeys

        builtKeys = mutableSetOf()

        JvmCompilation().compile(
            listOf(
                sourceFile("Test.kt", after.replace('%', '$'))
            )
        )
        val afterKeys = builtKeys

        assertEquals(
            beforeKeys.toList().sorted().joinToString(separator = "\n"),
            afterKeys.toList().sorted().joinToString(separator = "\n")
        )
    }

    private fun assertTransform(
        unchecked: String,
        checked: String,
        expectedTransformed: String,
        dumpTree: Boolean = false
    ) = verifyComposeIrTransform(
        """
            import androidx.compose.runtime.Composable
            $checked
        """.trimIndent(),
        expectedTransformed,
        """
            import androidx.compose.runtime.Composable
            $unchecked
        """.trimIndent(),
        dumpTree = dumpTree
    )
}