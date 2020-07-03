/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.importing

import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil.toSystemIndependentName
import com.intellij.openapi.vfs.VfsUtil
import org.gradle.tooling.model.kotlin.dsl.EditorReportSeverity
import org.gradle.tooling.model.kotlin.dsl.KotlinDslScriptsModel
import org.jetbrains.kotlin.gradle.BrokenKotlinDslScriptsModel
import org.jetbrains.kotlin.idea.KotlinIdeaGradleBundle
import org.jetbrains.kotlin.idea.scripting.gradle.getGradleScriptInputsStamp
import org.jetbrains.kotlin.idea.scripting.gradle.roots.GradleBuildRootsManager
import org.jetbrains.plugins.gradle.model.BuildScriptClasspathModel
import org.jetbrains.plugins.gradle.service.project.DefaultProjectResolverContext
import org.jetbrains.plugins.gradle.service.project.ProjectResolverContext
import java.io.File

fun saveGradleBuildEnvironment(resolverCtx: ProjectResolverContext) {
    val task = resolverCtx.externalSystemTaskId
    val tasks = KotlinDslSyncListener.instance.tasks
    val sync = synchronized(tasks) { tasks[task] }
    if (sync != null) {
        val gradleHome = resolverCtx.getExtraProject(BuildScriptClasspathModel::class.java)?.gradleHomeDir?.canonicalPath
            ?: resolverCtx.settings?.gradleHome
        synchronized(sync) {
            sync.gradleVersion = resolverCtx.projectGradleVersion
            sync.javaHome = (resolverCtx as? DefaultProjectResolverContext)
                ?.buildEnvironment
                ?.java?.javaHome?.canonicalPath
                ?.let { toSystemIndependentName(it) }

            if (gradleHome != null) {
                sync.gradleHome = toSystemIndependentName(gradleHome)
            }
        }
    }
}

fun processScriptModel(
    resolverCtx: ProjectResolverContext,
    model: KotlinDslScriptsModel,
    projectName: String
) {
    if (model is BrokenKotlinDslScriptsModel) {
        LOG.error(
            "Couldn't get KotlinDslScriptsModel for $projectName:\n${model.message}\n${model.stackTrace}"
        )
    } else {
        val task = resolverCtx.externalSystemTaskId
        val project = task.findProject() ?: return
        val models = model.toListOfScriptModels(project)

        val tasks = KotlinDslSyncListener.instance.tasks
        val sync = synchronized(tasks) { tasks[task] }
        if (sync != null) {
            synchronized(sync) {
                sync.models.addAll(models)
            }
        }

        if (models.containsErrors()) {
            if (sync != null) {
                synchronized(sync) {
                    sync.failed = true
                }
            }
            throw IllegalStateException(KotlinIdeaGradleBundle.message("title.kotlin.build.script"))
        }
    }
}

private fun Collection<KotlinDslScriptModel>.containsErrors(): Boolean {
    return any { it.messages.any { it.severity == KotlinDslScriptModel.Severity.ERROR } }
}

private fun KotlinDslScriptsModel.toListOfScriptModels(project: Project): List<KotlinDslScriptModel> =
    scriptModels.map { (file, model) ->
        val messages = mutableListOf<KotlinDslScriptModel.Message>()

        model.exceptions.forEach {
            val fromException = parsePositionFromException(it)
            if (fromException != null) {
                val (filePath, _) = fromException
                if (filePath != file.path) return@forEach
            }
            messages.add(
                KotlinDslScriptModel.Message(
                    KotlinDslScriptModel.Severity.ERROR,
                    it.substringBefore(System.lineSeparator()),
                    it,
                    fromException?.second
                )
            )
        }

        model.editorReports.forEach {
            messages.add(
                KotlinDslScriptModel.Message(
                    when (it.severity) {
                        EditorReportSeverity.WARNING -> KotlinDslScriptModel.Severity.WARNING
                        else -> KotlinDslScriptModel.Severity.ERROR
                    },
                    it.message,
                    position = KotlinDslScriptModel.Position(it.position?.line ?: 0, it.position?.column ?: 0)
                )
            )
        }

        // TODO: NPE
        val virtualFile = VfsUtil.findFile(file.toPath(), true)!!

        // todo(KT-34440): take inputs snapshot before starting import
        KotlinDslScriptModel(
            toSystemIndependentName(file.absolutePath),
            getGradleScriptInputsStamp(project, virtualFile)!!, // TODO: NPE
            model.classPath.map { toSystemIndependentName(it.absolutePath) },
            model.sourcePath.map { toSystemIndependentName(it.absolutePath) },
            model.implicitImports,
            messages
        )
    }

class KotlinDslGradleBuildSync(val workingDir: String, val taskId: ExternalSystemTaskId) {
    val ts = System.currentTimeMillis()
    var project: Project? = null
    var gradleVersion: String? = null
    var gradleHome: String? = null
    var javaHome: String? = null
    val projectRoots = mutableSetOf<String>()
    val models = mutableListOf<KotlinDslScriptModel>()
    var failed = false

    override fun toString(): String {
        return "KotlinGradleDslSync(workingDir=$workingDir, gradleVersion=$gradleVersion, gradleHome=$gradleHome, javaHome=$javaHome, projectRoots=$projectRoots, failed=$failed)"
    }
}

fun saveScriptModels(project: Project, build: KotlinDslGradleBuildSync) {
    synchronized(build) {
        val errorReporter = KotlinGradleDslErrorReporter(project, build.taskId)

        build.models.forEach { model ->
            errorReporter.reportError(File(model.file), model)
        }

        // todo: use real info about projects
        build.projectRoots.addAll(build.models.map { toSystemIndependentName(File(it.file).parent) })

        GradleBuildRootsManager.getInstance(project).update(build)
    }
}
