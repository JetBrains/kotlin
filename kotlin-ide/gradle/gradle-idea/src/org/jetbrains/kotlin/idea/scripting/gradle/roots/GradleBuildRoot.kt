/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.roots

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.core.script.ucache.ScriptClassRootsBuilder
import org.jetbrains.kotlin.idea.scripting.gradle.GradleScriptDefinitionsContributor
import org.jetbrains.kotlin.idea.scripting.gradle.LastModifiedFiles
import org.jetbrains.kotlin.idea.scripting.gradle.importing.KotlinDslScriptModel
import org.jetbrains.kotlin.idea.scripting.gradle.scriptingDebugLog
import org.jetbrains.kotlin.idea.scripting.gradle.scriptingErrorLog
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.VirtualFileScriptSource
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * [GradleBuildRoot] is a linked gradle build (don't confuse with gradle project and included build).
 * Each [GradleBuildRoot] may have it's own Gradle version, Java home and other settings.
 *
 * Typically, IntelliJ project have no more than one [GradleBuildRoot].
 *
 * See [GradleBuildRootsManager] for more details.
 */
sealed class GradleBuildRoot(
    private val lastModifiedFiles: LastModifiedFiles
) {
    enum class ImportingStatus {
        importing, updatingCaches, updated
    }

    val importing = AtomicReference(ImportingStatus.updated)

    abstract val pathPrefix: String

    abstract val projectRoots: Collection<String>

    val dir: VirtualFile?
        get() = LocalFileSystem.getInstance().findFileByPath(pathPrefix)

    fun saveLastModifiedFiles() {
        scriptingDebugLog { "LasModifiedFiles saved: $lastModifiedFiles" }
        LastModifiedFiles.write(dir ?: return, lastModifiedFiles)
    }

    fun areRelatedFilesChangedBefore(file: VirtualFile, lastModified: Long): Boolean =
        lastModifiedFiles.lastModifiedTimeStampExcept(file.path) < lastModified

    fun fileChanged(filePath: String, ts: Long) {
        lastModifiedFiles.fileChanged(ts, filePath)
    }

    fun isImportingInProgress(): Boolean {
        return importing.get() != ImportingStatus.updated
    }
}

sealed class WithoutScriptModels(
    settings: GradleProjectSettings,
    lastModifiedFiles: LastModifiedFiles
) : GradleBuildRoot(lastModifiedFiles) {
    final override val pathPrefix = settings.externalProjectPath!!
    final override val projectRoots = settings.modules.takeIf { it.isNotEmpty() } ?: listOf(pathPrefix)
}

/**
 * Gradle build with old Gradle version (<6.0)
 */
class Legacy(
    settings: GradleProjectSettings,
    lastModifiedFiles: LastModifiedFiles = settings.loadLastModifiedFiles() ?: LastModifiedFiles()
) : WithoutScriptModels(settings, lastModifiedFiles)

/**
 * Linked but not yet imported Gradle build.
 */
class New(
    settings: GradleProjectSettings,
    lastModifiedFiles: LastModifiedFiles = settings.loadLastModifiedFiles() ?: LastModifiedFiles()
) : WithoutScriptModels(settings, lastModifiedFiles)

/**
 * Imported Gradle build.
 * Each imported build have info about all of it's Kotlin Build Scripts.
 */
class Imported(
    override val pathPrefix: String,
    val data: GradleBuildRootData,
    lastModifiedFiles: LastModifiedFiles
) : GradleBuildRoot(lastModifiedFiles) {
    override val projectRoots: Collection<String>
        get() = data.projectRoots

    val javaHome = data.javaHome?.takeIf { it.isNotBlank() }?.let { File(it) }

    fun collectConfigurations(builder: ScriptClassRootsBuilder) {
        if (javaHome != null) {
            builder.sdks.addSdk(javaHome)
        }

        val definitions = GradleScriptDefinitionsContributor.getDefinitions(builder.project, pathPrefix, data.gradleHome, data.javaHome)
        if (definitions == null) {
            // needed to recreate classRoots if correct script definitions weren't loaded at this moment
            // in this case classRoots will be recreated after script definitions update
            builder.useCustomScriptDefinition()
        }

        builder.addTemplateClassesRoots(GradleScriptDefinitionsContributor.getDefinitionsTemplateClasspath(data.gradleHome))

        data.models.forEach { script ->
            val definition = definitions?.let { selectScriptDefinition(script, it) }

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

fun GradleProjectSettings.loadLastModifiedFiles(): LastModifiedFiles? {
    val externalProjectPath = externalProjectPath ?: return null
    return loadLastModifiedFiles(externalProjectPath)
}

fun loadLastModifiedFiles(rootDirPath: String): LastModifiedFiles? {
    val vFile = LocalFileSystem.getInstance().findFileByPath(rootDirPath) ?: return null
    return LastModifiedFiles.read(vFile)
}
