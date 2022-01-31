/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.kpm

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.dsl.pm20Extension
import org.jetbrains.kotlin.gradle.dsl.topLevelExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTargetPreset
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleModule
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.kpmModules

interface SourceSetMappedFragmentLocator {
    data class FragmentLocation(val moduleName: String, val fragmentName: String) {
        init {
            require(moduleName.isNotEmpty()) { "module name should not be empty, got empty one and fragment name $fragmentName" }
            require(fragmentName.isNotEmpty()) { "fragment name should not be empty, got empty one and module name $moduleName" }
        }
    }

    fun locateFragmentForSourceSet(project: Project, sourceSetName: String): FragmentLocation?

    companion object {
        fun get(project: Project): SourceSetMappedFragmentLocator = when (project.topLevelExtensionOrNull) {
            is KotlinMultiplatformExtension -> MultiplatformSourceSetMappedFragmentLocator()
            is KotlinSingleTargetExtension -> TODO()
            else -> error("couldn't provide model mapping utilities for project $project")
        }
    }
}

private class MultiplatformSourceSetMappedFragmentLocator : SourceSetMappedFragmentLocator {
    override fun locateFragmentForSourceSet(project: Project, sourceSetName: String): SourceSetMappedFragmentLocator.FragmentLocation? {
        val camelCaseParts = sourceSetName.camelCaseParts()
        if (camelCaseParts.size < 2) {
            SourceSetMappedFragmentLocator.FragmentLocation(KotlinGradleModule.MAIN_MODULE_NAME, sourceSetName)
        }

        val androidTarget = when (val ext = project.topLevelExtension) {
            is KotlinAndroidProjectExtension -> ext.target
            is KotlinMultiplatformExtension ->
                ext.targets.withType(KotlinAndroidTarget::class.java).singleOrNull()
                    ?: ext.presets.withType(KotlinAndroidTargetPreset::class.java).single().targetUnderConstruction
            else -> null
        }

        val candidateModuleNames = (1..camelCaseParts.size).asSequence().map { camelCaseParts.takeLast(it).joinToString("").decapitalize() }

        val moduleName =
            run {
                if (androidTarget != null && sourceSetName.startsWith(androidTarget.name))
                    androidSourceSetModuleName(androidTarget.name, sourceSetName)
                else null
            } ?: candidateModuleNames.firstOrNull { project.kpmModules.findByName(it) != null }
            ?: KotlinGradleModule.MAIN_MODULE_NAME

        val fragmentName =
            if (sourceSetName.endsWith(moduleName, ignoreCase = true)) sourceSetName.dropLast(moduleName.length) else sourceSetName

        return SourceSetMappedFragmentLocator.FragmentLocation(moduleName, fragmentName)
    }

    private fun androidSourceSetModuleName(targetName: String, sourceSetName: String): String? {
        val compilationName = sourceSetName.removePrefix(targetName).takeIf { it != sourceSetName } ?: return null
        return when {
            "Test" in compilationName -> "test"
            else -> KotlinGradleModule.MAIN_MODULE_NAME
        }
    }

    private fun String.camelCaseParts(): List<String> {
        val capitalizedMatches = Regex("[A-Z][^A-Z]*").findAll(this).toList()
        val firstNonCapitalizedWord = capitalizedMatches.firstOrNull()?.let { substring(it.range.first) } ?: this
        return listOf(firstNonCapitalizedWord) + capitalizedMatches.map { it.value }
    }
}

private class SingleTargetSourceSetMappedFragmentLocator : SourceSetMappedFragmentLocator {
    override fun locateFragmentForSourceSet(project: Project, sourceSetName: String): SourceSetMappedFragmentLocator.FragmentLocation {
        return SourceSetMappedFragmentLocator.FragmentLocation(sourceSetName, SINGLE_PLATFORM_FRAGMENT_NAME)
    }
}

internal const val SINGLE_PLATFORM_FRAGMENT_NAME = "common"