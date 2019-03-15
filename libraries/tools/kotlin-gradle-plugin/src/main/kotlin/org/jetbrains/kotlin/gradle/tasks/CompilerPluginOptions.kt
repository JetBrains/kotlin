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

import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class CompilerPluginOptions {
    internal val subpluginOptionsByPluginId =
        mutableMapOf<String, MutableList<SubpluginOption>>()

    val arguments: List<String>
        get() = subpluginOptionsByPluginId.flatMap { (pluginId, subplubinOptions) ->
            subplubinOptions.map { option ->
                "plugin:$pluginId:${option.key}=${option.value}"
            }
        }

    fun addPluginArgument(pluginId: String, option: SubpluginOption) {
        subpluginOptionsByPluginId.getOrPut(pluginId) { mutableListOf() }.add(option)
    }
}
