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
import androidx.compose.compiler.plugins.kotlin.newConfiguration
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots

class ComposableTargetCheckerTests : AbstractComposeDiagnosticsTest() {
    override fun setUp() {
        // intentionally don't call super.setUp() here since we are recreating an environment
        // every test
        System.setProperty(
            "user.dir",
            homeDir
        )
        System.setProperty(
            "idea.ignore.disabled.plugins",
            "true"
        )
    }

    private fun check(text: String) {
        val disposable = TestDisposable()
        val classPath = createClasspath()
        val configuration = newConfiguration()
        configuration.addJvmClasspathRoots(classPath)
        configuration.configureJdkClasspathRoots()

        val environment =
            KotlinCoreEnvironment.createForTests(
                disposable,
                configuration,
                EnvironmentConfigFiles.JVM_CONFIG_FILES
            )
        setupEnvironment(environment)

        try {
            doTest(text, environment)
        } catch (e: ComposableCheckerTests.ExpectedFailureException) {
            throw e
        } finally {
            Disposer.dispose(disposable)
        }
    }

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
}