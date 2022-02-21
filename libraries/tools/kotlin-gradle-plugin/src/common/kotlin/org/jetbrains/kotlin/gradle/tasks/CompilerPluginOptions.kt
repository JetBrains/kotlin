/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.plugin.*

class CompilerPluginOptions() : CompilerPluginConfig() {

    constructor(options: CompilerPluginConfig) : this() {
        copyOptionsFrom(options)
    }

    @get:Internal
    internal val subpluginOptionsByPluginId = optionsByPluginId

    @get:Internal
    val arguments: List<String>
        get() = optionsByPluginId.flatMap { (pluginId, subplubinOptions) ->
            subplubinOptions.map { option ->
                "plugin:$pluginId:${option.key}=${option.value}"
            }
        }

    fun addPluginArgument(options: CompilerPluginOptions) {
        copyOptionsFrom(options)
    }

    operator fun plus(options: CompilerPluginConfig?): CompilerPluginOptions {
        if (options == null) return this
        val newOptions = CompilerPluginOptions()
        newOptions.optionsByPluginId += subpluginOptionsByPluginId
        newOptions.copyOptionsFrom(options)

        return newOptions
    }

    @Input
    fun getAsTaskInputArgs(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        subpluginOptionsByPluginId.forEach { (id, subpluginOptions) ->
            result += computeForSubpluginId(id, subpluginOptions)
        }
        return result
    }

    private fun computeForSubpluginId(subpluginId: String, subpluginOptions: List<SubpluginOption>): Map<String, String> {
        // There might be several options with the same key. We group them together
        // and add an index to the Gradle input property name to resolve possible duplication:
        val result = mutableMapOf<String, String>()
        val pluginOptionsGrouped = subpluginOptions.groupBy { it.key }
        for ((optionKey, optionsGroup) in pluginOptionsGrouped) {
            optionsGroup.forEachIndexed { index, option ->
                val indexSuffix = if (optionsGroup.size > 1) ".$index" else ""
                when (option) {
                    is InternalSubpluginOption -> return@forEachIndexed

                    is CompositeSubpluginOption -> {
                        val subpluginIdWithWrapperKey = "$subpluginId.$optionKey$indexSuffix"
                        result += computeForSubpluginId(subpluginIdWithWrapperKey, option.originalOptions)
                    }

                    is FilesSubpluginOption -> when (option.kind) {
                        FilesOptionKind.INTERNAL -> Unit
                    }.run { /* exhaustive when */ }

                    else -> {
                        result["$subpluginId." + option.key + indexSuffix] = option.value
                    }
                }
            }
        }
        return result
    }

    private fun copyOptionsFrom(options: CompilerPluginConfig) {
        options.allOptions().forEach { entry ->
            optionsByPluginId[entry.key] = entry.value.toMutableList()
        }
    }
}

internal fun ListProperty<CompilerPluginOptions>.toSingleCompilerPluginOptions(): CompilerPluginOptions {
    val values = this.orNull ?: return CompilerPluginOptions()
    return values.reduceOrNull(CompilerPluginOptions::plus) ?: CompilerPluginOptions()
}