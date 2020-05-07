/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.roots

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.uсache.ScriptClassRootsBuilder
import org.jetbrains.kotlin.idea.scripting.gradle.GradleScriptDefinitionsContributor
import org.jetbrains.kotlin.idea.scripting.gradle.LastModifiedFiles
import org.jetbrains.kotlin.idea.scripting.gradle.importing.KotlinDslScriptModel
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.VirtualFileScriptSource
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
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
    @Suppress("CanSealedSubClassBeObject")
    class Unlinked : GradleBuildRoot()

    /**
     * Linked project, that may be itself: [Legacy], [New] or [Imported].
     */
    abstract class Linked : GradleBuildRoot() {
        @Volatile
        var importing = false

        abstract val manager: GradleBuildRootsManager

        abstract val pathPrefix: String

        abstract val projectRoots: Collection<String>

        val dir: VirtualFile?
            get() = LocalFileSystem.getInstance().findFileByPath(pathPrefix)

        private lateinit var lastModifiedFiles: LastModifiedFiles

        fun loadLastModifiedFiles() {
            lastModifiedFiles = dir?.let { LastModifiedFiles.read(it) } ?: LastModifiedFiles()
        }

        fun saveLastModifiedFiles() {
            LastModifiedFiles.write(dir ?: return, lastModifiedFiles)
        }

        fun areRelatedFilesChangedBefore(file: VirtualFile, lastModified: Long): Boolean =
            lastModifiedFiles.lastModifiedTimeStampExcept(file.path) < lastModified

        fun fileChanged(filePath: String, ts: Long) {
            lastModifiedFiles.fileChanged(ts, filePath)
            manager.scheduleLastModifiedFilesSave()
        }
    }

    abstract class WithoutScriptModels(settings: GradleProjectSettings) : Linked() {
        final override val pathPrefix = settings.externalProjectPath!!
        final override val projectRoots = settings.modules.takeIf { it.isNotEmpty() } ?: listOf(pathPrefix)

        init {
            loadLastModifiedFiles()
        }
    }

    /**
     * Gradle build with old Gradle version (<6.0)
     */
    class Legacy(
        override val manager: GradleBuildRootsManager,
        settings: GradleProjectSettings
    ) : WithoutScriptModels(settings) {
        init {
            loadLastModifiedFiles()
        }
    }

    /**
     * Linked but not yet imported Gradle build.
     */
    class New(
        override val manager: GradleBuildRootsManager,
        settings: GradleProjectSettings
    ) : WithoutScriptModels(settings) {
        init {
            loadLastModifiedFiles()
        }
    }

    /**
     * Imported Gradle build.
     * Each imported build have info about all of it's Kotlin Build Scripts.
     */
    class Imported(
        override val manager: GradleBuildRootsManager,
        override val pathPrefix: String,
        val javaHome: File?,
        val data: GradleBuildRootData
    ) : Linked() {
        val project: Project
            get() = manager.project

        override val projectRoots: Collection<String>
            get() = data.projectRoots

        init {
            loadLastModifiedFiles()
        }

        fun collectConfigurations(builder: ScriptClassRootsBuilder) {
            if (javaHome != null) {
                builder.sdks.addSdk(javaHome)
            }

            val definitions = GradleScriptDefinitionsContributor.getDefinitions(project)

            builder.addTemplateClassesRoots(data.templateClasspath)

            data.models.forEach { script ->
                val definition = selectScriptDefinition(script, definitions)

                builder.addCustom(
                    script.file,
                    script.classPath,
                    script.sourcePath,
                    GradleScriptInfo(this, definition, script)
                )
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
