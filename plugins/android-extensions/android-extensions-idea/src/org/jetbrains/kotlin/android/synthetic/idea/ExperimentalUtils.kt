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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import kotlinx.android.extensions.CacheImplementation
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.android.model.isAndroidModule
import org.jetbrains.kotlin.android.synthetic.AndroidCommandLineProcessor.Companion.ANDROID_COMPILER_PLUGIN_ID
import org.jetbrains.kotlin.android.synthetic.AndroidCommandLineProcessor.Companion.EXPERIMENTAL_OPTION
import org.jetbrains.kotlin.android.synthetic.AndroidCommandLineProcessor.Companion.ENABLED_OPTION
import org.jetbrains.kotlin.android.synthetic.AndroidCommandLineProcessor.Companion.DEFAULT_CACHE_IMPL_OPTION
import org.jetbrains.kotlin.android.synthetic.AndroidComponentRegistrar.Companion.parseCacheImplementationType
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.idea.core.unwrapModuleSourceInfo
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.resolve.isJvm

private val ANNOTATION_OPTION_PREFIX = "plugin:$ANDROID_COMPILER_PLUGIN_ID:"

private fun Module.getOptionValueInFacet(option: AbstractCliOption): String? {
    val kotlinFacet = KotlinFacet.get(this) ?: return null
    val commonArgs = kotlinFacet.configuration.settings.compilerArguments ?: return null

    val prefix = ANNOTATION_OPTION_PREFIX + option.optionName + "="

    val optionValue = commonArgs.pluginOptions
            ?.firstOrNull { it.startsWith(prefix) }
            ?.substring(prefix.length)

    return optionValue
}

private fun isTestMode(module: Module): Boolean {
    return ApplicationManager.getApplication().isUnitTestMode && module.isAndroidModule
}

internal val Module.androidExtensionsIsEnabled: Boolean
    get() = isTestMode(this) || getOptionValueInFacet(ENABLED_OPTION) == "true"

internal fun ModuleInfo.findAndroidModuleInfo() = unwrapModuleSourceInfo()?.takeIf { it.platform.isJvm() }

internal val ModuleInfo.androidExtensionsIsEnabled: Boolean
    get() {
        val module = this.findAndroidModuleInfo()?.module ?: return false
        return module.androidExtensionsIsEnabled
    }

internal val ModuleInfo.androidExtensionsIsExperimental: Boolean
    get() {
        val module = this.findAndroidModuleInfo()?.module ?: return false
        return module.androidExtensionsIsExperimental
    }

internal val Module.androidExtensionsIsExperimental: Boolean
    get() {
        if (isTestMode(this)) return true
        return getOptionValueInFacet(EXPERIMENTAL_OPTION) == "true"
    }

val ModuleInfo.androidExtensionsGlobalCacheImpl: CacheImplementation
    get() {
        val module = this.findAndroidModuleInfo()?.module ?: return CacheImplementation.NO_CACHE
        return parseCacheImplementationType(module.getOptionValueInFacet(DEFAULT_CACHE_IMPL_OPTION))
    }