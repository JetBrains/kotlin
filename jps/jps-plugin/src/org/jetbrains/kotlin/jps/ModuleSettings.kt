/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps

import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.config.CompilerSettings
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.jetbrains.kotlin.jps.model.kotlinFacetExtension

val JpsModule.targetPlatform: TargetPlatformKind<*>?
    get() = kotlinFacetExtension?.settings?.targetPlatformKind

val JpsModule.productionOutputFilePath: String?
    get() {
        val facetSettings = kotlinFacetExtension?.settings ?: return null
        if (facetSettings.useProjectSettings) return null
        return facetSettings.productionOutputPath
    }

val JpsModule.testOutputFilePath: String?
    get() {
        val facetSettings = kotlinFacetExtension?.settings ?: return null
        if (facetSettings.useProjectSettings) return null
        return facetSettings.testOutputPath
    }

val JpsModule.kotlinCompilerSettings: CompilerSettings
    get() {
        val defaultSettings = copyBean(project.kotlinCompilerSettings)
        val facetSettings = kotlinFacetExtension?.settings ?: return defaultSettings
        if (facetSettings.useProjectSettings) return defaultSettings
        return facetSettings.compilerSettings ?: defaultSettings
    }

val JpsModule.kotlinCompilerArguments
    get() = getCompilerArguments<CommonCompilerArguments>()

val JpsModule.k2MetadataCompilerArguments
    get() = getCompilerArguments<K2MetadataCompilerArguments>()

val JpsModule.k2JsCompilerArguments
    get() = getCompilerArguments<K2JSCompilerArguments>()

val JpsModule.k2JvmCompilerArguments
    get() = getCompilerArguments<K2JVMCompilerArguments>()

private inline fun <reified T : CommonCompilerArguments> JpsModule.getCompilerArguments(): T {
    val projectSettings = project.kotlinCompilerSettingsContainer[T::class.java]
    val projectSettingsCopy = copyBean(projectSettings)

    val facetSettings = kotlinFacetExtension?.settings ?: return projectSettingsCopy
    if (facetSettings.useProjectSettings) return projectSettingsCopy
    return facetSettings.compilerArguments as? T ?: projectSettingsCopy
}