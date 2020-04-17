/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.importing

import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.configuration.cache.CachedConfigurationInputs
import org.jetbrains.kotlin.idea.core.script.configuration.cache.ScriptConfigurationSnapshot
import org.jetbrains.kotlin.idea.scripting.gradle.GradleScriptInputsWatcher
import org.jetbrains.kotlin.idea.scripting.gradle.getGradleScriptInputsStamp
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import org.jetbrains.kotlin.scripting.resolve.VirtualFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.adjustByDefinition
import org.jetbrains.plugins.gradle.service.project.ProjectResolverContext
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.jdkHome
import kotlin.script.experimental.jvm.jvm

fun saveScriptModels(
    project: Project,
    task: ExternalSystemTaskId,
    javaHomeStr: String?,
    models: List<KotlinDslScriptModel>
) {
    val scriptConfigurations = mutableListOf<Pair<VirtualFile, ScriptConfigurationSnapshot>>()

    val errorReporter = KotlinGradleDslErrorReporter(project, task)

    val javaHome = javaHomeStr?.let { File(it) }
    models.forEach { model ->
        val scriptFile = File(model.file)
        val virtualFile = VfsUtil.findFile(scriptFile.toPath(), true)!!

        val inputs = getGradleScriptInputsStamp(
            project,
            virtualFile,
            givenTimeStamp = model.inputsTimeStamp
        )

        val definition = virtualFile.findScriptDefinition(project) ?: return@forEach

        val configuration =
            definition.compilationConfiguration.with {
                if (javaHome != null) {
                    jvm.jdkHome(javaHome)
                }
                defaultImports(model.imports)
                dependencies(JvmDependency(model.classPath.map {
                    File(
                        it
                    )
                }))
                ide.dependenciesSources(JvmDependency(model.sourcePath.map {
                    File(
                        it
                    )
                }))
            }.adjustByDefinition(definition)

        scriptConfigurations.add(
            Pair(
                virtualFile,
                ScriptConfigurationSnapshot(
                    inputs
                        ?: CachedConfigurationInputs.OutOfDate,
                    listOf(),
                    ScriptCompilationConfigurationWrapper.FromCompilationConfiguration(
                        VirtualFileScriptSource(virtualFile),
                        configuration,
                    ),
                ),
            ),
        )

        errorReporter.reportError(scriptFile, model)
    }

    project.service<GradleScriptInputsWatcher>().saveGradleProjectRootsAfterImport(
        scriptConfigurations.map { it.first.parent.path }.toSet()
    )

    project.service<ScriptConfigurationManager>().saveCompilationConfigurationAfterImport(scriptConfigurations)
    project.service<GradleScriptInputsWatcher>().clearState()
}