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

package org.jetbrains.kotlin.android.synthetic.idea

import com.intellij.openapi.module.Module
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.android.synthetic.AndroidCommandLineProcessor.Companion.ANDROID_COMPILER_PLUGIN_ID
import org.jetbrains.kotlin.android.synthetic.AndroidCommandLineProcessor.Companion.EXPERIMENTAL_OPTION
import org.jetbrains.kotlin.android.synthetic.AndroidCommandLineProcessor.Companion.ENABLED_OPTION
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.idea.caches.resolve.ModuleSourceInfo
import org.jetbrains.kotlin.idea.facet.KotlinFacet

private val ANNOTATION_OPTION_PREFIX = "plugin:$ANDROID_COMPILER_PLUGIN_ID:"

private fun Module.isOptionEnabledInFacet(option: CliOption): Boolean {
    val kotlinFacet = KotlinFacet.get(this) ?: return false
    val commonArgs = kotlinFacet.configuration.settings.compilerArguments ?: return false

    val prefix = ANNOTATION_OPTION_PREFIX + option.name + "="

    val optionValue = commonArgs.pluginOptions
            ?.firstOrNull { it.startsWith(prefix) }
            ?.substring(prefix.length)

    return optionValue == "true"
}

internal val Module.androidExtensionsIsEnabled: Boolean
    get() = isOptionEnabledInFacet(ENABLED_OPTION)

internal val ModuleInfo.androidExtensionsIsExperimental: Boolean
    get() {
        val module = (this as? ModuleSourceInfo)?.module ?: return false
        return module.isOptionEnabledInFacet(EXPERIMENTAL_OPTION)
    }