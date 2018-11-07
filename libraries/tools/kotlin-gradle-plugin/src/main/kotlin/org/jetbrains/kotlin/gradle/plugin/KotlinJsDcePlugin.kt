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
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleJavaTargetExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.multiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJsDce
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import java.io.File

class KotlinJsDcePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val kotlinExtension = project.multiplatformExtension ?: run {
            project.pluginManager.apply(Kotlin2JsPluginWrapper::class.java)
            project.kotlinExtension as KotlinSingleJavaTargetExtension
        }

        fun forEachJsTarget(action: (KotlinTarget) -> Unit) {
            when (kotlinExtension) {
                is KotlinSingleJavaTargetExtension -> action(kotlinExtension.target)
                is KotlinMultiplatformExtension ->
                    kotlinExtension.targets
                        .matching { it.platformType == KotlinPlatformType.js }
                        .all { action(it) }
            }
        }

        forEachJsTarget {
            it.compilations.all { processCompilation(project, it) }
        }
    }

    private fun processCompilation(project: Project, kotlinCompilation: KotlinCompilation) {
        val kotlinTaskName = kotlinCompilation.compileKotlinTaskName
        val kotlinTask = project.tasks.findByName(kotlinTaskName) as? Kotlin2JsCompile ?: return
        val dceTaskName = lowerCamelCaseName(
            DCE_TASK_PREFIX,
            kotlinCompilation.target.disambiguationClassifier,
            kotlinCompilation.name.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
            if (kotlinCompilation.target is KotlinWithJavaTarget) TASK_SUFFIX else MPP_TASK_SUFFIX
        )
        val dceTask = project.tasks.create(dceTaskName, KotlinJsDce::class.java).also {
            it.dependsOn(kotlinTask)
            project.tasks.findByName("build")!!.dependsOn(it)
        }

        project.afterEvaluate {
            val outputDir = project.buildDir
                .resolve(DEFAULT_OUT_DIR)
                .resolve(kotlinCompilation.target.disambiguationClassifier?.let { "$it/" }.orEmpty() + kotlinCompilation.name)

            val configuration = project.configurations.getByName(kotlinCompilation.compileDependencyConfigurationName)

            with(dceTask) {
                classpath = configuration
                destinationDir = dceTask.dceOptions.outputDirectory?.let { File(it) } ?: outputDir
                source(kotlinTask.outputFile)
            }
        }
    }

    companion object {
        private const val TASK_SUFFIX = "kotlinJs"
        private const val MPP_TASK_SUFFIX = "kotlin"
        private const val DCE_TASK_PREFIX = "runDce"
        private const val DEFAULT_OUT_DIR = "kotlin-js-min"
    }
}