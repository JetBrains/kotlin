/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.android.jps

import com.intellij.util.PathUtil
import org.jetbrains.jps.android.AndroidJpsUtil
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.kotlin.jps.build.KotlinJpsCompilerArgumentsProvider
import org.w3c.dom.Document
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class KotlinAndroidJpsPlugin : KotlinJpsCompilerArgumentsProvider {
    override fun getExtraArguments(moduleBuildTarget: ModuleBuildTarget, context: CompileContext): List<String> {
        val module = moduleBuildTarget.module
        if (!hasAndroidJpsPlugin() || !isAndroidModuleWithoutGradle(module)) return emptyList()

        val pluginId = ANDROID_COMPILER_PLUGIN_ID
        val resPath = getAndroidResPath(module)
        val applicationId = getAndroidManifest(module)?.let { getApplicationPackageFromManifest(it) }

        return if (resPath != null && applicationId != null) {
            listOf(
                    getPluginOptionString(pluginId, VARIANT_OPTION_NAME, "main;$resPath"),
                    getPluginOptionString(pluginId, PACKAGE_OPTION_NAME, applicationId))
        }
        else emptyList()
    }

    private fun isAndroidModuleWithoutGradle(module: JpsModule): Boolean {
        val androidFacet = AndroidJpsUtil.getExtension(module) ?: return false
        return !androidFacet.isGradleProject
    }

    private fun hasAndroidJpsPlugin(): Boolean {
        try {
            Class.forName(ANDROID_JPS_UTIL_CLASS_FQNAME)
            return true
        }
        catch (e: ClassNotFoundException) {
            return false
        }
    }

    override fun getClasspath(moduleBuildTarget: ModuleBuildTarget, context: CompileContext): List<String> {
        val module = moduleBuildTarget.module
        if (!hasAndroidJpsPlugin() || !isAndroidModuleWithoutGradle(module)) return emptyList()

        val inJar = File(PathUtil.getJarPathForClass(javaClass)).isFile
        val manifestFile = getAndroidManifest(moduleBuildTarget.module)
        return if (manifestFile != null) {
            listOf(
                    if (inJar) {
                        val libDirectory = File(PathUtil.getJarPathForClass(javaClass)).parentFile.parentFile
                        File(libDirectory, JAR_FILE_NAME).absolutePath
                    } else {
                        // We're in tests now (in out/production/android-extensions/android-extensions-jps)
                        val kotlinProjectDirectory = File(PathUtil.getJarPathForClass(javaClass)).parentFile.parentFile.parentFile
                        File(kotlinProjectDirectory, "dist/kotlinc/lib/$JAR_FILE_NAME").absolutePath
                    })
        }
        else emptyList()
    }

    private fun getAndroidResPath(module: JpsModule): String? {
        val extension = AndroidJpsUtil.getExtension(module) ?: return null
        return AndroidJpsUtil.getResourceDirForCompilationPath(extension)?.absolutePath
    }

    private fun getAndroidManifest(module: JpsModule): File? {
        val extension = AndroidJpsUtil.getExtension(module) ?: return null
        return AndroidJpsUtil.getManifestFileForCompilationPath(extension)
    }

    companion object {
        private val ANDROID_JPS_UTIL_CLASS_FQNAME = "org.jetbrains.jps.android.AndroidJpsUtil"

        private val JAR_FILE_NAME = "kotlin-android-extensions-compiler-plugin.jar"
        private val ANDROID_COMPILER_PLUGIN_ID = "org.jetbrains.kotlin.android"

        private val VARIANT_OPTION_NAME = "variant"
        private val PACKAGE_OPTION_NAME = "package"

        private fun getApplicationPackageFromManifest(manifestFile: File): String? {
            try {
                return manifestFile.parseXml().documentElement.getAttribute("package")
            }
            catch (e: Exception) {
                return null
            }
        }

        private fun File.parseXml(): Document {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            return builder.parse(this)
        }

        private fun getPluginOptionString(pluginId: String, key: String, value: String): String {
            return "plugin:$pluginId:$key=$value"
        }
    }
}