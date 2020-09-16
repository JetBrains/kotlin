/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Ref
import com.intellij.util.ThrowableRunnable
import org.jetbrains.kotlin.idea.artifacts.KotlinArtifacts
import org.jetbrains.kotlin.idea.test.runAll
import org.jetbrains.kotlin.test.KotlinRoot
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

abstract class AbstractConfigureKotlinInTempDirTest : AbstractConfigureKotlinTest() {
    private lateinit var vfsDisposable: Ref<Disposable>

    override fun createProjectRoot(): File = KotlinTestUtils.tmpDirForReusableFolder("configure")

    override fun setUp() {
        super.setUp()
        vfsDisposable = KotlinTestUtils.allowRootAccess(this, projectRoot.path)
    }

    override fun tearDown() {
        runAll(
            ThrowableRunnable { KotlinTestUtils.disposeVfsRootAccess(vfsDisposable) },
            ThrowableRunnable { super.tearDown() }
        )
    }

    override fun getProjectDirOrFile(isDirectoryBasedProject: Boolean): Path {
        val originalDir = KotlinRoot.DIR.resolve("idea/testData/configuration").resolve(projectName)
        originalDir.copyRecursively(projectRoot)
        val projectFile = projectRoot.resolve("projectFile.ipr")
        val projectRoot = (if (projectFile.exists()) projectFile else projectRoot).toPath()

        val kotlinRuntime = projectRoot.resolve("lib/kotlin-stdlib.jar")
        if (getTestName(true).toLowerCase().contains("latestruntime") && Files.exists(kotlinRuntime)) {
            KotlinArtifacts.instance.kotlinStdlib.copyTo(kotlinRuntime.toFile(), overwrite = true)
        }

        return projectRoot
    }
}