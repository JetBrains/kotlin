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

package org.jetbrains.kotlin.gradle.plugin.android


import com.android.builder.core.BuildToolsServiceLoader
import com.android.jill.api.v01.Api01Config
import com.android.sdklib.BuildToolInfo
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

open class KotlinJillTask : DefaultTask() {

    @InputFile
    lateinit var inputJarFile: File

    @OutputFile
    lateinit var outputJillFile: File

    lateinit var buildTools: BuildToolInfo

    @TaskAction
    fun transform() {
        val jillProvider = BuildToolsServiceLoader.INSTANCE
                .forVersion(buildTools)
                .getServiceLoader(BuildToolsServiceLoader.JILL)
                .firstOrNull() ?: error("Jill provider not found")

        outputJillFile.parentFile.mkdirs()
        val config01: Api01Config = jillProvider.createConfig(Api01Config::class.java)
        config01.setInputJavaBinaryFile(inputJarFile)
        config01.setOutputJackFile(outputJillFile)
        config01.task.run()
    }
}