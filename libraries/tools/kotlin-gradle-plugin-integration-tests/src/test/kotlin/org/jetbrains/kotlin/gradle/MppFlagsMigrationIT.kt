/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
@RunWith(Parameterized::class)
internal class MppFlagsMigrationIT : BaseGradleIT() {
    internal data class TestCase(
        var hierarchiesByDefault: Boolean = false,
        var hierarchiesSupportFlag: Boolean? = null,
        var granularMetadataFlag: Boolean? = null,
        var dependencyPropagationFlag: Boolean? = null,
        var expectedToPass: Boolean = true,
        var expectedPhraseInOutput: String? = null,
    ) {
        constructor(configure: TestCase.() -> Unit) : this() {
            configure()
        }
    }

    @Parameterized.Parameter(0)
    lateinit var testCase: TestCase

    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        private val testCases = buildList {
            add(TestCase {
                hierarchiesByDefault = true
                granularMetadataFlag = true
                expectedToPass = true
                expectedPhraseInOutput = "It is safe to remove the property."
            })
            add(TestCase {
                hierarchiesByDefault = true
                granularMetadataFlag = false
                expectedToPass = false
                expectedPhraseInOutput = "Multiplatform Hierarchical Structures support is now enabled by default"
            })
            add(TestCase {
                hierarchiesByDefault = true
                dependencyPropagationFlag = false
                expectedToPass = true
                expectedPhraseInOutput = "It is safe to remove the property"
            })
            add(TestCase {
                hierarchiesByDefault = true
                dependencyPropagationFlag = true
                expectedToPass = false
                expectedPhraseInOutput = "Kotlin/Native dependencies commonization is now enabled by default"
            })
            add(TestCase {
                hierarchiesByDefault = true
                hierarchiesSupportFlag = false
                granularMetadataFlag = true
                expectedToPass = false
                expectedPhraseInOutput = "Conflicting properties"
            })
            add(TestCase {
                hierarchiesByDefault = true
                hierarchiesSupportFlag = false
                granularMetadataFlag = false
                expectedToPass = true
                expectedPhraseInOutput = "is redundant"
            })
            add(TestCase {
                hierarchiesByDefault = true
                hierarchiesSupportFlag = false
                dependencyPropagationFlag = false
                expectedToPass = false
                expectedPhraseInOutput = "Conflicting properties"
            })
            add(TestCase {
                hierarchiesByDefault = false
                hierarchiesSupportFlag = false
                expectedToPass = false
                expectedPhraseInOutput = "not yet supported"
            })
            add(TestCase {
                hierarchiesByDefault = false
                hierarchiesSupportFlag = true
                expectedToPass = false
                expectedPhraseInOutput = "not yet supported"
            })
        }

        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun testCases() = testCases.map { arrayOf(it) }
    }

    val testProject by lazy {
        Project("new-mpp-published").apply {
            setupWorkingDir()
            gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun doTest() {
        val args = buildList {
            with(testCase) {
                if (hierarchiesByDefault)
                    add("-Pkotlin.internal.mpp.hierarchicalStructureByDefault=true")
                if (hierarchiesSupportFlag != null)
                    add("-Pkotlin.mpp.hierarchicalStructureSupport=$hierarchiesSupportFlag")
                if (granularMetadataFlag != null)
                    add("-Pkotlin.mpp.enableGranularSourceSetsMetadata=$granularMetadataFlag")
                if (dependencyPropagationFlag != null) {
                    add("-Pkotlin.native.enableDependencyPropagation=$dependencyPropagationFlag")
                }
            }
        }

        // just run the configuration phase:
        testProject.build(*args.toTypedArray()) {
            if (testCase.expectedToPass)
                assertSuccessful()
            else
                assertFailed()

            testCase.expectedPhraseInOutput?.let { assertContains(it, ignoreCase = true) }
        }
    }
}