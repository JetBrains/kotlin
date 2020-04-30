/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsCache
import org.jetbrains.kotlin.idea.scripting.gradle.importing.KotlinDslScriptModel
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import org.jetbrains.kotlin.scripting.resolve.VirtualFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.adjustByDefinition
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.jdkHome
import kotlin.script.experimental.jvm.jvm

sealed class GradleBuildRoot {
    abstract val pathPrefix: String

    /**
     * Add Gradle Project
     * for other scripts too
     *
     * definitely not precompiled scripts (it is detected as in sources roots)
     * may be also included scripts not returned by gradle: todo proper notification
     */
    class Unlinked(override val pathPrefix: String) : GradleBuildRoot()

    abstract class Linked : GradleBuildRoot() {
        @Volatile
        var importing = false
    }

    /**
     * Notification: please update to Gradle 6.0
     * default loader, cases:
     * - not loaded: todo: Notification: Load configration to get code insights
     * - loaded, not up-to-date: Notifaction: Reload configuraiton
     * - loaded, up-to-date: Nothing
     */
    class Legacy(override val pathPrefix: String) : Linked()

    /**
     * not imported:
     *  Notification: Import Gradle project to get code insights
     * during import:
     * - todo: disable action on importing. don't miss failed import
     * - pause analyzing, todo: change status text to: importing gradle project
     *
     * todo:
     *  detect precompiled scripts (in sources roots)
     */
    class New(override val pathPrefix: String) : Linked()

    // precompiled scripts not detected by gradle

    class Imported(
        val project: Project,
        val dir: VirtualFile,
        val context: GradleKtsContext,
        val data: GradleImportedBuildRootData
    ) : Linked() {
        override val pathPrefix: String = dir.path

        fun collectConfigurations(builder: ScriptClassRootsCache.Builder) {
            val javaHome = context.javaHome
            javaHome?.let { builder.addSdk(it) }

            val anyScript = data.models.firstOrNull()?.let {
                LocalFileSystem.getInstance().findFileByPath(it.file)
            }

            val scriptDefinition: ScriptDefinition? = anyScript?.findScriptDefinition(project)

            if (scriptDefinition != null) {
                builder.classes.addAll(data.templateClasspath)
            }

            val root = ScriptInfoRoot(this, scriptDefinition)

            data.models.forEach {
                builder.scripts[it.file] = ScriptInfo(root, it)

                builder.classes.addAll(it.classPath)
                builder.sources.addAll(it.sourcePath)
            }
        }

        /**
         * Common info between scripts (to save bits inside ScriptInfo)
         */
        data class ScriptInfoRoot(
            val buildRoot: Imported,
            val scriptDefinition: ScriptDefinition?
        )

        data class ScriptInfo(
            val root: ScriptInfoRoot,
            val model: KotlinDslScriptModel
        ) : ScriptClassRootsCache.LightScriptInfo() {
            val buildRoot get() = root.buildRoot

            override fun buildConfiguration(): ScriptCompilationConfigurationWrapper? {
                val javaHome = buildRoot.context.javaHome
                val scriptDefinition = root.scriptDefinition

                val scriptFile = File(model.file)
                val virtualFile = VfsUtil.findFile(scriptFile.toPath(), true)!!

                if (scriptDefinition == null) return null

                return ScriptCompilationConfigurationWrapper.FromCompilationConfiguration(
                    VirtualFileScriptSource(virtualFile),
                    scriptDefinition.compilationConfiguration.with {
                        if (javaHome != null) {
                            jvm.jdkHome(javaHome)
                        }
                        defaultImports(model.imports)
                        dependencies(JvmDependency(model.classPath.map { File(it) }))
                        ide.dependenciesSources(JvmDependency(model.sourcePath.map { File(it) }))
                    }.adjustByDefinition(scriptDefinition)
                )
            }
        }
    }
}
