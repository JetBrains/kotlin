package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DisplayName("Source file modifications")
@JvmGradlePluginTests
class SourceFileIsModifiedIT : KGPBaseTest() {

    @DisplayName("Removed in non-incremental compilation")
    @GradleTest
    fun testClassIsRemovedNonIC(gradleVersion: GradleVersion) {
        doTestClassIsRemoved(gradleVersion, defaultBuildOptions)
    }

    @DisplayName("Removed in incremental compilation")
    @GradleTest
    fun testClassIsRemovedIC(gradleVersion: GradleVersion) {
        doTestClassIsRemoved(gradleVersion, defaultBuildOptions.copy(incremental = true))
    }

    private fun doTestClassIsRemoved(
        gradleVersion: GradleVersion,
        buildOptions: BuildOptions
    ) {
        doTest(gradleVersion, buildOptions) { dummyFile ->
            assertTrue(Files.deleteIfExists(dummyFile), "Could not delete $dummyFile")
        }
    }

    @DisplayName("Renamed in non-incremental compilation")
    @GradleTest
    fun testClassIsRenamedNonIC(gradleVersion: GradleVersion) {
        doTestClassIsRenamed(gradleVersion, defaultBuildOptions)
    }

    @DisplayName("Renamed in incremental compilation")
    @GradleTest
    fun testClassIsRenamedIC(gradleVersion: GradleVersion) {
        doTestClassIsRenamed(gradleVersion, defaultBuildOptions.copy(incremental = true))
    }

    private fun doTestClassIsRenamed(
        gradleVersion: GradleVersion,
        buildOptions: BuildOptions
    ) {
        doTest(gradleVersion, buildOptions) { dummyFile ->
            dummyFile.modify { it.replace("Dummy", "ForDummies") }
        }
    }

    private fun doTest(
        gradleVersion: GradleVersion,
        buildOptions: BuildOptions,
        transformDummy: (Path) -> Unit
    ) {
        project("kotlinInJavaRoot", gradleVersion) {
            build("build", buildOptions = buildOptions) {
                assertTasksExecuted(":compileKotlin")
                assertTasksNoSource(":compileTestKotlin")
            }

            val dummyFile = javaSourcesDir().resolve("kotlinPackage/Dummy.kt")
            transformDummy(dummyFile)

            build("build", buildOptions = buildOptions) {
                val dummyClassFile = kotlinClassesDir().findInPath("Dummy.class")
                assertNull(dummyClassFile, "$dummyClassFile should not exist!")
            }

            // check that class removal does not trigger rebuild
            build("build", buildOptions = buildOptions) {
                assertTasksUpToDate(":compileKotlin", ":compileJava")
            }
        }
    }
}