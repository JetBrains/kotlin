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
        var hierarchiesByDefault: Boolean? = null,
        var hierarchiesSupportFlag: Boolean? = null,
        var granularMetadataFlag: Boolean? = null,
        var dependencyPropagationFlag: Boolean? = null,
        var expectedToPass: Boolean = true,
        var expectedPhraseInOutput: String? = null,
        var notExpectedPhraseInOutput: String? = null,
    ) {
        constructor(configure: TestCase.() -> Unit) : this() {
            configure()
        }
    }

    @Parameterized.Parameter(0)
    lateinit var testCase: TestCase

    @Parameterized.Parameter(1)
    lateinit var testProjectName: String

    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        private val testCases = buildList {
            add(TestCase {
                granularMetadataFlag = null
                dependencyPropagationFlag = null
                expectedToPass = true
                notExpectedPhraseInOutput = "It is safe to remove the property."
            })
            add(TestCase {
                granularMetadataFlag = true
                expectedToPass = true
                expectedPhraseInOutput = "It is safe to remove the property."
            })
            add(TestCase {
                granularMetadataFlag = false
                expectedToPass = false
                expectedPhraseInOutput = "Multiplatform Hierarchical Structures support is now enabled by default"
            })
            add(TestCase {
                dependencyPropagationFlag = false
                expectedToPass = true
                expectedPhraseInOutput = "It is safe to remove the property"
            })
            add(TestCase {
                dependencyPropagationFlag = true
                expectedToPass = false
                expectedPhraseInOutput = "Kotlin/Native dependencies commonization is now enabled by default"
            })
            add(TestCase {
                hierarchiesSupportFlag = false
                granularMetadataFlag = true
                expectedToPass = false
                expectedPhraseInOutput = "Conflicting properties"
            })
            add(TestCase {
                hierarchiesSupportFlag = false
                granularMetadataFlag = false
                expectedToPass = true
                expectedPhraseInOutput = "is redundant"
            })
            add(TestCase {
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
                hierarchiesSupportFlag = true
                expectedToPass = true
            })
        }

        private val projectsToTest = listOf(
            "new-mpp-published",
            "hierarchical-mpp-project-dependency"
        )

        @Parameterized.Parameters(name = "{1}: {0}")
        @JvmStatic
        fun testCases() = testCases
            .flatMap { testCase -> projectsToTest.map { arrayOf(testCase, it) } }
    }

    val testProject by lazy {
        Project(testProjectName).apply {
            setupWorkingDir()
            gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)
            gradleProperties().delete()
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun doTest() {
        val args = buildList {
            with(testCase) {
                if (hierarchiesByDefault == false)
                    add("-Pkotlin.internal.mpp.hierarchicalStructureByDefault=false")
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
            testCase.notExpectedPhraseInOutput?.let { assertNotContains(it) }
        }
    }
}