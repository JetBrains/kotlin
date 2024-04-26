/*
 * Copyright 2021 The Android Open Source Project
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

import org.junit.Ignore
import org.junit.Test

@Suppress("SpellCheckingInspection") // Expected strings can have partial words
class TargetAnnotationsTransformTests(useFir: Boolean) : AbstractIrTransformTest(useFir) {
    @Test
    fun testInferUIFromCall() = verify(
        """
        import androidx.compose.runtime.Composable

        @Composable
        fun Test() {
            Text("Hello")
        }
        """
    )

    @Test
    fun testInferVectorFromCall() = verify(
        """
        import androidx.compose.runtime.Composable

        @Composable
        fun Test() {
            Circle()
        }
        """
    )

    // No annotations is the same as leaving the applier open.
    @Test
    fun testInferSimpleOpen() = verify(
        """
        import androidx.compose.runtime.Composable

        @Composable
        fun Test() { }
        """
    )

    @Test
    fun testInferUnifiedParameters() = verify(
        """
        import androidx.compose.runtime.Composable

        @Composable
        fun Test(content: @Composable () -> Unit) {
          content()
        }
        """
    )

    @Test
    fun testInferLambdaParameter() = verify(
        """
        import androidx.compose.runtime.Composable

        @Composable
        fun Test(content: @Composable () -> Unit) {
          Row {
            Text("test")
          }
        }
        """
    )

    @Test
    fun testInferInlineLambdaParameter() = verify(
        """
        import androidx.compose.runtime.Composable

        @Composable
        fun Test(content: @Composable () -> Unit) {
          InlineRow {
            Text("test")
          }
        }
        """
    )

    @Test
    fun testCanInferWithGeneric() = verify(
        """
        import androidx.compose.runtime.Composable

        @Composable
        fun Test() {
          Wrapper {
            Text("test")
          }
        }
        """
    )

    @Test
    fun testCompositionLocalsProvider() = verify(
        """
        import androidx.compose.runtime.Composable
        import androidx.compose.runtime.CompositionLocalProvider

        @Composable
        fun Test() {
          CompositionLocalProvider {
            Text("test")
          }
        }
        """
    )

    @Test
    fun testInferringFunInterfaceParameterAnnotations() = verify(
        """
        import androidx.compose.runtime.Composable

        fun interface CustomComposable {
            @Composable
            fun call()
        }

        @Composable
        fun OpenCustom(content: CustomComposable) {
            content.call()
        }

        @Composable
        fun ClosedCustom(content: CustomComposable) {
            Text("Test")
            content.call()
        }

        @Composable
        fun Test() {
            OpenCustom {
                Text("Test")
            }
            ClosedCustom  {
                Text("Test")
            }
        }
        """
    )

    @Test
    fun testLetIt() = verifyGoldenComposeIrTransform(
        """
        import androidx.compose.runtime.*

        @Composable
        fun Test(content: (@Composable () -> Unit?)) {
            content?.let { it() }
        }
        """
    )

    @Test
    fun testOptionalParameters() = verifyGoldenComposeIrTransform(
        """
        import androidx.compose.runtime.*

        @Composable
        @ComposableTarget("UI")
        fun Leaf() { }

        @Composable
        fun Wrapper(content: @Composable () -> Unit) { content() }

        // [0,[0],[0],[0],[0],[0],[0],[0]]
        @Composable
        fun Optional(
            one: @Composable () -> Unit = { },
            two: (@Composable () -> Unit)? = null,
            three: (@Composable () -> Unit)? = null,
            four: (@Composable () -> Unit)? = null,
            five: (@Composable () -> Unit)? = null,
            six: (@Composable () -> Unit)? = null,
            content: @Composable () -> Unit
        ) {
            one()

            // Invoke through a ?.
            two?.invoke()

            // Invoke through a let
            three?.let { it() }

            // Invoke through a let test
            four?.let { four() }

            // Invoke through in an then block
            if (five != null)
                five()

            six?.let { it -> Wrapper(it) }

            content()
        }

        @Composable
        fun UseOptional() {
            Optional {
                Leaf()
            }
        }
        """
    )

    @Test
    fun testReceiverScope() = verifyGoldenComposeIrTransform(
        """
        import androidx.compose.runtime.*
        import androidx.compose.ui.*
        import androidx.compose.ui.layout.*

        @Immutable
        interface LocalBoxScope {
            @Stable
            fun Modifier.align(alignment: Alignment): Modifier
        }

        object LocalBoxScopeInstance : LocalBoxScope {
            override fun Modifier.align(alignment: Alignment): Modifier = Modifier
        }

        val localBoxMeasurePolicy = MeasurePolicy { _, constraints ->
            layout(
                constraints.minWidth,
                constraints.minHeight
            ) {}
        }

        @Composable
        inline fun LocalBox(
            modifier: Modifier = Modifier,
            content: @Composable LocalBoxScope.() -> Unit
        ) {
            Layout(
                modifier = modifier,
                measurePolicy = localBoxMeasurePolicy,
                content = { LocalBoxScopeInstance.content() }
            )
        }
        """
    )

    @Test
    fun testCallingLayout() = verifyGoldenComposeIrTransform(
        """
        import androidx.compose.runtime.*
        import androidx.compose.ui.layout.*
        import androidx.compose.ui.text.*
        import androidx.compose.foundation.text.*

        @Composable
        fun Test1() {
            Layout(content = { }) { _, _ -> error("") }
        }

        @Composable
        fun Test2(content: @Composable ()->Unit) {
            Layout(content = content) { _, _ -> error("") }
        }

        @Composable
        fun Test3() {
          Test1()
        }

        @Composable
        fun Test4() {
          BasicText(text = AnnotatedString("Some text"))
        }

        val Local = compositionLocalOf { 0 }

        @Composable
        fun Test5(content: @Composable () -> Unit) {
          CompositionLocalProvider(Local provides 5) {
              Test1()
              content()
          }
        }

        @Composable
        fun Test6(test: String) {
          CompositionLocalProvider(Local provides 6) {
             T(test)
             Test1()
          }
        }

        @Composable
        fun T(value: String) { }
        """
    )

    @Suppress("unused")
    fun testCollectAsState() = verifyGoldenComposeIrTransform(
        """
            import kotlin.coroutines.*
            import kotlinx.coroutines.flow.*
            import androidx.compose.runtime.*

            @Composable
            fun <T> StateFlow<T>.collectAsState(
                context: CoroutineContext = EmptyCoroutineContext
            ): State<T> = collectAsState(value, context)

            @Composable
            fun <T : R, R> Flow<T>.collectAsState(
                initial: R,
                context: CoroutineContext = EmptyCoroutineContext
            ): State<R> = mutableStateOf(initial)
        """
    )

    @Test
    fun testRememberUpdatedState() = verifyGoldenComposeIrTransform(
        source = """
        import androidx.compose.runtime.*

        @Composable
        fun Test(content: @Composable () -> Unit) {
            val updatedContent by rememberUpdatedState(content)
            Defer {
                UiContent {
                    updatedContent()
                }
            }
        }
        """,
        extra = """
        import androidx.compose.runtime.*

        fun Defer(content: @Composable () -> Unit) { }

        fun UiContent(content: @Composable @ComposableTarget("UI") () -> Unit) { }
        """
    )

    @Test
    fun testAddingComposablesToAList() = verifyGoldenComposeIrTransform(
        """
        import androidx.compose.runtime.*

        class Scope {
            private val list = IntervalList<Scope.(Int) -> (@Composable () -> Unit)>()
            fun item(content: @Composable Scope.() -> Unit) {
                list.add(1) { @Composable { content() } }
            }
        }
        """,
        extra = """
        class IntervalList<T> {
            fun add(size: Int, content: T) { }
        }
        """
    )

    @Test
    fun testCallingNullableComposableWithNull() = verifyGoldenComposeIrTransform(
        """
        import androidx.compose.runtime.*

        @Composable
        fun Test() {
            Widget(null)
        }
        """,
        extra = """
        import androidx.compose.runtime.*

        @Composable
        fun Widget(content: (@Composable () -> Unit)?) {
            if (content != null) content()
        }
        """
    )

    @Test
    fun testCallingComposableParameterWithComposableParameter() = verify(
        """
        import androidx.compose.runtime.*

        @Composable
        fun Test(decorator: @Composable (content: @Composable () -> Unit) -> Unit) {
            decorator {
              Text("Some text")
            }
        }
        """
    )

    @Test
    fun testFileScoped() = verifyGoldenComposeIrTransform(
        source = """
            @file:NComposable

            import androidx.compose.runtime.*

            @Composable
            fun NFromFile() {
                Open()
            }

            @Composable
            fun NFromInference() {
                N()
            }

        """,
        extra = """
            import androidx.compose.runtime.*

            @ComposableTargetMarker(description = "An N Composable")
            @Target(
                AnnotationTarget.FILE,
                AnnotationTarget.FUNCTION,
                AnnotationTarget.PROPERTY_GETTER,
                AnnotationTarget.TYPE,
                AnnotationTarget.TYPE_PARAMETER,
            )
            annotation class NComposable()

            @Composable @ComposableOpenTarget(0) fun Open() { }
            @Composable @NComposable fun N() { }
        """.trimIndent()
    )

    @Test
    fun testCrossfileFileScope() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun InferN() { N() }
        """,
        extra = """
            @file:NComposable

            import androidx.compose.runtime.*

            @ComposableTargetMarker(description = "An N Composable")
            @Target(
                AnnotationTarget.FILE,
                AnnotationTarget.FUNCTION,
                AnnotationTarget.PROPERTY_GETTER,
                AnnotationTarget.TYPE,
                AnnotationTarget.TYPE_PARAMETER,
            )
            annotation class NComposable()

            @Composable fun N() { }
        """
    )

    @Test
    fun testInferringTargetFromAncestorMethod() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.ComposableTarget
            import androidx.compose.runtime.ComposableOpenTarget

            @Composable @ComposableOpenTarget(0) fun OpenTarget() { }

            abstract class Base {
              @Composable @ComposableTarget("N") abstract fun Compose()
            }

            class Valid : Base () {
              @Composable override fun Compose() {
                OpenTarget()
              }
            }
        """
    )

    private fun verify(source: String) =
        verifyGoldenComposeIrTransform(source, baseDefinition)

    private val baseDefinition = """
        import androidx.compose.runtime.Composable
        import androidx.compose.runtime.ComposableTarget
        import androidx.compose.runtime.ComposableOpenTarget
        import androidx.compose.runtime.Applier

        @Composable
        @ComposableTarget("UI")
        fun Layout() { }

        @Composable
        @ComposableTarget("UI")
        fun Layout(content: @Composable @ComposableTarget("UI") () -> Unit) { }

        @Composable
        @ComposableTarget("UI")
        inline fun InlineLayout(content: @Composable @ComposableTarget("UI") () -> Unit) { }


        @Composable
        fun Text(text: String) { Layout() }

        @Composable
        fun Row(content: @Composable () -> Unit) {
            Layout(content)
        }

        @Composable
        inline fun InlineRow(content: @Composable () -> Unit) {
            InlineLayout(content)
        }

        @Composable
        @ComposableTarget("Vector")
        fun Vector() { }

        @Composable
        fun Circle() { Vector() }

        @Composable
        fun Square() { Vector() }

        @Composable
        @ComposableTarget("Vector")
        fun Vector(content: @Composable @ComposableTarget("Vector") () -> Unit) { }

        @Composable
        fun Layer(content: @Composable () -> Unit) { Vector(content) }

        @Composable
        @ComposableTarget("UI")
        fun Drawing(content: @Composable @ComposableTarget("Vector") () -> Unit) { }

        @Composable
        fun Wrapper(content: @Composable () -> Unit) { content() }
    """
}
