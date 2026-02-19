/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.testkit.runner.BuildResult
import java.nio.file.Path

typealias KotlinErrorPaths = Set<Path>

internal fun GradleProject.getErrorPaths(): KotlinErrorPaths = setOf(
    projectPersistentCache.resolve("errors"),
    projectPath.resolve(".gradle/kotlin/errors")
)

internal fun BuildResult.assertNoErrorFileCreatedInOutput() {
    assertOutputDoesNotContain("errors were stored into file")
}

internal fun TestProject.assertNoErrorFilesCreated(test: TestProject.() -> Unit = {}){
    val kotlinErrorPaths = getErrorPaths()
    test()
    for (kotlinErrorPath in kotlinErrorPaths) {
        assertDirectoryDoesNotExist(kotlinErrorPath)
    }
}