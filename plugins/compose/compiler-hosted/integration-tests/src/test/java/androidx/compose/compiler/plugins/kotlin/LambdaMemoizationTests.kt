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

import android.os.Looper.getMainLooper
import android.widget.Button
import org.intellij.lang.annotations.Language
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    manifest = Config.NONE,
    minSdk = 23,
    maxSdk = 23
)
class LambdaMemoizationTests : AbstractLoweringTests() {

    @Test
    @Ignore("b/179279455")
    fun nonCapturingEventLambda() = skipping(
        """
            fun eventFired() { }

            @Composable
            fun EventHolder(event: () -> Unit, block: @Composable () -> Unit) {
              workToBeAvoided()
              block()
            }

            @Composable
            fun Example(model: String) {
                EventHolder(event = { eventFired() }) {
                  workToBeRepeated()
                  ValidateModel(text = model)
                }
            }
        """
    )

    @Test
    @Ignore("b/179279455")
    fun lambdaInClassInitializer() = skipping(
        """
            @Composable
            fun EventHolder(event: () -> Unit) {
              workToBeRepeated()
            }

            @Composable
            fun Example(model: String) {
              class Nested {
                // Should not memoize the initializer
                val lambda: () -> Unit
                  get() {
                    val capturedParameter = Math.random()
                    return { capturedParameter }
                  }
              }
              val n = Nested()
              ValidateModel(model)
              EventHolder(event = n.lambda)
            }
        """
    )

    @Test
    @Ignore("b/179279455")
    fun methodReferenceEvent() = skipping(
        """
            fun eventFired() { }

            @Composable
            fun EventHolder(event: () -> Unit, block: @Composable () -> Unit) {
              workToBeAvoided()
              block()
            }

            @Composable
            fun Example(model: String) {
              workToBeRepeated()
              EventHolder(event = ::eventFired) {
                workToBeRepeated()
                ValidateModel(text = model)
              }
            }
        """
    )

    @Test
    @Ignore("b/179279455")
    fun methodReferenceOnValue() = skipping(
        """
        fun eventFired(value: String) { }

        @Composable
        fun ValidateEvent(expected: String, event: () -> String) {
          val value = event()
          require(expected == value) {
            "Expected '${'$'}expected', received '${'$'}value'"
          }
        }

        @Composable
        fun Test(model: String, unchanged: String) {
          ValidateEvent(unchanged, unchanged::toString)
          ValidateEvent(model, model::toString)
        }

        @Composable
        fun Example(model: String) {
          Test(model, "unchanged")
        }
    """
    )

    @Test
    @Ignore("b/179279455")
    fun extensionMethodReferenceOnValue() = skipping(
        """
        fun eventFired(value: String) { }

        fun String.self() = this

        @Composable
        fun ValidateEvent(expected: String, event: () -> String) {
          val value = event()
          require(expected == value) {
            "Expected '${'$'}expected', received '${'$'}value'"
          }
        }

        @Composable
        fun Test(model: String, unchanged: String) {
          ValidateEvent(unchanged, unchanged::self)
          ValidateEvent(model, model::self)
        }

        @Composable
        fun Example(model: String) {
          Test(model, "unchanged")
        }
    """
    )

    @Test
    @Ignore("b/179279455")
    fun doNotMemoizeCallsToInlines() = skipping(
        """
            fun eventFired(data: String) { }

            @Composable
            fun EventHolder(event: () -> Unit, block: @Composable () -> Unit) {
               workToBeRepeated()
               block()
            }

            @Composable
            inline fun <T, V1> inlined(value: V1, block: () -> T) = block()

            @Composable
            fun Example(model: String) {
              val e1 = inlined(model) { { eventFired(model) } }
              EventHolder(event = e1) {
                workToBeRepeated()
                ValidateModel(model)
              }
              val e2 = remember(model) { { eventFired(model) } }
              EventHolder(event = e2) {
                workToBeRepeated()
                ValidateModel(model)
              }
            }
        """
    )

    @Test
    @Ignore("b/179279455")
    fun captureParameterDirectEventLambda() = skipping(
        """
            fun eventFired(data: String) { }

            @Composable
            fun EventHolder(event: () -> Unit, block: @Composable () -> Unit) {
               workToBeRepeated()
               block()
            }

            @Composable
            fun Example(model: String) {
                EventHolder(event = { eventFired(model) }) {
                  workToBeRepeated()
                  ValidateModel(text = model)
                }
            }
        """
    )

