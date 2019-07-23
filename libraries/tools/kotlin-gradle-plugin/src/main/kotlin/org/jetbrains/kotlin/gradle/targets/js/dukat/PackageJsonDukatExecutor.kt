/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dukat

import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject

class PackageJsonDukatExecutor(
    val nodeJs: NodeJsRootExtension,
    val typeDefinitions: List<DtsResolver.Dts>,
    val npmProject: NpmProject,
    val packageJsonIsUpdated: Boolean,
    val operation: String = OPERATION,
    val compareInputs: Boolean = true
) {
    companion object {
        const val OPERATION = "Generating Kotlin/JS external declarations"
    }

    val versionFile = npmProject.externalsDirRoot.resolve("version.txt")
    val version = DukatCompilationResolverPlugin.VERSION + ", " + nodeJs.versions.dukat.version
    val prevVersion = if (versionFile.exists()) versionFile.readText() else null

    val inputsFile = npmProject.externalsDirRoot.resolve("inputs.txt")

    val shouldSkip: Boolean
        get() = inputsFile.isFile && prevVersion == version && !packageJsonIsUpdated

    fun execute() {
        // delete file to run visit on error even without package.json updates
        versionFile.delete()

        npmProject.externalsDirRoot.mkdirs()
        val inputs = typeDefinitions.joinToString("\n") { it.inputKey }

        if (!compareInputs || !inputsFile.isFile || inputsFile.readText() != inputs) {
            // delete file to run visit on error even without package.json updates
            inputsFile.delete()

            npmProject.externalsDir.deleteRecursively()
            DukatExecutor(
                npmProject.compilation,
                typeDefinitions.map { it.file },
                npmProject.externalsDir,
                operation = operation
            ).execute()

            inputsFile.writeText(inputs)
        }

        versionFile.writeText(version)
    }
}