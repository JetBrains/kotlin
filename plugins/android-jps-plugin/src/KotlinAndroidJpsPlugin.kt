/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

import org.jetbrains.jet.jps.build.KotlinJpsCompilerArgumentsProvider
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.model.module.JpsModule
import java.io.File
import org.jetbrains.jps.android.AndroidJpsUtil
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.android.AndroidCommandLineProcessor
import org.jetbrains.jet.utils.PathUtil
import org.jetbrains.kotlin.compiler.plugin.getPluginOptionString

public class KotlinAndroidJpsPlugin : KotlinJpsCompilerArgumentsProvider {
    private val jarFileName = "android-compiler-plugin.jar"

    override fun getExtraArguments(moduleBuildTarget: ModuleBuildTarget, context: CompileContext): List<String> {
        val module = moduleBuildTarget.getModule()
        val pluginId = AndroidCommandLineProcessor.ANDROID_COMPILER_PLUGIN_ID
        return listOf(
                getPluginOptionString(pluginId, AndroidCommandLineProcessor.RESOURCE_PATH_OPTION.name, getAndroidResPath(module)),
                getPluginOptionString(pluginId, AndroidCommandLineProcessor.MANIFEST_FILE_OPTION.name, getAndroidManifest(module))
        )
    }

    override fun getClasspath(moduleBuildTarget: ModuleBuildTarget, context: CompileContext): List<String> {
        val inJar = PathUtil.getJarPathForClass(javaClass).isFile()
        return listOf(
            if (inJar) {
                val libDirectory = PathUtil.getJarPathForClass(javaClass).getParentFile().getParentFile()
                File(libDirectory, jarFileName).getAbsolutePath()
            } else {
                // We're in tests now (in out/production/android-jps-plugin)
                val kotlinProjectDirectory = PathUtil.getJarPathForClass(javaClass).getParentFile().getParentFile().getParentFile()
                File(kotlinProjectDirectory, "dist/kotlinc/lib/$jarFileName").getAbsolutePath()
            }
        )
    }

    private fun getAndroidResPath(module: JpsModule): String {
        val extension = AndroidJpsUtil.getExtension(module)
        if (extension == null) return ""
        val path = AndroidJpsUtil.getResourceDirForCompilationPath(extension)
        return File(path!!.getAbsolutePath() + "/layout").getAbsolutePath()
    }

    private fun getAndroidManifest(module: JpsModule): String {
        val extension = AndroidJpsUtil.getExtension(module)
        if (extension == null) return ""
        return AndroidJpsUtil.getManifestFileForCompilationPath(extension)!!.getAbsolutePath()
    }
}