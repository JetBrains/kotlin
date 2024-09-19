package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writeText
import kotlin.test.assertEquals

@DisplayName("K2Kapt incremental compilation")
@OtherGradlePluginTests
open class Kapt4IncrementalIT : KaptIncrementalIT() {
    override fun TestProject.customizeProject() {
        forceKapt4()
    }

    @Disabled("KT-71786: K2KAPT task does not fail")
    @GradleTest
    override fun testKaptError(gradleVersion: GradleVersion) {}

    @DisplayName("Incremental kapt run is correct after removing all Kotlin sources")
    @GradleTest
    override fun testRemoveAllKotlinSources(gradleVersion: GradleVersion) {
        kaptProject(gradleVersion) {
            build("assemble") {
                assertFileInProjectExists("$KAPT3_STUBS_PATH/bar/UseBKt.java")
            }

            with(projectPath) {
                resolve("src/").deleteRecursively()
                resolve("src/main/java/bar").createDirectories()
                resolve("src/main/java/bar/MyClass.java").writeText(
                    """
                    package bar;
                    public class MyClass {}
                    """.trimIndent()
                )
            }

            build("assemble") {
                // Make sure all generated stubs are removed
                assertEquals(
                    emptyList(),  // K2KAPT removes NonExistentClass as well. It seems to be ok.
                    projectPath
                        .resolve(KAPT3_STUBS_PATH)
                        .toFile()
                        .walk()
                        .filter { it.extension == "java" }
                        .map { it.canonicalPath }
                        .toList()
                )
                // Make sure all compiled kt files are cleaned up.
                assertEquals(
                    emptyList(),
                    projectPath
                        .resolve("build/classes/kotlin")
                        .toFile()
                        .walk()
                        .filter { it.extension == "class" }
                        .toList()
                )
            }
        }
    }
}

@DisplayName("K2Kapt incremental compilation with disabled precise compilation outputs backup")
class Kapt4IncrementalWithoutPreciseBackupIT : Kapt4IncrementalIT() {
    override val defaultBuildOptions =
        super.defaultBuildOptions.copy(usePreciseOutputsBackup = false, keepIncrementalCompilationCachesInMemory = false)
}