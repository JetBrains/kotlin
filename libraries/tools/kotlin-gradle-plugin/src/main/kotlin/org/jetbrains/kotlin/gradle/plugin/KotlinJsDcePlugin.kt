/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.file.UnionFileTree
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJsDce
import java.io.File

class KotlinJsDcePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply(Kotlin2JsPluginWrapper::class.java)

        val javaPluginConvention = project.convention.getPlugin(JavaPluginConvention::class.java)
        javaPluginConvention.sourceSets.forEach { processSourceSet(project, it) }
    }

    private fun processSourceSet(project: Project, sourceSet: SourceSet) {
        val kotlinTaskName = sourceSet.getCompileTaskName("kotlin2Js")
        val kotlinTask = project.tasks.findByName(kotlinTaskName) as? Kotlin2JsCompile ?: return
        val dceTaskName = sourceSet.getTaskName(DCE_TASK_PREFIX, TASK_SUFFIX)
        val dceTask = project.tasks.create(dceTaskName, KotlinJsDce::class.java).also {
            it.dependsOn(kotlinTask)
            project.tasks.findByName("build").dependsOn(it)
        }

        project.afterEvaluate {
            val outputDir = File(kotlinTask.outputFile).parentFile

            val configuration = project.configurations.findByName(sourceSet.compileConfigurationName)
            val dceInputTrees = listOf(project.fileTree(kotlinTask.outputFile)) + configuration.map { project.fileTree(it) }
            val dceInputFiles = UnionFileTree("dce-input", dceInputTrees)

            with (dceTask) {
                classpath = sourceSet.compileClasspath
                destinationDir = File(outputDir, "min")
                source(dceInputFiles)
            }
        }
    }

    companion object {
        private const val TASK_SUFFIX = "kotlinJs"
        private const val DCE_TASK_PREFIX = "runDce"
    }
}