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

package org.jetbrains.kotlin.config

import com.intellij.util.PathUtil
import com.intellij.util.xmlb.SkipDefaultsSerializationFilter
import com.intellij.util.xmlb.XmlSerializer
import org.jdom.DataConversionException
import org.jdom.Element
import org.jetbrains.kotlin.cli.common.arguments.*

fun Element.getOption(name: String) = getChildren("option").firstOrNull { it.getAttribute("name").value == name }

private fun Element.getOptionValue(name: String) = getOption(name)?.getAttribute("value")?.value

private fun Element.getOptionBody(name: String) = getOption(name)?.children?.firstOrNull()

private fun readV1Config(element: Element): KotlinFacetSettings {
    return KotlinFacetSettings().apply {
        val useProjectSettings = element.getOptionValue("useProjectSettings")?.toBoolean()

        val versionInfoElement = element.getOptionBody("versionInfo")
        val targetPlatformName = versionInfoElement?.getOptionValue("targetPlatformName")
        val languageLevel = versionInfoElement?.getOptionValue("languageLevel")
        val apiLevel = versionInfoElement?.getOptionValue("apiLevel")
        val targetPlatform = TargetPlatformKind.ALL_PLATFORMS.firstOrNull { it.description == targetPlatformName }
                             ?: TargetPlatformKind.Jvm[JvmTarget.DEFAULT]

        val compilerInfoElement = element.getOptionBody("compilerInfo")

        val compilerSettings = CompilerSettings().apply {
            compilerInfoElement?.getOptionBody("compilerSettings")?.let { compilerSettingsElement ->
                XmlSerializer.deserializeInto(this, compilerSettingsElement)
            }
        }

        val commonArgumentsElement = compilerInfoElement?.getOptionBody("_commonCompilerArguments")
        val jvmArgumentsElement = compilerInfoElement?.getOptionBody("k2jvmCompilerArguments")
        val jsArgumentsElement = compilerInfoElement?.getOptionBody("k2jsCompilerArguments")

        val compilerArguments = targetPlatform.createCompilerArguments()

        commonArgumentsElement?.let { XmlSerializer.deserializeInto(compilerArguments, it) }
        when (compilerArguments) {
            is K2JVMCompilerArguments -> jvmArgumentsElement?.let { XmlSerializer.deserializeInto(compilerArguments, it) }
            is K2JSCompilerArguments -> jsArgumentsElement?.let { XmlSerializer.deserializeInto(compilerArguments, it) }
        }

        if (languageLevel != null) {
            compilerArguments.languageVersion = languageLevel
        }

        if (apiLevel != null) {
            compilerArguments.apiVersion = apiLevel
        }

        if (useProjectSettings != null) {
            this.useProjectSettings = useProjectSettings
        }
        else {
            // Migration problem workaround for pre-1.1-beta releases (mainly 1.0.6) -> 1.1-rc+
            // Problematic cases: 1.1-beta/1.1-beta2 -> 1.1-rc+ (useProjectSettings gets reset to false)
            // This heuristic detects old enough configurations:
            if (jvmArgumentsElement == null) {
                this.useProjectSettings = false
            }
        }

        this.compilerSettings = compilerSettings
        this.compilerArguments = compilerArguments
    }
}

private fun readV2Config(element: Element): KotlinFacetSettings {
    return KotlinFacetSettings().apply {
        element.getAttributeValue("useProjectSettings")?.let { useProjectSettings = it.toBoolean() }
        val platformName = element.getAttributeValue("platform")
        val platformKind = TargetPlatformKind.ALL_PLATFORMS.firstOrNull { it.description == platformName } ?: TargetPlatformKind.DEFAULT_PLATFORM
        element.getChild("compilerSettings")?.let {
            compilerSettings = CompilerSettings()
            XmlSerializer.deserializeInto(compilerSettings!!, it)
        }
        element.getChild("compilerArguments")?.let {
            compilerArguments = platformKind.createCompilerArguments()
            XmlSerializer.deserializeInto(compilerArguments!!, it)
        }
    }
}

fun deserializeFacetSettings(element: Element): KotlinFacetSettings {
    val version =
            try {
                element.getAttribute("version")?.intValue
            }
            catch(e: DataConversionException) {
                null
            } ?: KotlinFacetSettings.DEFAULT_VERSION
    return when (version) {
        1 -> readV1Config(element)
        2 -> readV2Config(element)
        else -> KotlinFacetSettings() // Reset facet configuration if versions don't match
    }
}

fun CommonCompilerArguments.convertPathsToSystemIndependent() {
    pluginClasspaths?.forEachIndexed { index, s -> pluginClasspaths[index] = PathUtil.toSystemIndependentName(s) }

    when (this) {
        is K2JVMCompilerArguments -> {
            destination = PathUtil.toSystemIndependentName(destination)
            classpath = PathUtil.toSystemIndependentName(classpath)
            jdkHome = PathUtil.toSystemIndependentName(jdkHome)
            kotlinHome = PathUtil.toSystemIndependentName(kotlinHome)
            friendPaths?.forEachIndexed { index, s -> friendPaths[index] = PathUtil.toSystemIndependentName(s) }
            declarationsOutputPath = PathUtil.toSystemIndependentName(declarationsOutputPath)
        }

        is K2JSCompilerArguments -> {
            outputFile = PathUtil.toSystemIndependentName(outputFile)
            libraries = PathUtil.toSystemIndependentName(libraries)
        }

        is K2MetadataCompilerArguments -> {
            destination = PathUtil.toSystemIndependentName(destination)
            classpath = PathUtil.toSystemIndependentName(classpath)
        }
    }
}

fun CompilerSettings.convertPathsToSystemIndependent() {
    scriptTemplatesClasspath = PathUtil.toSystemIndependentName(scriptTemplatesClasspath)
    outputDirectoryForJsLibraryFiles = PathUtil.toSystemIndependentName(outputDirectoryForJsLibraryFiles)
}

fun KotlinFacetSettings.serializeFacetSettings(element: Element) {
    val filter = SkipDefaultsSerializationFilter()

    element.setAttribute("version", KotlinFacetSettings.CURRENT_VERSION.toString())
    targetPlatformKind?.let {
        element.setAttribute("platform", it.description)
    }
    if (!useProjectSettings) {
        element.setAttribute("useProjectSettings", useProjectSettings.toString())
    }
    compilerSettings?.let { copyBean(it) }?.let {
        it.convertPathsToSystemIndependent()
        Element("compilerSettings").apply {
            XmlSerializer.serializeInto(it, this, filter)
            element.addContent(this)
        }
    }
    compilerArguments?.let { copyBean(it) }?.let {
        it.convertPathsToSystemIndependent()
        Element("compilerArguments").apply {
            XmlSerializer.serializeInto(it, this, filter)
            element.addContent(this)
        }
    }
}