    @Test
    @Ignore("b/179279455")
    fun shouldNotRememberDirectLambdaParameter() = skipping(
        """
        fun eventFired(data: String) {
          // println("Validating ${'$'}data")
          validateModel(data)
        }

        @Composable
        fun EventHolder(event: () -> Unit) {
          workToBeRepeated()
          event()
        }

        @Composable
        fun EventWrapper(event: () -> Unit) {
          EventHolder(event)
        }

        @Composable
        fun Example(model: String) {
          EventWrapper(event = { eventFired(model) })
        }
    """
    )

    @Test
    @Ignore("b/179279455")
    fun narrowCaptureValidation() = skipping(
        """
        fun eventFired(data: String) { }

        @Composable
        fun ExpectUnmodified(event: () -> Unit) {
          workToBeAvoided()
        }

        @Composable
        fun ExpectModified(event: () -> Unit) {
          workToBeRepeated()
        }

        @Composable
        fun Example(model: String) {
          // Unmodified
          val unmodified = model.substring(0, 4)
          val modified = model + " abc"

          ExpectUnmodified(event = { eventFired(unmodified) })
          ExpectModified(event = { eventFired(modified) })
          ExpectModified(event = { eventFired(model) })
        }
    """
    )

    @Test
    @Ignore("b/179279455")
    fun captureInANestedScope() = skipping(
        """
        fun eventFired(data: String) { }

        @Composable
        fun ExpectUnmodified(event: () -> Unit) {
          workToBeAvoided()
        }

        @Composable
        fun ExpectModified(event: () -> Unit) {
          workToBeRepeated()
        }

        @Composable
        fun Wrapped(block: @Composable () -> Unit) {
          block()
        }

        @Composable
        fun Example(model: String) {
          val unmodified = model.substring(0, 4)
          val modified = model + " abc"

          Wrapped {
            ExpectUnmodified(event = { eventFired(unmodified) })
            ExpectModified(event = { eventFired(modified) })
            ExpectModified(event = { eventFired(model) })
          }
          Wrapped {
            Wrapped {
              ExpectUnmodified(event = { eventFired(unmodified) })
              ExpectModified(event = { eventFired(modified) })
              ExpectModified(event = { eventFired(model) })
            }
          }
        }
    """
    )

    @Test
    @Ignore("b/179279455")
    fun twoCaptures() = skipping(
        """
        fun eventFired(data: String) { }

        @Composable
        fun ExpectUnmodified(event: () -> Unit) {
          workToBeAvoided()
        }

        @Composable
        fun ExpectModified(event: () -> Unit) {
          workToBeRepeated()
        }

        @Composable
        fun Wrapped(block: @Composable () -> Unit) {
          block()
        }

        @Composable
        fun Example(model: String) {
          val unmodified1 = model.substring(0, 4)
          val unmodified2 = model.substring(0, 5)
          val modified1 = model + " abc"
          val modified2 = model + " abcd"

          ExpectUnmodified(event = { eventFired(unmodified1 + unmodified2) })
          ExpectModified(event = { eventFired(modified1 + unmodified1) })
          ExpectModified(event = { eventFired(unmodified2 + modified2) })
          ExpectModified(event = { eventFired(modified1 + modified2) })
        }
    """
    )

    @Test
    @Ignore("b/179279455")
    fun threeCaptures() = skipping(
        """
        fun eventFired(data: String) { }

        @Composable
        fun ExpectUnmodified(event: () -> Unit) {
          workToBeAvoided()
        }

        @Composable
        fun ExpectModified(event: () -> Unit) {
          workToBeRepeated()
        }

        @Composable
        fun Example(model: String) {
          val unmodified1 = model.substring(0, 4)
          val unmodified2 = model.substring(0, 5)
          val unmodified3 = model.substring(0, 6)
          val modified1 = model + " abc"
          val modified2 = model + " abcd"
          val modified3 = model + " abcde"

          ExpectUnmodified(event = { eventFired(unmodified1 + unmodified2 + unmodified3) })
          ExpectModified(event = { eventFired(unmodified1 + unmodified2 + modified3) })
          ExpectModified(event = { eventFired(unmodified1 + modified2 + unmodified3) })
          ExpectModified(event = { eventFired(unmodified1 + modified2 + modified3) })
          ExpectModified(event = { eventFired(modified1 + unmodified2 + unmodified3) })
          ExpectModified(event = { eventFired(modified1 + unmodified2 + modified3) })
          ExpectModified(event = { eventFired(modified1 + modified2 + unmodified3) })
          ExpectModified(event = { eventFired(modified1 + modified2 + modified3) })
        }
    """
    )

