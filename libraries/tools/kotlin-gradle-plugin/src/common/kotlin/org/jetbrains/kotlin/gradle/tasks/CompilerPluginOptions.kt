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
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.CompilerPluginConfig
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

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

    private fun copyOptionsFrom(options: CompilerPluginConfig) {
        options.allOptions().forEach { (pluginId, pluginOptions) ->
            val existingValue = optionsByPluginId[pluginId]
            optionsByPluginId[pluginId] = if (existingValue != null) {
                (existingValue + pluginOptions).toMutableList()
            } else {
                pluginOptions.toMutableList()
            }
        }
    }
}

internal fun ListProperty<out CompilerPluginConfig>.toSingleCompilerPluginOptions(): CompilerPluginOptions {
    var res = CompilerPluginOptions()
    this.get().forEach { res += it }
    return res
}

internal fun Provider<List<SubpluginOption>>.toCompilerPluginOptions(): Provider<CompilerPluginOptions> {
    return map {
        val res = CompilerPluginOptions()
        it.forEach { res.addPluginArgument(Kapt3GradleSubplugin.KAPT_SUBPLUGIN_ID, it) }
        return@map res
    }
}