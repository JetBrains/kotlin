/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android.externalAndroidTarget

import com.android.build.api.dsl.KotlinMultiplatformAndroidDeviceTest
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@AndroidTestVersions(minVersion = TestVersions.AGP.AGP_90)
@AndroidGradlePluginTests
class AndroidTestReportsExternalAndroidTargetIT : KGPBaseTest() {

    // Uses `com.android.kotlin.multiplatform.library`, requires AGP new DSL.
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(enableLegacyAgpDsl = false)

    @GradleAndroidTest
    fun `test - host tests execute and generate reports`(
        gradleVersion: GradleVersion,
        androidVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        externalAndroidLibraryProject(
            gradleVersion = gradleVersion,
            androidVersion = androidVersion,
            jdkVersion = jdkVersion,
            namespace = "org.jetbrains.sample.hosttestreports",
            androidLibraryConfiguration = {
                withHostTest {}
            },
        ) {
            buildScriptInjection {
                kotlinMultiplatform.apply {
                    sourceSets.getByName("commonTest").dependencies {
                        implementation(kotlin("test"))
                    }
                }
            }

            kotlinSourcesDir("commonTest").resolve("CommonTestBase.kt").apply {
                parent.toFile().mkdirs()
                toFile().writeText(
                    """
                    package org.jetbrains.sample

                    import kotlin.test.Test
                    import kotlin.test.assertTrue

                    open class CommonTestBase {
                        @Test
                        fun testInCommon() {
                            assertTrue(true)
                        }
                    }
                    """.trimIndent()
                )
            }

            kotlinSourcesDir("androidHostTest").resolve("HostTest.kt").apply {
                parent.toFile().mkdirs()
                toFile().writeText(
                    """
                    package org.jetbrains.sample

                    import kotlin.test.Test
                    import kotlin.test.assertEquals

                    class HostTest : CommonTestBase() {
                        @Test
                        fun testInHost() {
                            assertEquals("host", "host")
                        }
                    }
                    """.trimIndent()
                )
            }

            build(":testAndroidHostTest") {
                assertTasksExecuted(":compileAndroidHostTest")
                assertTasksExecuted(":testAndroidHostTest")
                assertFileInProjectExists("build/reports/tests/testAndroidHostTest/index.html")
                assertExecutedTestCases(
                    ":testAndroidHostTest",
                    "org.jetbrains.sample.CommonTestBase#testInCommon",
                    "org.jetbrains.sample.HostTest#testInCommon",
                    "org.jetbrains.sample.HostTest#testInHost",
                )
            }
        }
    }
}
