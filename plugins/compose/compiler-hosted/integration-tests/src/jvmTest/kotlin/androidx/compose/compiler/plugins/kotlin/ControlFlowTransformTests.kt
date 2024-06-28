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

import org.intellij.lang.annotations.Language
import org.junit.Ignore
import org.junit.Test

class ControlFlowTransformTests(useFir: Boolean) : AbstractControlFlowTransformTests(useFir) {
    @Test
    fun testIfNonComposable(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                // No composable calls, so no group generated except for at function boundary
                if (x > 0) {
                    NA()
                }
            }
        """
    )

    @Test
    fun testAND(): Unit = controlFlow(
        """
            @NonRestartableComposable
            @Composable
            fun Example() {
                B() && B()
            }
        """
    )

    @Test
    fun testOR(): Unit = controlFlow(
        """
            @NonRestartableComposable
            @Composable
            fun Example() {
                B() || B()
            }
        """
    )

    @Test
    fun testIfWithCallsInBranch(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                // Only one composable call in the result blocks, so we can just generate
                // a single group around the whole expression.
                if (x > 0) {
                    A()
                }
            }
        """
    )

    @Test
    fun testIfElseWithCallsInBranch(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                // Composable calls in the result blocks, so we can determine static number of
                // groups executed. This means we put a group around the "then" and the
                // "else" blocks
                if (x > 0) {
                    A(a)
                } else {
                    A(b)
                }
            }
        """
    )

    @Test
    fun testIfWithCallInCondition(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                // Since the first condition of an if/else is unconditionally executed, it does not
                // necessitate a group of any kind, so we just end up with the function boundary
                // group
                if (B()) {
                    NA()
                } else {
                    NA()
                }
            }
        """
    )

    private fun verifyInlineReturn(
        @Language("kotlin")
        source: String,
    ) = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable
            $source
        """,
        """
            import androidx.compose.runtime.Composable

            @Composable
            fun A() { }

            @Composable
            fun Text(text: String) { }

            @Composable
            inline fun Wrapper(content: @Composable () -> Unit) = content()

            @Composable
            inline fun M1(content: @Composable () -> Unit) = Wrapper {
                content()
            }

            @Composable
            inline fun M2(content: @Composable () -> Unit) = Wrapper {
                Wrapper {
                    content()
                }
            }

            @Composable
            inline fun M3(content: @Composable () -> Unit) = Wrapper {
                Wrapper {
                    Wrapper {
                        content()
                    }
                }
            }

            inline fun <T> Identity(block: () -> T): T = block()

            @Composable
            fun Stack(content: @Composable () -> Unit) = content()
        """
    )

    @Test
    fun testInline_CM3_RFun() = verifyInlineReturn(
        """
            @Composable
            fun Test(condition: Boolean) {
                A()
                M3 {
                    A()
                    if (condition) {
                        return
                    }
                    A()
                }
                A()
            }
        """
    )

    @Test
    fun testInline_CM3_RFun_CM3_RFun() = verifyInlineReturn(
        """
            @Composable
            fun Test(a: Boolean, b: Boolean) {
                A()
                M3 {
                    A()
                    if (a) {
                        return
                    }
                    A()
                }
                M3 {
                    A()
                    if (b) {
                        return
                    }
                    A()
                }
                A()
            }
        """
    )

    @Test
    fun testInline_CM3_RM3() = verifyInlineReturn(
        """
            @Composable
            fun Test(condition: Boolean) {
                A()
                M3 {
                    A()
                    if (condition) {
                        return@M3
                    }
                    A()
                }
                A()
            }
        """
    )

    @Test
    fun testInline_Lambda() = verifyGoldenComposeIrTransform(
        """
            fun Test(condition: Boolean) {
                T {
                    compose {
                        M1 {
                            if (condition) return@compose
                        }
                    }
                }
            }
        """,
        """
            import androidx.compose.runtime.*

            class Scope {
                fun compose(block: @Composable () -> Unit) { }
            }

            fun T(block: suspend Scope.() -> Unit) { }

            @Composable
            inline fun M1(block: @Composable () -> Unit) { block() }

            @Composable
            fun Text(text: String) { }
        """
    )

    @Test
    fun testInline_M3_M1_Return_M1() = verifyInlineReturn(
        """
            @Composable
            fun Test_M3_M1_Return_M1(condition: Boolean) {
                A()
                M3 {
                    A()
                    M1 {
                        if (condition) {
                            return@M1
                        }
                    }
                    A()
                }
                A()
            }
        """
    )

    @Test
    fun testInline_M3_M1_Return_M3() = verifyInlineReturn(
        """
            @Composable
            fun Test_M3_M1_Return_M3(condition: Boolean) {
                A()
                M3 {
                    A()
                    M1 {
                        if (condition) {
                            return@M3
                        }
                    }
                    A()
                }
                A()
            }
        """
    )

    @Test
    fun testInline_M1_W_Return_Func() = verifyInlineReturn(
        """
            @Composable
            fun testInline_M1_W_Return_Func(condition: Boolean) {
                A()
                M1 {
                    A()
                    while(true) {
                        A()
                        if (condition) {
                            return
                        }
                        A()
                    }
                    A()
                }
                A()
            }
        """
    )

    @Test
    fun testInline_CM3_Return_M3_CM3_Return_M3() = verifyInlineReturn(
        """
            @Composable
            fun testInline_M1_W_Return_Func(condition: Boolean) {
                A()
                M3 {
                    A()
                    if (condition) {
                        return@M3
                    }
                    A()
                }
                M3 {
                    A()
                    if (condition) {
                        return@M3
                    }
                    A()
                }
                A()
            }
        """
    )

    @Test
    fun test_CM1_CCM1_RetFun(): Unit = verifyInlineReturn(
        """
            @Composable
            fun test_CM1_CCM1_RetFun(condition: Boolean) {
                Text("Root - before")
                M1 {
                    Text("M1 - begin")
                    if (condition) {
                        Text("if - begin")
                        M1 {
                            Text("In CCM1")
                            return@test_CM1_CCM1_RetFun
                        }
                    }
                    Text("M1 - end")
                }
                Text("Root - end")
            }
        """
    )

    @Test
    fun testInlineReturnLabel(): Unit = controlFlow(
        """
            @Composable
            @NonRestartableComposable
            fun CustomTextBroken(condition: Boolean) {
                FakeBox {
                    if (condition) {
                        return@FakeBox
                    }
                    A()
                }
            }
            @Composable
            inline fun FakeBox(content: @Composable () -> Unit) {
                content()
            }
        """
    )

    @Test // regression 255350755
    fun testEnsureEarlyExitInNonInline_NormalComposable() = controlFlow(
        """
            object obj {
                val condition = true
            }

            @Composable
            fun Test(condition: Boolean) {
                if (condition) return
                with (obj) {
                    if (condition) return
                }
                A()
            }
        """
    )

    @Test // regression 255350755
    fun testEnsureEarlyExitInNonInline_ReadOnlyComposable() = controlFlow(
        """
            import androidx.compose.runtime.currentComposer

            object obj {
                val condition = false
            }

            @Composable
            @ReadOnlyComposable
            fun Calculate(condition: Boolean): Boolean {
                if (condition) return false

                with (obj) {
                    if (condition) return false
                    return currentComposer.inserting
                }
            }
        """
    )

    @Test // regression 255350755
    fun testEnsureEarlyExitInInline_Labeled() = controlFlow(
        """
            @Composable
            fun Test(condition: Boolean) {
                IW iw@ {
                    if (condition) return@iw
                    A()
                }
            }
        """
    )

    @Test
    fun testVerifyEarlyExitFromNonComposable() = verifyInlineReturn(
        source = """
            @Composable
            fun Test(condition: Boolean) {
                Text("Some text")
                Identity {
                    if (condition) return@Test
                }
                Text("Some more text")
            }
        """
    )

    @Test
    fun testVerifyEarlyExitFromNonComposable_M1() = verifyInlineReturn(
        source = """
            @Composable
            fun Test(condition: Boolean) {
                Text("Some text")
                M1 {
                    Identity {
                        if (condition) return@Test
                    }
                }
                Text("Some more text")
            }
        """
    )

    @Test
    fun testVerifyEarlyExitFromNonComposable_M1_RM1() = verifyInlineReturn(
        source = """
            @Composable
            fun Test(condition: Boolean) {
                Text("Some text")
                M1 {
                    Identity {
                        if (condition) return@M1
                    }
                }
                Text("Some more text")
            }
        """
    )

    @Test
    fun verifyEarlyExitFromNestedInlineFunction() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable
            @NonRestartableComposable
            fun Test(condition: Boolean) {
                Text("Before outer")
                InlineLinearA {
                    Text("Before inner")
                    InlineLinearB inner@{
                        Text("Before return")
                        if (condition) return@inner
                        Text("After return")
                    }
                    Text("After inner")
                }
                Text("Before outer")
            }
        """,
        extra = """
            import androidx.compose.runtime.*

            @Composable
            fun Text(value: String) { }

            @Composable
            inline fun InlineLinearA(content: @Composable () -> Unit) {
                content()
            }

            @Composable
            inline fun InlineLinearB(content: @Composable () -> Unit) {
                content()
            }
        """
    )

    @Test
    fun verifyEarlyExitFromMultiLevelNestedInlineFunction() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable
            @NonRestartableComposable
            fun Test(condition: Boolean) {
                Text("Before outer")
                InlineLinearA outer@{
                    Text("Before inner")
                    InlineLinearB {
                        Text("Before return")
                        if (condition) return@outer
                        Text("After return")
                    }
                    Text("After inner")
                }
                Text("Before outer")
            }
        """,
        extra = """
            import androidx.compose.runtime.*

            @Composable
            fun Text(value: String) { }

            @Composable
            inline fun InlineLinearA(content: @Composable () -> Unit) {
                content()
            }

            @Composable
            inline fun InlineLinearB(content: @Composable () -> Unit) {
                content()
            }
        """
    )

    @Test
    fun testEnsureRuntimeTestWillCompile_CL() {
        classLoader(
            """
            import androidx.compose.runtime.Composable

            @Composable
            fun test_CM1_RetFun(condition: Boolean) {
                Text("Root - before")
                M1 {
                    Text("M1 - before")
                    if (condition) return
                    Text("M1 - after")
                }
                Text("Root - after")
            }
            @Composable
            inline fun InlineWrapper(content: @Composable () -> Unit) = content()

            @Composable
            inline fun M1(content: @Composable () -> Unit) = InlineWrapper { content() }

            @Composable
            fun Text(value: String) { }
            """,
            "Test.kt"
        )
    }

    @Test // regression 255350755
    fun testEnsureRuntimeTestWillCompile_CG() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            @Composable
            fun test_CM1_RetFun(condition: Boolean) {
                Text("Root - before")
                M1 {
                    Text("M1 - before")
                    if (condition) return
                    Text("M1 - after")
                }
                Text("Root - after")
            }

        """,
        extra = """
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.NonRestartableComposable

            @Composable
            inline fun InlineWrapper(content: @Composable () -> Unit) = content()

            @Composable
            inline fun M1(content: @Composable () -> Unit) = InlineWrapper { content() }

            @Composable @NonRestartableComposable
            fun Text(value: String) { }
        """
    )

    @Test
    fun testIfElseWithCallsInConditions(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                // Since the condition in the else-if is conditionally executed, it means we have
                // dynamic execution and we can't statically guarantee the number of groups. As a
                // result, we generate a group around the if statement in addition to a group around
                // each of the conditions with composable calls in them. Note that no group is
                // needed around the else condition
                if (B(a)) {
                    NA()
                } else if (B(b)) {
                    NA()
                } else {
                    NA()
                }
            }
        """
    )

    @Test
    fun testWhenWithSubjectAndNoCalls(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                // nothing needed except for the function boundary group
                when (x) {
                    0 -> 8
                    1 -> 10
                    else -> x
                }
            }
        """
    )

    @Test
    fun testWhenWithSubjectAndCalls(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                // calls only in the result block, which means we can statically guarantee the
                // number of groups, so no group around the when is needed, just groups around the
                // result blocks.
                when (x) {
                    0 -> A(a)
                    1 -> A(b)
                    else -> A(c)
                }
            }
        """
    )

    @Test
    fun testWhenWithSubjectAndCallsWithResult(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                // no need for a group around the when expression overall, but since the result
                // of the expression is now being used, we need to generate temporary variables to
                // capture the result but still do the execution of the expression inside of groups.
                val y = when (x) {
                    0 -> R(a)
                    1 -> R(b)
                    else -> R(c)
                }
            }
        """
    )

    @Test
    fun testWhenWithCalls(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                // result blocks have composable calls, so we generate groups round them. It's a
                // statically guaranteed number of groups at execution, so no wrapping group is
                // needed.
                when {
                    x < 0 -> A(a)
                    x > 30 -> A(b)
                    else -> A(c)
                }
            }
        """
    )

    @Test
    fun testWhenWithCallsInSomeResults(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                // result blocks have composable calls, so we generate groups round them. It's a
                // statically guaranteed number of groups at execution, so no wrapping group is
                // needed.
                when {
                    x < 0 -> A(a)
                    x > 30 -> NA()
                    else -> A(b)
                }
            }
        """
    )

    @Test
    fun testWhenWithCallsInConditions(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                // composable calls are in the condition blocks of the when statement. Since these
                // are conditionally executed, we can't statically know the number of groups during
                // execution. as a result, we must wrap the when clause with a group. Since there
                // are no other composable calls, the function body group will suffice.
                when {
                    x == R(a) -> NA()
                    x > R(b) -> NA()
                    else -> NA()
                }
            }
        """
    )

    @Test
    fun testWhenWithCallsInConditionsAndCallAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                // composable calls are in the condition blocks of the when statement. Since these
                // are conditionally executed, we can't statically know the number of groups during
                // execution. as a result, we must wrap the when clause with a group.
                when {
                    x == R(a) -> NA()
                    x > R(b) -> NA()
                    else -> NA()
                }
                A()
            }
        """
    )

    @Test
    fun testSafeCall(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int?) {
                // The composable call is made conditionally, which means it is like an if with
                // only one result having a composable call, so we just generate a single group
                // around the whole expression.
                x?.A()
            }
        """
    )

    @Test
    fun testElvis(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int?) {
                // The composable call is made conditionally, which means it is like an if, but with
                // only one result having a composable call, so we just generate a single group
                // around the whole expression.
                val y = x ?: R()
            }
        """
    )

    @Test
    fun testForLoopWithCallsInBody(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(items: List<Int>) {
                // The composable call is made a conditional number of times, so we need to wrap
                // the loop with a dynamic wrapping group. Since there are no other calls, the
                // function body group will suffice.
                for (i in items) {
                    P(i)
                }
            }
        """
    )

    @Test
    fun testForLoopWithCallsInBodyAndCallsAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(items: List<Int>) {
                // The composable call is made a conditional number of times, so we need to wrap
                // the loop with a dynamic wrapping group.
                for (i in items) {
                    P(i)
                }
                A()
            }
        """
    )

    @Test
    fun testForLoopWithCallsInSubject(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example() {
                // The for loop's subject expression is only executed once, so we don't need any
                // additional groups
                for (i in L()) {
                    print(i)
                }
            }
        """
    )

    @Test
    fun testWhileLoopWithCallsInBody(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(items: MutableList<Int>) {
                // since we have a composable call which is called a conditional number of times,
                // we need to generate groups around the loop's block as well as a group around the
                // overall statement. Since there are no calls after the while loop, the function
                // body group will suffice.
                while (items.isNotEmpty()) {
                    val item = items.removeAt(items.size - 1)
                    P(item)
                }
            }
        """
    )

    @Test
    fun testWhileLoopWithCallsInBodyAndCallsAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(items: MutableList<Int>) {
                // since we have a composable call which is called a conditional number of times,
                // we need to generate groups around the loop's block as well as a group around the
                // overall statement.
                while (items.isNotEmpty()) {
                    val item = items.removeAt(items.size - 1)
                    P(item)
                }
                A()
            }
        """
    )

    @Test
    fun testWhileLoopWithCallsInCondition(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example() {
                // A while loop's condition block gets executed a conditional number of times, so
                // so we must generate a group around the while expression overall. The function
                // body group will suffice.
                while (B()) {
                    print("hello world")
                }
            }
        """
    )

    @Test
    fun testWhileLoopWithCallsInConditionAndCallsAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example() {
                // A while loop's condition block gets executed a conditional number of times, so
                // so we must generate a group around the while expression overall.
                while (B()) {
                    print("hello world")
                }
                A()
            }
        """
    )

    @Test
    fun testWhileLoopWithCallsInConditionAndBody(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example() {
                // Both the condition and the body of the loop get groups because they have
                // composable calls in them. We must generate a group around the while statement
                // overall, but the function body group will suffice.
                while (B()) {
                    A()
                }
            }
        """
    )

    @Test
    fun testWhileLoopWithCallsInConditionAndBodyAndCallsAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example() {
                // Both the condition and the body of the loop get groups because they have
                // composable calls in them. We must generate a group around the while statement
                // overall.
                while (B()) {
                    A(a)
                }
                A(b)
            }
        """
    )

    @Test
    fun testEarlyReturnWithCallsBeforeButNotAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                // in the early return path, we need only close out the opened groups
                if (x > 0) {
                    A()
                    return
                }
                print("hello")
            }
        """
    )

    @Test
    fun testEarlyReturnWithCallsAfterButNotBefore(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                // we can just close out the open groups at the return.
                if (x > 0) {
                    return
                }
                A()
            }
        """
    )

    @Test
    fun testEarlyReturnValue(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int): Int {
                if (x > 0) {
                    A()
                    return 1
                }
                return 2
            }
        """
    )

    @Test
    fun testEarlyReturnValueWithCallsAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int): Int {
                if (x > 0) {
                    return 1
                }
                A()
                return 2
            }
        """
    )

    @Test
    fun testReturnCallValue(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(): Int {
                // since the return expression is a composable call, we need to generate a
                // temporary variable and then return it after ending the open groups.
                A()
                return R()
            }
        """
    )

    @Test
    fun testEarlyReturnCallValue(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int): Int {
                if (x > 0) {
                    return R()
                }
                return R()
            }
        """
    )

    @Test
    fun testReturnFromLoop(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(items: Iterator<Int>) {
                while (items.hasNext()) {
                    val i = items.next()
                    val j = i
                    val k = i
                    val l = i
                    P(i)
                    if (i == 0) {
                        P(j)
                        return
                    } else {
                        P(k)
                    }
                    P(l)
                }
            }
        """
    )

    @Test
    fun testOrderingOfPushedEndCallsWithEarlyReturns(): Unit = controlFlow(
        """
            @Composable
            fun Example(items: Iterator<Int>) {
                while (items.hasNext()) {
                    val i = items.next()
                    val j = i
                    val k = i
                    val l = i
                    P(i)
                    if (i == 0) {
                        P(j)
                        return
                    } else {
                        P(k)
                    }
                    P(l)
                }
            }
        """
    )

    @Test
    fun testBreakWithCallsAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(items: Iterator<Int>) {
                while (items.hasNext()) {
                    val i = items.next()
                    if (i == 0) {
                        break
                    }
                    P(i)
                }
            }
        """
    )

    @Test
    fun testBreakWithCallsBefore(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(items: Iterator<Int>) {
                while (items.hasNext()) {
                    val i = items.next()
                    P(i)
                    if (i == 0) {
                        break
                    }
                }
            }
        """
    )

    @Test
    fun testBreakWithCallsBeforeAndAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(items: Iterator<Int>) {
                // a group around while is needed here, but the function body group will suffice
                while (items.hasNext()) {
                    val i = items.next()
                    val j = i
                    P(i)
                    if (i == 0) {
                        break
                    }
                    P(j)
                }
            }
        """
    )

    @Test
    fun testBreakWithCallsBeforeAndAfterAndCallAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(items: Iterator<Int>) {
                // a group around while is needed here
                while (items.hasNext()) {
                    val i = items.next()
                    P(i)
                    if (i == 0) {
                        break
                    }
                    P(i)
                }
                A()
            }
        """
    )

    @Test
    fun testContinueWithCallsAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(items: Iterator<Int>) {
                while (items.hasNext()) {
                    val i = items.next()
                    if (i == 0) {
                        continue
                    }
                    P(i)
                }
            }
        """
    )

    @Test
    fun testContinueWithCallsBefore(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(items: Iterator<Int>) {
                while (items.hasNext()) {
                    val i = items.next()
                    P(i)
                    if (i == 0) {
                        continue
                    }
                    print(i)
                }
            }
        """
    )

    @Test
    fun testContinueWithCallsBeforeAndAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(items: Iterator<Int>) {
                while (items.hasNext()) {
                    val i = items.next()
                    P(i)
                    if (i == 0) {
                        continue
                    }
                    P(i)
                }
            }
        """
    )

    @Test
    fun testLoopWithReturn(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(a: Iterator<Int>, b: Iterator<Int>) {
                while (a.hasNext()) {
                    val x = a.next()
                    if (x > 100) {
                        return
                    }
                    A()
                }
            }
        """
    )

    @Test
    fun testLoopWithBreak(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(a: Iterator<Int>, b: Iterator<Int>) {
                a@while (a.hasNext()) {
                    val x = a.next()
                    b@while (b.hasNext()) {
                        val y = b.next()
                        if (y == x) {
                            break@a
                        }
                        A()
                    }
                    A()
                }
            }
        """
    )

    @Test
    fun testNestedLoopsAndBreak(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(a: Iterator<Int>, b: Iterator<Int>) {
                a@while (a.hasNext()) {
                    val x = a.next()
                    if (x == 0) {
                        break
                    }
                    b@while (b.hasNext()) {
                        val y = b.next()
                        if (y == 0) {
                            break
                        }
                        if (y == x) {
                            break@a
                        }
                        if (y > 100) {
                            return
                        }
                        A()
                    }
                    A()
                }
            }
        """
    )

    @Test
    fun testNestedLoops(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(a: Iterator<Int>, b: Iterator<Int>) {
                a@while (a.hasNext()) {
                    b@while (b.hasNext()) {
                        A()
                    }
                    A()
                }
                A()
            }
        """
    )

    @Test
    fun testWhileInsideIfAndCallAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                if (x > 0) {
                    while (x > 0) {
                        A()
                    }
                    A()
                }
            }
        """
    )

    @Test
    fun testWhileInsideIfAndCallBefore(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                if (x > 0) {
                    A()
                    while (x > 0) {
                        A()
                    }
                }
            }
        """
    )

    @Test
    fun testWhileInsideIf(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                if (x > 0) {
                    while (x > 0) {
                        A()
                    }
                }
            }
        """
    )

    @Test
    fun testWhileWithKey(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                while (x > 0) {
                    key(x) {
                        A()
                    }
                }
            }
        """
    )

    @Test
    fun testWhileWithTwoKeys(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                while (x > 0) {
                    key(x) {
                        A(a)
                    }
                    key(x+1) {
                        A(b)
                    }
                }
            }
        """
    )

    @Test
    fun testWhileWithKeyAndCallAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                while (x > 0) {
                    key(x) {
                        A(a)
                    }
                    A(b)
                }
            }
        """
    )

    @Test
    fun testWhileWithKeyAndCallBefore(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                while (x > 0) {
                    A(a)
                    key(x) {
                        A(b)
                    }
                }
            }
        """
    )

    @Test
    fun testWhileWithKeyAndCallBeforeAndAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                while (x > 0) {
                    A(a)
                    key(x) {
                        A(b)
                    }
                    A(c)
                }
            }
        """
    )

    @Test
    fun testKeyAtRootLevel(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                key(x) {
                    A()
                }
            }
        """
    )

    @Test
    fun testKeyAtRootLevelAndCallsAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                key(x) {
                    A(a)
                }
                A(b)
            }
        """
    )

    @Test
    fun testKeyAtRootLevelAndCallsBefore(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                A(a)
                key(x) {
                    A(b)
                }
            }
        """
    )

    @Test
    fun testKeyInIf(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                if (x > 0) {
                    key(x) {
                        A()
                    }
                }
            }
        """
    )

    @Test
    fun testKeyInIfAndCallsAfter(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                if (x > 0) {
                    key(x) {
                        A(a)
                    }
                    A(b)
                }
            }
        """
    )

    @Test
    fun testKeyInIfAndCallsBefore(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                if (x > 0) {
                    A(a)
                    key(x) {
                        A(b)
                    }
                }
            }
        """
    )

    @Test
    fun testKeyWithLotsOfValues(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(a: Int, b: Int, c: Int, d: Int) {
                key(a, b, c, d) {
                    A()
                }
            }
        """
    )

    @Test
    fun testKeyWithComposableValue(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                while(x > 0) {
                    key(R()) {
                        A()
                    }
                }
            }
        """
    )

    @Test
    fun testKeyAsAValue(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int) {
                val y = key(x) { R() }
                P(y)
            }
        """
    )

    @Test
    fun testDynamicWrappingGroupWithReturnValue(): Unit = controlFlow(
        """
            @NonRestartableComposable @Composable
            fun Example(x: Int): Int {
                return if (x > 0) {
                    if (B()) 1
                    else if (B()) 2
                    else 3
                } else 4
            }
        """
    )

    @Test
    fun testTheThing(): Unit = controlFlow(
        """
            @NonRestartableComposable
            @Composable
            fun Simple() {
              // this has a composable call in it, and since we don't know the number of times the
              // lambda will get called, we place a group around the whole call
              run {
                A()
              }
              A()
            }

            @NonRestartableComposable
            @Composable
            fun WithReturn() {
              // this has an early return in it, so it needs to end all of the groups present.
              run {
                A()
                return@WithReturn
              }
              A()
            }

            @NonRestartableComposable
            @Composable
            fun NoCalls() {
              // this has no composable calls in it, so shouldn't cause any groups to get created
              run {
                println("hello world")
              }
              A()
            }

            @NonRestartableComposable
            @Composable
            fun NoCallsAfter() {
              // this has a composable call in the lambda, but not after it, which means the
              // group should be able to be coalesced into the group of the function
              run {
                A()
              }
            }
        """
    )

    @Test
    fun testLetWithComposableCalls(): Unit = controlFlow(
        """
            @Composable
            fun Example(x: Int?) {
              x?.let {
                if (it > 0) {
                  A(a)
                }
                A(b)
              }
              A(c)
            }
        """
    )

    @Test
    fun testLetWithoutComposableCalls(): Unit = controlFlow(
        """
            @Composable
            fun Example(x: Int?) {
              x?.let {
                if (it > 0) {
                  NA()
                }
                NA()
              }
              A()
            }
        """
    )

    @Test
    fun testApplyOnComposableCallResult(): Unit = controlFlow(
        """
            import androidx.compose.runtime.mutableStateOf
            import androidx.compose.runtime.remember
            import androidx.compose.runtime.State

            @Composable
            fun <T> provided(value: T): State<T> = remember { mutableStateOf(value) }.apply {
                this.value = value
            }
        """
    )

    @Test
    fun testReturnInlinedExpressionWithCall(): Unit = controlFlow(
        """
            import androidx.compose.runtime.mutableStateOf
            import androidx.compose.runtime.remember
            import androidx.compose.runtime.State

            @Composable
            fun Test(x: Int): Int {
                return x.let {
                    A()
                    123
                }
            }
        """
    )

    @Test
    fun testCallingAWrapperComposable(): Unit = controlFlow(
        """
            @Composable
            fun Test() {
              W {
                A()
              }
            }
        """
    )

    @Test
    fun testCallingAnInlineWrapperComposable(): Unit = controlFlow(
        """
            @Composable
            fun Test() {
              IW {
                A()
              }
            }
        """
    )

    @Test
    fun testRepeatedCallsToEffects(): Unit = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            @Composable
            fun Test() {
                Wrap {
                    repeat(number) {
                        effects[it] = effect { 0 }
                    }
                    outside = effect { "0" }
                }
            }
        """,
        """
            import androidx.compose.runtime.Composable

            var effects = mutableListOf<Any>()
            var outside: Any = ""
            var number = 1

            @Composable fun Wrap(block: @Composable () -> Unit) =  block()
            @Composable fun <T> effect(block: () -> T): T = block()
        """

    )

    @Test
    fun testComposableWithInlineClass(): Unit = controlFlow(
        """
            @Composable
            fun Test(value: InlineClass) {
                used(value)
                A()
            }
        """
    )

    @Test
    fun testParameterOrderInformation(): Unit = controlFlow(
        """
            @Composable fun Test01(p0: Int, p1: Int, p2: Int, p3: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test02(p0: Int, p1: Int, p3: Int, p2: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test03(p0: Int, p2: Int, p1: Int, p3: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test04(p0: Int, p2: Int, p3: Int, p1: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test05(p0: Int, p3: Int, p1: Int, p2: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test06(p0: Int, p3: Int, p2: Int, p1: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test07(p1: Int, p0: Int, p2: Int, p3: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test08(p1: Int, p0: Int, p3: Int, p2: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test09(p1: Int, p2: Int, p0: Int, p3: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test00(p1: Int, p2: Int, p3: Int, p0: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test11(p1: Int, p3: Int, p0: Int, p2: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test12(p1: Int, p3: Int, p2: Int, p0: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test13(p2: Int, p0: Int, p1: Int, p3: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test14(p2: Int, p0: Int, p3: Int, p1: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test15(p2: Int, p1: Int, p0: Int, p3: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test16(p2: Int, p1: Int, p3: Int, p0: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test17(p2: Int, p3: Int, p0: Int, p1: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test18(p2: Int, p3: Int, p1: Int, p0: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test19(p3: Int, p0: Int, p1: Int, p2: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test20(p3: Int, p0: Int, p2: Int, p1: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test21(p3: Int, p1: Int, p0: Int, p2: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test22(p3: Int, p1: Int, p2: Int, p0: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test23(p3: Int, p2: Int, p0: Int, p1: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
            @Composable fun Test24(p3: Int, p2: Int, p1: Int, p0: Int) {
                used(p0)
                used(p1)
                used(p2)
                used(p3)
            }
        """
    )

    @Test
    fun testSourceInformationWithPackageName(): Unit = verifyGoldenComposeIrTransform(
        source = """
            package androidx.compose.runtime.tests

            import androidx.compose.runtime.Composable

            @Composable
            fun Test(value: LocalInlineClass) {
                used(value)
            }
        """,
        extra = """
            package androidx.compose.runtime.tests

            inline class LocalInlineClass(val value: Int)
            fun used(x: Any?) {}
        """,
        truncateTracingInfoMode = TruncateTracingInfoMode.KEEP_INFO_STRING
    )

    @Test
    fun testSourceOffsetOrderForParameterExpressions(): Unit = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.Composable

            @Composable
            fun Test() {
                A(b(), c(), d())
                B()
            }
        """,
        extra = """
            import androidx.compose.runtime.Composable

            @Composable fun A(a: Int, b: Int, c: Int) { }
            @Composable fun B() { }
            @Composable fun b(): Int = 1
            @Composable fun c(): Int = 1
            @Composable fun d(): Int = 1
        """,
        truncateTracingInfoMode = TruncateTracingInfoMode.KEEP_INFO_STRING
    )

    @Test
    fun testSourceLocationOfCapturingComposableLambdas(): Unit = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.Composable

            class SomeClass {
                var a = "Test"
                fun onCreate() {
                    setContent {
                        B(a)
                        B(a)
                    }
                }
            }

            fun Test() {
                var a = "Test"
                setContent {
                    B(a)
                    B(a)
                }
            }
        """,
        extra = """
            import androidx.compose.runtime.Composable

            fun setContent(block: @Composable () -> Unit) { }
            @Composable fun B(value: String) { }
        """,
        truncateTracingInfoMode = TruncateTracingInfoMode.KEEP_INFO_STRING
    )

    @Test
    fun testSourceLineInformationForNormalInline(): Unit = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.Composable

            @Composable
            fun Test() {
              W {
                IW {
                    T(2)
                    repeat(3) {
                        T(3)
                    }
                    T(4)
                }
              }
            }
        """,
        extra = """
            import androidx.compose.runtime.Composable

            @Composable fun W(block: @Composable () -> Unit) = block()
            @Composable inline fun IW(block: @Composable () -> Unit) = block()
            @Composable fun T(value: Int) { }
        """,
        truncateTracingInfoMode = TruncateTracingInfoMode.KEEP_INFO_STRING
    )

    @Test
    fun testInlineReadOnlySourceLocations() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.ReadOnlyComposable

            val current
                @Composable
                @ReadOnlyComposable
                get() = 0

            @Composable
            @ReadOnlyComposable
            fun calculateSometing(): Int {
                return 0;
            }

            @Composable
            fun Test() {
                val c = current
                val cl = calculateSometing()
                Layout {
                    Text("${'$'}c ${'$'}cl")
                }
            }
        """,
        """
            import androidx.compose.runtime.Composable

            @Composable
            inline fun Layout(content: @Composable () -> Unit) { content() }

            @Composable
            fun Text(text: String) { }
        """,
        truncateTracingInfoMode = TruncateTracingInfoMode.KEEP_INFO_STRING
    )

    @Test
    fun testReadOnlyInlineValSourceLocations() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.ReadOnlyComposable

            class CurrentHolder {
                inline val current: Int
                    @ReadOnlyComposable
                    @Composable
                    get() = 0
            }

            class HolderHolder {
                private val _currentHolder = CurrentHolder()
                val current: Int
                    @ReadOnlyComposable
                    @Composable
                    get() = _currentHolder.current
            }

            val holderHolder = HolderHolder()

            @Composable
            @ReadOnlyComposable
            fun calculateSomething(): Int {
                return 0;
            }

            @Composable
            fun Test() {
                val c = holderHolder.current
                val cl = calculateSomething()
                Layout {
                    Text("${'$'}c ${'$'}cl")
                }
            }
        """,
        """
            import androidx.compose.runtime.Composable

            @Composable
            inline fun Layout(content: @Composable () -> Unit) { content() }

            @Composable
            fun Text(text: String) { }
        """,
        truncateTracingInfoMode = TruncateTracingInfoMode.KEEP_INFO_STRING
    )

    @Test
    fun testReadOnlyComposableWithEarlyReturn() = controlFlow(
        source = """
            @ReadOnlyComposable
            @Composable
            fun getSomeValue(a: Int): Int {
                if (a < 100) return 0
                return 1
            }
        """
    )

    @Test
    fun testMultipleNestedInlines() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.Composable

            @Composable
            fun AttemptedToRealizeGroupTwice() {
                Wrapper {
                    repeat(1) {
                        repeat(1) {
                            Leaf(0)
                        }
                        Leaf(0)
                    }
                }
            }
        """,
        extra = """
            import androidx.compose.runtime.Composable

            @Composable
            inline fun Wrapper(content: @Composable () -> Unit) { }

            @Composable
            fun Leaf(default: Int = 0) {}
        """
    )

    @Test // Regression test for 205590513
    fun testGroupAroundExtensionFunctions() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Test(start: Int, end: Int) {
                val a = remember { A() }
                for (i in start until end) {
                    val b = a.get(bKey)
                    if (i == 2) {
                        a.get(cKey)
                    }
                }
            }
        """,
        extra = """
            import androidx.compose.runtime.*

            class A

            class B

            class C

            val bKey: () -> B = { B() }
            val cKey: () -> C = { C() }

            @Composable
            fun <T> A.get(block: () -> T) = block()
        """
    )

    // There are a number of "inline constructors" in the Kotlin standard library for Array types.
    // These are special cases, since normal constructors cannot be inlined.
    @Test
    fun testInlineArrayConstructor() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.*

            @Composable
            fun ArrayConstructorTest(n: Int) {
                Array(n) { remember { it } }
                ByteArray(n) { remember { it.toByte() } }
                CharArray(n) { remember { it.toChar() } }
                ShortArray(n) { remember { it.toShort() } }
                IntArray(n) { remember { it } }
                LongArray(n) { remember { it.toLong() } }
                FloatArray(n) { remember { it.toFloat() } }
                DoubleArray(n) { remember { it.toDouble() } }
                BooleanArray(n) { remember { false } }
            }
        """
    )

    @Test
    fun testComposeIrSkippingWithDefaultsRelease() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.*
            import androidx.compose.foundation.layout.*

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
        """,
        extra = """
            import androidx.compose.runtime.Composable

            @Composable
            fun Text(
                text: String,
                softWrap: Boolean = true,
                maxLines: Int = Int.MAX_VALUE,
                minLines: Int = 1,
            ) {}
        """
    )

    @Test
    fun testRememberInConditionalCallArgument() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable
            private fun Test(param: String?) {
                Test(
                    if (param == null) {
                       remember { "" }
                    } else {
                        null
                    },
                )
            }
        """
    )

    @Test
    fun testRememberInNestedConditionalCallArgument() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable
            private fun Test(param: String?): String? {
                return Test(
                    if (param == null) {
                       Test(
                            if (param == null) {
                                remember { "" }
                            } else {
                                null
                            }
                       )
                    } else {
                        null
                    },
                )
            }
        """
    )

    @Test
    fun testInlineLambdaBeforeACall() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable
            private fun Test(param: String?): String? {
                InlineNonComposable {
                    repeat(10) {
                        Test("InsideInline")
                    }
                }
                return Test("AfterInline")
            }
        """,
        extra = """
            import androidx.compose.runtime.*

            inline fun InlineNonComposable(block: () -> Unit) {}
        """
    )

    @Test
    fun testInlineLambda_nonLocalReturn() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable
            private fun Test(param: String?) {
                Inline1 {
                    Inline2 {
                        if (true) return@Inline1
                    }
                }
            }
        """,
        extra = """
            import androidx.compose.runtime.*

            @Composable
            inline fun Inline1(block: @Composable () -> Unit) {
                block()
            }

            @Composable
            inline fun Inline2(block: @Composable () -> Unit) {
                block()
            }
        """
    )

    @Test
    fun testNothingBody() = verifyGoldenComposeIrTransform(
        source = """
        import androidx.compose.runtime.*

        val test1: @Composable () -> Unit = TODO()

        @Composable
        fun Test2(): Unit = TODO()

        @Composable
        fun Test3() {
            Wrapper {
                TODO()
            }
        }
        """,
        extra = """
        import androidx.compose.runtime.*

        @Composable
        fun Wrapper(content: @Composable () -> Unit) = content()
        """
    )

    @Test
    fun testEarlyReturnFromCrossInlinedLambda() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable
            private fun Test(param: String?) {
                Dialog {
                    if (false) Test(param)
                }
            }
        """,
        extra = """
            import androidx.compose.runtime.*

            @Composable
            internal inline fun Dialog(crossinline block: @Composable () -> Unit) {}
        """.trimIndent(),
    )

    @Test
    fun testEarlyReturnFromWhenStatement() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable
            private fun Test(param: String?) {
                val state = remember { mutableStateOf(false) }
                when (state.value) {
                    true -> return Text(text = "true")
                    else -> Text(text = "false")
                }
            }
        """,
        extra = """
            import androidx.compose.runtime.*

            @Composable fun Text(text: String) {}
        """
    )

    @Test
    fun testComposableInAnonymousObjectDelegate() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

                interface A

                interface B {
                    val property: A @Composable get() = TODO()
                }

                @Composable fun Test(b: B) {
                    val a = object : A by b.property {}
                    println(a)
                }
        """
    )

    @Test
    fun testReturnNull() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Test(): String? {
                return null
            }
            @Composable
            fun Test2(b: Boolean): String? {
                if (b) return "true"
                return null
            }
            @Composable
            fun Test3(b: Boolean): String? {
                if (b) {
                    return "true"
                } else {
                    return null
                }
            }
            @Composable
            fun Test4(b: Boolean): String? {
                return if (b) {
                    "true"
                } else {
                    null
                }
            }
            @Composable
            fun Test5(): String? {
                var varNull = null
                return varNull
            }
            @Composable
            fun Test6(): String? {
                TODO()
            }
            @Composable
            fun Test7(b: Boolean): String? {
                if (b) {
                    return null
                }
                return "false"
            }
            @Composable
            fun Test8(): Unit? {
                var unitNull: Unit? = null
                Test6()
                return unitNull
            }
            @Composable
            fun Test9(): Unit? {
                var unitNotNull: Unit? = Unit
                Test6()
                return unitNotNull
            }
            @Composable
            fun Test10(): Unit? {
                Test6()
                return Unit
            }
        """.trimIndent()
    )

    @Test
    fun testGroupsInLoops() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.*

            @Composable
            private fun KeyContent1(items: List<Int>) {
                items.forEach { item ->
                    if (item > -1) {
                        key(item) {
                            remember {
                                item
                            }
                        }
                    }
                }
            }

            @Composable
            private fun KeyContent2(items: List<Int>) {
                for (item in items) {
                    if (item > -1) {
                        key(item) {
                            remember {
                                item
                            }
                        }
                    }
                }
            }
        """
    )

    @Test
    fun testIfWithEarlyReturnInsideInlineLambda() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable

            @Composable fun Test() {
                run {
                    if (true) {
                        return@run
                    } else {
                        Test()
                        return@run
                    }
                }
            }
        """
    )

    @Test
    fun testLambdaWithNonUnitResult() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.*

            @Composable
            fun Test() {
                val factory = createFactory {
                    10
                }
                factory()
            }
        """,
        extra = """
            import androidx.compose.runtime.*

            fun createFactory(factory: @Composable () -> Int) = factory
        """
    )

    @Test
    fun testOverrideWithNonUnitResult() = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.*

            class SomeClassImpl: SomeClass() {
                @Composable
                override fun SomeFunction(): Int = 10
            }
        """,
        """
            import androidx.compose.runtime.*

            abstract class SomeClass {
                @Composable
                abstract fun SomeFunction(): Int
            }
        """
    )

    @Test
    fun testConditionalReturnFromInline() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*

            @Composable inline fun Column(content: @Composable () -> Unit) {}
            inline fun NonComposable(content: () -> Unit) {}
            @Composable fun Text(text: String) {}
        """,
        source = """
            import androidx.compose.runtime.*

            @Composable fun Test(test: Boolean) {
                Column {
                   if (!test) {
                       Text("Say")
                       return@Column
                   }
                   Text("Hello")
                }

                NonComposable {
                    if (!test) {
                       Text("Say")
                       return@NonComposable
                   }
                   Text("Hello")
                }
            }
        """
    )

    @Test
    fun ifInsideInlineComposableFunction() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*

            fun interface MeasurePolicy {
                fun invoke(size: Int)
            }
            @Composable inline fun Layout(content: @Composable () -> Unit) {}
            @Composable fun Box() {}
        """,
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Label(test: Boolean) {
                Layout(
                    content = {
                        Box()
                        if (test) {
                            Box()
                        }
                    }
                )
            }
        """
    )
}
