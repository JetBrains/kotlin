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

import org.gradle.api.DefaultTask
import org.gradle.api.GradleScriptException
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.konan.DependencyDownloader
import java.io.File

open class CompilerDownloadTask: DefaultTask() {

    internal companion object {
        internal const val DOWNLOAD_URL = "http://download.jetbrains.com/kotlin/native"

        private  val KONAN_PARENT_DIR = "${System.getProperty("user.home")}/.konan"

        internal fun simpleOsName(): String {
            val osName = System.getProperty("os.name")
            return when (osName) {
                "Mac OS X" -> "macos"
                "Linux" -> "linux"
                else -> throw IllegalStateException("Unsupported platform: $osName")
            }
        }
    }

    @TaskAction
    fun downloadAndExtract() {
        if (project.hasProperty(KonanPlugin.KONAN_HOME_PROPERTY_NAME)) {
            val konanHome = project.property(KonanPlugin.KONAN_HOME_PROPERTY_NAME)
            logger.info("Use a user-defined compiler path: $konanHome")
            return
        }
        try {
            val konanCompiler = "kotlin-native-${simpleOsName()}-${project.konanVersion}"
            logger.info("Downloading Kotlin Native compiler from $DOWNLOAD_URL/$konanCompiler into $KONAN_PARENT_DIR")
            DependencyDownloader(File(KONAN_PARENT_DIR), DOWNLOAD_URL, listOf(konanCompiler)).run()
            project.extensions.extraProperties.set(KonanPlugin.KONAN_HOME_PROPERTY_NAME, "$KONAN_PARENT_DIR/$konanCompiler")
        } catch (e: RuntimeException) {
            throw GradleScriptException("Cannot download Kotlin Native compiler", e)
        }
    }
}