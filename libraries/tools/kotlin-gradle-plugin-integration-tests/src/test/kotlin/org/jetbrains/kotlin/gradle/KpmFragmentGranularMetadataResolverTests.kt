/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.gradle.kpm.FragmentKind
import org.jetbrains.kotlin.gradle.kpm.TestDependencyKind
import org.jetbrains.kotlin.gradle.util.KpmDependencyResolutionTestCase
import org.jetbrains.kotlin.gradle.util.PublishAllTestCaseExecutor
import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.gradle.util.prepare
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.Assert
import org.junit.Test
import java.io.File

@TestDataPath("\$CONTENT_ROOT")
@TestMetadata(KpmFragmentGranularMetadataResolverTests.TESTDATA_ROOT)
class KpmFragmentGranularMetadataResolverTests : BaseGradleIT() {
    companion object {
        private val GRANULAR_METADATA_RESOLUTINS_REPORT_START_MARKER = "<fragment-granular-metadata-resolutions-report>"
        private val GRANULAR_METADATA_RESOLUTINS_REPORT_END_MARKER = "</fragment-granular-metadata-resolutions-report>"
        internal const val TESTDATA_ROOT = "testData/kpmFragmentGranularMetadataResolverTests" //

        internal val testCases = listOf(
            simpleProjectToProjectDependency()
        )

        internal fun simpleProjectToProjectDependency() = KpmDependencyResolutionTestCase("simpleProjectToProjectDependency").apply {
            allModules {
                val jvmAndLinux = fragment("jvmAndLinux", FragmentKind.COMMON_FRAGMENT)
                val native = fragment("native", FragmentKind.COMMON_FRAGMENT)
                fragment("linuxX64", FragmentKind.LINUXX64_VARIANT) { refines(native, jvmAndLinux) }
                fragment("jvm", FragmentKind.JVM_VARIANT) { refines(jvmAndLinux) }
                val ios = fragment("ios", FragmentKind.COMMON_FRAGMENT) { refines(native) }
                fragment("iosArm64", FragmentKind.IOSARM64_VARIANT) { refines(ios) }
                fragment("iosX64", FragmentKind.IOSX64_VARIANT) { refines(ios) }
                // TODO: add host-specific fragments for hosts other than macOS
            }

            val projectA = project("a")
            val projectB = project("b")

            projectB.main.depends(projectA.main, TestDependencyKind.PROJECT)
        }

        private fun String.collectGranularResolutionReports(): List<String> {
            var cur = 0
            val lines = lines()
            val result = mutableListOf<String>()

            fun consumeReport(): String = buildString {
                while (cur < lines.size && lines[cur] != GRANULAR_METADATA_RESOLUTINS_REPORT_END_MARKER) {
                    appendLine(lines[cur++])
                }
                cur++ // skip the end marker
            }

            while (cur < lines.size) {
                if (lines[cur] == GRANULAR_METADATA_RESOLUTINS_REPORT_START_MARKER)
                    result += consumeReport()
                else
                    cur++ // skip line
            }

            return result
        }
    }

    private fun doTest(testCase: KpmDependencyResolutionTestCase) {
        with(PublishAllTestCaseExecutor()) {
            val buildable = prepare(testCase)
            val buildOptions = defaultBuildOptions().copy(withBuildCache = true)

            val actualGranularResolutionsReports = mutableListOf<String>()

            buildable.topologicallySortedProjects.forEach { kpmProject ->
                // Running multiple builds and not just one is required because otherwise Gradle will fail to resolve dependencies on modules
                // that haven't been published yet.
                buildable.project.injectReportCollectingScript()
                buildable.project.build(":${kpmProject.name}:publish", options = buildOptions) {
                    assertSuccessful()

                    actualGranularResolutionsReports += output.collectGranularResolutionReports()
                }
            }

            testCase.compareWithActual(actualGranularResolutionsReports)
        }
    }

    private fun KpmDependencyResolutionTestCase.compareWithActual(actualGranularResolutionsReports: List<String>) {
        val fileWithExpectedReport = File(TESTDATA_ROOT, this.name + ".txt")

        KotlinTestUtils.assertEqualsToFile(fileWithExpectedReport, actualGranularResolutionsReports.joinToString(separator = "\n\n"))
    }

    private fun Project.injectReportCollectingScript() {
        val buildGradleKts = gradleBuildScript()
        assert(buildGradleKts.extension == "kts") { "Only Kotlin scripts are supported." }

        val testTaskName = "reportDependencyTransformationsForTest"

        if (testTaskName !in buildGradleKts.readText()) {
            buildGradleKts.modify {
                "import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.FragmentGranularMetadataResolver\n" + it + "\n" + """
                    val $testTaskName by tasks.creating {
                        doFirst {
                            val resolvers = project.properties["org.jetbrains.kotlin.dependencyResolution.fragmentGranularMetadataResolvers.${KotlinVersion.CURRENT}"]
                            println("<fragment-granular-metadata-resolutions-report>")
                            resolvers.forEach { (module, resolver)  ->
                                println(module)
                                println(resolver.renderDependenciesTransformations())
                            }
                            println("</fragment-granular-metadata-resolutions-report>")
                        }
                    }
                    """.trimIndent()
            }
        }
    }

    @Test
    @TestMetadata("simpleProjectToProjectDependency.txt")
    fun testSimpleProjectToProjectDependency() {
        doTest(simpleProjectToProjectDependency())
    }
}