    @Test
    @Ignore("b/179279455")
    fun fiveCaptures() = skipping(
        """
        fun eventFired(data: String) { }

        @Composable
        fun ExpectUnmodified(event: () -> Unit) {
          workToBeAvoided()
        }

        @Composable
        fun ExpectModified(event: () -> Unit) {
          workToBeRepeated()
        }

        @Composable
        fun Example(model: String) {
          val modified = model
          val unmodified1 = model.substring(0, 1)
          val unmodified2 = model.substring(0, 2)
          val unmodified3 = model.substring(0, 3)
          val unmodified4 = model.substring(0, 4)
          val unmodified5 = model.substring(0, 5)

          ExpectUnmodified(event = { eventFired(
              unmodified1 + unmodified2 + unmodified3 + unmodified4 + unmodified1
            ) })

          ExpectModified(event = { eventFired(
              unmodified1 + unmodified2 + unmodified3 + unmodified4 + unmodified1 + modified
            ) })
        }
    """
    )

    @Test
    @Ignore("b/179279455")
    fun doNotMemoizeNonStableCaptures() = skipping(
        """
        val unmodifiedUnstable = Any()
        val unmodifiedString = "unmodified"

        fun eventFired(data: String) { }

        @Composable
        fun ExpectUnmodified(event: () -> Unit) {
          workToBeAvoided()
        }

        @Composable
        fun ExpectModified(event: () -> Unit) {
          workToBeRepeated()
        }

        @Composable
        fun NonStable(model: String, nonStable: Any, unmodified: String) {
          workToBeRepeated()
          ExpectModified(event = { eventFired(nonStable.toString()) })
          ExpectModified(event = { eventFired(model) })
          ExpectUnmodified(event = { eventFired(unmodified) })
        }

        @Composable
        fun Example(model: String) {
          NonStable(model, unmodifiedUnstable, unmodifiedString)
        }
    """
    )

    @Test
    @Ignore("b/179279455")
    fun doNotMemoizeVarCapures() = skipping(
        """
        fun eventFired(data: Int) { }

        @Composable
        fun ExpectUnmodified(event: () -> Unit) {
          workToBeAvoided()
        }

        @Composable
        fun ExpectModified(event: () -> Unit) {
          workToBeRepeated()
        }

        @Composable
        fun Wrap(block: @Composable () -> Unit) {
          workToBeAvoided()
          block()
          workToBeAvoided()
        }

        @Composable
        fun Test(model: String, b: Int) {
          var a = 1
          var c = false
          ExpectModified(event = { a++ })
          ExpectModified(event = { eventFired(a) })
          ExpectModified(event = { c = true })
          ExpectUnmodified(event = { eventFired(b) })
          Wrap {
            ExpectModified(event = { a++ })
            ExpectModified(event = { eventFired(a) })
            ExpectModified(event = { c = true })
            ExpectUnmodified(event = { eventFired(b) })
          }
        }

        @Composable
        fun Example(model: String) {
          Test(model, 1)
        }
    """
    )

    @Test
    @Ignore("b/179279455")
    fun considerNonComposableCaptures() = skipping(
        """
        fun eventFired(data: Int) {}

        @Composable
        fun ExpectUnmodified(event: () -> Unit) {
          workToBeAvoided()
        }

        @Composable
        fun ExpectModified(event: () -> Unit) {
          workToBeRepeated()
        }

        inline fun wrap(value: Int, block: (value: Int) -> Unit) {
          block(value)
        }

        @Composable
        fun Example(model: String) {
           wrap(iterations) { number ->
             ExpectModified(event = { eventFired(number) })
             ExpectUnmodified(event = { eventFired(5) })
           }
        }
    """
    )

