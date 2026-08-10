/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.utils.normalizedAbsoluteFile
import org.jetbrains.kotlin.gradle.utils.userKotlinPersistentDir
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.test.Test
import kotlin.test.assertEquals

class UserKotlinPersistentDirTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun relativeKotlinUserHomePath() {
        val project = projectWithKotlinUserHome("../.kotlin")
        project.evaluate()
        assertEquals(project.projectDir.parentFile.resolve(".kotlin"), project.userKotlinPersistentDir.normalizedAbsoluteFile())
    }

    @Test
    fun absoluteKotlinUserHomePath() {
        val kotlinUserHomeDir = tempDir.resolve(".kotlin")
        val project = projectWithKotlinUserHome(kotlinUserHomeDir.absolutePathString())
        project.evaluate()
        assertEquals(kotlinUserHomeDir.toFile(), project.userKotlinPersistentDir)
    }

    @Test
    fun tildeNotResolvedInKotlinUserHomePath() {
        val project = projectWithKotlinUserHome("~/.kotlin")
        project.evaluate()
        assertEquals(project.projectDir.resolve("~/.kotlin"), project.userKotlinPersistentDir)
    }

    private fun projectWithKotlinUserHome(kotlinUserHomePath: String): ProjectInternal {
        return buildProjectWithJvm(
            preApplyCode = {
                project.extraProperties["kotlin.user.home"] = kotlinUserHomePath
            }
        )
    }
}
