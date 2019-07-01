/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File
import java.util.*

internal fun PropertiesProvider.mapKotlinTaskProperties(task: AbstractKotlinCompile<*>) {
    coroutines?.let { task.coroutinesFromGradleProperties = it }
    useFallbackCompilerSearch?.let { task.useFallbackCompilerSearch = it }

    if (task is KotlinCompile) {
        incrementalJvm?.let { task.incremental = it }
        usePreciseJavaTracking?.let {
            task.usePreciseJavaTracking = it
        }
    }

    if (task is Kotlin2JsCompile) {
        incrementalJs?.let { task.incremental = it }
    }
}

internal class PropertiesProvider(private val project: Project) {
    private val localProperties = Properties()

    init {
        val localPropertiesFile = project.rootProject.file("local.properties")
        if (localPropertiesFile.isFile) {
            localPropertiesFile.inputStream().use {
                localProperties.load(it)
            }
        }
    }

    val coroutines: Coroutines?
        get() = property("kotlin.coroutines")?.let { Coroutines.byCompilerArgument(it) }

    val buildReportEnabled: Boolean
        get() = booleanProperty("kotlin.build.report.enable") ?: false

    val buildReportVerbose: Boolean
        get() = booleanProperty("kotlin.build.report.verbose") ?: false

    val buildReportDir: File?
        get() = property("kotlin.build.report.dir")?.let { File(it) }

    val incrementalJvm: Boolean?
        get() = booleanProperty("kotlin.incremental")

    val incrementalJs: Boolean?
        get() = booleanProperty("kotlin.incremental.js")

    val incrementalMultiplatform: Boolean?
        get() = booleanProperty("kotlin.incremental.multiplatform")

    val usePreciseJavaTracking: Boolean?
        get() = booleanProperty("kotlin.incremental.usePreciseJavaTracking")

    val useFallbackCompilerSearch: Boolean?
        get() = booleanProperty("kotlin.useFallbackCompilerSearch")

    val keepMppDependenciesIntactInPoms: Boolean?
        get() = booleanProperty("kotlin.mpp.keepMppDependenciesIntactInPoms")

    val ignorePluginLoadedInMultipleProjects: Boolean?
        get() = booleanProperty("kotlin.pluginLoadedInMultipleProjects.ignore")

    val setJvmTargetFromAndroidCompileOptions: Boolean?
        get() = booleanProperty("kotlin.setJvmTargetFromAndroidCompileOptions")

    val enableGranularSourceSetsMetadata: Boolean?
        get() = booleanProperty("kotlin.mpp.enableGranularSourceSetsMetadata")

    /**
     * Enables parallel tasks execution within a project with Workers API.
     * Does not enable using actual worker proccesses
     * (Kotlin Daemon can be shared which uses less memory)
     */
    val parallelTasksInProject: Boolean?
        get() = booleanProperty("kotlin.parallel.tasks.in.project")

    /**
     * Enables individual test task reporting for aggregated test tasks.
     *
     * By default individual test tasks will not fail build if this task will be executed,
     * also individual html and xml reports will replaced by one consolidated html report.
     */
    val individualTaskReports: Boolean?
        get() = booleanProperty("kotlin.tests.individualTaskReports")

    private fun booleanProperty(propName: String): Boolean? =
        property(propName)?.toBoolean()

    private fun property(propName: String): String? =
        if (project.hasProperty(propName)) {
            project.property(propName) as? String
        } else {
            localProperties.getProperty(propName)
        }
}
