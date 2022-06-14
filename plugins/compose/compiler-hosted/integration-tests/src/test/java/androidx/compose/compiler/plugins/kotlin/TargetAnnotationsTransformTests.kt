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

import org.junit.Test

@Suppress("SpellCheckingInspection") // Expected strings can have partial words
class TargetAnnotationsTransformTests : ComposeIrTransformTest() {
    @Test
    fun testInferUIFromCall() = verify(
        """
        import androidx.compose.runtime.Composable

        @Composable
        fun Test() {
            Text("Hello")
        }
        """,
        """
        @Composable
        @ComposableTarget(applier = "UI")
        fun Test(%composer: Composer?, %changed: Int) {
          if (isTraceInProgress()) {
            traceEventStart(<>)
          }
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(Test)<Text("...>:Test.kt")
          if (%changed !== 0 || !%composer.skipping) {
            Text("Hello", %composer, 0b0110)
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            Test(%composer, %changed or 0b0001)
          }
          if (isTraceInProgress()) {
            traceEventEnd()
          }
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
        """,
        """
        @Composable
        @ComposableTarget(applier = "Vector")
        fun Test(%composer: Composer?, %changed: Int) {
          if (isTraceInProgress()) {
            traceEventStart(<>)
          }
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(Test)<Circle...>:Test.kt")
          if (%changed !== 0 || !%composer.skipping) {
            Circle(%composer, 0)
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            Test(%composer, %changed or 0b0001)
          }
          if (isTraceInProgress()) {
            traceEventEnd()
          }
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
        """,
        """
        @Composable
        fun Test(%composer: Composer?, %changed: Int) {
          if (isTraceInProgress()) {
            traceEventStart(<>)
          }
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(Test):Test.kt")
          if (%changed !== 0 || !%composer.skipping) {
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            Test(%composer, %changed or 0b0001)
          }
          if (isTraceInProgress()) {
            traceEventEnd()
          }
        }
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
        """,
        """
        @Composable
        @ComposableInferredTarget(scheme = "[0[0]]")
        fun Test(content: Function2<Composer, Int, Unit>, %composer: Composer?, %changed: Int) {
          if (isTraceInProgress()) {
            traceEventStart(<>)
          }
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(Test)<conten...>:Test.kt")
          val %dirty = %changed
          if (%changed and 0b1110 === 0) {
            %dirty = %dirty or if (%composer.changed(content)) 0b0100 else 0b0010
          }
          if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
            content(%composer, 0b1110 and %dirty)
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            Test(content, %composer, %changed or 0b0001)
          }
          if (isTraceInProgress()) {
            traceEventEnd()
          }
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
        """,
        """
        @Composable
        @ComposableInferredTarget(scheme = "[UI[_]]")
        fun Test(content: Function2<Composer, Int, Unit>, %composer: Composer?, %changed: Int) {
          if (isTraceInProgress()) {
            traceEventStart(<>)
          }
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(Test)<Row>:Test.kt")
          if (%changed and 0b0001 !== 0 || !%composer.skipping) {
            Row(ComposableSingletons%TestKt.lambda-1, %composer, 0b0110)
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            Test(content, %composer, %changed or 0b0001)
          }
          if (isTraceInProgress()) {
            traceEventEnd()
          }
        }
        internal object ComposableSingletons%TestKt {
          val lambda-1: Function2<Composer, Int, Unit> = composableLambdaInstance(<>, false) { %composer: Composer?, %changed: Int ->
            sourceInformation(%composer, "C<Text("...>:Test.kt")
            if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
              Text("test", %composer, 0b0110)
            } else {
              %composer.skipToGroupEnd()
            }
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
        """,
        """
        @Composable
        @ComposableInferredTarget(scheme = "[UI[_]]")
        fun Test(content: Function2<Composer, Int, Unit>, %composer: Composer?, %changed: Int) {
          if (isTraceInProgress()) {
            traceEventStart(<>)
          }
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(Test)<Inline...>:Test.kt")
          if (%changed and 0b0001 !== 0 || !%composer.skipping) {
            InlineRow({ %composer: Composer?, %changed: Int ->
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C<Text("...>:Test.kt")
              if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                Text("test", %composer, 0b0110)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endReplaceableGroup()
            }, %composer, 0)
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            Test(content, %composer, %changed or 0b0001)
          }
          if (isTraceInProgress()) {
            traceEventEnd()
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
        """,
        """
        @Composable
        @ComposableTarget(applier = "UI")
        fun Test(%composer: Composer?, %changed: Int) {
          if (isTraceInProgress()) {
            traceEventStart(<>)
          }
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(Test)<Wrappe...>:Test.kt")
          if (%changed !== 0 || !%composer.skipping) {
            Wrapper(ComposableSingletons%TestKt.lambda-1, %composer, 0b0110)
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            Test(%composer, %changed or 0b0001)
          }
          if (isTraceInProgress()) {
            traceEventEnd()
          }
        }
        internal object ComposableSingletons%TestKt {
          val lambda-1: Function2<Composer, Int, Unit> = composableLambdaInstance(<>, false) { %composer: Composer?, %changed: Int ->
            sourceInformation(%composer, "C<Text("...>:Test.kt")
            if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
              Text("test", %composer, 0b0110)
            } else {
              %composer.skipToGroupEnd()
            }
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
        """,
        """
        @Composable
        @ComposableTarget(applier = "UI")
        fun Test(%composer: Composer?, %changed: Int) {
          if (isTraceInProgress()) {
            traceEventStart(<>)
          }
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(Test)<Compos...>:Test.kt")
          if (%changed !== 0 || !%composer.skipping) {
            CompositionLocalProvider(
              content = ComposableSingletons%TestKt.lambda-1,
              %composer = %composer,
              %changed = 56
            )
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            Test(%composer, %changed or 0b0001)
          }
          if (isTraceInProgress()) {
            traceEventEnd()
          }
        }
        internal object ComposableSingletons%TestKt {
          val lambda-1: Function2<Composer, Int, Unit> = composableLambdaInstance(<>, false) { %composer: Composer?, %changed: Int ->
            sourceInformation(%composer, "C<Text("...>:Test.kt")
            if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
              Text("test", %composer, 0b0110)
            } else {
              %composer.skipToGroupEnd()
            }
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
        """,
        """
        interface CustomComposable {
          @Composable
          abstract fun call(%composer: Composer?, %changed: Int)
        }
        @Composable
        @ComposableInferredTarget(scheme = "[0[0]]")
        fun OpenCustom(content: CustomComposable, %composer: Composer?, %changed: Int) {
          if (isTraceInProgress()) {
            traceEventStart(<>)
          }
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(OpenCustom)<call()>:Test.kt")
          val %dirty = %changed
          if (%changed and 0b1110 === 0) {
            %dirty = %dirty or if (%composer.changed(content)) 0b0100 else 0b0010
          }
          if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
            content.call(%composer, 0b1110 and %dirty)
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            OpenCustom(content, %composer, %changed or 0b0001)
          }
          if (isTraceInProgress()) {
            traceEventEnd()
          }
        }
        @Composable
        @ComposableInferredTarget(scheme = "[UI[UI]]")
        fun ClosedCustom(content: CustomComposable, %composer: Composer?, %changed: Int) {
          if (isTraceInProgress()) {
            traceEventStart(<>)
          }
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(ClosedCustom)<Text("...>,<call()>:Test.kt")
          val %dirty = %changed
          if (%changed and 0b1110 === 0) {
            %dirty = %dirty or if (%composer.changed(content)) 0b0100 else 0b0010
          }
          if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
            Text("Test", %composer, 0b0110)
            content.call(%composer, 0b1110 and %dirty)
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            ClosedCustom(content, %composer, %changed or 0b0001)
          }
          if (isTraceInProgress()) {
            traceEventEnd()
          }
        }
        @Composable
        @ComposableTarget(applier = "UI")
        fun Test(%composer: Composer?, %changed: Int) {
          if (isTraceInProgress()) {
            traceEventStart(<>)
          }
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(Test)<OpenCu...>,<Closed...>:Test.kt")
          if (%changed !== 0 || !%composer.skipping) {
            OpenCustom(class <no name provided> : CustomComposable {
              @Composable
              @ComposableTarget(applier = "UI")
              override fun call(%composer: Composer?, %changed: Int) {
                if (isTraceInProgress()) {
                  traceEventStart(<>)
                }
                %composer = %composer.startRestartGroup(<>)
                sourceInformation(%composer, "C(call)<Text("...>:Test.kt")
                if (%changed and 0b0001 !== 0 || !%composer.skipping) {
                  Text("Test", %composer, 0b0110)
                } else {
                  %composer.skipToGroupEnd()
                }
                val tmp0_rcvr = <this>
                %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                  tmp0_rcvr.call(%composer, %changed or 0b0001)
                }
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              }
            }
            <no name provided>(), %composer, 0)
            ClosedCustom(class <no name provided> : CustomComposable {
              @Composable
              @ComposableTarget(applier = "UI")
              override fun call(%composer: Composer?, %changed: Int) {
                if (isTraceInProgress()) {
                  traceEventStart(<>)
                }
                %composer = %composer.startRestartGroup(<>)
                sourceInformation(%composer, "C(call)<Text("...>:Test.kt")
                if (%changed and 0b0001 !== 0 || !%composer.skipping) {
                  Text("Test", %composer, 0b0110)
                } else {
                  %composer.skipToGroupEnd()
                }
                val tmp0_rcvr = <this>
                %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                  tmp0_rcvr.call(%composer, %changed or 0b0001)
                }
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              }
            }
            <no name provided>(), %composer, 0)
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            Test(%composer, %changed or 0b0001)
          }
          if (isTraceInProgress()) {
            traceEventEnd()
          }
        }
        """
    )