    @Test
    @Ignore("b/179279455")
    fun wrapLambaExpressions() = skipping(
        """
            @Composable
            fun Wrapper(block: @Composable () -> Unit) {
               workToBeAvoided()
               block()
               workToBeAvoided()
            }

            @Composable
            fun Example(model: String) {
              workToBeRepeated()
              Wrapper {
                workToBeRepeated()
                ValidateModel(model)
              }
            }
        """
    )

    @Test
    @Ignore("b/179279455")
    fun nonCapturingComposableLambda() = skipping(
        """
            @Composable
            fun Wrapper1(block: @Composable () -> Unit) {
              workToBeAvoided("Wrapper1.1")
              block()
              workToBeAvoided("Wrapper1.2")
            }

            @Composable
            fun Wrapper2(block: @Composable () -> Unit) {
              workToBeAvoided("Wrapper2.1")
              Wrapper1(block = block)
              workToBeAvoided("Wrapper2.2")
            }

            @Composable
            fun Wrapper3(block: @Composable () -> Unit) {
              workToBeAvoided("Wrapper3.1")
              Wrapper2 {
                block()
              }
              workToBeAvoided("Wrapper3.2")
            }

            @Composable
            fun Example(model: String) {
              Wrapper3 {
                workToBeRepeated("Example1.1")
                ValidateModel(model)
                Wrapper3 {
                  workToBeRepeated("Example1.2")
                  ValidateModel(model)
                }
              }
            }
        """
    )

    @Test
    @Ignore("b/179279455")
    fun wrappingOneParameter() = skipping(
        """
        @Composable
        fun Wrap(block: @Composable (p1: String) -> Unit) {
          workToBeAvoided()
          block("test")
          workToBeAvoided()
        }

        @Composable
        fun Example(model: String) {
          workToBeRepeated()
          Wrap { p1 ->
            require(p1 == "test")
            workToBeRepeated()
            Display(p1)
            ValidateModel(model)
          }
        }
     """
    )

    @Test // Selecting 23 as 22 is the maximum number handled by RestartingFunction
    @Ignore("b/179279455")
    fun wrapping23Parameters() = skipping(
        """
        @Composable
        fun Wrap(block: @Composable (
            p1: String,
            p2: String,
            p3: String,
            p4: String,
            p5: String,
            p6: String,
            p7: String,
            p8: String,
            p9: String,
            p10: String,
            p11: String,
            p12: String,
            p13: String,
            p14: String,
            p15: String,
            p16: String,
            p17: String,
            p18: String,
            p19: String,
            p20: String,
            p21: String,
            p22: String,
            p23: String
          ) -> Unit) {
          workToBeAvoided()
          block(
            "test1", "test2", "test3", "test4", "test5",
            "test6", "test7", "test8", "test9", "test10",
            "test11", "test12", "test13", "test14", "test15",
            "test16", "test17", "test18", "test19", "test20",
            "test21", "test22", "test23"
          )
          workToBeAvoided()
        }

        @Composable
        fun Example(model: String) {
          workToBeRepeated()
          Wrap {
            p1, p2, p3, p4, p5, p6, p7, p8, p9, p10,
            p11, p12, p13, p14, p15, p16, p17, p18, p19, p20,
            p21, p22, p23 ->
            require(p1 == "test1") { "p1 should be test1 but was ${'$'}p1" }
            require(p2 == "test2") { "p2 should be test2 but was ${'$'}p2" }
            require(p3 == "test3") { "p3 should be test3 but was ${'$'}p3" }
            require(p4 == "test4") { "p4 should be test4 but was ${'$'}p4" }
            require(p5 == "test5") { "p5 should be test5 but was ${'$'}p5" }
            require(p6 == "test6") { "p6 should be test6 but was ${'$'}p6" }
            require(p7 == "test7") { "p7 should be test7 but was ${'$'}p7" }
            require(p8 == "test8") { "p8 should be test8 but was ${'$'}p8" }
            require(p9 == "test9") { "p9 should be test9 but was ${'$'}p9" }
            require(p10 == "test10") { "p10 should be test10 but was ${'$'}p10" }
            require(p11 == "test11") { "p11 should be test11 but was ${'$'}p11" }
            require(p12 == "test12") { "p12 should be test12 but was ${'$'}p12" }
            require(p13 == "test13") { "p13 should be test13 but was ${'$'}p13" }
            require(p14 == "test14") { "p14 should be test14 but was ${'$'}p14" }
            require(p15 == "test15") { "p15 should be test15 but was ${'$'}p15" }
            require(p16 == "test16") { "p16 should be test16 but was ${'$'}p16" }
            require(p17 == "test17") { "p17 should be test17 but was ${'$'}p17" }
            require(p18 == "test18") { "p18 should be test18 but was ${'$'}p18" }
            require(p19 == "test19") { "p19 should be test19 but was ${'$'}p19" }
            require(p20 == "test20") { "p20 should be test20 but was ${'$'}p20" }
            require(p21 == "test21") { "p21 should be test21 but was ${'$'}p21" }
            require(p22 == "test22") { "p22 should be test22 but was ${'$'}p22" }
            require(p23 == "test23") { "p23 should be test23 but was ${'$'}p23" }
            workToBeRepeated()
            Display(p1)
            ValidateModel(model)
          }
        }
     """
    )

