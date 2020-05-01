/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.roots

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsCache
import org.jetbrains.kotlin.idea.scripting.gradle.GradleScriptDefinitionsContributor
import org.jetbrains.kotlin.idea.scripting.gradle.importing.KotlinDslScriptModel
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.VirtualFileScriptSource
import java.io.File

/**
 * See [GradleBuildRootsManager]
 */
sealed class GradleBuildRoot {
    /**
     * Add Gradle Project
     * for other scripts too
     *
     * precompiled script
     * may be also included scripts not returned by gradle: todo proper notification
     */
    class Unlinked() : GradleBuildRoot()

    abstract class Linked : GradleBuildRoot() {
        @Volatile
        var importing = false

        abstract val pathPrefix: String

        open val projectRoots: Collection<String> get() = listOf()
    }

    /**
     * Notification: please update to Gradle 6.0
     * default loader, cases:
     * - not loaded: Notification: Load configration to get code insights
     * - loaded, not up-to-date: Notifaction: Reload configuraiton
     * - loaded, up-to-date: Nothing
     */
    class Legacy(override val pathPrefix: String) : Linked()

    /**
     * not imported:
     *  Notification: Import Gradle project to get code insights
     * during import:
     * - disable action on importing. todo: don't miss failed import
     * - pause analyzing, todo: change status text to: importing gradle project
     */
    class New(override val pathPrefix: String) : Linked()

    // precompiled scripts not detected by gradle

    class Imported(
        val project: Project,
        val dir: VirtualFile,
        val javaHome: File?,
        val data: GradleBuildRootData
    ) : Linked() {
        override val pathPrefix: String = dir.path

        override val projectRoots: Collection<String>
            get() = data.projectRoots

        fun collectConfigurations(builder: ScriptClassRootsCache.Builder) {
            if (javaHome != null) {
                builder.addSdk(javaHome)
            }

            val definitions = GradleScriptDefinitionsContributor.getDefinitions(project)

            builder.classes.addAll(data.templateClasspath)
            data.models.forEach { script ->
                val definition = selectScriptDefinition(script, definitions)

                builder.scripts[script.file] = GradleScriptInfo(this, definition, script)

                builder.classes.addAll(script.classPath)
                builder.sources.addAll(script.sourcePath)
            }
        }

        private fun selectScriptDefinition(
            script: KotlinDslScriptModel,
            definitions: List<ScriptDefinition>
        ): ScriptDefinition? {
            val file = LocalFileSystem.getInstance().findFileByPath(script.file) ?: return null
            val scriptSource = VirtualFileScriptSource(file)
            return definitions.firstOrNull { it.isScript(scriptSource) }
        }
    }
}