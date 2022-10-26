/*
 * Copyright 2016-2022 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation.test

import kotlinx.validation.api.*
import org.junit.Assume
import org.junit.Ignore
import org.junit.Test
import java.io.File

@Ignore // Leftovers after revert of #94
internal class AndroidLibraryTest : BaseKotlinGradleTest() {

    // region Kotlin Android Library

    @Test
    fun `Given a Kotlin Android Library, when api is dumped, then task should be successful`() {
        assumeHasAndroid()
        val runner = test {
            createProjectWithSubModules()
            runner {
                arguments.add(":kotlin-library:apiDump")
                arguments.add("--full-stacktrace")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":kotlin-library:apiDump")
        }
    }

    @Test
    fun `Given a Kotlin Android Library, when api is checked, then it should match the expected`() {
        assumeHasAndroid()
        test {
            createProjectWithSubModules()
            runner {
                arguments.add(":kotlin-library:apiCheck")
            }
        }.build().apply {
            assertTaskSuccess(":kotlin-library:apiCheck")
        }
    }

    //endregion

    //region Java Android Library

    @Test
    fun `Given a Java Android Library, when api is dumped, then task should be successful`() {
        assumeHasAndroid()
        val runner = test {
            createProjectWithSubModules()
            runner {
                arguments.add(":java-library:apiDump")
                arguments.add("--full-stacktrace")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":java-library:apiDump")
        }
    }

    @Test
    fun `Given a Java Android Library, when api is checked, then it should match the expected`() {
        assumeHasAndroid()
        test {
            createProjectWithSubModules()
            runner {
                arguments.add(":java-library:apiCheck")
            }
        }.build().apply {
            assertTaskSuccess(":java-library:apiCheck")
        }
    }

    //endregion

    /**
     * Creates a single project with 2 (Kotlin and Java Android Library) modules, applies
     * the plugin on the root project.
     */
    private fun BaseKotlinScope.createProjectWithSubModules() {
        settingsGradleKts {
            resolve("examples/gradle/settings/settings-android-project.gradle.kts")
        }
        buildGradleKts {
            resolve("examples/gradle/base/androidProjectRoot.gradle.kts")
        }
        initLocalProperties()

        dir("kotlin-library") {
            buildGradleKts {
                resolve("examples/gradle/base/androidKotlinLibrary.gradle.kts")
            }
            kotlin("KotlinLib.kt") {
                resolve("examples/classes/KotlinLib.kt")
            }
            apiFile(projectName = "kotlin-library") {
                resolve("examples/classes/KotlinLib.dump")
            }
        }
        dir("java-library") {
            buildGradleKts {
                resolve("examples/gradle/base/androidJavaLibrary.gradle.kts")
            }
            java("JavaLib.java") {
                resolve("examples/classes/JavaLib.java")
            }
            apiFile(projectName = "java-library") {
                resolve("examples/classes/JavaLib.dump")
            }
        }
    }

    private fun initLocalProperties() {
        val home = System.getenv("ANDROID_HOME") ?: System.getenv("HOME")
        File(rootProjectDir, "local.properties").apply {
            writeText("sdk.dir=$home/Android/Sdk")
        }
    }

    // We do not have ANDROID_HOME on CI, and this functionality is not critical, so we are disabling these
    // tests on CI
    private fun assumeHasAndroid() {
        Assume.assumeFalse(System.getenv("ANDROID_HOME").isNullOrEmpty())
    }
}