    @Test
    @Ignore("b/179279455")
    fun wrappingReceiverParameter() = skipping(
        """
        class Receiver() { }

        @Composable
        fun Wrapper(block: @Composable Receiver.() -> Unit) {
          workToBeAvoided()
          val receiver = Receiver()
          receiver.block()
          workToBeAvoided()
        }

        @Composable
        fun Example(model: String) {
          workToBeRepeated()
          Wrapper {
            workToBeRepeated()
            ValidateModel(model)
          }
        }
    """
    )

    @Test
    @Ignore("b/179279455")
    fun untrackedLambdasShouldNotForceEvaluation() = skipping(
        """
        @Composable
        fun Wrapper(block: @Composable () -> Unit) {
          workToBeAvoided()
          block()
          workToBeAvoided()
        }

        @Composable
        fun Example(model: String) {
          workToBeRepeated()
          Wrapper @ComposableContract(tracked = false) {
            workToBeAvoided()
            ValidateModel(model)
          }
          Wrapper {
            workToBeRepeated()
            ValidateModel(model)
          }
          workToBeRepeated()
        }
    """
    )

    @Test
    @Ignore("b/179279455")
    fun lambdasWithReturnResultsShouldBeUntracked() = skipping(
        """

        @Composable
        fun Test1(param: @Composable () -> String) {
          workToBeRepeated()
          param()
          workToBeRepeated()
        }

        @Composable
        fun Test2(param: @Composable () -> String) {
          workToBeAvoided()
          Test1(param)
          workToBeAvoided()
        }

        @Composable
        fun Example(model: String) {
          val s = remember { mutableStateOf(model) }
          s.value = model
          Test1({ s.value })
        }
    """
    )

    @Test
    @Ignore("b/179279455")
    fun method_lambdaCapturingThis_Unstable() = skipping(
        """
            fun log(data: String) { }

            @Composable
            fun Test(param: () -> Unit) {
              workToBeRepeated()
              param()
              workToBeRepeated()
            }

            class Component(val data: String) {
              @Composable
              fun Render() {
                Test {
                  log(data)
                  workToBeRepeated()
                }
              }
            }

            @Composable
            fun Example(model: String) {
              val component = remember(model) { Component(model) }
              component.Render()
            }
        """
    )

    @Test
    @Ignore("b/179279455")
    fun method_lambdaCapturingThis_Stable() = skipping(
        """
            fun log(data: String) { }

            @Composable
            fun TestChanges(param: () -> Unit) {
              workToBeRepeated()
              param()
              workToBeRepeated()
            }

            @Composable
            fun TestUnchanged(param: () -> Unit) {
              workToBeAvoided()
              param()
              workToBeAvoided()
            }

            @Stable
            class Component(val data: String, val expectChanges: Boolean) {
              @Composable
              fun Render() {
                if (expectChanges) {
                  TestChanges {
                    workToBeRepeated()
                    log(data)
                  }
                } else {
                  TestUnchanged {
                    workToBeAvoided()
                    log(data)
                  }
                }
              }
            }

            @Composable
            fun Example(model: String) {
              val changingComponent = remember(model) { Component(model, true) }
              val unchangingComponent = remember { Component("unchanging", false) }
              changingComponent.Render()
              unchangingComponent.Render()
            }
        """
    )

