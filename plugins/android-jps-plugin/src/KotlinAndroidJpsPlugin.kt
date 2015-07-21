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

import org.jetbrains.kotlin.jps.build.KotlinJpsCompilerArgumentsProvider
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.model.module.JpsModule
import java.io.File
import org.jetbrains.jps.android.AndroidJpsUtil
import com.intellij.util.PathUtil
import org.jetbrains.jps.model.library.JpsOrderRootType
import org.jetbrains.jps.model.module.JpsLibraryDependency
import org.jetbrains.jps.model.module.JpsModuleDependency

public class KotlinAndroidJpsPlugin : KotlinJpsCompilerArgumentsProvider {
    override fun getExtraArguments(moduleBuildTarget: ModuleBuildTarget, context: CompileContext): List<String> {
        val module = moduleBuildTarget.getModule()
        val pluginId = ANDROID_COMPILER_PLUGIN_ID
        val resPath = getAndroidResPath(module)
        val manifestFile = getAndroidManifest(module)
        val supportV4 = if (isSupportV4LibraryAttached(module)) "true" else "false"

        return if (resPath != null && manifestFile != null) listOf(
                getPluginOptionString(pluginId, RESOURCE_PATH_OPTION_NAME, resPath),
                getPluginOptionString(pluginId, MANIFEST_FILE_OPTION_NAME, manifestFile),
                getPluginOptionString(pluginId, SUPPORT_V4_OPTION_NAME, supportV4))
        else listOf()
    }

    private fun isSupportV4LibraryAttached(module: JpsModule): Boolean {
        return module.getDependenciesList().getDependencies().any { dep ->
            when (dep) {
                is JpsLibraryDependency ->
                    dep.getLibrary()?.getFiles(JpsOrderRootType.COMPILED)?.any {
                        it.name.startsWith("support-v4") && it.extension.toUpperCase() == "JAR"
                    } ?: false
                else -> false
            }
        }
    }

    override fun getClasspath(moduleBuildTarget: ModuleBuildTarget, context: CompileContext): List<String> {
        val inJar = File(PathUtil.getJarPathForClass(javaClass)).isFile()
        val manifestFile = getAndroidManifest(moduleBuildTarget.getModule())
        return if (manifestFile != null) {
            listOf(
                    if (inJar) {
                        val libDirectory = File(PathUtil.getJarPathForClass(javaClass)).getParentFile().getParentFile()
                        File(libDirectory, JAR_FILE_NAME).getAbsolutePath()
                    } else {
                        // We're in tests now (in out/production/android-jps-plugin)
                        val kotlinProjectDirectory = File(PathUtil.getJarPathForClass(javaClass)).getParentFile().getParentFile().getParentFile()
                        File(kotlinProjectDirectory, "dist/kotlinc/lib/$JAR_FILE_NAME").getAbsolutePath()
                    })
        }
        else listOf()
    }

    private fun getAndroidResPath(module: JpsModule): String? {
        val extension = AndroidJpsUtil.getExtension(module) ?: return null
        val path = AndroidJpsUtil.getResourceDirForCompilationPath(extension)
        return File(path!!.getAbsolutePath() + "/layout").getAbsolutePath()
    }

    private fun getAndroidManifest(module: JpsModule): String? {
        val extension = AndroidJpsUtil.getExtension(module) ?: return null
        return AndroidJpsUtil.getManifestFileForCompilationPath(extension)!!.getAbsolutePath()
    }

    companion object {
        private val JAR_FILE_NAME = "android-compiler-plugin.jar"
        private val ANDROID_COMPILER_PLUGIN_ID = "org.jetbrains.kotlin.android"

        private val RESOURCE_PATH_OPTION_NAME = "androidRes"
        private val MANIFEST_FILE_OPTION_NAME = "androidManifest"
        private val SUPPORT_V4_OPTION_NAME = "supportV4"

        private fun getPluginOptionString(pluginId: String, key: String, value: String): String {
            return "plugin:$pluginId:$key=$value"
        }
    }
}