/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.model

import org.jetbrains.jps.model.ex.JpsElementBase
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.platform.TargetPlatform

val JpsModule.kotlinFacet: JpsKotlinFacetModuleExtension?
    get() = container.getChild(JpsKotlinFacetModuleExtension.KIND)

val JpsModule.platform: TargetPlatform?
    get() = kotlinFacet?.settings?.targetPlatform

val JpsModule.kotlinKind: KotlinModuleKind
    get() = kotlinFacet?.settings?.kind ?: KotlinModuleKind.DEFAULT

val JpsModule.isTestModule: Boolean
    get() = kotlinFacet?.settings?.isTestModule ?: false

/**
 * Modules which is imported from sources sets of the compilation represented by this module.
 * This module is not included.
 */
val JpsModule.sourceSetModules: List<JpsModule>
    get() = findDependencies(kotlinFacet?.settings?.sourceSetNames)

/**
 * Legacy. List of modules with `expectedBy` dependency.
 */
val JpsModule.expectedByModules: List<JpsModule>
    get() = findDependencies(kotlinFacet?.settings?.implementedModuleNames)

private fun JpsModule.findDependencies(moduleNames: List<String>?): List<JpsModule> {
    if (moduleNames == null || moduleNames.isEmpty()) return listOf()

    val result = mutableSetOf<JpsModule>()

    JpsJavaExtensionService.dependencies(this)
        .processModules {
            if (it.name in moduleNames) {
                // Note, production sources should be added for both production and tests targets
                result.add(it)
            }
        }

    return result.toList()
}

val JpsModule.productionOutputFilePath: String?
    get() {
        val facetSettings = kotlinFacet?.settings ?: return null
        if (facetSettings.useProjectSettings) return null
        return facetSettings.productionOutputPath
    }

val JpsModule.testOutputFilePath: String?
    get() {
        val facetSettings = kotlinFacet?.settings ?: return null
        if (facetSettings.useProjectSettings) return null
        return facetSettings.testOutputPath
    }

val JpsModule.kotlinCompilerSettings: CompilerSettings
    get() {
        val defaultSettings = copyBean(project.kotlinCompilerSettings)
        val facetSettings = kotlinFacet?.settings ?: return defaultSettings
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

    val facetSettings = kotlinFacet?.settings ?: return projectSettingsCopy
    if (facetSettings.useProjectSettings) return projectSettingsCopy
    facetSettings.updateMergedArguments()
    return facetSettings.mergedCompilerArguments as? T ?: projectSettingsCopy
}

class JpsKotlinFacetModuleExtension(settings: KotlinFacetSettings) : JpsElementBase<JpsKotlinFacetModuleExtension>() {
    var settings = settings
        private set

    companion object {
        val KIND = JpsElementChildRoleBase.create<JpsKotlinFacetModuleExtension>("kotlin facet extension")
        // These must be changed in sync with KotlinFacetType.TYPE_ID and KotlinFacetType.NAME
        val FACET_TYPE_ID = "kotlin-language"
        val FACET_NAME = "Kotlin"
    }

    override fun createCopy() = JpsKotlinFacetModuleExtension(settings)

    override fun applyChanges(modified: JpsKotlinFacetModuleExtension) {
        this.settings = modified.settings
    }
}