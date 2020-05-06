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
 * [GradleBuildRoot] is a linked gradle build (don't confuse with gradle project and included build).
 * Each [GradleBuildRoot] may have it's own Gradle version, Java home and other settings.
 *
 * Typically, IntelliJ project have no more than one [GradleBuildRoot].
 *
 * See [GradleBuildRootsManager] for more details.
 */
sealed class GradleBuildRoot {
    /**
     * The script not related to any Gradle build that is linked to IntelliJ Project,
     * or we cannot known what is it
     */
    class Unlinked : GradleBuildRoot()

    /**
     * Linked project, that may be itself: [Legacy], [New] or [Imported].
     */
    abstract class Linked : GradleBuildRoot() {
        @Volatile
        var importing = false

        abstract val pathPrefix: String

        open val projectRoots: Collection<String> get() = listOf()
    }

    /**
     * Gradle build with old Gradle version (<6.0)
     */
    class Legacy(override val pathPrefix: String) : Linked()

    /**
     * Linked but not yet imported Gradle build.
     */
    class New(override val pathPrefix: String) : Linked()

    /**
     * Imported Gradle build.
     * Each imported build have info about all of it's Kotlin Build Scripts.
     */
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
