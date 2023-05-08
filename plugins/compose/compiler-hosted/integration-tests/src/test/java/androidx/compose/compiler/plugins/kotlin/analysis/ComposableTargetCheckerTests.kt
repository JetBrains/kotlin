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

package androidx.compose.compiler.plugins.kotlin.analysis

import androidx.compose.compiler.plugins.kotlin.AbstractComposeDiagnosticsTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ComposableTargetCheckerTests : AbstractComposeDiagnosticsTest(useFir = false) {
    @Test
    fun testExplicitTargetAnnotations() = check(
        """
        import androidx.compose.runtime.*

        @Composable
        @ComposableTarget("a")
        fun A() {
          B()
        }

        @Composable
        @ComposableTarget("a")
        fun B() { }

        @Composable
        @ComposableTarget("a")
        fun C() {
          B()
        }
        """
    )

    @Test
    fun testInferredTargets() = check(
        """
        import androidx.compose.runtime.*

        @Composable
        fun A() {
          N()
        }

        @Composable
        fun B() {
          N()
        }

        @Composable
        fun C() {
          A()
          B()
        }

        @Composable
        @ComposableTarget("N")
        fun N() { }
        """
    )

    @Test
    fun testInferBoundContainer() = check(
        """
        import androidx.compose.runtime.*

        @Composable
        fun Wrapper(content: @Composable ()->Unit) {
            N()
            content()
        }

        @Composable
        fun A() {
          Wrapper {
              B()
          }
        }

        @Composable
        fun B() {
           N()
        }

        @Composable
        @ComposableTarget("N")
        fun N() {}
        """
    )

    @Test
    fun testInferGenericContainer() = check(
        """
        import androidx.compose.runtime.*

        @Composable
        fun Wrapper(content: @Composable () -> Unit) {
            content()
        }

        @Composable
        fun WI(content: @Composable () -> Unit) {
            Wrapper(content)
        }

        @Composable
        fun ANW() {
          Wrapper {
              BN()
          }
        }

        @Composable
        fun ANW_I() {
          WI {
            BN()
          }
        }

        @Composable
        fun BN() {
           N()
        }

        @Composable
        @ComposableTarget("N")
        fun N() {}

        @Composable
        fun AMW() {
          Wrapper {
              BM()
          }
        }

        @Composable
        fun AMW_I() {
          WI {
            BM()
          }
        }

        @Composable
        fun BM() {
           M()
        }

        @Composable
        @ComposableTarget("M")
        fun M() {}
        """
    )

    @Test
    fun testReportExplicitFailure() = check(
        """
        import androidx.compose.runtime.*

        @Composable
        @ComposableTarget("N")
        fun T() {
            <!COMPOSE_APPLIER_CALL_MISMATCH!>M<!>()
        }

        @Composable
        @ComposableTarget("M")
        fun M() {}
        """
    )

    @Test
    fun testReportDisagreementFailure() = check(
        """
        import androidx.compose.runtime.*

        @Composable
        fun T() {
            N()
            <!COMPOSE_APPLIER_CALL_MISMATCH!>M<!>()
        }

        @Composable
        @ComposableTarget("N")
        fun N() {}

        @Composable
        @ComposableTarget("M")
        fun M() {}
        """
    )

    @Test
    fun testGenericDisagreement() = check(
        """
        import androidx.compose.runtime.*

        @Composable
        fun W(content: @Composable () -> Unit) { content() }

        @Composable
        @ComposableTarget("N")
        fun N() {}

        @Composable
        @ComposableTarget("M")
        fun M() {}

        @Composable
        fun T() {
            W {
                N()
            }
            <!COMPOSE_APPLIER_PARAMETER_MISMATCH!>W<!> {
                M()
            }
        }
        """
    )

    @Test
    fun testFunInterfaceInference() = check(
        """
        import androidx.compose.runtime.*

        @Composable
        fun W(content: @Composable () -> Unit) { content() }

        @Composable
        @ComposableTarget("N")
        fun N() {}

        @Composable
        @ComposableTarget("M")
        fun M() {}

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
          N()
          content.call()
        }

        @Composable
        fun UseOpen() {
          N()
          OpenCustom {
            N()
          }
        }

        @Composable
        fun UseClosed() {
          N()
          ClosedCustom {
            N()
          }
        }

        @Composable
        fun OpenDisagree() {
          OpenCustom {
            N()
          }
          <!COMPOSE_APPLIER_CALL_MISMATCH!>M<!>()
        }

        @Composable
        fun ClosedDisagree() {
          ClosedCustom {
            N()
            <!COMPOSE_APPLIER_CALL_MISMATCH!>M<!>()
          }
          <!COMPOSE_APPLIER_CALL_MISMATCH!>M<!>()
        }
        """
    )

    @Test
    fun testFileScopeTargetDeclaration() = check(
        """
        @file:ComposableTarget("N")

        import androidx.compose.runtime.Composable
        import androidx.compose.runtime.ComposableTarget

        @Composable @ComposableTarget("N") fun N() {}
        @Composable @ComposableTarget("M") fun M() {}

        @Composable
        fun AssumesN() {
            <!COMPOSE_APPLIER_CALL_MISMATCH!>M<!>()
        }
        """
    )

    @Test
    fun testTargetMarker() = check(
        """
        import androidx.compose.runtime.Composable
        import androidx.compose.runtime.ComposableTarget
        import androidx.compose.runtime.ComposableTargetMarker

        @Retention(AnnotationRetention.BINARY)
        @ComposableTargetMarker(description = "N")
        @Target(
            AnnotationTarget.FILE,
            AnnotationTarget.FUNCTION,
            AnnotationTarget.PROPERTY_GETTER,
            AnnotationTarget.TYPE,
            AnnotationTarget.TYPE_PARAMETER,
        )
        annotation class NComposable()

        @Composable @ComposableTarget("M") fun M() {}

        @Composable
        @NComposable
        fun AssumesN() {
            <!COMPOSE_APPLIER_CALL_MISMATCH!>M<!>()
        }
        """
    )

    @Test
    fun testFileScopeTargetMarker() = check(
        """
        @file: NComposable

        import androidx.compose.runtime.Composable
        import androidx.compose.runtime.ComposableTarget
        import androidx.compose.runtime.ComposableTargetMarker

        @Retention(AnnotationRetention.BINARY)
        @ComposableTargetMarker(description = "An N Composable")
        @Target(
            AnnotationTarget.FILE,
            AnnotationTarget.FUNCTION,
            AnnotationTarget.PROPERTY_GETTER,
            AnnotationTarget.TYPE,
            AnnotationTarget.TYPE_PARAMETER,
        )
        annotation class NComposable()

        @Composable @ComposableTarget("M") fun M() {}

        @Composable
        fun AssumesN() {
            <!COMPOSE_APPLIER_CALL_MISMATCH!>M<!>()
        }
        """
    )

    @Test
    fun testUiTextAndInvalid() = check(
        """
        import androidx.compose.runtime.Composable
        import androidx.compose.runtime.ComposableTarget
        import androidx.compose.foundation.text.BasicText

        @Composable @ComposableTarget("N")
        fun Invalid() { }

        @Composable
        fun UseText() {
           BasicText("Some text")
           <!COMPOSE_APPLIER_CALL_MISMATCH!>Invalid<!>()
        }
        """
    )

    @Test
    fun testOpenOverrideAttributesInheritTarget() = check(
        """
        import androidx.compose.runtime.Composable
        import androidx.compose.runtime.ComposableTarget

        @Composable @ComposableTarget("N") fun N() { }
        @Composable @ComposableTarget("M") fun M() { }

        abstract class Base {
          @Composable @ComposableTarget("N") abstract fun Compose()
        }

        class Invalid : Base() {
          @Composable override fun Compose() {
            <!COMPOSE_APPLIER_CALL_MISMATCH!>M<!>()
          }
        }

        class Valid : Base () {
          @Composable override fun Compose() {
            N()
          }
        }
        """
    )

    @Test
    fun testOpenOverrideTargetsMustAgree() = check(
        """
        import androidx.compose.runtime.Composable
        import androidx.compose.runtime.ComposableTarget

        @Composable @ComposableTarget("N") fun N() { }
        @Composable @ComposableTarget("M") fun M() { }

        abstract class Base {
          @Composable @ComposableTarget("N") abstract fun Compose()
        }

        class Invalid : Base() {
          <!COMPOSE_APPLIER_DECLARATION_MISMATCH!>@Composable @ComposableTarget("M") override fun Compose() { }<!>
        }

        class Valid : Base () {
          @Composable override fun Compose() {
            N()
          }
        }
        """
    )

    @Test
    fun testOpenOverrideInferredToAgree() = check(
        """
        import androidx.compose.runtime.Composable
        import androidx.compose.runtime.ComposableTarget

        @Composable @ComposableTarget("N") fun N() { }
        @Composable @ComposableTarget("M") fun M() { }

        abstract class Base {
          @Composable @ComposableTarget("N") abstract fun Compose()
        }

        class Invalid : Base() {
          @Composable override fun Compose() {
            N()
          }
        }
        """)
}
