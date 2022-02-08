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
import org.jetbrains.kotlin.gradle.plugin.konan.*
import org.jetbrains.kotlin.konan.MetaVersion
import org.jetbrains.kotlin.konan.util.DependencyProcessor
import org.jetbrains.kotlin.konan.util.DependencySource
import java.io.IOException
import org.jetbrains.kotlin.*

open class KonanCompilerDownloadTask : DefaultTask() {

    internal companion object {
        internal const val BASE_DOWNLOAD_URL = "https://download.jetbrains.com/kotlin/native/builds"
    }

    /**
     * If true the task will also download dependencies for targets specified by the konan.targets project extension.
     */
    @Internal var downloadDependencies: Boolean = false

    @TaskAction
    fun downloadAndExtract() {
        if (!project.hasProperty(KonanPlugin.ProjectProperty.DOWNLOAD_COMPILER)) {
            val konanHome = project.kotlinNativeDist
            logger.info("Use a user-defined compiler path: $konanHome")
        } else {
            try {
                val downloadUrlDirectory = buildString {
                    append("$BASE_DOWNLOAD_URL/")
                    val version = project.konanVersion
                    when (version.meta) {
                        MetaVersion.DEV -> append("dev/")
                        else -> append("releases/")
                    }
                    append("$version/")
                    append(project.simpleOsName)
                }
                val konanCompiler = project.konanCompilerName()
                val parentDir = DependencyProcessor.localKonanDir
                logger.info("Downloading Kotlin/Native compiler from $downloadUrlDirectory/$konanCompiler into $parentDir")
                DependencyProcessor(
                        parentDir,
                        downloadUrlDirectory,
                        mapOf(konanCompiler to listOf(DependencySource.Remote.Public))
                ).run()
            } catch (e: IOException) {
                throw GradleScriptException("Cannot download Kotlin/Native compiler", e)
            }
        }

        // Download dependencies if a user said so.
        if (downloadDependencies) {
            val runner = KonanCliCompilerRunner(project, project.konanExtension.jvmArgs)
            project.konanTargets.forEach {
                runner.run("-Xcheck_dependencies", "-target", it.visibleName)
            }
        }
    }
}
