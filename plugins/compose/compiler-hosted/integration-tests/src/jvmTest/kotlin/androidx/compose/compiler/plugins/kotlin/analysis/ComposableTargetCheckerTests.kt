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
import androidx.compose.compiler.plugins.kotlin.Classpath
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

class ComposableTargetCheckerTests(useFir: Boolean) : AbstractComposeDiagnosticsTest(useFir) {
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
            <!COMPOSE_APPLIER_CALL_MISMATCH!>M${psiEnd()}()${firEnd()}
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
            <!COMPOSE_APPLIER_CALL_MISMATCH!>M${psiEnd()}()${firEnd()}
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
            ${psiParStart()}W${psiEnd()} {
                ${firMisStart()}M()${firEnd()}
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
        fun OpenCustom(oContent: CustomComposable) {
          oContent.call()
        }

        @Composable
        fun ClosedCustom(cContent: CustomComposable) {
          N()
          cContent.call()
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
          <!COMPOSE_APPLIER_CALL_MISMATCH!>M${psiEnd()}()${firEnd()}
        }

        @Composable
        fun ClosedDisagree() {
          ClosedCustom {
            N()
            <!COMPOSE_APPLIER_CALL_MISMATCH!>M${psiEnd()}()${firEnd()}
          }
          <!COMPOSE_APPLIER_CALL_MISMATCH!>M${psiEnd()}()${firEnd()}
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
            <!COMPOSE_APPLIER_CALL_MISMATCH!>M${psiEnd()}()${firEnd()}
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
            <!COMPOSE_APPLIER_CALL_MISMATCH!>M${psiEnd()}()${firEnd()}
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
            <!COMPOSE_APPLIER_CALL_MISMATCH!>M${psiEnd()}()${firEnd()}
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
           <!COMPOSE_APPLIER_CALL_MISMATCH!>Invalid${psiEnd()}()${firEnd()}
        }
        """,
        additionalPaths = listOf(
            Classpath.composeUiJar(),
            Classpath.composeUiGraphicsJar(),
            Classpath.composeUiTextJar(),
            Classpath.composeFoundationTextJar()
        )
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
            <!COMPOSE_APPLIER_CALL_MISMATCH!>M${psiEnd()}()${firEnd()}
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
          ${psiDecStart()}@Composable @ComposableTarget("M") override fun ${firDecStart()}Compose${firEnd()}() { }${psiEnd()}
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

    private fun firEnd() = if (useFir) "<!>" else ""
    private fun psiEnd() = if (!useFir) "<!>" else ""
    private fun firMisStart() = if (useFir) "<!COMPOSE_APPLIER_CALL_MISMATCH!>" else ""
    private fun psiParStart() = if (!useFir) "<!COMPOSE_APPLIER_PARAMETER_MISMATCH!>" else ""
    private fun firDecStart() = if (useFir) "<!COMPOSE_APPLIER_DECLARATION_MISMATCH!>" else ""
    private fun psiDecStart() = if (!useFir) "<!COMPOSE_APPLIER_DECLARATION_MISMATCH!>" else ""
}
