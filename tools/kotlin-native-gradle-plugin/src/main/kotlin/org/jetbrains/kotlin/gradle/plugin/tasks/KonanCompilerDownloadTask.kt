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

package org.jetbrains.kotlin.gradle.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleScriptException
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.konan.util.DependencyProcessor
import org.jetbrains.kotlin.konan.util.DependencySource
import org.jetbrains.kotlin.konan.util.visibleName
import java.io.File
import java.io.IOException

open class KonanCompilerDownloadTask : DefaultTask() {

    internal companion object {
        internal const val BASE_DOWNLOAD_URL = "https://download.jetbrains.com/kotlin/native/builds"

        internal val KONAN_PARENT_DIR = "${System.getProperty("user.home")}/.konan"
    }

    /**
     * If true the task will also download dependencies for targets specified by the konan.targets project extension.
     */
    @Internal var downloadDependencies: Boolean = false

    @TaskAction
    fun downloadAndExtract() {
        if (!project.hasProperty(KonanPlugin.ProjectProperty.DOWNLOAD_COMPILER)) {
            val konanHome = project.getProperty(KonanPlugin.ProjectProperty.KONAN_HOME)
            logger.info("Use a user-defined compiler path: $konanHome")
            if (project.hasProperty(KonanPlugin.ProjectProperty.KONAN_VERSION)) {
                val konanVersion = project.getProperty(KonanPlugin.ProjectProperty.KONAN_VERSION)
                logger.warn("${KonanPlugin.ProjectProperty.KONAN_VERSION.propertyName} " +
                        "(=$konanVersion) property is ignored " +
                        "because a user-defined compiler path is specified: $konanHome")
            }
        } else {
            try {
                val downloadUrlDirectory = buildString {
                    append("$BASE_DOWNLOAD_URL/")
                    val version = project.konanVersion
                    append(if (version.contains("dev")) "dev/" else "releases/")
                    append("$version/")
                    append(project.simpleOsName)
                }
                val konanCompiler = project.konanCompilerName()
                logger.info("Downloading Kotlin/Native compiler from $downloadUrlDirectory/$konanCompiler into $KONAN_PARENT_DIR")
                DependencyProcessor(
                        File(KONAN_PARENT_DIR),
                        downloadUrlDirectory,
                        mapOf(konanCompiler to listOf(DependencySource.Remote.Public))
                ).run()
            } catch (e: IOException) {
                throw GradleScriptException("Cannot download Kotlin/Native compiler", e)
            }
        }

        // Download dependencies if a user said so.
        if (downloadDependencies) {
            val runner = KonanCompilerRunner(project)
            project.konanTargets.forEach {
                runner.run("--check_dependencies", "-target", it.visibleName)
            }
        }
    }
}
