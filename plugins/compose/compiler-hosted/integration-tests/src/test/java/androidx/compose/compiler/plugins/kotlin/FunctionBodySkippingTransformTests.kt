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
import org.junit.Test

abstract class FunctionBodySkippingTransfomrTestsBase : ComposeIrTransformTest() {
    protected fun comparisonPropagation(
        @Language("kotlin")
        unchecked: String,
        @Language("kotlin")
        checked: String,
        expectedTransformed: String,
        dumpTree: Boolean = false
    ) = verifyComposeIrTransform(
        """
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.NonRestartableComposable
            import androidx.compose.runtime.ReadOnlyComposable

            $checked
        """.trimIndent(),
        expectedTransformed,
        """
            import androidx.compose.runtime.Composable

            $unchecked
            fun used(x: Any?) {}
        """.trimIndent(),
        dumpTree = dumpTree
    )
}

class FunctionBodySkippingTransformTests : FunctionBodySkippingTransfomrTestsBase() {

    @Test
    fun testIfInLambda(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0) {}
            @Composable fun Wrap(content: @Composable () -> Unit) {
                content()
            }
        """,
        """
            @Composable
            fun Test(x: Int = 0, y: Int = 0) {
                used(y)
                Wrap {
                    if (x > 0) {
                        A(x)
                    } else {
                        A(x)
                    }
                }
            }
        """,
        """
            @Composable
            fun Test(x: Int, y: Int, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<Wrap>:Test.kt")
              val %dirty = %changed
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0100 else 0b0010
              }
              if (%default and 0b0010 !== 0) {
                %dirty = %dirty or 0b00110000
              } else if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(y)) 0b00100000 else 0b00010000
              }
              if (%dirty and 0b01011011 !== 0b00010010 || !%composer.skipping) {
                if (%default and 0b0001 !== 0) {
                  x = 0
                }
                if (%default and 0b0010 !== 0) {
                  y = 0
                }
                used(y)
                Wrap(composableLambda(%composer, <>, true) { %composer: Composer?, %changed: Int ->
                  sourceInformation(%composer, "C:Test.kt")
                  if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                    if (x > 0) {
                      %composer.startReplaceableGroup(<>)
                      sourceInformation(%composer, "<A(x)>")
                      A(x, 0, %composer, 0b1110 and %dirty, 0b0010)
                      %composer.endReplaceableGroup()
                    } else {
                      %composer.startReplaceableGroup(<>)
                      sourceInformation(%composer, "<A(x)>")
                      A(x, 0, %composer, 0b1110 and %dirty, 0b0010)
                      %composer.endReplaceableGroup()
                    }
                  } else {
                    %composer.skipToGroupEnd()
                  }
                }, %composer, 0b0110)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(x, y, %composer, %changed or 0b0001, %default)
              }
            }
        """
    )

    @Test
    fun testBasicText(): Unit = comparisonPropagation(
        """
        """,
        """
            import androidx.compose.ui.text.style.TextOverflow
            import androidx.compose.ui.text.TextStyle
            import androidx.compose.ui.text.TextLayoutResult

            @Composable
            fun BasicText(
                style: TextStyle = TextStyle.Default,
                onTextLayout: (TextLayoutResult) -> Unit = {},
                overflow: TextOverflow = TextOverflow.Clip,
            ) {
                used(style)
                used(onTextLayout)
                used(overflow)
            }
        """,
        """
            @Composable
            fun BasicText(style: TextStyle?, onTextLayout: Function1<TextLayoutResult, Unit>?, overflow: TextOverflow, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(BasicText)P(2!,1:c#ui.text.style.TextOverflow):Test.kt")
              val %dirty = %changed
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(style)) 0b0100 else 0b0010
              }
              if (%default and 0b0010 !== 0) {
                %dirty = %dirty or 0b00110000
              } else if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(onTextLayout)) 0b00100000 else 0b00010000
              }
              if (%default and 0b0100 !== 0) {
                %dirty = %dirty or 0b000110000000
              } else if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(<unsafe-coerce>(overflow))) 0b000100000000 else 0b10000000
              }
              if (%dirty and 0b001011011011 !== 0b10010010 || !%composer.skipping) {
                if (%default and 0b0001 !== 0) {
                  style = Companion.Default
                }
                if (%default and 0b0010 !== 0) {
                  onTextLayout = { it: TextLayoutResult ->
                  }
                }
                if (%default and 0b0100 !== 0) {
                  overflow = Companion.Clip
                }
                used(style)
                used(onTextLayout)
                used(overflow)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                BasicText(style, onTextLayout, overflow, %composer, %changed or 0b0001, %default)
              }
            }
        """
    )

    @Test
    fun testArrangement(): Unit = comparisonPropagation(
        """
        """,
        """
            import androidx.compose.foundation.layout.Arrangement
            import androidx.compose.foundation.layout.Arrangement.Vertical

            @Composable
            fun A(
                arrangement: Vertical = Arrangement.Top
            ) {
                used(arrangement)
            }
        """,
        """
            @Composable
            fun A(arrangement: Vertical?, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(A):Test.kt")
              val %dirty = %changed
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(arrangement)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (%default and 0b0001 !== 0) {
                  arrangement = Arrangement.Top
                }
                used(arrangement)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                A(arrangement, %composer, %changed or 0b0001, %default)
              }
            }
        """
    )

    @Test
    fun testComposableSingletonsAreStatic(): Unit = comparisonPropagation(
        """
        """,
        """
            @Composable
            fun Example(
                content: @Composable () -> Unit = {}
            ) {
                content()
            }
        """,
        """
            @Composable
            @ComposableInferredTarget(scheme = "[0[0]]")
            fun Example(content: Function2<Composer, Int, Unit>?, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Example)<conten...>:Test.kt")
              val %dirty = %changed
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(content)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (%default and 0b0001 !== 0) {
                  content = ComposableSingletons%TestKt.lambda-1
                }
                content(%composer, 0b1110 and %dirty)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Example(content, %composer, %changed or 0b0001, %default)
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
            }
        """
    )

    @Test
    fun testFunInterfaces(): Unit = comparisonPropagation(
        """
            fun interface A {
                @Composable fun compute(value: Int): Unit
            }
        """,
        """
            fun Example(a: A) {
                used(a)
                Example { it -> a.compute(it) }
            }
        """,
        """
            fun Example(a: A) {
              used(a)
              Example(class <no name provided> : A {
                @Composable
                override fun compute(it: Int, %composer: Composer?, %changed: Int) {
                  %composer = %composer.startRestartGroup(<>)
                  sourceInformation(%composer, "C(compute)<comput...>:Test.kt")
                  val %dirty = %changed
                  if (%changed and 0b1110 === 0) {
                    %dirty = %dirty or if (%composer.changed(it)) 0b0100 else 0b0010
                  }
                  if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                    a.compute(it, %composer, 0b1110 and %dirty)
                  } else {
                    %composer.skipToGroupEnd()
                  }
                  val tmp0_rcvr = <this>
                  %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                    tmp0_rcvr.compute(it, %composer, %changed or 0b0001)
                  }
                }
              }
              <no name provided>())
            }
        """
    )

    @Test
    fun testFunInterfaces2(): Unit = comparisonPropagation(
        """
            import androidx.compose.ui.graphics.Color

            @Composable fun condition(): Boolean = true

            fun interface ButtonColors {
                @Composable fun getColor(): Color
            }
        """,
        """
            import androidx.compose.material.Text
            import androidx.compose.ui.graphics.Color

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
            @Composable
            @ComposableInferredTarget(scheme = "[androidx.compose.ui.UiComposable[androidx.compose.ui.UiComposable]]")
            fun Button(colors: ButtonColors, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Button)<getCol...>,<Text("...>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(colors)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                Text("hello world", null, colors.getColor(%composer, 0b1110 and %dirty), <unsafe-coerce>(0L), null, null, null, <unsafe-coerce>(0L), null, null, <unsafe-coerce>(0L), <unsafe-coerce>(0), false, 0, null, null, %composer, 0b0110, 0, 0b1111111111111010)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Button(colors, %composer, %changed or 0b0001)
              }
            }
            @Composable
            @ComposableTarget(applier = "androidx.compose.ui.UiComposable")
            fun Test(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<Button>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                Button(class <no name provided> : ButtonColors {
                  @Composable
                  override fun getColor(%composer: Composer?, %changed: Int): Color {
                    %composer.startReplaceableGroup(<>)
                    sourceInformation(%composer, "C(getColor)<condit...>:Test.kt")
                    val tmp0 = if (condition(%composer, 0)) {
                      Companion.Red
                    } else {
                      Companion.Blue
                    }
                    %composer.endReplaceableGroup()
                    return tmp0
                  }
                }
                <no name provided>(), %composer, 0)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testSimpleColumn(): Unit = comparisonPropagation(
        """
            import androidx.compose.runtime.Stable
            import androidx.compose.runtime.Immutable

            @Stable
            interface Modifier {
              companion object : Modifier { }
            }

            @Immutable
            interface Arrangement {
              @Immutable
              interface Vertical : Arrangement

              object Top : Vertical
            }

            enum class LayoutOrientation {
                Horizontal,
                Vertical
            }

            enum class SizeMode {
                Wrap,
                Expand
            }

            @Immutable
            data class Alignment(
                val verticalBias: Float,
                val horizontalBias: Float
            ) {
                @Immutable
                data class Horizontal(val bias: Float)

                companion object {
                  val Start = Alignment.Horizontal(-1f)
                }
            }
        """,
        """
            @Composable
            fun RowColumnImpl(
              orientation: LayoutOrientation,
              modifier: Modifier = Modifier,
              arrangement: Arrangement.Vertical = Arrangement.Top,
              crossAxisAlignment: Alignment.Horizontal = Alignment.Start,
              crossAxisSize: SizeMode = SizeMode.Wrap,
              content: @Composable() ()->Unit
            ) {
                used(orientation)
                used(modifier)
                used(arrangement)
                used(crossAxisAlignment)
                used(crossAxisSize)
                content()
            }

            @Composable
            fun Column(
                modifier: Modifier = Modifier,
                verticalArrangement: Arrangement.Vertical = Arrangement.Top,
                horizontalGravity: Alignment.Horizontal = Alignment.Start,
                content: @Composable() ()->Unit
            ) {
              RowColumnImpl(
                orientation = LayoutOrientation.Vertical,
                arrangement = verticalArrangement,
                crossAxisAlignment = horizontalGravity,
                crossAxisSize = SizeMode.Wrap,
                modifier = modifier,
                content = content
              )
            }
        """,
        """
            @Composable
            @ComposableInferredTarget(scheme = "[0[0]]")
            fun RowColumnImpl(orientation: LayoutOrientation, modifier: Modifier?, arrangement: Vertical?, crossAxisAlignment: Horizontal?, crossAxisSize: SizeMode?, content: Function2<Composer, Int, Unit>, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(RowColumnImpl)P(5,4!1,2,3)<conten...>:Test.kt")
              val %dirty = %changed
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(orientation)) 0b0100 else 0b0010
              }
              if (%default and 0b0010 !== 0) {
                %dirty = %dirty or 0b00110000
              } else if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(modifier)) 0b00100000 else 0b00010000
              }
              if (%default and 0b0100 !== 0) {
                %dirty = %dirty or 0b000110000000
              } else if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(arrangement)) 0b000100000000 else 0b10000000
              }
              if (%default and 0b1000 !== 0) {
                %dirty = %dirty or 0b110000000000
              } else if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(crossAxisAlignment)) 0b100000000000 else 0b010000000000
              }
              if (%default and 0b00010000 !== 0) {
                %dirty = %dirty or 0b0110000000000000
              } else if (%changed and 0b1110000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(crossAxisSize)) 0b0100000000000000 else 0b0010000000000000
              }
              if (%default and 0b00100000 !== 0) {
                %dirty = %dirty or 0b00110000000000000000
              } else if (%changed and 0b01110000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(content)) 0b00100000000000000000 else 0b00010000000000000000
              }
              if (%dirty and 0b01011011011011011011 !== 0b00010010010010010010 || !%composer.skipping) {
                if (%default and 0b0010 !== 0) {
                  modifier = Companion
                }
                if (%default and 0b0100 !== 0) {
                  arrangement = Top
                }
                if (%default and 0b1000 !== 0) {
                  crossAxisAlignment = Companion.Start
                }
                if (%default and 0b00010000 !== 0) {
                  crossAxisSize = SizeMode.Wrap
                }
                used(orientation)
                used(modifier)
                used(arrangement)
                used(crossAxisAlignment)
                used(crossAxisSize)
                content(%composer, 0b1110 and %dirty shr 0b1111)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                RowColumnImpl(orientation, modifier, arrangement, crossAxisAlignment, crossAxisSize, content, %composer, %changed or 0b0001, %default)
              }
            }
            @Composable
            @ComposableInferredTarget(scheme = "[0[0]]")
            fun Column(modifier: Modifier?, verticalArrangement: Vertical?, horizontalGravity: Horizontal?, content: Function2<Composer, Int, Unit>, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Column)P(2,3,1)<RowCol...>:Test.kt")
              val %dirty = %changed
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(modifier)) 0b0100 else 0b0010
              }
              if (%default and 0b0010 !== 0) {
                %dirty = %dirty or 0b00110000
              } else if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(verticalArrangement)) 0b00100000 else 0b00010000
              }
              if (%default and 0b0100 !== 0) {
                %dirty = %dirty or 0b000110000000
              } else if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(horizontalGravity)) 0b000100000000 else 0b10000000
              }
              if (%default and 0b1000 !== 0) {
                %dirty = %dirty or 0b110000000000
              } else if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(content)) 0b100000000000 else 0b010000000000
              }
              if (%dirty and 0b0001011011011011 !== 0b010010010010 || !%composer.skipping) {
                if (%default and 0b0001 !== 0) {
                  modifier = Companion
                }
                if (%default and 0b0010 !== 0) {
                  verticalArrangement = Top
                }
                if (%default and 0b0100 !== 0) {
                  horizontalGravity = Companion.Start
                }
                val tmp0_orientation = LayoutOrientation.Vertical
                val tmp1_crossAxisSize = SizeMode.Wrap
                RowColumnImpl(tmp0_orientation, modifier, verticalArrangement, horizontalGravity, tmp1_crossAxisSize, content, %composer, 0b0110000000000110 or 0b01110000 and %dirty shl 0b0011 or 0b001110000000 and %dirty shl 0b0011 or 0b0001110000000000 and %dirty shl 0b0011 or 0b01110000000000000000 and %dirty shl 0b0110, 0)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Column(modifier, verticalArrangement, horizontalGravity, content, %composer, %changed or 0b0001, %default)
              }
            }
        """
    )

    @Test
    fun testSimplerBox(): Unit = comparisonPropagation(
        """
            import androidx.compose.runtime.Stable

            @Stable
            interface Modifier {
              companion object : Modifier { }
            }
        """,
        """
            @Composable
            fun SimpleBox(modifier: Modifier = Modifier) {
               used(modifier)
            }
        """,
        """
            @Composable
            fun SimpleBox(modifier: Modifier?, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(SimpleBox):Test.kt")
              val %dirty = %changed
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(modifier)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (%default and 0b0001 !== 0) {
                  modifier = Companion
                }
                used(modifier)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                SimpleBox(modifier, %composer, %changed or 0b0001, %default)
              }
            }
        """
    )

    @Test
    fun testDefaultSkipping(): Unit = comparisonPropagation(
        """
            fun newInt(): Int = 123
        """,
        """
            @Composable
            fun Example(a: Int = newInt()) {
               print(a)
            }
        """,
        """
            @Composable
            fun Example(a: Int, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Example):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%default and 0b0001 === 0 && %composer.changed(a)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                %composer.startDefaults()
                if (%changed and 0b0001 === 0 || %composer.defaultsInvalid) {
                  if (%default and 0b0001 !== 0) {
                    a = newInt()
                    %dirty = %dirty and 0b1110.inv()
                  }
                } else {
                  %composer.skipToGroupEnd()
                  if (%default and 0b0001 !== 0) {
                    %dirty = %dirty and 0b1110.inv()
                  }
                }
                %composer.endDefaults()
                print(a)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Example(a, %composer, %changed or 0b0001, %default)
              }
            }
        """
    )

    @Test
    fun testLocalComposableFunctions(): Unit = comparisonPropagation(
        """
            @Composable fun A(a: Int) {}
        """,
        """
            @Composable
            fun Example(a: Int) {
                @Composable fun Inner() {
                    A(a)
                }
                Inner()
            }
        """,
        """
            @Composable
            fun Example(a: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Example)<Inner(...>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(a)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                @Composable
                fun Inner(%composer: Composer?, %changed: Int) {
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C(Inner)<A(a)>:Test.kt")
                  A(a, %composer, 0b1110 and %dirty)
                  %composer.endReplaceableGroup()
                }
                Inner(%composer, 0)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Example(a, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testLoopWithContinueAndCallAfter(): Unit = comparisonPropagation(
        """
            @Composable fun Call() {}
            fun condition(): Boolean = true
        """,
        """
            @Composable
            @NonRestartableComposable
            fun Example() {
                Call()
                for (index in 0..1) {
                    Call()
                    if (condition())
                        continue
                    Call()
                }
            }
        """,
        """
            @Composable
            @NonRestartableComposable
            fun Example(%composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<Call()>:Test.kt")
              Call(%composer, 0)
              val tmp0_iterator = 0 .. 1.iterator()
              while (tmp0_iterator.hasNext()) {
                %composer.startReplaceableGroup(<>)
                sourceInformation(%composer, "<Call()>,<Call()>")
                val index = tmp0_iterator.next()
                Call(%composer, 0)
                if (condition()) {
                  %composer.endReplaceableGroup()
                  continue
                }
                Call(%composer, 0)
                %composer.endReplaceableGroup()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testSimpleBoxWithShape(): Unit = comparisonPropagation(
        """
            import androidx.compose.runtime.Stable

            @Stable
            interface Modifier {
              companion object : Modifier { }
            }

            interface Shape {
            }

            val RectangleShape = object : Shape { }
        """,
        """
            @Composable
            fun SimpleBox(modifier: Modifier = Modifier, shape: Shape = RectangleShape) {
                used(modifier)
                used(shape)
            }
        """,
        """
            @Composable
            fun SimpleBox(modifier: Modifier?, shape: Shape?, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(SimpleBox):Test.kt")
              val %dirty = %changed
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(modifier)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%default and 0b0010 === 0 && %composer.changed(shape)) 0b00100000 else 0b00010000
              }
              if (%dirty and 0b01011011 !== 0b00010010 || !%composer.skipping) {
                %composer.startDefaults()
                if (%changed and 0b0001 === 0 || %composer.defaultsInvalid) {
                  if (%default and 0b0001 !== 0) {
                    modifier = Companion
                  }
                  if (%default and 0b0010 !== 0) {
                    shape = RectangleShape
                    %dirty = %dirty and 0b01110000.inv()
                  }
                } else {
                  %composer.skipToGroupEnd()
                  if (%default and 0b0010 !== 0) {
                    %dirty = %dirty and 0b01110000.inv()
                  }
                }
                %composer.endDefaults()
                used(modifier)
                used(shape)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                SimpleBox(modifier, shape, %composer, %changed or 0b0001, %default)
              }
            }
        """
    )

    @Test
    fun testSimpleBox(): Unit = comparisonPropagation(
        """
            import androidx.compose.runtime.Stable

            @Stable
            interface Modifier {
              companion object : Modifier { }
            }
        """,
        """
            @Composable
            fun SimpleBox(modifier: Modifier = Modifier, content: @Composable() () -> Unit = {}) {
                used(modifier)
                content()
            }
        """,
        """
            @Composable
            @ComposableInferredTarget(scheme = "[0[0]]")
            fun SimpleBox(modifier: Modifier?, content: Function2<Composer, Int, Unit>?, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(SimpleBox)P(1)<conten...>:Test.kt")
              val %dirty = %changed
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(modifier)) 0b0100 else 0b0010
              }
              if (%default and 0b0010 !== 0) {
                %dirty = %dirty or 0b00110000
              } else if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(content)) 0b00100000 else 0b00010000
              }
              if (%dirty and 0b01011011 !== 0b00010010 || !%composer.skipping) {
                if (%default and 0b0001 !== 0) {
                  modifier = Companion
                }
                if (%default and 0b0010 !== 0) {
                  content = ComposableSingletons%TestKt.lambda-1
                }
                used(modifier)
                content(%composer, 0b1110 and %dirty shr 0b0011)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                SimpleBox(modifier, content, %composer, %changed or 0b0001, %default)
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
            }
        """
    )

    @Test
    fun testComposableLambdaWithStableParams(): Unit = comparisonPropagation(
        """
            import androidx.compose.runtime.Immutable

            @Immutable class Foo
            @Composable fun A(x: Int) {}
            @Composable fun B(y: Foo) {}
        """,
        """
            val foo = @Composable { x: Int, y: Foo ->
                A(x)
                B(y)
            }
        """,
        """
            val foo: Function4<Int, Foo, Composer, Int, Unit> = ComposableSingletons%TestKt.lambda-1
            internal object ComposableSingletons%TestKt {
              val lambda-1: Function4<Int, Foo, Composer, Int, Unit> = composableLambdaInstance(<>, false) { x: Int, y: Foo, %composer: Composer?, %changed: Int ->
                sourceInformation(%composer, "C<A(x)>,<B(y)>:Test.kt")
                val %dirty = %changed
                if (%changed and 0b1110 === 0) {
                  %dirty = %dirty or if (%composer.changed(x)) 0b0100 else 0b0010
                }
                if (%changed and 0b01110000 === 0) {
                  %dirty = %dirty or if (%composer.changed(y)) 0b00100000 else 0b00010000
                }
                if (%dirty and 0b001011011011 !== 0b10010010 || !%composer.skipping) {
                  A(x, %composer, 0b1110 and %dirty)
                  B(y, %composer, 0b1110 and %dirty shr 0b0011)
                } else {
                  %composer.skipToGroupEnd()
                }
              }
            }
        """
    )

    @Test
    fun testComposableLambdaWithUnstableParams(): Unit = comparisonPropagation(
        """
            class Foo(var value: Int = 0)
            @Composable fun A(x: Int) {}
            @Composable fun B(y: Foo) {}
        """,
        """
            val foo = @Composable { x: Int, y: Foo ->
                A(x)
                B(y)
            }
        """,
        """
            val foo: Function4<Int, Foo, Composer, Int, Unit> = ComposableSingletons%TestKt.lambda-1
            internal object ComposableSingletons%TestKt {
              val lambda-1: Function4<Int, Foo, Composer, Int, Unit> = composableLambdaInstance(<>, false) { x: Int, y: Foo, %composer: Composer?, %changed: Int ->
                sourceInformation(%composer, "C<A(x)>,<B(y)>:Test.kt")
                A(x, %composer, 0b1110 and %changed)
                B(y, %composer, 0b1000)
              }
            }
        """
    )

    @Test
    fun testComposableLambdaWithStableParamsAndReturnValue(): Unit = comparisonPropagation(
        """
        """,
        """
            @Composable fun SomeThing(content: @Composable() () -> Unit) { content() }

            @Composable
            fun Example() {
                SomeThing {
                    val id = object {}
                }
            }
        """,
        """
            @Composable
            @ComposableInferredTarget(scheme = "[0[0]]")
            fun SomeThing(content: Function2<Composer, Int, Unit>, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(SomeThing)<conten...>:Test.kt")
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
                SomeThing(content, %composer, %changed or 0b0001)
              }
            }
            @Composable
            fun Example(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Example)<SomeTh...>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                SomeThing(ComposableSingletons%TestKt.lambda-1, %composer, 0b0110)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Example(%composer, %changed or 0b0001)
              }
            }
            internal object ComposableSingletons%TestKt {
              val lambda-1: Function2<Composer, Int, Unit> = composableLambdaInstance(<>, false) { %composer: Composer?, %changed: Int ->
                sourceInformation(%composer, "C:Test.kt")
                if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                  val id = object
                } else {
                  %composer.skipToGroupEnd()
                }
              }
            }
        """
    )

    @Test
    fun testPrimitiveVarargParams(): Unit = comparisonPropagation(
        """
        """,
        """
            @Composable
            fun B(vararg values: Int) {
                print(values)
            }
        """,
        """
            @Composable
            fun B(values: IntArray, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(B):Test.kt")
              val %dirty = %changed
              %composer.startMovableGroup(<>, values.size)
              val tmp0_iterator = values.iterator()
              while (tmp0_iterator.hasNext()) {
                val value = tmp0_iterator.next()
                %dirty = %dirty or if (%composer.changed(value)) 0b0100 else 0
              }
              %composer.endMovableGroup()
              if (%dirty and 0b1110 === 0) {
                %dirty = %dirty or 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                print(values)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                B(*values, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testStableVarargParams(): Unit = comparisonPropagation(
        """
            import androidx.compose.runtime.Immutable
            @Immutable class Foo
        """,
        """
            @Composable
            fun B(vararg values: Foo) {
                print(values)
            }
        """,
        """
            @Composable
            fun B(values: Array<out Foo>, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(B):Test.kt")
              val %dirty = %changed
              %composer.startMovableGroup(<>, values.size)
              val tmp0_iterator = values.iterator()
              while (tmp0_iterator.hasNext()) {
                val value = tmp0_iterator.next()
                %dirty = %dirty or if (%composer.changed(value)) 0b0100 else 0
              }
              %composer.endMovableGroup()
              if (%dirty and 0b1110 === 0) {
                %dirty = %dirty or 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                print(values)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                B(*values, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testUnstableVarargParams(): Unit = comparisonPropagation(
        """
            class Foo(var value: Int = 0)
        """,
        """
            @Composable
            fun B(vararg values: Foo) {
                print(values)
            }
        """,
        """
            @Composable
            fun B(values: Array<out Foo>, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(B):Test.kt")
              print(values)
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                B(*values, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testReceiverParamSkippability(): Unit = comparisonPropagation(
        """
        """,
        """
            class Foo {
             var counter: Int = 0
             @Composable fun A() {
                print("hello world")
             }
             @Composable fun B() {
                print(counter)
             }
            }
        """,
        """
            @StabilityInferred(parameters = 0)
            class Foo {
              var counter: Int = 0
              @Composable
              fun A(%composer: Composer?, %changed: Int) {
                %composer = %composer.startRestartGroup(<>)
                sourceInformation(%composer, "C(A):Test.kt")
                if (%changed and 0b0001 !== 0 || !%composer.skipping) {
                  print("hello world")
                } else {
                  %composer.skipToGroupEnd()
                }
                val tmp0_rcvr = <this>
                %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                  tmp0_rcvr.A(%composer, %changed or 0b0001)
                }
              }
              @Composable
              fun B(%composer: Composer?, %changed: Int) {
                %composer = %composer.startRestartGroup(<>)
                sourceInformation(%composer, "C(B):Test.kt")
                print(counter)
                val tmp0_rcvr = <this>
                %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                  tmp0_rcvr.B(%composer, %changed or 0b0001)
                }
              }
              static val %stable: Int = 8
            }
        """
    )

    @Test
    fun testComposableParameter(): Unit = comparisonPropagation(
        """
            @Composable fun makeInt(): Int = 10
        """,
        """
            @Composable
            fun Example(a: Int = 0, b: Int = makeInt(), c: Int = 0) {
                used(a)
                used(b)
                used(c)
            }
        """,
        """
            @Composable
            fun Example(a: Int, b: Int, c: Int, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Example)<makeIn...>:Test.kt")
              val %dirty = %changed
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(a)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%default and 0b0010 === 0 && %composer.changed(b)) 0b00100000 else 0b00010000
              }
              if (%default and 0b0100 !== 0) {
                %dirty = %dirty or 0b000110000000
              } else if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(c)) 0b000100000000 else 0b10000000
              }
              if (%dirty and 0b001011011011 !== 0b10010010 || !%composer.skipping) {
                %composer.startDefaults()
                if (%changed and 0b0001 === 0 || %composer.defaultsInvalid) {
                  if (%default and 0b0001 !== 0) {
                    a = 0
                  }
                  if (%default and 0b0010 !== 0) {
                    b = makeInt(%composer, 0)
                    %dirty = %dirty and 0b01110000.inv()
                  }
                  if (%default and 0b0100 !== 0) {
                    c = 0
                  }
                } else {
                  %composer.skipToGroupEnd()
                  if (%default and 0b0010 !== 0) {
                    %dirty = %dirty and 0b01110000.inv()
                  }
                }
                %composer.endDefaults()
                used(a)
                used(b)
                used(c)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Example(a, b, c, %composer, %changed or 0b0001, %default)
              }
            }
        """
    )

    @Test
    fun testComposableWithAndWithoutDefaultParams(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0) {}
        """,
        """
            @Composable fun Wrap(y: Int, content: @Composable (x: Int) -> Unit) {
                content(y)
            }
            @Composable
            fun Test(x: Int = 0, y: Int = 0) {
                used(y)
                Wrap(10) {
                    used(it)
                    A(x)
                }
            }
        """,
        """
            @Composable
            @ComposableInferredTarget(scheme = "[0[0]]")
            fun Wrap(y: Int, content: Function3<@[ParameterName(name = 'x')] Int, Composer, Int, Unit>, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Wrap)P(1)<conten...>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(y)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(content)) 0b00100000 else 0b00010000
              }
              if (%dirty and 0b01011011 !== 0b00010010 || !%composer.skipping) {
                content(y, %composer, 0b1110 and %dirty or 0b01110000 and %dirty)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Wrap(y, content, %composer, %changed or 0b0001)
              }
            }
            @Composable
            fun Test(x: Int, y: Int, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<Wrap(1...>:Test.kt")
              val %dirty = %changed
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0100 else 0b0010
              }
              if (%default and 0b0010 !== 0) {
                %dirty = %dirty or 0b00110000
              } else if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(y)) 0b00100000 else 0b00010000
              }
              if (%dirty and 0b01011011 !== 0b00010010 || !%composer.skipping) {
                if (%default and 0b0001 !== 0) {
                  x = 0
                }
                if (%default and 0b0010 !== 0) {
                  y = 0
                }
                used(y)
                Wrap(10, composableLambda(%composer, <>, true) { it: Int, %composer: Composer?, %changed: Int ->
                  sourceInformation(%composer, "C<A(x)>:Test.kt")
                  val %dirty = %changed
                  if (%changed and 0b1110 === 0) {
                    %dirty = %dirty or if (%composer.changed(it)) 0b0100 else 0b0010
                  }
                  if (%dirty and 0b01011011 !== 0b00010010 || !%composer.skipping) {
                    used(it)
                    A(x, 0, %composer, 0b1110 and %dirty, 0b0010)
                  } else {
                    %composer.skipToGroupEnd()
                  }
                }, %composer, 0b00110110)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(x, y, %composer, %changed or 0b0001, %default)
              }
            }
        """
    )

    @Test
    fun testComposableWithReturnValue(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0) {}
        """,
        """
            @Composable
            fun Test(x: Int = 0, y: Int = 0): Int {
                A(x, y)
                return x + y
            }
        """,
        """
            @Composable
            fun Test(x: Int, y: Int, %composer: Composer?, %changed: Int, %default: Int): Int {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Test)<A(x,>:Test.kt")
              if (%default and 0b0001 !== 0) {
                x = 0
              }
              if (%default and 0b0010 !== 0) {
                y = 0
              }
              A(x, y, %composer, 0b1110 and %changed or 0b01110000 and %changed, 0)
              val tmp0 = x + y
              %composer.endReplaceableGroup()
              return tmp0
            }
        """
    )

    @Test
    fun testComposableLambda(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0) {}
        """,
        """
            val test = @Composable { x: Int ->
                A(x)
            }
        """,
        """
            val test: Function3<Int, Composer, Int, Unit> = ComposableSingletons%TestKt.lambda-1
            internal object ComposableSingletons%TestKt {
              val lambda-1: Function3<Int, Composer, Int, Unit> = composableLambdaInstance(<>, false) { x: Int, %composer: Composer?, %changed: Int ->
                sourceInformation(%composer, "C<A(x)>:Test.kt")
                val %dirty = %changed
                if (%changed and 0b1110 === 0) {
                  %dirty = %dirty or if (%composer.changed(x)) 0b0100 else 0b0010
                }
                if (%dirty and 0b01011011 !== 0b00010010 || !%composer.skipping) {
                  A(x, 0, %composer, 0b1110 and %dirty, 0b0010)
                } else {
                  %composer.skipToGroupEnd()
                }
              }
            }
        """
    )

    @Test
    fun testComposableFunExprBody(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0): Int { return 10 }
        """,
        """
            @Composable fun Test(x: Int) = A()
        """,
        """
            @Composable
            fun Test(x: Int, %composer: Composer?, %changed: Int): Int {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Test)<A()>:Test.kt")
              val tmp0 = A(0, 0, %composer, 0, 0b0011)
              %composer.endReplaceableGroup()
              return tmp0
            }
        """
    )

    @Test
    fun testParamReordering(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0): Int { return 10 }
        """,
        """
            @Composable fun Test(x: Int, y: Int) {
                A(y = y, x = x)
            }
        """,
        """
            @Composable
            fun Test(x: Int, y: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<A(y>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(y)) 0b00100000 else 0b00010000
              }
              if (%dirty and 0b01011011 !== 0b00010010 || !%composer.skipping) {
                A(x, y, %composer, 0b1110 and %dirty or 0b01110000 and %dirty, 0)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(x, y, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testStableUnstableParams(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0): Int { return 10 }
            class Foo(var value: Int = 0)
        """,
        """
            @Composable fun CanSkip(a: Int = 0, b: Foo = Foo()) {
                used(a)
                used(b)
            }
            @Composable fun CannotSkip(a: Int, b: Foo) {
                used(a)
                used(b)
                print("Hello World")
            }
            @Composable fun NoParams() {
                print("Hello World")
            }
        """,
        """
            @Composable
            fun CanSkip(a: Int, b: Foo?, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(CanSkip):Test.kt")
              val %dirty = %changed
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(a)) 0b0100 else 0b0010
              }
              if (%default and 0b0010 !== 0) {
                %dirty = %dirty or 0b00010000
              }
              if (%default and 0b0010 !== 0b0010 || %dirty and 0b01011011 !== 0b00010010 || !%composer.skipping) {
                %composer.startDefaults()
                if (%changed and 0b0001 === 0 || %composer.defaultsInvalid) {
                  if (%default and 0b0001 !== 0) {
                    a = 0
                  }
                  if (%default and 0b0010 !== 0) {
                    b = Foo()
                    %dirty = %dirty and 0b01110000.inv()
                  }
                } else {
                  %composer.skipToGroupEnd()
                  if (%default and 0b0010 !== 0) {
                    %dirty = %dirty and 0b01110000.inv()
                  }
                }
                %composer.endDefaults()
                used(a)
                used(b)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                CanSkip(a, b, %composer, %changed or 0b0001, %default)
              }
            }
            @Composable
            fun CannotSkip(a: Int, b: Foo, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(CannotSkip):Test.kt")
              used(a)
              used(b)
              print("Hello World")
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                CannotSkip(a, b, %composer, %changed or 0b0001)
              }
            }
            @Composable
            fun NoParams(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(NoParams):Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                print("Hello World")
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                NoParams(%composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testOptionalUnstableWithStableExtensionReceiver(): Unit = comparisonPropagation(
        """
            class Foo(var value: Int = 0)
            class Bar
        """,
        """
            @Composable fun Bar.CanSkip(b: Foo = Foo()) {
                print("Hello World")
            }
        """,
        """
            @Composable
            fun Bar.CanSkip(b: Foo?, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(CanSkip):Test.kt")
              val %dirty = %changed
              if (%default and 0b0001 !== 0b0001 || %dirty and 0b0001 !== 0 || !%composer.skipping) {
                %composer.startDefaults()
                if (%changed and 0b0001 === 0 || %composer.defaultsInvalid) {
                  if (%default and 0b0001 !== 0) {
                    b = Foo()
                    %dirty = %dirty and 0b01110000.inv()
                  }
                } else {
                  %composer.skipToGroupEnd()
                  if (%default and 0b0001 !== 0) {
                    %dirty = %dirty and 0b01110000.inv()
                  }
                }
                %composer.endDefaults()
                print("Hello World")
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                CanSkip(b, %composer, %changed or 0b0001, %default)
              }
            }
        """
    )

    @Test
    fun testNoParams(): Unit = comparisonPropagation(
        """
            @Composable fun A() {}
        """,
        """
            @Composable
            fun Test() {
                A()
            }
        """,
        """
            @Composable
            fun Test(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<A()>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                A(%composer, 0)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testSingleStableParam(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int) {}
        """,
        """
            @Composable
            fun Test(x: Int) {
                A(x)
            }
        """,
        """
            @Composable
            fun Test(x: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<A(x)>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                A(x, %composer, 0b1110 and %dirty)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(x, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testInlineClassDefaultParameter(): Unit = comparisonPropagation(
        """
            inline class Color(val value: Int) {
                companion object {
                    val Unset = Color(0)
                }
            }
        """,
        """
            @Composable
            fun A(text: String) {
                B(text)
            }

            @Composable
            fun B(text: String, color: Color = Color.Unset) {
                used(text)
                used(color)
            }
        """,
        """
            @Composable
            fun A(text: String, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(A)<B(text...>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(text)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                B(text, <unsafe-coerce>(0), %composer, 0b1110 and %dirty, 0b0010)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                A(text, %composer, %changed or 0b0001)
              }
            }
            @Composable
            fun B(text: String, color: Color, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(B)P(1,0:Color):Test.kt")
              val %dirty = %changed
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(text)) 0b0100 else 0b0010
              }
              if (%default and 0b0010 !== 0) {
                %dirty = %dirty or 0b00110000
              } else if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(<unsafe-coerce>(color))) 0b00100000 else 0b00010000
              }
              if (%dirty and 0b01011011 !== 0b00010010 || !%composer.skipping) {
                if (%default and 0b0010 !== 0) {
                  color = Companion.Unset
                }
                used(text)
                used(color)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                B(text, color, %composer, %changed or 0b0001, %default)
              }
            }
        """
    )

    @Test
    fun testStaticDetection(): Unit = comparisonPropagation(
        """
            import androidx.compose.runtime.Stable

            enum class Foo {
                Bar,
                Bam
            }
            const val constInt: Int = 123
            val normInt = 345
            val stableTopLevelProp: Modifier = Modifier
            @Composable fun C(x: Any?) {}
            @Stable
            interface Modifier {
              companion object : Modifier { }
            }
            inline class Dp(val value: Int)
            @Stable
            fun stableFun(x: Int): Int = x * x
            @Stable
            operator fun Dp.plus(other: Dp): Dp = Dp(this.value + other.value)
            @Stable
            val Int.dp: Dp get() = Dp(this)
            @Composable fun D(content: @Composable() () -> Unit) {}
        """,
        """
            // all of these should result in 0b0110
            @Composable fun A() {
                val x = 123
                D {}
                C({})
                C(stableFun(123))
                C(16.dp + 10.dp)
                C(Dp(16))
                C(16.dp)
                C(normInt)
                C(Int.MAX_VALUE)
                C(stableTopLevelProp)
                C(Modifier)
                C(Foo.Bar)
                C(constInt)
                C(123)
                C(123 + 345)
                C(x)
                C(x * 123)
            }
            // all of these should result in 0b0000
            @Composable fun B() {
                C(Math.random())
                C(Math.random() / 100f)
            }
        """,
        """
            @Composable
            fun A(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(A)<D>,<C({})>,<C(stab...>,<C(16.d...>,<C(Dp(1...>,<C(16.d...>,<C(norm...>,<C(Int....>,<C(stab...>,<C(Modi...>,<C(Foo....>,<C(cons...>,<C(123)>,<C(123>,<C(x)>,<C(x>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                val x = 123
                D(ComposableSingletons%TestKt.lambda-1, %composer, 0b0110)
                C({
                }, %composer, 0b0110)
                C(stableFun(123), %composer, 0b0110)
                C(16.dp + 10.dp, %composer, 0b0110)
                C(Dp(16), %composer, 0b0110)
                C(16.dp, %composer, 0b0110)
                C(normInt, %composer, 0b0110)
                C(Companion.MAX_VALUE, %composer, 0b0110)
                C(stableTopLevelProp, %composer, 0b0110)
                C(Companion, %composer, 0b0110)
                C(Foo.Bar, %composer, 0b0110)
                C(constInt, %composer, 0b0110)
                C(123, %composer, 0b0110)
                C(123 + 345, %composer, 0b0110)
                C(x, %composer, 0b0110)
                C(x * 123, %composer, 0b0110)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                A(%composer, %changed or 0b0001)
              }
            }
            @Composable
            fun B(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(B)<C(Math...>,<C(Math...>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                C(random(), %composer, 0)
                C(random() / 100.0f, %composer, 0)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                B(%composer, %changed or 0b0001)
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
            }
        """
    )

    @Test
    fun testAnnotationChecker(): Unit = comparisonPropagation(
        """
            @Composable fun D(content: @Composable() () -> Unit) {}
        """,
        """
            @Composable fun Example() {
                D {}
            }
        """,
        """
            @Composable
            fun Example(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Example)<D>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                D(ComposableSingletons%TestKt.lambda-1, %composer, 0b0110)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Example(%composer, %changed or 0b0001)
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
            }

        """
    )

    @Test
    fun testSingleStableParamWithDefault(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int) {}
        """,
        """
            @Composable
            fun Test(x: Int = 0) {
                A(x)
            }
        """,
        """
            @Composable
            fun Test(x: Int, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<A(x)>:Test.kt")
              val %dirty = %changed
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (%default and 0b0001 !== 0) {
                  x = 0
                }
                A(x, %composer, 0b1110 and %dirty)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(x, %composer, %changed or 0b0001, %default)
              }
            }
        """
    )

    @Test
    fun testSingleStableParamWithComposableDefault(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int) {}
            @Composable fun I(): Int { return 10 }
        """,
        """
            @Composable
            fun Test(x: Int = I()) {
                A(x)
            }
        """,
        """
            @Composable
            fun Test(x: Int, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<I()>,<A(x)>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%default and 0b0001 === 0 && %composer.changed(x)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                %composer.startDefaults()
                if (%changed and 0b0001 === 0 || %composer.defaultsInvalid) {
                  if (%default and 0b0001 !== 0) {
                    x = I(%composer, 0)
                    %dirty = %dirty and 0b1110.inv()
                  }
                } else {
                  %composer.skipToGroupEnd()
                  if (%default and 0b0001 !== 0) {
                    %dirty = %dirty and 0b1110.inv()
                  }
                }
                %composer.endDefaults()
                A(x, %composer, 0b1110 and %dirty)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(x, %composer, %changed or 0b0001, %default)
              }
            }
        """
    )

    @Test
    fun testSingleUnstableParam(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Foo) {}
            class Foo(var value: Int = 0)
        """,
        """
            @Composable
            fun Test(x: Foo) {
                A(x)
            }
        """,
        """
            @Composable
            fun Test(x: Foo, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<A(x)>:Test.kt")
              A(x, %composer, 0b1000)
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(x, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testSingleUnstableParamWithDefault(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Foo) {}
            class Foo
        """,
        """
            @Composable
            fun Test(x: Foo = Foo()) {
                A(x)
            }
        """,
        """
            @Composable
            fun Test(x: Foo?, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<A(x)>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%default and 0b0001 === 0 && %composer.changed(x)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                %composer.startDefaults()
                if (%changed and 0b0001 === 0 || %composer.defaultsInvalid) {
                  if (%default and 0b0001 !== 0) {
                    x = Foo()
                    %dirty = %dirty and 0b1110.inv()
                  }
                } else {
                  %composer.skipToGroupEnd()
                  if (%default and 0b0001 !== 0) {
                    %dirty = %dirty and 0b1110.inv()
                  }
                }
                %composer.endDefaults()
                A(x, %composer, 0b1110 and %dirty)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(x, %composer, %changed or 0b0001, %default)
              }
            }
        """
    )

    @Test
    fun testManyNonOptionalParams(): Unit = comparisonPropagation(
        """
            @Composable fun A(a: Int, b: Boolean, c: Int, d: Foo, e: List<Int>) {}
            class Foo
        """,
        """
            @Composable
            fun Test(a: Int, b: Boolean, c: Int = 0, d: Foo = Foo(), e: List<Int> = emptyList()) {
                A(a, b, c, d, e)
            }
        """,
        """
            @Composable
            fun Test(a: Int, b: Boolean, c: Int, d: Foo?, e: List<Int>?, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<A(a,>:Test.kt")
              val %dirty = %changed
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(a)) 0b0100 else 0b0010
              }
              if (%default and 0b0010 !== 0) {
                %dirty = %dirty or 0b00110000
              } else if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(b)) 0b00100000 else 0b00010000
              }
              if (%default and 0b0100 !== 0) {
                %dirty = %dirty or 0b000110000000
              } else if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(c)) 0b000100000000 else 0b10000000
              }
              if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%default and 0b1000 === 0 && %composer.changed(d)) 0b100000000000 else 0b010000000000
              }
              if (%default and 0b00010000 !== 0) {
                %dirty = %dirty or 0b0010000000000000
              }
              if (%default and 0b00010000 !== 0b00010000 || %dirty and 0b1011011011011011 !== 0b0010010010010010 || !%composer.skipping) {
                %composer.startDefaults()
                if (%changed and 0b0001 === 0 || %composer.defaultsInvalid) {
                  if (%default and 0b0100 !== 0) {
                    c = 0
                  }
                  if (%default and 0b1000 !== 0) {
                    d = Foo()
                    %dirty = %dirty and 0b0001110000000000.inv()
                  }
                  if (%default and 0b00010000 !== 0) {
                    e = emptyList()
                    %dirty = %dirty and 0b1110000000000000.inv()
                  }
                } else {
                  %composer.skipToGroupEnd()
                  if (%default and 0b1000 !== 0) {
                    %dirty = %dirty and 0b0001110000000000.inv()
                  }
                  if (%default and 0b00010000 !== 0) {
                    %dirty = %dirty and 0b1110000000000000.inv()
                  }
                }
                %composer.endDefaults()
                A(a, b, c, d, e, %composer, 0b1000000000000000 or 0b1110 and %dirty or 0b01110000 and %dirty or 0b001110000000 and %dirty or 0b0001110000000000 and %dirty)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(a, b, c, d, e, %composer, %changed or 0b0001, %default)
              }
            }
        """
    )

    @Test
    fun testRecursiveCall(): Unit = comparisonPropagation(
        """
        """,
        """
            @Composable
            fun X(x: Int) {
                X(x + 1)
                X(x)
            }
        """,
        """
            @Composable
            fun X(x: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(X)<X(x>,<X(x)>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                X(x + 1, %composer, 0)
                X(x, %composer, 0b1110 and %dirty)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                X(x, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testLambdaSkipping(): Unit = comparisonPropagation(
        """
        import androidx.compose.runtime.*

        data class User(
            val id: Int,
            val name: String
        )

        interface LazyPagingItems<T> {
            val itemCount: Int
            operator fun get(index: Int): State<T?>
        }

        @Stable interface LazyListScope {
            fun items(itemCount: Int, itemContent: @Composable LazyItemScope.(Int) -> Unit)
        }

        @Stable interface LazyItemScope

        public fun <T : Any> LazyListScope.itemsIndexed(
            lazyPagingItems: LazyPagingItems<T>,
            itemContent: @Composable LazyItemScope.(Int, T?) -> Unit
        ) {
            items(lazyPagingItems.itemCount) { index ->
                val item = lazyPagingItems[index].value
                itemContent(index, item)
            }
        }
        """,
        """
            fun LazyListScope.Example(items: LazyPagingItems<User>) {
                itemsIndexed(items) { index, user ->
                    print("Hello World")
                }
            }
        """,
        """
            fun LazyListScope.Example(items: LazyPagingItems<User>) {
              itemsIndexed(items, ComposableSingletons%TestKt.lambda-1)
            }
            internal object ComposableSingletons%TestKt {
              val lambda-1: @[ExtensionFunctionType] Function5<LazyItemScope, Int, User?, Composer, Int, Unit> = composableLambdaInstance(<>, false) { index: Int, user: User?, %composer: Composer?, %changed: Int ->
                sourceInformation(%composer, "C:Test.kt")
                if (%changed and 0b0001010000000001 !== 0b010000000000 || !%composer.skipping) {
                  print("Hello World")
                } else {
                  %composer.skipToGroupEnd()
                }
              }
            }
        """
    )

    @Test
    fun testPassedExtensionWhenExtensionIsPotentiallyUnstable(): Unit = comparisonPropagation(
        """
            interface Unstable
        """,
        """
            @Composable fun Unstable.Test() {
                doSomething(this) // does this reference %dirty without %dirty
            }

            @Composable fun doSomething(x: Unstable) {}
        """,
        """
            @Composable
            fun Unstable.Test(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<doSome...>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(<this>)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                doSomething(<this>, %composer, 0b1110 and %dirty)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, %changed or 0b0001)
              }
            }
            @Composable
            fun doSomething(x: Unstable, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(doSomething):Test.kt")
              if (%changed and 0b0001 !== 0 || !%composer.skipping) {
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                doSomething(x, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testReceiverIssue(): Unit = comparisonPropagation(
        """
            class Foo
        """,
        """
            import androidx.compose.runtime.ExplicitGroupsComposable

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
                print(label)
            }
        """,
        """
            @Composable
            @ExplicitGroupsComposable
            fun A(foo: Foo, %composer: Composer?, %changed: Int) {
              foo.b(null, %composer, 0b1110 and %changed, 0b0001)
            }
            @Composable
            @ExplicitGroupsComposable
            fun Foo.b(label: String?, %composer: Composer?, %changed: Int, %default: Int) {
              if (%default and 0b0001 !== 0) {
                label = ""
              }
              c(<this>, label, %composer, 0b1110 and %changed or 0b01110000 and %changed)
            }
            @Composable
            @ExplicitGroupsComposable
            fun c(foo: Foo, label: String, %composer: Composer?, %changed: Int) {
              print(label)
            }
        """
    )

    @Test
    fun testDifferentParameters(): Unit = comparisonPropagation(
        """
            @Composable fun B(a: Int, b: Int, c: Int, d: Int) {}
            val fooGlobal = 10
        """,
        """
            @Composable
            fun A(x: Int) {
                B(
                    // direct parameter
                    x,
                    // transformation
                    x + 1,
                    // literal
                    123,
                    // expression with no parameter
                    fooGlobal
                )
            }
        """,
        """
            @Composable
            fun A(x: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(A)<B(>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                B(x, x + 1, 123, fooGlobal, %composer, 0b110110000000 or 0b1110 and %dirty)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                A(x, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testReceiverLambdaCall(): Unit = comparisonPropagation(
        """
            import androidx.compose.runtime.Stable

            interface Foo { val x: Int }
            @Stable
            interface StableFoo { val x: Int }
        """,
        """
            val unstableUnused: @Composable Foo.() -> Unit = {
            }
            val unstableUsed: @Composable Foo.() -> Unit = {
                used(x)
            }
            val stableUnused: @Composable StableFoo.() -> Unit = {
            }
            val stableUsed: @Composable StableFoo.() -> Unit = {
                used(x)
            }
        """,
        """
            val unstableUnused: @[ExtensionFunctionType] Function3<Foo, Composer, Int, Unit> = ComposableSingletons%TestKt.lambda-1
            val unstableUsed: @[ExtensionFunctionType] Function3<Foo, Composer, Int, Unit> = ComposableSingletons%TestKt.lambda-2
            val stableUnused: @[ExtensionFunctionType] Function3<StableFoo, Composer, Int, Unit> = ComposableSingletons%TestKt.lambda-3
            val stableUsed: @[ExtensionFunctionType] Function3<StableFoo, Composer, Int, Unit> = ComposableSingletons%TestKt.lambda-4
            internal object ComposableSingletons%TestKt {
              val lambda-1: @[ExtensionFunctionType] Function3<Foo, Composer, Int, Unit> = composableLambdaInstance(<>, false) { %composer: Composer?, %changed: Int ->
                sourceInformation(%composer, "C:Test.kt")
                if (%changed and 0b01010001 !== 0b00010000 || !%composer.skipping) {
                  Unit
                } else {
                  %composer.skipToGroupEnd()
                }
              }
              val lambda-2: @[ExtensionFunctionType] Function3<Foo, Composer, Int, Unit> = composableLambdaInstance(<>, false) { %composer: Composer?, %changed: Int ->
                sourceInformation(%composer, "C:Test.kt")
                val %dirty = %changed
                if (%changed and 0b1110 === 0) {
                  %dirty = %dirty or if (%composer.changed(%this%null)) 0b0100 else 0b0010
                }
                if (%dirty and 0b01011011 !== 0b00010010 || !%composer.skipping) {
                  used(%this%null.x)
                } else {
                  %composer.skipToGroupEnd()
                }
              }
              val lambda-3: @[ExtensionFunctionType] Function3<StableFoo, Composer, Int, Unit> = composableLambdaInstance(<>, false) { %composer: Composer?, %changed: Int ->
                sourceInformation(%composer, "C:Test.kt")
                if (%changed and 0b01010001 !== 0b00010000 || !%composer.skipping) {
                  Unit
                } else {
                  %composer.skipToGroupEnd()
                }
              }
              val lambda-4: @[ExtensionFunctionType] Function3<StableFoo, Composer, Int, Unit> = composableLambdaInstance(<>, false) { %composer: Composer?, %changed: Int ->
                sourceInformation(%composer, "C:Test.kt")
                val %dirty = %changed
                if (%changed and 0b1110 === 0) {
                  %dirty = %dirty or if (%composer.changed(%this%null)) 0b0100 else 0b0010
                }
                if (%dirty and 0b01011011 !== 0b00010010 || !%composer.skipping) {
                  used(%this%null.x)
                } else {
                  %composer.skipToGroupEnd()
                }
              }
            }
        """
    )

    @Test
    fun testNestedCalls(): Unit = comparisonPropagation(
        """
            @Composable fun B(a: Int = 0, b: Int = 0, c: Int = 0) {}
            @Composable fun Provide(content: @Composable (Int) -> Unit) {}
        """,
        """
            @Composable
            fun A(x: Int) {
                Provide { y ->
                    Provide { z ->
                        B(x, y, z)
                    }
                    B(x, y)
                }
                B(x)
            }
        """,
        """
            @Composable
            fun A(x: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(A)<Provid...>,<B(x)>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                Provide(composableLambda(%composer, <>, true) { y: Int, %composer: Composer?, %changed: Int ->
                  sourceInformation(%composer, "C<Provid...>,<B(x,>:Test.kt")
                  val %dirty = %changed
                  if (%changed and 0b1110 === 0) {
                    %dirty = %dirty or if (%composer.changed(y)) 0b0100 else 0b0010
                  }
                  if (%dirty and 0b01011011 !== 0b00010010 || !%composer.skipping) {
                    Provide(composableLambda(%composer, <>, true) { z: Int, %composer: Composer?, %changed: Int ->
                      sourceInformation(%composer, "C<B(x,>:Test.kt")
                      val %dirty = %changed
                      if (%changed and 0b1110 === 0) {
                        %dirty = %dirty or if (%composer.changed(z)) 0b0100 else 0b0010
                      }
                      if (%dirty and 0b01011011 !== 0b00010010 || !%composer.skipping) {
                        B(x, y, z, %composer, 0b1110 and %dirty or 0b01110000 and %dirty shl 0b0011 or 0b001110000000 and %dirty shl 0b0110, 0)
                      } else {
                        %composer.skipToGroupEnd()
                      }
                    }, %composer, 0b0110)
                    B(x, y, 0, %composer, 0b1110 and %dirty or 0b01110000 and %dirty shl 0b0011, 0b0100)
                  } else {
                    %composer.skipToGroupEnd()
                  }
                }, %composer, 0b0110)
                B(x, 0, 0, %composer, 0b1110 and %dirty, 0b0110)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                A(x, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testLocalFunction(): Unit = comparisonPropagation(
        """
            @Composable fun B(a: Int, b: Int) {}
        """,
        """
            @Composable
            fun A(x: Int) {
                @Composable fun foo(y: Int) {
                    B(x, y)
                }
                foo(x)
            }
        """,
        """
            @Composable
            fun A(x: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(A)<foo(x)>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                @Composable
                fun foo(y: Int, %composer: Composer?, %changed: Int) {
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C(foo)<B(x,>:Test.kt")
                  B(x, y, %composer, 0b1110 and %dirty or 0b01110000 and %changed shl 0b0011)
                  %composer.endReplaceableGroup()
                }
                foo(x, %composer, 0b1110 and %dirty)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                A(x, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun test15Parameters(): Unit = comparisonPropagation(
        """
        """,
        """
            @Composable
            fun Example(
                a00: Int = 0,
                a01: Int = 0,
                a02: Int = 0,
                a03: Int = 0,
                a04: Int = 0,
                a05: Int = 0,
                a06: Int = 0,
                a07: Int = 0,
                a08: Int = 0,
                a09: Int = 0,
                a10: Int = 0,
                a11: Int = 0,
                a12: Int = 0,
                a13: Int = 0,
                a14: Int = 0
            ) {
                // in order
                Example(
                    a00,
                    a01,
                    a02,
                    a03,
                    a04,
                    a05,
                    a06,
                    a07,
                    a08,
                    a09,
                    a10,
                    a11,
                    a12,
                    a13,
                    a14
                )
                // in opposite order
                Example(
                    a14,
                    a13,
                    a12,
                    a11,
                    a10,
                    a09,
                    a08,
                    a07,
                    a06,
                    a05,
                    a04,
                    a03,
                    a02,
                    a01,
                    a00
                )
            }
        """,
        """
            @Composable
            fun Example(a00: Int, a01: Int, a02: Int, a03: Int, a04: Int, a05: Int, a06: Int, a07: Int, a08: Int, a09: Int, a10: Int, a11: Int, a12: Int, a13: Int, a14: Int, %composer: Composer?, %changed: Int, %changed1: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Example)<Exampl...>,<Exampl...>:Test.kt")
              val %dirty = %changed
              val %dirty1 = %changed1
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(a00)) 0b0100 else 0b0010
              }
              if (%default and 0b0010 !== 0) {
                %dirty = %dirty or 0b00110000
              } else if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(a01)) 0b00100000 else 0b00010000
              }
              if (%default and 0b0100 !== 0) {
                %dirty = %dirty or 0b000110000000
              } else if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a02)) 0b000100000000 else 0b10000000
              }
              if (%default and 0b1000 !== 0) {
                %dirty = %dirty or 0b110000000000
              } else if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a03)) 0b100000000000 else 0b010000000000
              }
              if (%default and 0b00010000 !== 0) {
                %dirty = %dirty or 0b0110000000000000
              } else if (%changed and 0b1110000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a04)) 0b0100000000000000 else 0b0010000000000000
              }
              if (%default and 0b00100000 !== 0) {
                %dirty = %dirty or 0b00110000000000000000
              } else if (%changed and 0b01110000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a05)) 0b00100000000000000000 else 0b00010000000000000000
              }
              if (%default and 0b01000000 !== 0) {
                %dirty = %dirty or 0b000110000000000000000000
              } else if (%changed and 0b001110000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a06)) 0b000100000000000000000000 else 0b10000000000000000000
              }
              if (%default and 0b10000000 !== 0) {
                %dirty = %dirty or 0b110000000000000000000000
              } else if (%changed and 0b0001110000000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a07)) 0b100000000000000000000000 else 0b010000000000000000000000
              }
              if (%default and 0b000100000000 !== 0) {
                %dirty = %dirty or 0b0110000000000000000000000000
              } else if (%changed and 0b1110000000000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a08)) 0b0100000000000000000000000000 else 0b0010000000000000000000000000
              }
              if (%default and 0b001000000000 !== 0) {
                %dirty = %dirty or 0b00110000000000000000000000000000
              } else if (%changed and 0b01110000000000000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a09)) 0b00100000000000000000000000000000 else 0b00010000000000000000000000000000
              }
              if (%default and 0b010000000000 !== 0) {
                %dirty1 = %dirty1 or 0b0110
              } else if (%changed1 and 0b1110 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a10)) 0b0100 else 0b0010
              }
              if (%default and 0b100000000000 !== 0) {
                %dirty1 = %dirty1 or 0b00110000
              } else if (%changed1 and 0b01110000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a11)) 0b00100000 else 0b00010000
              }
              if (%default and 0b0001000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b000110000000
              } else if (%changed1 and 0b001110000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a12)) 0b000100000000 else 0b10000000
              }
              if (%default and 0b0010000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b110000000000
              } else if (%changed1 and 0b0001110000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a13)) 0b100000000000 else 0b010000000000
              }
              if (%default and 0b0100000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b0110000000000000
              } else if (%changed1 and 0b1110000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a14)) 0b0100000000000000 else 0b0010000000000000
              }
              if (%dirty and 0b01011011011011011011011011011011 !== 0b00010010010010010010010010010010 || %dirty1 and 0b1011011011011011 !== 0b0010010010010010 || !%composer.skipping) {
                if (%default and 0b0001 !== 0) {
                  a00 = 0
                }
                if (%default and 0b0010 !== 0) {
                  a01 = 0
                }
                if (%default and 0b0100 !== 0) {
                  a02 = 0
                }
                if (%default and 0b1000 !== 0) {
                  a03 = 0
                }
                if (%default and 0b00010000 !== 0) {
                  a04 = 0
                }
                if (%default and 0b00100000 !== 0) {
                  a05 = 0
                }
                if (%default and 0b01000000 !== 0) {
                  a06 = 0
                }
                if (%default and 0b10000000 !== 0) {
                  a07 = 0
                }
                if (%default and 0b000100000000 !== 0) {
                  a08 = 0
                }
                if (%default and 0b001000000000 !== 0) {
                  a09 = 0
                }
                if (%default and 0b010000000000 !== 0) {
                  a10 = 0
                }
                if (%default and 0b100000000000 !== 0) {
                  a11 = 0
                }
                if (%default and 0b0001000000000000 !== 0) {
                  a12 = 0
                }
                if (%default and 0b0010000000000000 !== 0) {
                  a13 = 0
                }
                if (%default and 0b0100000000000000 !== 0) {
                  a14 = 0
                }
                Example(a00, a01, a02, a03, a04, a05, a06, a07, a08, a09, a10, a11, a12, a13, a14, %composer, 0b1110 and %dirty or 0b01110000 and %dirty or 0b001110000000 and %dirty or 0b0001110000000000 and %dirty or 0b1110000000000000 and %dirty or 0b01110000000000000000 and %dirty or 0b001110000000000000000000 and %dirty or 0b0001110000000000000000000000 and %dirty or 0b1110000000000000000000000000 and %dirty or 0b01110000000000000000000000000000 and %dirty, 0b1110 and %dirty1 or 0b01110000 and %dirty1 or 0b001110000000 and %dirty1 or 0b0001110000000000 and %dirty1 or 0b1110000000000000 and %dirty1, 0)
                Example(a14, a13, a12, a11, a10, a09, a08, a07, a06, a05, a04, a03, a02, a01, a00, %composer, 0b1110 and %dirty1 shr 0b1100 or 0b01110000 and %dirty1 shr 0b0110 or 0b001110000000 and %dirty1 or 0b0001110000000000 and %dirty1 shl 0b0110 or 0b1110000000000000 and %dirty1 shl 0b1100 or 0b01110000000000000000 and %dirty shr 0b1100 or 0b001110000000000000000000 and %dirty shr 0b0110 or 0b0001110000000000000000000000 and %dirty or 0b1110000000000000000000000000 and %dirty shl 0b0110 or 0b01110000000000000000000000000000 and %dirty shl 0b1100, 0b1110 and %dirty shr 0b1100 or 0b01110000 and %dirty shr 0b0110 or 0b001110000000 and %dirty or 0b0001110000000000 and %dirty shl 0b0110 or 0b1110000000000000 and %dirty shl 0b1100, 0)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Example(a00, a01, a02, a03, a04, a05, a06, a07, a08, a09, a10, a11, a12, a13, a14, %composer, %changed or 0b0001, %changed1, %default)
              }
            }
        """
    )

    @Test
    fun test16Parameters(): Unit = comparisonPropagation(
        """
        """,
        """
            @Composable
            fun Example(
                a00: Int = 0,
                a01: Int = 0,
                a02: Int = 0,
                a03: Int = 0,
                a04: Int = 0,
                a05: Int = 0,
                a06: Int = 0,
                a07: Int = 0,
                a08: Int = 0,
                a09: Int = 0,
                a10: Int = 0,
                a11: Int = 0,
                a12: Int = 0,
                a13: Int = 0,
                a14: Int = 0,
                a15: Int = 0
            ) {
                // in order
                Example(
                    a00,
                    a01,
                    a02,
                    a03,
                    a04,
                    a05,
                    a06,
                    a07,
                    a08,
                    a09,
                    a10,
                    a11,
                    a12,
                    a13,
                    a14,
                    a15
                )
                // in opposite order
                Example(
                    a15,
                    a14,
                    a13,
                    a12,
                    a11,
                    a10,
                    a09,
                    a08,
                    a07,
                    a06,
                    a05,
                    a04,
                    a03,
                    a02,
                    a01,
                    a00
                )
            }
        """,
        """
            @Composable
            fun Example(a00: Int, a01: Int, a02: Int, a03: Int, a04: Int, a05: Int, a06: Int, a07: Int, a08: Int, a09: Int, a10: Int, a11: Int, a12: Int, a13: Int, a14: Int, a15: Int, %composer: Composer?, %changed: Int, %changed1: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Example)<Exampl...>,<Exampl...>:Test.kt")
              val %dirty = %changed
              val %dirty1 = %changed1
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(a00)) 0b0100 else 0b0010
              }
              if (%default and 0b0010 !== 0) {
                %dirty = %dirty or 0b00110000
              } else if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(a01)) 0b00100000 else 0b00010000
              }
              if (%default and 0b0100 !== 0) {
                %dirty = %dirty or 0b000110000000
              } else if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a02)) 0b000100000000 else 0b10000000
              }
              if (%default and 0b1000 !== 0) {
                %dirty = %dirty or 0b110000000000
              } else if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a03)) 0b100000000000 else 0b010000000000
              }
              if (%default and 0b00010000 !== 0) {
                %dirty = %dirty or 0b0110000000000000
              } else if (%changed and 0b1110000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a04)) 0b0100000000000000 else 0b0010000000000000
              }
              if (%default and 0b00100000 !== 0) {
                %dirty = %dirty or 0b00110000000000000000
              } else if (%changed and 0b01110000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a05)) 0b00100000000000000000 else 0b00010000000000000000
              }
              if (%default and 0b01000000 !== 0) {
                %dirty = %dirty or 0b000110000000000000000000
              } else if (%changed and 0b001110000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a06)) 0b000100000000000000000000 else 0b10000000000000000000
              }
              if (%default and 0b10000000 !== 0) {
                %dirty = %dirty or 0b110000000000000000000000
              } else if (%changed and 0b0001110000000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a07)) 0b100000000000000000000000 else 0b010000000000000000000000
              }
              if (%default and 0b000100000000 !== 0) {
                %dirty = %dirty or 0b0110000000000000000000000000
              } else if (%changed and 0b1110000000000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a08)) 0b0100000000000000000000000000 else 0b0010000000000000000000000000
              }
              if (%default and 0b001000000000 !== 0) {
                %dirty = %dirty or 0b00110000000000000000000000000000
              } else if (%changed and 0b01110000000000000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a09)) 0b00100000000000000000000000000000 else 0b00010000000000000000000000000000
              }
              if (%default and 0b010000000000 !== 0) {
                %dirty1 = %dirty1 or 0b0110
              } else if (%changed1 and 0b1110 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a10)) 0b0100 else 0b0010
              }
              if (%default and 0b100000000000 !== 0) {
                %dirty1 = %dirty1 or 0b00110000
              } else if (%changed1 and 0b01110000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a11)) 0b00100000 else 0b00010000
              }
              if (%default and 0b0001000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b000110000000
              } else if (%changed1 and 0b001110000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a12)) 0b000100000000 else 0b10000000
              }
              if (%default and 0b0010000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b110000000000
              } else if (%changed1 and 0b0001110000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a13)) 0b100000000000 else 0b010000000000
              }
              if (%default and 0b0100000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b0110000000000000
              } else if (%changed1 and 0b1110000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a14)) 0b0100000000000000 else 0b0010000000000000
              }
              if (%default and 0b1000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b00110000000000000000
              } else if (%changed1 and 0b01110000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a15)) 0b00100000000000000000 else 0b00010000000000000000
              }
              if (%dirty and 0b01011011011011011011011011011011 !== 0b00010010010010010010010010010010 || %dirty1 and 0b01011011011011011011 !== 0b00010010010010010010 || !%composer.skipping) {
                if (%default and 0b0001 !== 0) {
                  a00 = 0
                }
                if (%default and 0b0010 !== 0) {
                  a01 = 0
                }
                if (%default and 0b0100 !== 0) {
                  a02 = 0
                }
                if (%default and 0b1000 !== 0) {
                  a03 = 0
                }
                if (%default and 0b00010000 !== 0) {
                  a04 = 0
                }
                if (%default and 0b00100000 !== 0) {
                  a05 = 0
                }
                if (%default and 0b01000000 !== 0) {
                  a06 = 0
                }
                if (%default and 0b10000000 !== 0) {
                  a07 = 0
                }
                if (%default and 0b000100000000 !== 0) {
                  a08 = 0
                }
                if (%default and 0b001000000000 !== 0) {
                  a09 = 0
                }
                if (%default and 0b010000000000 !== 0) {
                  a10 = 0
                }
                if (%default and 0b100000000000 !== 0) {
                  a11 = 0
                }
                if (%default and 0b0001000000000000 !== 0) {
                  a12 = 0
                }
                if (%default and 0b0010000000000000 !== 0) {
                  a13 = 0
                }
                if (%default and 0b0100000000000000 !== 0) {
                  a14 = 0
                }
                if (%default and 0b1000000000000000 !== 0) {
                  a15 = 0
                }
                Example(a00, a01, a02, a03, a04, a05, a06, a07, a08, a09, a10, a11, a12, a13, a14, a15, %composer, 0b1110 and %dirty or 0b01110000 and %dirty or 0b001110000000 and %dirty or 0b0001110000000000 and %dirty or 0b1110000000000000 and %dirty or 0b01110000000000000000 and %dirty or 0b001110000000000000000000 and %dirty or 0b0001110000000000000000000000 and %dirty or 0b1110000000000000000000000000 and %dirty or 0b01110000000000000000000000000000 and %dirty, 0b1110 and %dirty1 or 0b01110000 and %dirty1 or 0b001110000000 and %dirty1 or 0b0001110000000000 and %dirty1 or 0b1110000000000000 and %dirty1 or 0b01110000000000000000 and %dirty1, 0)
                Example(a15, a14, a13, a12, a11, a10, a09, a08, a07, a06, a05, a04, a03, a02, a01, a00, %composer, 0b1110 and %dirty1 shr 0b1111 or 0b01110000 and %dirty1 shr 0b1001 or 0b001110000000 and %dirty1 shr 0b0011 or 0b0001110000000000 and %dirty1 shl 0b0011 or 0b1110000000000000 and %dirty1 shl 0b1001 or 0b01110000000000000000 and %dirty1 shl 0b1111 or 0b001110000000000000000000 and %dirty shr 0b1001 or 0b0001110000000000000000000000 and %dirty shr 0b0011 or 0b1110000000000000000000000000 and %dirty shl 0b0011 or 0b01110000000000000000000000000000 and %dirty shl 0b1001, 0b1110 and %dirty shr 0b1111 or 0b01110000 and %dirty shr 0b1001 or 0b001110000000 and %dirty shr 0b0011 or 0b0001110000000000 and %dirty shl 0b0011 or 0b1110000000000000 and %dirty shl 0b1001 or 0b01110000000000000000 and %dirty shl 0b1111, 0)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Example(a00, a01, a02, a03, a04, a05, a06, a07, a08, a09, a10, a11, a12, a13, a14, a15, %composer, %changed or 0b0001, %changed1, %default)
              }
            }
        """
    )

    @Test
    fun testGrouplessProperty(): Unit = comparisonPropagation(
        """
        """,
        """
            import androidx.compose.runtime.currentComposer

            open class Foo {
                inline val current: Int
                    @Composable
                    @ReadOnlyComposable get() = currentComposer.hashCode()

                @ReadOnlyComposable
                @Composable
                fun getHashCode(): Int = currentComposer.hashCode()
            }

            @ReadOnlyComposable
            @Composable
            fun getHashCode(): Int = currentComposer.hashCode()
        """,
        """
            @StabilityInferred(parameters = 0)
            open class Foo {
              val current: Int
                @Composable @ReadOnlyComposable @JvmName(name = "getCurrent")
                get() {
                  sourceInformationMarkerStart(%composer, <>, "C:Test.kt")
                  val tmp0 = %composer.hashCode()
                  sourceInformationMarkerEnd(%composer)
                  return tmp0
                }
              @ReadOnlyComposable
              @Composable
              fun getHashCode(%composer: Composer?, %changed: Int): Int {
                sourceInformationMarkerStart(%composer, <>, "C(getHashCode):Test.kt")
                val tmp0 = %composer.hashCode()
                sourceInformationMarkerEnd(%composer)
                return tmp0
              }
              static val %stable: Int = 0
            }
            @ReadOnlyComposable
            @Composable
            fun getHashCode(%composer: Composer?, %changed: Int): Int {
              sourceInformationMarkerStart(%composer, <>, "C(getHashCode):Test.kt")
              val tmp0 = %composer.hashCode()
              sourceInformationMarkerEnd(%composer)
              return tmp0
            }
        """
    )

    @Test
    fun testStaticAndNonStaticDefaultValueSkipping(): Unit = comparisonPropagation(
        """
            import androidx.compose.runtime.compositionLocalOf

            val LocalColor = compositionLocalOf { 123 }
            @Composable fun A(a: Int) {}
        """,
        """
            @Composable
            fun Example(
                wontChange: Int = 123,
                mightChange: Int = LocalColor.current
            ) {
                A(wontChange)
                A(mightChange)
            }
        """,
        """
            @Composable
            fun Example(wontChange: Int, mightChange: Int, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Example)P(1)<curren...>,<A(wont...>,<A(migh...>:Test.kt")
              val %dirty = %changed
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(wontChange)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%default and 0b0010 === 0 && %composer.changed(mightChange)) 0b00100000 else 0b00010000
              }
              if (%dirty and 0b01011011 !== 0b00010010 || !%composer.skipping) {
                %composer.startDefaults()
                if (%changed and 0b0001 === 0 || %composer.defaultsInvalid) {
                  if (%default and 0b0001 !== 0) {
                    wontChange = 123
                  }
                  if (%default and 0b0010 !== 0) {
                    mightChange = LocalColor.current
                    %dirty = %dirty and 0b01110000.inv()
                  }
                } else {
                  %composer.skipToGroupEnd()
                  if (%default and 0b0010 !== 0) {
                    %dirty = %dirty and 0b01110000.inv()
                  }
                }
                %composer.endDefaults()
                A(wontChange, %composer, 0b1110 and %dirty)
                A(mightChange, %composer, 0b1110 and %dirty shr 0b0011)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Example(wontChange, mightChange, %composer, %changed or 0b0001, %default)
              }
            }
        """
    )

    @Test
    fun testComposableLambdaInvoke(): Unit = comparisonPropagation(
        """
        """,
        """
            @Composable fun Example(content: @Composable() () -> Unit) {
                content.invoke()
            }
        """,
        """
            @Composable
            @ComposableInferredTarget(scheme = "[0[0]]")
            fun Example(content: Function2<Composer, Int, Unit>, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Example)<invoke...>:Test.kt")
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
                Example(content, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testComposableLambdasWithReturnGetGroups(): Unit = comparisonPropagation(
        """
        """,
        """
            fun A(factory: @Composable () -> Int): Unit {}
            fun B() = A { 123 }
        """,
        """
            fun A(factory: Function2<Composer, Int, Int>) { }
            fun B() {
              return A { %composer: Composer?, %changed: Int ->
                %composer.startReplaceableGroup(<>)
                val tmp0 = 123
                %composer.endReplaceableGroup()
                tmp0
              }
            }
        """
    )

    @Test
    fun testDefaultsIssue(): Unit = comparisonPropagation(
        """
        """,
        """
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.Dp

            @Composable
            fun Box2(
                modifier: Modifier = Modifier,
                paddingStart: Dp = Dp.Unspecified,
                content: @Composable () -> Unit = {}
            ) {
                used(modifier)
                used(paddingStart)
                content()
            }
        """,
        """
            @Composable
            @ComposableInferredTarget(scheme = "[0[0]]")
            fun Box2(modifier: Modifier?, paddingStart: Dp, content: Function2<Composer, Int, Unit>?, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Box2)P(1,2:c#ui.unit.Dp)<conten...>:Test.kt")
              val %dirty = %changed
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(modifier)) 0b0100 else 0b0010
              }
              if (%default and 0b0010 !== 0) {
                %dirty = %dirty or 0b00110000
              } else if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(<unsafe-coerce>(paddingStart))) 0b00100000 else 0b00010000
              }
              if (%default and 0b0100 !== 0) {
                %dirty = %dirty or 0b000110000000
              } else if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(content)) 0b000100000000 else 0b10000000
              }
              if (%dirty and 0b001011011011 !== 0b10010010 || !%composer.skipping) {
                if (%default and 0b0001 !== 0) {
                  modifier = Companion
                }
                if (%default and 0b0010 !== 0) {
                  paddingStart = Companion.Unspecified
                }
                if (%default and 0b0100 !== 0) {
                  content = ComposableSingletons%TestKt.lambda-1
                }
                used(modifier)
                used(paddingStart)
                content(%composer, 0b1110 and %dirty shr 0b0110)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Box2(modifier, paddingStart, content, %composer, %changed or 0b0001, %default)
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
            }
        """
    )

    @Test
    fun testSiblingIfsWithoutElseHaveUniqueKeys(): Unit = comparisonPropagation(
        """
            @Composable fun A(){}
            @Composable fun B(){}
        """,
        """
            @Composable
            fun Test(cond: Boolean) {
                if (cond) {
                    A()
                }
                if (cond) {
                    B()
                }
            }
        """,
        """
            @Composable
            fun Test(cond: Boolean, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<B()>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(cond)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                %composer.startReplaceableGroup(<>)
                sourceInformation(%composer, "<A()>")
                if (cond) {
                  A(%composer, 0)
                }
                %composer.endReplaceableGroup()
                if (cond) {
                  B(%composer, 0)
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(cond, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testUnusedParameters(): Unit = comparisonPropagation(
        """
            class Unstable(var count: Int)
            class Stable(val count: Int)
            interface MaybeStable
        """,
        """
            @Composable
            fun Unskippable(a: Unstable, b: Stable, c: MaybeStable) {
                used(a)
            }
            @Composable
            fun Skippable1(a: Unstable, b: Stable, c: MaybeStable) {
                used(b)
            }
            @Composable
            fun Skippable2(a: Unstable, b: Stable, c: MaybeStable) {
                used(c)
            }
            @Composable
            fun Skippable3(a: Unstable, b: Stable, c: MaybeStable) { }
        """,
        """
            @Composable
            fun Unskippable(a: Unstable, b: Stable, c: MaybeStable, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Unskippable):Test.kt")
              used(a)
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Unskippable(a, b, c, %composer, %changed or 0b0001)
              }
            }
            @Composable
            fun Skippable1(a: Unstable, b: Stable, c: MaybeStable, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Skippable1):Test.kt")
              val %dirty = %changed
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(b)) 0b00100000 else 0b00010000
              }
              if (%dirty and 0b01010001 !== 0b00010000 || !%composer.skipping) {
                used(b)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Skippable1(a, b, c, %composer, %changed or 0b0001)
              }
            }
            @Composable
            fun Skippable2(a: Unstable, b: Stable, c: MaybeStable, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Skippable2):Test.kt")
              val %dirty = %changed
              if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(c)) 0b000100000000 else 0b10000000
              }
              if (%dirty and 0b001010000001 !== 0b10000000 || !%composer.skipping) {
                used(c)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Skippable2(a, b, c, %composer, %changed or 0b0001)
              }
            }
            @Composable
            fun Skippable3(a: Unstable, b: Stable, c: MaybeStable, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Skippable3):Test.kt")
              if (%changed and 0b0001 !== 0 || !%composer.skipping) {
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Skippable3(a, b, c, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testExtensionReceiver(): Unit = comparisonPropagation(
        """
            interface MaybeStable
        """,
        """
            @Composable fun MaybeStable.example(x: Int) {
                used(this)
                used(x)
            }
            val example: @Composable MaybeStable.(Int) -> Unit = {
                used(this)
                used(it)
            }
        """,
        """
            @Composable
            fun MaybeStable.example(x: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(example):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(<this>)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(x)) 0b00100000 else 0b00010000
              }
              if (%dirty and 0b01011011 !== 0b00010010 || !%composer.skipping) {
                used(<this>)
                used(x)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                example(x, %composer, %changed or 0b0001)
              }
            }
            val example: @[ExtensionFunctionType] Function4<MaybeStable, Int, Composer, Int, Unit> = ComposableSingletons%TestKt.lambda-1
            internal object ComposableSingletons%TestKt {
              val lambda-1: @[ExtensionFunctionType] Function4<MaybeStable, Int, Composer, Int, Unit> = composableLambdaInstance(<>, false) { it: Int, %composer: Composer?, %changed: Int ->
                sourceInformation(%composer, "C:Test.kt")
                val %dirty = %changed
                if (%changed and 0b1110 === 0) {
                  %dirty = %dirty or if (%composer.changed(%this%null)) 0b0100 else 0b0010
                }
                if (%changed and 0b01110000 === 0) {
                  %dirty = %dirty or if (%composer.changed(it)) 0b00100000 else 0b00010000
                }
                if (%dirty and 0b001011011011 !== 0b10010010 || !%composer.skipping) {
                  used(%this%null)
                  used(it)
                } else {
                  %composer.skipToGroupEnd()
                }
              }
            }
        """
    )

    @Test
    fun testArrayDefaultArgWithState(): Unit = comparisonPropagation(
        """
        """,
        """
            import androidx.compose.runtime.MutableState

            @Composable
            fun VarargComposable(state: MutableState<Int>, vararg values: String = Array(1) { "value " + it }) {
                state.value
            }
        """,
        """
            @Composable
            fun VarargComposable(state: MutableState<Int>, values: Array<out String>?, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(VarargComposable):Test.kt")
              val %dirty = %changed
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(state)) 0b0100 else 0b0010
              }
              %composer.startMovableGroup(<>, values.size)
              val tmp0_iterator = values.iterator()
              while (tmp0_iterator.hasNext()) {
                val value = tmp0_iterator.next()
                %dirty = %dirty or if (%composer.changed(value)) 0b00100000 else 0
              }
              %composer.endMovableGroup()
              if (%dirty and 0b01110000 === 0) {
                %dirty = %dirty or 0b00010000
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                %composer.startDefaults()
                if (%changed and 0b0001 === 0 || %composer.defaultsInvalid) {
                  if (%default and 0b0010 !== 0) {
                    values = Array(1) { it: Int ->
                      val tmp0_return = "value " + it
                      tmp0_return
                    }
                    %dirty = %dirty and 0b01110000.inv()
                  }
                } else {
                  %composer.skipToGroupEnd()
                  if (%default and 0b0010 !== 0) {
                    %dirty = %dirty and 0b01110000.inv()
                  }
                }
                %composer.endDefaults()
                state.value
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                VarargComposable(state, *values, %composer, %changed or 0b0001, %default)
              }
            }
        """
    )
}

class FunctionBodySkippingTransformTestsNoSource : FunctionBodySkippingTransfomrTestsBase() {
    override val sourceInformationEnabled: Boolean get() = false

    @Test
    fun testGrouplessProperty(): Unit = comparisonPropagation(
        """
        """,
        """
            import androidx.compose.runtime.currentComposer

            open class Foo {
                inline val current: Int
                    @Composable
                    @ReadOnlyComposable get() = currentComposer.hashCode()

                @ReadOnlyComposable
                @Composable
                fun getHashCode(): Int = currentComposer.hashCode()
            }

            @ReadOnlyComposable
            @Composable
            fun getHashCode(): Int = currentComposer.hashCode()
        """,
        """
            @StabilityInferred(parameters = 0)
            open class Foo {
              val current: Int
                @Composable @ReadOnlyComposable @JvmName(name = "getCurrent")
                get() {
                  val tmp0 = %composer.hashCode()
                  return tmp0
                }
              @ReadOnlyComposable
              @Composable
              fun getHashCode(%composer: Composer?, %changed: Int): Int {
                val tmp0 = %composer.hashCode()
                return tmp0
              }
              static val %stable: Int = 0
            }
            @ReadOnlyComposable
            @Composable
            fun getHashCode(%composer: Composer?, %changed: Int): Int {
              val tmp0 = %composer.hashCode()
              return tmp0
            }
        """
    )
}
