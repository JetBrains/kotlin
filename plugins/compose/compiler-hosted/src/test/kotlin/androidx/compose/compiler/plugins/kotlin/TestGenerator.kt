/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.plugins.kotlin

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.generators.tests.provider
import org.jetbrains.kotlin.generators.tests.standalone
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeCodegenBoxTest
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedHostTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UseExtTestCaseGroupProvider
import org.junit.jupiter.api.Tag

fun main() {
    System.setProperty("java.awt.headless", "true")

    generateTestGroupSuiteWithJUnit5 {
        testGroup("plugins/compose/compiler-hosted/tests-gen", "plugins/compose/compiler-hosted/testData") {
            testClass<AbstractCompilerFacilityTestForComposeCompilerPlugin> {
                model("codegen")
            }
            testClass<AbstractPhasedJvmDiagnosticLightTreeForComposeTest> {
                model("diagnostics")
            }
            testClass<AbstractJsLightTreePluginBlackBoxCodegenForComposeTest> {
                model("js")
            }
            testClass<AbstractComposeWithSerializationBoxTest> {
                model("withSerialization")
            }
            testClass<AbstractJvmIrTextTestForCompose> {
                model("common")
            }
            testClass<AbstractJsIrTextTestForCompose> {
                model("common")
            }
            // Requires Kotlin/Native, run with `nativeTest` task.
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "ComposeNativeBoxTestGenerated",
                annotations = listOf(
                    // Package names are not patched in standalone mode, so imports of Compose runtime are preserved.
                    standalone(),
                    annotation(Tag::class.java, COMPOSE_NATIVE_TEST_TAG),
                    // Compose runtime klibs are provided only for the host target.
                    annotation(EnforcedHostTarget::class.java),
                    provider<UseExtTestCaseGroupProvider>(),
                )
            ) {
                model("nativeBox")
            }
        }
    }
}

private const val COMPOSE_NATIVE_TEST_TAG = "compose-native"
