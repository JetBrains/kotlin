/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.testkit.runner.BuildResult
import org.jetbrains.kotlin.gradle.util.filterBackwardCompatibilityKotlinFusFiles
import org.jetbrains.kotlin.gradle.util.filterKotlinFusFiles
import java.nio.file.Path
import kotlin.String
import kotlin.emptyArray
import kotlin.io.path.readLines

fun TestProject.collectFusEvents(
    vararg buildArguments: String = emptyArray(),
    buildAction: BuildAction = BuildActions.build,
    deriveBuildOptions: TestProject.() -> BuildOptions = { buildOptions },
): Set<String> {
    val fusEventPath = projectPath.resolve("fusEvent_${generateIdentifier()}")
    buildAction(
        arrayOf(
            *buildArguments,
        ),
        deriveBuildOptions().copy(fusReportDirectory = { fusEventPath }),
        {}
    )
    return fusEventPath.resolve("kotlin-profile").filterKotlinFusFiles().single().readLines().toSet()
}

enum class FusFileType {
    KOTLIN_PROFILE_FILES,
    BACKWARD_COMPATIBILITY_PROFILE_FILES,
}

fun TestProject.validateFusFiles(
    vararg buildArguments: String = emptyArray(),
    buildAction: BuildAction = BuildActions.build,
    buildOptions: BuildOptions = this.buildOptions,
    fusFilesType: List<FusFileType> = FusFileType.entries,
    fusReportRootDirectory: Path = defaultFusReportRootDirectory(),
    buildAssertions: BuildResult.() -> Unit = { },
    validateFusFiles: (List<Path>) -> Unit = {},
) = validateFusDirectory(
    *buildArguments,
    buildAction = buildAction,
    buildOptions = buildOptions,
    fusReportRootDirectory = fusReportRootDirectory,
    buildAssertions = buildAssertions
) { fusDirectory ->
    if (fusFilesType.contains(FusFileType.KOTLIN_PROFILE_FILES))
        fusDirectory.filterKotlinFusFiles().also(validateFusFiles)
    if (fusFilesType.contains(FusFileType.BACKWARD_COMPATIBILITY_PROFILE_FILES))
        fusDirectory.filterBackwardCompatibilityKotlinFusFiles().also(validateFusFiles)
}

fun TestProject.validateFusDirectory(
    vararg buildArguments: String = emptyArray(),
    buildAction: BuildAction = BuildActions.build,
    buildOptions: BuildOptions = this.buildOptions,
    fusReportRootDirectory: Path = defaultFusReportRootDirectory(),
    buildAssertions: BuildResult.() -> Unit = { },
    validateFusDirectory: (Path) -> Unit = {},
) {
    assertNoErrorFilesCreated {
        buildAction(
            arrayOf(
                *buildArguments,
            ),
            buildOptions.copy(fusReportDirectory = { fusReportRootDirectory} ),
            {
                buildAssertions()
            }
        )
        validateFusDirectory(fusReportRootDirectory.resolve("kotlin-profile"))
    }
}

/**
 * A unique FUS report directory, so that reports of different builds do not affect each other.
 *
 * The FUS report directory is a configuration cache input,
 * so builds that are expected to reuse the configuration cache have to share one directory.
 */
fun TestProject.defaultFusReportRootDirectory(): Path = projectPath.resolve("fusEvent_${generateIdentifier()}")