    fun testLetIt() = verifyComposeIrTransform(
        """
        import androidx.compose.runtime.*

        @Composable
        fun Test(content: (@Composable () -> Unit?)) {
            content?.let { it() }
        }
        """,
        """
        @Composable
        @ComposableInferredTarget(scheme = "[0[0]]")
        fun Test(content: Function2<Composer, Int, Unit?>, %composer: Composer?, %changed: Int) {
          if (isTraceInProgress()) {
            traceEventStart(<>)
          }
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(Test)*<it()>:Test.kt")
          val %dirty = %changed
          if (%changed and 0b1110 === 0) {
            %dirty = %dirty or if (%composer.changed(content)) 0b0100 else 0b0010
          }
          if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
            val tmp0_safe_receiver = content
            val tmp1_group = when {
              tmp0_safe_receiver == null -> {
                null
              }
              else -> {
                val tmp0_group = tmp0_safe_receiver.let { it: Function2<Composer, Int, Unit?> ->
                  it(%composer, 0)
                }
                tmp0_group
              }
            }
            tmp1_group
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            Test(content, %composer, %changed or 0b0001)
          }
          if (isTraceInProgress()) {
            traceEventEnd()
          }
        }
        """
    )

    @Test
    fun testOptionalParameters() = verifyComposeIrTransform(
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
        """,
        """
        @Composable
        @ComposableTarget(applier = "UI")
        fun Leaf(%composer: Composer?, %changed: Int) {
          if (isTraceInProgress()) {
            traceEventStart(<>)
          }
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(Leaf):Test.kt")
          if (%changed !== 0 || !%composer.skipping) {
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            Leaf(%composer, %changed or 0b0001)
          }
          if (isTraceInProgress()) {
            traceEventEnd()
          }
        }
        @Composable
        @ComposableInferredTarget(scheme = "[0[0]]")
        fun Wrapper(content: Function2<Composer, Int, Unit>, %composer: Composer?, %changed: Int) {
          if (isTraceInProgress()) {
            traceEventStart(<>)
          }
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(Wrapper)<conten...>:Test.kt")
          val %dirty = %changed
          if (%changed and 0b1110 === 0) {
            %dirty = %dirty or if (%composer.changed(content)) 0b0100 else 0b0010
          }
          if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
            content(%composer, 0b1110 and %dirty)
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            Wrapper(content, %composer, %changed or 0b0001)
          }
          if (isTraceInProgress()) {
            traceEventEnd()
          }
        }
        @Composable
        @ComposableInferredTarget(scheme = "[0[0][0][0][0][0][0][0]]")
        fun Optional(one: Function2<Composer, Int, Unit>?, two: Function2<Composer, Int, Unit>?, three: Function2<Composer, Int, Unit>?, four: Function2<Composer, Int, Unit>?, five: Function2<Composer, Int, Unit>?, six: Function2<Composer, Int, Unit>?, content: Function2<Composer, Int, Unit>, %composer: Composer?, %changed: Int, %default: Int) {
          if (isTraceInProgress()) {
            traceEventStart(<>)
          }
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(Optional)P(3,6,5,2,1,4)<one()>,<conten...>:Test.kt")
          val %dirty = %changed
          if (%default and 0b0001 !== 0) {
            %dirty = %dirty or 0b0110
          } else if (%changed and 0b1110 === 0) {
            %dirty = %dirty or if (%composer.changed(one)) 0b0100 else 0b0010
          }
          if (%default and 0b0010 !== 0) {
            %dirty = %dirty or 0b00110000
          } else if (%changed and 0b01110000 === 0) {
            %dirty = %dirty or if (%composer.changed(two)) 0b00100000 else 0b00010000
          }
          if (%default and 0b0100 !== 0) {
            %dirty = %dirty or 0b000110000000
          } else if (%changed and 0b001110000000 === 0) {
            %dirty = %dirty or if (%composer.changed(three)) 0b000100000000 else 0b10000000
          }
          if (%default and 0b1000 !== 0) {
            %dirty = %dirty or 0b110000000000
          } else if (%changed and 0b0001110000000000 === 0) {
            %dirty = %dirty or if (%composer.changed(four)) 0b100000000000 else 0b010000000000
          }
          if (%default and 0b00010000 !== 0) {
            %dirty = %dirty or 0b0110000000000000
          } else if (%changed and 0b1110000000000000 === 0) {
            %dirty = %dirty or if (%composer.changed(five)) 0b0100000000000000 else 0b0010000000000000
          }
          if (%default and 0b00100000 !== 0) {
            %dirty = %dirty or 0b00110000000000000000
          } else if (%changed and 0b01110000000000000000 === 0) {
            %dirty = %dirty or if (%composer.changed(six)) 0b00100000000000000000 else 0b00010000000000000000
          }
          if (%default and 0b01000000 !== 0) {
            %dirty = %dirty or 0b000110000000000000000000
          } else if (%changed and 0b001110000000000000000000 === 0) {
            %dirty = %dirty or if (%composer.changed(content)) 0b000100000000000000000000 else 0b10000000000000000000
          }
          if (%dirty and 0b001011011011011011011011 !== 0b10010010010010010010 || !%composer.skipping) {
            if (%default and 0b0001 !== 0) {
              one = ComposableSingletons%TestKt.lambda-1
            }
            if (%default and 0b0010 !== 0) {
              two = null
            }
            if (%default and 0b0100 !== 0) {
              three = null
            }
            if (%default and 0b1000 !== 0) {
              four = null
            }
            if (%default and 0b00010000 !== 0) {
              five = null
            }
            if (%default and 0b00100000 !== 0) {
              six = null
            }
            one(%composer, 0b1110 and %dirty)
            two?.invoke(%composer, 0b1110 and %dirty shr 0b0011)
            val tmp1_safe_receiver = three
            %composer.startReplaceableGroup(<>)
            sourceInformation(%composer, "*<it()>")
            val tmp1_group = when {
              tmp1_safe_receiver == null -> {
                null
              }
              else -> {
                tmp1_safe_receiver.let { it: Function2<Composer, Int, Unit> ->
                  it(%composer, 0)
                }
              }
            }
            %composer.endReplaceableGroup()
            tmp1_group
            val tmp2_safe_receiver = four
            %composer.startReplaceableGroup(<>)
            sourceInformation(%composer, "*<four()>")
            val tmp2_group = when {
              tmp2_safe_receiver == null -> {
                null
              }
              else -> {
                tmp2_safe_receiver.let { it: Function2<Composer, Int, Unit> ->
                  four(%composer, 0b1110 and %dirty shr 0b1001)
                }
              }
            }
            %composer.endReplaceableGroup()
            tmp2_group
            %composer.startReplaceableGroup(<>)
            sourceInformation(%composer, "<five()>")
            if (five != null) {
              five(%composer, 0b1110 and %dirty shr 0b1100)
            }
            %composer.endReplaceableGroup()
            val tmp3_safe_receiver = six
            %composer.startReplaceableGroup(<>)
            sourceInformation(%composer, "*<Wrappe...>")
            val tmp3_group = when {
              tmp3_safe_receiver == null -> {
                null
              }
              else -> {
                tmp3_safe_receiver.let { it: Function2<Composer, Int, Unit> ->
                  Wrapper(it, %composer, 0)
                }
              }
            }
            %composer.endReplaceableGroup()
            tmp3_group
            content(%composer, 0b1110 and %dirty shr 0b00010010)
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            Optional(one, two, three, four, five, six, content, %composer, %changed or 0b0001, %default)
          }
          if (isTraceInProgress()) {
            traceEventEnd()
          }
        }
        @Composable
        @ComposableTarget(applier = "UI")
        fun UseOptional(%composer: Composer?, %changed: Int) {
          if (isTraceInProgress()) {
            traceEventStart(<>)
          }
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(UseOptional)<Option...>:Test.kt")
          if (%changed !== 0 || !%composer.skipping) {
            Optional(null, null, null, null, null, null, ComposableSingletons%TestKt.lambda-2, %composer, 0b000110000000000000000000, 0b00111111)
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            UseOptional(%composer, %changed or 0b0001)
          }
          if (isTraceInProgress()) {
            traceEventEnd()
          }
        }
        internal object ComposableSingletons%TestKt {
          val lambda-1: Function2<Composer, Int, Unit> = composableLambdaInstance(<>, false) { %composer: Composer?, %changed: Int ->
            sourceInformation(%composer, "C:Test.kt")
            if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
              Unit
            } else {
              %composer.skipToGroupEnd()
            }
          }
          val lambda-2: Function2<Composer, Int, Unit> = composableLambdaInstance(<>, false) { %composer: Composer?, %changed: Int ->
            sourceInformation(%composer, "C<Leaf()>:Test.kt")
            if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
              Leaf(%composer, 0)
            } else {
              %composer.skipToGroupEnd()
            }
          }
        }
        """
    )

    @Test
    fun testReceiverScope() = verifyComposeIrTransform(
        """
        import androidx.compose.runtime.*
        import androidx.compose.ui.layout.*
        import androidx.compose.foundation.text.*
        import androidx.compose.ui.text.*
        import androidx.compose.ui.text.style.*
        import androidx.compose.ui.*

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
        """,
        """
        @Immutable
        interface LocalBoxScope {
          @Stable
          abstract fun Modifier.align(alignment: Alignment): Modifier
        }
        @StabilityInferred(parameters = 0)
        object LocalBoxScopeInstance : LocalBoxScope {
          override fun Modifier.align(alignment: Alignment): Modifier {
            return Companion
          }
          static val %stable: Int = 0
        }
        val localBoxMeasurePolicy: MeasurePolicy = class <no name provided> : MeasurePolicy {
          override fun measure(%this%MeasurePolicy: MeasureScope, <anonymous parameter 0>: List<Measurable>, constraints: Constraints): MeasureResult {
            return %this%MeasurePolicy.layout(
              width = constraints.minWidth,
              height = constraints.minHeight
            ) {
            }
          }
        }
        <no name provided>()
        @Composable
        @ComposableInferredTarget(scheme = "[androidx.compose.ui.UiComposable[androidx.compose.ui.UiComposable]]")
        fun LocalBox(modifier: Modifier?, content: @[ExtensionFunctionType] Function3<LocalBoxScope, Composer, Int, Unit>, %composer: Composer?, %changed: Int, %default: Int) {
          %composer.startReplaceableGroup(<>)
          sourceInformation(%composer, "C(LocalBox)P(1)<Layout...>:Test.kt")
          if (%default and 0b0001 !== 0) {
            modifier = Companion
          }
          val tmp0_measurePolicy = localBoxMeasurePolicy
          Layout({ %composer: Composer?, %changed: Int ->
            %composer.startReplaceableGroup(<>)
            sourceInformation(%composer, "C<conten...>:Test.kt")
            if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
              content(LocalBoxScopeInstance, %composer, 0b0110 or 0b01110000 and %changed)
            } else {
              %composer.skipToGroupEnd()
            }
            %composer.endReplaceableGroup()
          }, modifier, tmp0_measurePolicy, %composer, 0b000110000000 or 0b01110000 and %changed shl 0b0011, 0)
          %composer.endReplaceableGroup()
        }
        """
    )

    @Test
    fun testCallingLayout() = verifyComposeIrTransform(
        """
        import androidx.compose.runtime.*
        import androidx.compose.ui.layout.*
        import androidx.compose.foundation.text.*
        import androidx.compose.ui.text.*
        import androidx.compose.ui.text.style.*

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
        """,
        """
        @Composable
        @ComposableTarget(applier = "androidx.compose.ui.UiComposable")
        fun Test1(%composer: Composer?, %changed: Int) {
          if (isTraceInProgress()) {
            traceEventStart(<>)
          }
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(Test1)<Layout...>:Test.kt")
          if (%changed !== 0 || !%composer.skipping) {
            Layout({ %composer: Composer?, %changed: Int ->
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C:Test.kt")
              if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                Unit
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endReplaceableGroup()
            }, null, class <no name provided> : MeasurePolicy {
              override fun measure(%this%Layout: MeasureScope, <anonymous parameter 0>: List<Measurable>, <anonymous parameter 1>: Constraints): MeasureResult {
                return error("")
              }
            }
            <no name provided>(), %composer, 0, 0b0010)
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            Test1(%composer, %changed or 0b0001)
          }
          if (isTraceInProgress()) {
            traceEventEnd()
          }
        }
        @Composable
        @ComposableInferredTarget(scheme = "[androidx.compose.ui.UiComposable[androidx.compose.ui.UiComposable]]")
        fun Test2(content: Function2<Composer, Int, Unit>, %composer: Composer?, %changed: Int) {
          if (isTraceInProgress()) {
            traceEventStart(<>)
          }
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(Test2)<Layout...>:Test.kt")
          val %dirty = %changed
          if (%changed and 0b1110 === 0) {
            %dirty = %dirty or if (%composer.changed(content)) 0b0100 else 0b0010
          }
          if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
            Layout(content, null, class <no name provided> : MeasurePolicy {
              override fun measure(%this%Layout: MeasureScope, <anonymous parameter 0>: List<Measurable>, <anonymous parameter 1>: Constraints): MeasureResult {
                return error("")
              }
            }
            <no name provided>(), %composer, 0b1110 and %dirty, 0b0010)
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            Test2(content, %composer, %changed or 0b0001)
          }
          if (isTraceInProgress()) {
            traceEventEnd()
          }
        }
        @Composable
        @ComposableTarget(applier = "androidx.compose.ui.UiComposable")
        fun Test3(%composer: Composer?, %changed: Int) {
          if (isTraceInProgress()) {
            traceEventStart(<>)
          }
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(Test3)<Test1(...>:Test.kt")
          if (%changed !== 0 || !%composer.skipping) {
            Test1(%composer, 0)
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            Test3(%composer, %changed or 0b0001)
          }
          if (isTraceInProgress()) {
            traceEventEnd()
          }
        }
        @Composable
        @ComposableTarget(applier = "androidx.compose.ui.UiComposable")
        fun Test4(%composer: Composer?, %changed: Int) {
          if (isTraceInProgress()) {
            traceEventStart(<>)
          }
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(Test4)<BasicT...>:Test.kt")
          if (%changed !== 0 || !%composer.skipping) {
            BasicText(AnnotatedString(
              text = "Some text"
            ), null, null, null, <unsafe-coerce>(0), false, 0, null, %composer, 0, 0b11111110)
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            Test4(%composer, %changed or 0b0001)
          }
          if (isTraceInProgress()) {
            traceEventEnd()
          }
        }
        val Local: ProvidableCompositionLocal<Int> = compositionLocalOf {
          0
        }
        @Composable
        @ComposableInferredTarget(scheme = "[androidx.compose.ui.UiComposable[androidx.compose.ui.UiComposable]]")
        fun Test5(content: Function2<Composer, Int, Unit>, %composer: Composer?, %changed: Int) {
          if (isTraceInProgress()) {
            traceEventStart(<>)
          }
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(Test5)<Compos...>:Test.kt")
          val %dirty = %changed
          if (%changed and 0b1110 === 0) {
            %dirty = %dirty or if (%composer.changed(content)) 0b0100 else 0b0010
          }
          if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
            CompositionLocalProvider(Local provides 5, composableLambda(%composer, <>, true) { %composer: Composer?, %changed: Int ->
              sourceInformation(%composer, "C<Test1(...>,<conten...>:Test.kt")
              if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                Test1(%composer, 0)
                content(%composer, 0b1110 and %dirty)
              } else {
                %composer.skipToGroupEnd()
              }
            }, %composer, 0b00111000)
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            Test5(content, %composer, %changed or 0b0001)
          }
          if (isTraceInProgress()) {
            traceEventEnd()
          }
        }
        @Composable
        @ComposableTarget(applier = "androidx.compose.ui.UiComposable")
        fun Test6(test: String, %composer: Composer?, %changed: Int) {
          if (isTraceInProgress()) {
            traceEventStart(<>)
          }
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(Test6)<Compos...>:Test.kt")
          val %dirty = %changed
          if (%changed and 0b1110 === 0) {
            %dirty = %dirty or if (%composer.changed(test)) 0b0100 else 0b0010
          }
          if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
            CompositionLocalProvider(Local provides 6, composableLambda(%composer, <>, true) { %composer: Composer?, %changed: Int ->
              sourceInformation(%composer, "C<T(test...>,<Test1(...>:Test.kt")
              if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                T(test, %composer, 0b1110 and %dirty)
                Test1(%composer, 0)
              } else {
                %composer.skipToGroupEnd()
              }
            }, %composer, 0b00111000)
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            Test6(test, %composer, %changed or 0b0001)
          }
          if (isTraceInProgress()) {
            traceEventEnd()
          }
        }
        @Composable
        fun T(value: String, %composer: Composer?, %changed: Int) {
          if (isTraceInProgress()) {
            traceEventStart(<>)
          }
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(T):Test.kt")
          if (%changed and 0b0001 !== 0 || !%composer.skipping) {
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            T(value, %composer, %changed or 0b0001)
          }
          if (isTraceInProgress()) {
            traceEventEnd()
          }
        }
        """
    )

    @Suppress("unused")
    fun testCollectAsState() = verifyComposeIrTransform(
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
        """,
        """
        @Composable
        fun <T> StateFlow<T>.collectAsState(context: CoroutineContext?, %composer: Composer?, %changed: Int, %default: Int): State<T> {
          %composer.startReplaceableGroup(<>)
          sourceInformation(%composer, "C(collectAsState)<collec...>:Test.kt")
          if (%default and 0b0001 !== 0) {
            context = EmptyCoroutineContext
          }
          val tmp0 = collectAsState(value, context, %composer, 0b001000001000, 0)
          %composer.endReplaceableGroup()
          return tmp0
        }
        @Composable
        fun <T: R, R> Flow<T>.collectAsState(initial: R, context: CoroutineContext?, %composer: Composer?, %changed: Int, %default: Int): State<R> {
          %composer.startReplaceableGroup(<>)
          sourceInformation(%composer, "C(collectAsState)P(1):Test.kt")
          if (%default and 0b0010 !== 0) {
            context = EmptyCoroutineContext
          }
          val tmp0 = mutableStateOf(
            value = initial
          )
          %composer.endReplaceableGroup()
          return tmp0
        }
        """
    )

    @Test
    fun testRememberUpdatedState() = verifyComposeIrTransform(
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
        """,
        expectedTransformed = """
        @Composable
        @ComposableInferredTarget(scheme = "[UI[UI]]")
        fun Test(content: Function2<Composer, Int, Unit>, %composer: Composer?, %changed: Int) {
          if (isTraceInProgress()) {
            traceEventStart(<>)
          }
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(Test)<rememb...>:Test.kt")
          val %dirty = %changed
          if (%changed and 0b1110 === 0) {
            %dirty = %dirty or if (%composer.changed(content)) 0b0100 else 0b0010
          }
          if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
            <<LOCALDELPROP>>
            Defer(composableLambda(%composer, <>, true) { %composer: Composer?, %changed: Int ->
              sourceInformation(%composer, "C:Test.kt")
              if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                UiContent(composableLambda(%composer, <>, true) { %composer: Composer?, %changed: Int ->
                  sourceInformation(%composer, "C<update...>:Test.kt")
                  if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                    <get-updatedContent>()(%composer, 0)
                  } else {
                    %composer.skipToGroupEnd()
                  }
                }
                )
              } else {
                %composer.skipToGroupEnd()
              }
            }
            )
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            Test(content, %composer, %changed or 0b0001)
          }
          if (isTraceInProgress()) {
            traceEventEnd()
          }
        }
        """
    )

    @Test
    fun testAddingComposablesToAList() = verifyComposeIrTransform(
        """
        import androidx.compose.runtime.*

        class Scope {
            private val list = IntervalList<Scope.(Int) -> (@Composable () -> Unit)>()
            fun item(content: @Composable Scope.() -> Unit) {
                list.add(1) { @Composable { content() } }
            }
        }
        """,
        """
        @StabilityInferred(parameters = 0)
        class Scope {
          val list: IntervalList<@[ExtensionFunctionType] Function2<Scope, Int, Function2<Composer, Int, Unit>>> = IntervalList()
          fun item(content: @[ExtensionFunctionType] Function3<Scope, Composer, Int, Unit>) {
            list.add(1) { it: Int ->
              composableLambdaInstance(<>, true) { %composer: Composer?, %changed: Int ->
                sourceInformation(%composer, "C<conten...>:Test.kt")
                if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                  content(%this%add, %composer, 0)
                } else {
                  %composer.skipToGroupEnd()
                }
              }
            }
          }
          static val %stable: Int = 0
        }
        """,
        extra = """
        class IntervalList<T> {
            fun add(size: Int, content: T) { }
        }
        """
    )

    @Test
    fun testCallingNullableComposableWithNull() = verifyComposeIrTransform(
        """
        import androidx.compose.runtime.*

        @Composable
        fun Test() {
            Widget(null)
        }
        """,
        """
        @Composable
        fun Test(%composer: Composer?, %changed: Int) {
          if (isTraceInProgress()) {
            traceEventStart(<>)
          }
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(Test)<Widget...>:Test.kt")
          if (%changed !== 0 || !%composer.skipping) {
            Widget(null, %composer, 0b0110)
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            Test(%composer, %changed or 0b0001)
          }
          if (isTraceInProgress()) {
            traceEventEnd()
          }
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
        """,
        """
        @Composable
        @ComposableInferredTarget(scheme = "[UI[UI[UI]]]")
        fun Test(decorator: Function3<@[ParameterName(name = 'content')] Function2<Composer, Int, Unit>, Composer, Int, Unit>, %composer: Composer?, %changed: Int) {
          if (isTraceInProgress()) {
            traceEventStart(<>)
          }
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(Test)<decora...>:Test.kt")
          val %dirty = %changed
          if (%changed and 0b1110 === 0) {
            %dirty = %dirty or if (%composer.changed(decorator)) 0b0100 else 0b0010
          }
          if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
            decorator(ComposableSingletons%TestKt.lambda-1, %composer, 0b0110 or 0b01110000 and %dirty shl 0b0011)
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            Test(decorator, %composer, %changed or 0b0001)
          }
          if (isTraceInProgress()) {
            traceEventEnd()
          }
        }
        internal object ComposableSingletons%TestKt {
          val lambda-1: Function2<Composer, Int, Unit> = composableLambdaInstance(<>, false) { %composer: Composer?, %changed: Int ->
            sourceInformation(%composer, "C<Text("...>:Test.kt")
            if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
              Text("Some text", %composer, 0b0110)
            } else {
              %composer.skipToGroupEnd()
            }
          }
        }
        """
    )

    @Test
    fun testFileScoped() = verifyComposeIrTransform(
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
        expectedTransformed = """
            @Composable
            fun NFromFile(%composer: Composer?, %changed: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(NFromFile)<Open()>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                Open(%composer, 0)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                NFromFile(%composer, %changed or 0b0001)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
            @Composable
            fun NFromInference(%composer: Composer?, %changed: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(NFromInference)<N()>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                N(%composer, 0)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                NFromInference(%composer, %changed or 0b0001)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
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
    fun testCrossfileFileScope() = verifyComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun InferN() { N() }
        """,
        expectedTransformed = """
            @Composable
            @ComposableTarget(applier = "NComposable")
            fun InferN(%composer: Composer?, %changed: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(InferN)<N()>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                N(%composer, 0)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                InferN(%composer, %changed or 0b0001)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
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

    private fun verify(source: String, expected: String) =
        verifyComposeIrTransform(source, expected, baseDefinition)

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