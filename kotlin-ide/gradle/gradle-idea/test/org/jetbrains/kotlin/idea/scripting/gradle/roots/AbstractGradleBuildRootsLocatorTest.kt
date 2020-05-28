/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.roots

import org.jetbrains.kotlin.idea.scripting.gradle.GradleKotlinScriptConfigurationInputs
import org.jetbrains.kotlin.idea.scripting.gradle.importing.KotlinDslScriptModel

open class AbstractGradleBuildRootsLocatorTest {
    init {
        GradleBuildRoot.skipLastModifiedFilesLoading = true
    }

    private val scripts = mutableMapOf<String, GradleScriptInfo>()
    private val locator = MyRootsLocator()

    private inner class MyRootsLocator : GradleBuildRootsLocator() {
        override fun getScriptInfo(localPath: String): GradleScriptInfo? = scripts[localPath]
        fun accessRoots() = roots
    }

    private fun add(root: GradleBuildRoot.Linked) {
        locator.accessRoots().add(root)
    }

    fun newImportedGradleProject(
        dir: String,
        relativeProjectRoots: List<String> = listOf(""),
        relativeScripts: List<String> = listOf("build.gradle.kts")
    ) {
        val pathPrefix = "$dir/"
        val root = GradleBuildRoot.Imported(
            dir,
            null,
            GradleBuildRootData(
                relativeProjectRoots.map { (pathPrefix + it).removeSuffix("/") },
                listOf(),
                listOf()
            )
        )

        add(root)

        relativeScripts.forEach {
            val path = pathPrefix + it
            scripts[path] = GradleScriptInfo(
                root,
                null,
                KotlinDslScriptModel(
                    file = path,
                    inputs = GradleKotlinScriptConfigurationInputs("", 0, dir),
                    classPath = listOf(),
                    sourcePath = listOf(),
                    imports = listOf(),
                    messages = listOf()
                )
            )
        }
    }

    fun findScriptBuildRoot(filePath: String, searchNearestLegacy: Boolean = true) =
        locator.findScriptBuildRoot(filePath, searchNearestLegacy)
}