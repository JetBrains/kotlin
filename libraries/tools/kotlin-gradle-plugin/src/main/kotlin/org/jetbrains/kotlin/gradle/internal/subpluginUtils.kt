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

package org.jetbrains.kotlin.gradle.internal

import org.jetbrains.kotlin.gradle.plugin.CompositeSubpluginOption
import org.jetbrains.kotlin.gradle.plugin.FilesSubpluginOption
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.tasks.CompilerPluginOptions
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectOutputStream
import java.util.*

fun encodePluginOptions(options: Map<String, List<String>>): String {
    val os = ByteArrayOutputStream()
    val oos = ObjectOutputStream(os)

    oos.writeInt(options.size)
    for ((key, values) in options.entries) {
        oos.writeUTF(key)

        oos.writeInt(values.size)
        for (value in values) {
            oos.writeUTF(value)
        }
    }

    oos.flush()
    return Base64.getEncoder().encodeToString(os.toByteArray())
}

internal fun CompilerPluginOptions.withWrappedKaptOptions(withApClasspath: Iterable<File>): CompilerPluginOptions {
    val resultOptionsByPluginId: MutableMap<String, List<SubpluginOption>> =
        subpluginOptionsByPluginId.toMutableMap()

    resultOptionsByPluginId.compute(Kapt3KotlinGradleSubplugin.KAPT_SUBPLUGIN_ID) { _, kaptOptions ->
        val kaptOptionsWithClasspath =
            kaptOptions.orEmpty() + withApClasspath.map { FilesSubpluginOption("apclasspath", listOf(it)) }

        wrapPluginOptions(kaptOptionsWithClasspath, "configuration")
    }

    val result = CompilerPluginOptions()
    resultOptionsByPluginId.forEach { pluginId, options ->
        options.forEach { option -> result.addPluginArgument(pluginId, option) }
    }
    return result
}

fun wrapPluginOptions(options: List<SubpluginOption>, newOptionName: String): List<SubpluginOption> {
    val groupedOptions = options.groupBy { it.key }.mapValues { opt -> opt.value.map { it.value } }
    val encodedOptions = encodePluginOptions(groupedOptions)
    val singleOption = CompositeSubpluginOption(newOptionName, encodedOptions, options)
    return listOf(singleOption)
}