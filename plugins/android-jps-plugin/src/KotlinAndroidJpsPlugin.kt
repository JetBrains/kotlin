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

public class KotlinAndroidJpsPlugin : KotlinJpsCompilerArgumentsProvider {
    private val jarFileName = "android-compiler-plugin.jar"

    private fun getPluginOptionString(pluginId: String, key: String, value: String): String {
        return "plugin:$pluginId:$key=$value"
    }

    override fun getExtraArguments(moduleBuildTarget: ModuleBuildTarget, context: CompileContext): List<String> {
        val module = moduleBuildTarget.getModule()
        val pluginId = "org.jetbrains.kotlin.android"
        val resPath = getAndroidResPath(module)
        val manifestFile = getAndroidManifest(module)
        return if (resPath != null && manifestFile != null) listOf(
                getPluginOptionString(pluginId, "androidRes", resPath),
                getPluginOptionString(pluginId, "androidManifest", manifestFile))
        else listOf()
    }

    override fun getClasspath(moduleBuildTarget: ModuleBuildTarget, context: CompileContext): List<String> {
        val inJar = File(PathUtil.getJarPathForClass(javaClass)).isFile()
        val manifestFile = getAndroidManifest(moduleBuildTarget.getModule())
        return if (manifestFile != null) {
            listOf(
                    if (inJar) {
                        val libDirectory = File(PathUtil.getJarPathForClass(javaClass)).getParentFile().getParentFile()
                        File(libDirectory, jarFileName).getAbsolutePath()
                    } else {
                        // We're in tests now (in out/production/android-jps-plugin)
                        val kotlinProjectDirectory = File(PathUtil.getJarPathForClass(javaClass)).getParentFile().getParentFile().getParentFile()
                        File(kotlinProjectDirectory, "dist/kotlinc/lib/$jarFileName").getAbsolutePath()
                    })
        }
        else listOf()
    }

    private fun getAndroidResPath(module: JpsModule): String? {
        val extension = AndroidJpsUtil.getExtension(module)
        if (extension == null) return null
        val path = AndroidJpsUtil.getResourceDirForCompilationPath(extension)
        return File(path!!.getAbsolutePath() + "/layout").getAbsolutePath()
    }

    private fun getAndroidManifest(module: JpsModule): String? {
        val extension = AndroidJpsUtil.getExtension(module)
        if (extension == null) return null
        return AndroidJpsUtil.getManifestFileForCompilationPath(extension)!!.getAbsolutePath()
    }
}