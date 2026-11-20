/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.assertEquals

@OtherGradlePluginTests
@DisplayName("Build script path references")
class BuildScriptPathReferencesTest {

    @TempDir
    lateinit var projectDir: Path

    /** A path inside this test's own JUnit temp directory, so it must be reported. */
    private val tempRepo: String get() = projectDir.resolve("repo").toString()

    @DisplayName("Reports the line of a build script that references a test temp directory")
    @Test
    fun reportsLineReferencingTestTempDirectory() {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            |rootProject.name = "test"
            |dependencyResolutionManagement { repositories { maven { url = uri("$tempRepo") } } }
            |""".trimMargin()
        )

        assertEquals(
            listOf("settings.gradle.kts:2: dependencyResolutionManagement { repositories { maven { url = uri(\"$tempRepo\") } } }"),
            findBuildScriptPathReferences(projectDir),
        )
    }

    @DisplayName("Reports Groovy and Kotlin scripts of nested builds")
    @Test
    fun reportsScriptsOfNestedBuilds() {
        projectDir.resolve("buildSrc").createDirectories().resolve("build.gradle").writeText("def repo = '$tempRepo'\n")
        projectDir.resolve("included").createDirectories().resolve("settings.gradle.kts").writeText("\n\nval repo = \"$tempRepo\"\n")
        projectDir.resolve("build.gradle.kts").writeText("val repo = \"/allowed/repo\"\n")

        assertEquals(
            listOf(
                "buildSrc/build.gradle:1: def repo = '$tempRepo'",
                "included/settings.gradle.kts:3: val repo = \"$tempRepo\"",
            ),
            findBuildScriptPathReferences(projectDir),
        )
    }

    @DisplayName("Ignores non-script files and Gradle output directories")
    @Test
    fun ignoresNonScriptFilesAndOutputDirectories() {
        projectDir.resolve("gradle.properties").writeText("repo=$tempRepo\n")
        projectDir.resolve("build").createDirectories().resolve("generated.gradle.kts").writeText("\"$tempRepo\"\n")
        projectDir.resolve(".gradle").createDirectories().resolve("cached.gradle").writeText("'$tempRepo'\n")

        assertEquals(
            emptyList(),
            findBuildScriptPathReferences(projectDir),
        )
    }
}