    @Test
    @Ignore("b/179279455")
    fun receiver_lambdaCapturingThis_Unstable() = skipping(
        """
            fun log(data: String) { }

            @Composable
            fun Test(param: () -> Unit) {
              workToBeRepeated()
              param()
              workToBeRepeated()
            }

            class Component(val data: String) {
              var i = 0
            }

            @Composable
            fun Component.Render() {
              Test {
                log(data)
                workToBeRepeated()
              }
            }

            @Composable
            fun Example(model: String) {
              val component = remember(model) { Component(model) }
              component.Render()
            }
        """
    )

    @Test
    @Ignore("b/179279455")
    fun receiver_lambdaCapturingThis_Stable() = skipping(
        """
            fun log(data: String) { }

            @Composable
            fun TestChanges(param: () -> Unit) {
              workToBeRepeated()
              param()
              workToBeRepeated()
            }

            @Composable
            fun TestUnchanged(param: () -> Unit) {
              workToBeAvoided()
              param()
              workToBeAvoided()
            }

            @Stable
            class Component(val data: String, val expectChanges: Boolean)

            @Composable
            fun Component.Render() {
              if (expectChanges) {
                TestChanges {
                  workToBeRepeated()
                  log(data)
                }
              } else {
                TestUnchanged {
                  workToBeAvoided()
                  log(data)
                }
              }
            }

            @Composable
            fun Example(model: String) {
              val changingComponent = remember(model) { Component(model, true) }
              val unchangingComponent = remember { Component("unchanging", false) }
              changingComponent.Render()
              unchangingComponent.Render()
            }
        """
    )

    private fun skipping(@Language("kotlin") text: String, dumpClasses: Boolean = false) =
        ensureSetup {
            compose(
                """
                var avoidedWorkCount = 0
                var repeatedWorkCount = 0
                var expectedAvoidedWorkCount = 0
                var expectedRepeatedWorkCount = 0

                fun workToBeAvoided(msg: String = "") {
                   avoidedWorkCount++
                   // println("Work to be avoided ${'$'}avoidedWorkCount ${'$'}msg")
                }
                fun workToBeRepeated(msg: String = "") {
                   repeatedWorkCount++
                   // println("Work to be repeated ${'$'}repeatedWorkCount ${'$'}msg")
                }

                $text

                @Composable
                fun Display(text: String) {}

                fun validateModel(text: String) {
                  require(text == "Iteration ${'$'}iterations")
                }

                @Composable
                fun ValidateModel(text: String) {
                  validateModel(text)
                }

                @Composable
                fun TestHost() {
                   // println("START: Iteration - ${'$'}iterations")
                   val scope = currentRecomposeScope
                   emitView(::Button) {
                     it.id=42
                     it.setOnClickListener(View.OnClickListener { scope.invalidate() })
                   }
                   Example("Iteration ${'$'}iterations")
                   // println("END  : Iteration - ${'$'}iterations")
                   validate()
                }

                var iterations = 0

                fun validate() {
                  if (iterations++ == 0) {
                    expectedAvoidedWorkCount = avoidedWorkCount
                    expectedRepeatedWorkCount = repeatedWorkCount
                    repeatedWorkCount = 0
                  } else {
                    if (expectedAvoidedWorkCount != avoidedWorkCount) {
                      println("Executed avoided work")
                    }
                    require(expectedAvoidedWorkCount == avoidedWorkCount) {
                      "Executed avoided work unexpectedly, expected " +
                      "${'$'}expectedAvoidedWorkCount" +
                      ", received ${'$'}avoidedWorkCount"
                    }
                    if (expectedRepeatedWorkCount != repeatedWorkCount) {
                      println("Will throw Executed more work")
                    }
                    require(expectedRepeatedWorkCount == repeatedWorkCount) {
                      "Expected more repeated work, expected ${'$'}expectedRepeatedWorkCount" +
                      ", received ${'$'}repeatedWorkCount"
                    }
                    repeatedWorkCount = 0
                  }
                }

            """,
                """
                TestHost()
            """,
                dumpClasses = dumpClasses
            ).then { activity ->
                val button = activity.findViewById(42) as Button
                button.performClick()
            }.then { activity ->
                val button = activity.findViewById(42) as Button
                button.performClick()
            }.then {
                // Wait for test to complete
                shadowOf(getMainLooper()).idle()
            }
        }
}
