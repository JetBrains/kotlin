/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.konan.blackboxtest.AbstractNativeCodegenBoxTest
import org.jetbrains.kotlin.konan.blackboxtest.AbstractNativeBlackBoxTest
import org.jetbrains.kotlin.konan.blackboxtest.AbstractNativeKlibABITest
import org.jetbrains.kotlin.konan.blackboxtest.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.blackboxtest.support.EnforcedProperty
import org.jetbrains.kotlin.konan.blackboxtest.support.group.UseExtTestCaseGroupProvider
import org.jetbrains.kotlin.konan.blackboxtest.support.group.UseStandardTestCaseGroupProvider
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.CacheMode
import org.jetbrains.kotlin.test.TargetBackend
import org.junit.jupiter.api.Tag

fun main() {
    System.setProperty("java.awt.headless", "true")

    generateTestGroupSuiteWithJUnit5 {
        // Codegen box tests.
        testGroup("native/native.tests/tests-gen", "compiler/testData") {
            testClass<AbstractNativeCodegenBoxTest>(
                suiteTestClassName = "NativeCodegenBoxTestGenerated",
                annotations = listOf(codegen(), provider<UseExtTestCaseGroupProvider>())
            ) {
                model("codegen/box", targetBackend = TargetBackend.NATIVE)
                model("codegen/boxInline", targetBackend = TargetBackend.NATIVE)
            }
        }

        // Samples (how to utilize the abilities of new test infrastructure).
        testGroup("native/native.tests/tests-gen", "native/native.tests/testData") {
            testClass<AbstractNativeBlackBoxTest>(
                suiteTestClassName = "InfrastructureTestGenerated",
                annotations = listOf(infrastructure(), provider<UseStandardTestCaseGroupProvider>())
            ) {
                model("samples")
                model("samples2")
            }
        }

        // KLIB ABI tests.
        testGroup("native/native.tests/tests-gen", "compiler/testData") {
            testClass<AbstractNativeKlibABITest>(
                suiteTestClassName = "KlibABITestGenerated",
                annotations = listOf(enforcedCacheMode(CacheMode.Alias.NO))
            ) {
                model("klibABI/", pattern = "^([^_](.+))$", recursive = false)
            }
            testClass<AbstractNativeKlibABITest>(
                suiteTestClassName = "KlibABITestWithCacheGenerated",
                annotations = listOf(enforcedCacheMode(CacheMode.Alias.STATIC_EVERYWHERE))
            ) {
                model("klibABI/", pattern = "^([^_](.+))$", recursive = false)
            }
        }
    }
}

private inline fun <reified T : Annotation> provider() = annotation(T::class.java)

private fun codegen() = annotation(Tag::class.java, "codegen")
private fun infrastructure() = annotation(Tag::class.java, "infrastructure")

private fun enforcedCacheMode(alias: CacheMode.Alias) = annotation(
    EnforcedProperty::class.java,
    EnforcedProperty::property.name to ClassLevelProperty.CACHE_MODE,
    EnforcedProperty::propertyValue.name to alias.name
)
