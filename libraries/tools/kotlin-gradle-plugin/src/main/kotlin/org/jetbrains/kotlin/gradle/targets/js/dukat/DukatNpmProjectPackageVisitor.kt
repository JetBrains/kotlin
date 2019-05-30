/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dukat

import org.jetbrains.kotlin.gradle.targets.js.nodejs.nodeJs
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectPackage
import java.io.File

class DukatNpmProjectPackageVisitor(val it: NpmProjectPackage) {
    val npmProject = it.npmProject
    val compilation = npmProject.compilation

    fun visit(updated: Boolean) {
        if (!it.project.nodeJs.root.experimental.generateKotlinExternals) return

        val inputsFile = npmProject.externalsDirRoot.resolve("inputs.txt")
        val versionFile = npmProject.externalsDirRoot.resolve("version.txt")
        val prevVersion = if (versionFile.exists()) versionFile.readText() else null
        if (inputsFile.exists() && prevVersion == VERSION && !updated) return

        // delete file to run visit on error even without package.json
        versionFile.delete()

        val modules = npmProject.modules.copy(
            packageJsonEntries = listOf("types"),
            indexFileSuffixes = listOf(".d.ts")
        )

        val all = it.npmDependencies
            .filter { it.scope != NpmDependency.Scope.DEV }
            .flatMapTo(mutableSetOf()) { it.getDependenciesRecursively() }

        val inputsList = mutableListOf<String>()
        val typeDefinitions = mutableListOf<File>()
        all.forEach { npmDependency ->
            val typeDefFile = modules.resolve(npmDependency.key)
            if (typeDefFile != null) {
                typeDefinitions.add(typeDefFile)
                inputsList.add(npmDependency.key + "@" + npmDependency.resolvedVersion + "#" + npmDependency.integrity)
            }
        }

        npmProject.externalsDirRoot.mkdirs()
        val inputs = inputsList.sorted().joinToString("\n")

        if (!inputsFile.isFile || inputsFile.readText() != inputs) {
            // delete file to run visit on error even without package.json
            inputsFile.delete()

            npmProject.externalsDir.deleteRecursively()
            DukatExecutor(
                compilation,
                typeDefinitions,
                npmProject.externalsDir,
                operation = "${npmProject.name} > Generating Kotlin/JS external declarations"
            ).execute()

            inputsFile.writeText(inputs)
        }

        versionFile.writeText(VERSION)
    }

    companion object {
        const val VERSION = "2"
    }
}