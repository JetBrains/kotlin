/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.roots

import org.jetbrains.kotlin.idea.scripting.gradle.GradleKotlinScriptConfigurationInputs
import org.jetbrains.kotlin.idea.scripting.gradle.LastModifiedFiles
import org.jetbrains.kotlin.idea.scripting.gradle.importing.KotlinDslScriptModel
import kotlin.test.assertEquals

open class AbstractGradleBuildRootsLocatorTest {
    private val scripts = mutableMapOf<String, GradleScriptInfo>()
    private val locator = MyRootsLocator()

    private inner class MyRootsLocator : GradleBuildRootsLocator() {
        override fun getScriptInfo(localPath: String): GradleScriptInfo? = scripts[localPath]
        fun accessRoots() = roots
    }

    private fun add(root: GradleBuildRoot) {
        locator.accessRoots().add(root)
    }

    fun newImportedGradleProject(
        dir: String,
        relativeProjectRoots: List<String> = listOf(""),
        relativeScripts: List<String> = listOf("build.gradle.kts")
    ) {
        val pathPrefix = "$dir/"
        val root = Imported(
            dir,
            null,
            GradleBuildRootData(
                relativeProjectRoots.map { (pathPrefix + it).removeSuffix("/") },
                listOf(),
                listOf()
            ),
            LastModifiedFiles()
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

    fun assertNotificationKind(filePath: String, notificationKind: GradleBuildRootsLocator.NotificationKind) {
        assertEquals(notificationKind, findScriptBuildRoot(filePath)?.notificationKind)
    }
}
