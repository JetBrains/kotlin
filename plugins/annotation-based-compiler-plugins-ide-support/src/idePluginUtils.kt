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

package org.jetbrains.kotlin.annotation.plugin.ide

import com.intellij.openapi.module.Module
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import java.io.File

fun Module.getSpecialAnnotations(prefix: String): List<String> {
    val kotlinFacet = org.jetbrains.kotlin.idea.facet.KotlinFacet.get(this) ?: return emptyList()
    val commonArgs = kotlinFacet.configuration.settings.compilerArguments ?: return emptyList()

    return commonArgs.pluginOptions
            ?.filter { it.startsWith(prefix) }
            ?.map { it.substring(prefix.length) }
    ?: emptyList()
}

internal class AnnotationBasedCompilerPluginSetup(val annotationFqNames: List<String>, val classpath: List<String>)

internal fun modifyCompilerArgumentsForPlugin(
        facet: KotlinFacet,
        setup: AnnotationBasedCompilerPluginSetup?,
        compilerPluginId: String,
        pluginName: String,
        annotationOptionName: String
) {
    val facetSettings = facet.configuration.settings

    // investigate why copyBean() sometimes throws exceptions
    val commonArguments = facetSettings.compilerArguments ?: CommonCompilerArguments.DummyImpl()

    /** See [CommonCompilerArguments.PLUGIN_OPTION_FORMAT] **/
    val annotationOptions = setup?.annotationFqNames?.map { "plugin:$compilerPluginId:$annotationOptionName=$it" } ?: emptyList()

    val oldPluginOptions = (commonArguments.pluginOptions ?: emptyArray()).filterTo(mutableListOf()) { !it.startsWith("plugin:$compilerPluginId:") }
    val newPluginOptions = oldPluginOptions + annotationOptions

    val oldPluginClasspaths = (commonArguments.pluginClasspaths ?: emptyArray()).filterTo(mutableListOf()) {
        val lastIndexOfFile = it.lastIndexOfAny(charArrayOf('/', File.separatorChar))
        if (lastIndexOfFile < 0) {
            return@filterTo true
        }
        !it.drop(lastIndexOfFile + 1).matches("(kotlin-)?(maven-)?$pluginName-.*\\.jar".toRegex())
    }

    val newPluginClasspaths = oldPluginClasspaths + (setup?.classpath ?: emptyList())

    commonArguments.pluginOptions = newPluginOptions.toTypedArray()
    commonArguments.pluginClasspaths = newPluginClasspaths.toTypedArray()

    facetSettings.compilerArguments = commonArguments
}