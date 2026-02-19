/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.jetbrains.kotlin.gradle.util.filterKotlinFusFiles
import kotlin.String
import kotlin.emptyArray
import kotlin.io.path.pathString
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
            "-Pkotlin.session.logger.root.path=${fusEventPath.pathString}"
        ),
        deriveBuildOptions()
    )
    return fusEventPath.resolve("kotlin-profile").filterKotlinFusFiles().single().readLines().toSet()
}