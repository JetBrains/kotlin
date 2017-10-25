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



package org.jetbrains.kotlinx.serialization.jps

import com.intellij.util.PathUtil
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.kotlin.jps.build.KotlinJpsCompilerArgumentsProvider
import java.io.File

class KotlinSerializationJpsPlugin : KotlinJpsCompilerArgumentsProvider {
    val JAR_FILE_NAME = "kotlinx-serialization-compiler-plugin.jar"

    override fun getExtraArguments(moduleBuildTarget: ModuleBuildTarget, context: CompileContext): List<String> {
        return emptyList()
    }

    override fun getClasspath(moduleBuildTarget: ModuleBuildTarget, context: CompileContext): List<String> {
        val inJar = File(PathUtil.getJarPathForClass(this::class.java)).isFile
        return listOf(
                if (inJar) {
                    File(PathUtil.getJarPathForClass(this::class.java)).absolutePath
                } else {
                    // We're in tests now
                    val kotlinProjectDirectory = File(PathUtil.getJarPathForClass(javaClass)).parentFile.parentFile.parentFile
                    File(kotlinProjectDirectory, "dist/artifacts/Serialization/lib/$JAR_FILE_NAME").absolutePath
                })
    }
}
