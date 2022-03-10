/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.kpm

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.dsl.topLevelExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleModule
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.kpmModules
import org.jetbrains.kotlin.gradle.utils.getOrPut

interface SourceSetMappedFragmentLocator {
    data class FragmentLocation(val moduleName: String, val fragmentName: String) {
        init {
            require(moduleName.isNotEmpty()) { "module name should not be empty, got empty one and fragment name $fragmentName" }
            require(fragmentName.isNotEmpty()) { "fragment name should not be empty, got empty one and module name $moduleName" }
        }
    }

    fun locateFragmentForSourceSet(sourceSetName: String): FragmentLocation?

    companion object {
        private const val EXTRA_PROPERTY_KEY = "kotlin.kpm.fragments.locator"

        fun get(project: Project): CompositeSourceSetMappedFragmentLocator =
            project.extensions.extraProperties.getOrPut(EXTRA_PROPERTY_KEY) {
                when (project.topLevelExtensionOrNull) {
                    is KotlinMultiplatformExtension -> DefaultCompositeLocator(MultiplatformSourceSetMappedFragmentLocator(project))
                    is KotlinSingleTargetExtension -> error("KPM model mapping is not yet supported in single-platform projects; tried to apply to $project")
                    else -> error("couldn't provide model mapping utilities for project $project")
                }
            }

    }
}

/**
 * Extensible [SourceSetMappedFragmentLocator]. It supports adding new implementations using [registerLocator].
 * A new implementation that is added has lower priority than ones registered before it. Registered implementations should only handle
 * their own cases and should return `null` from [SourceSetMappedFragmentLocator.locateFragmentForSourceSet] for inputs that they don't
 * recognize in order to allow the other implementations to handle such inputs properly.
 * An implementing class may or may not provide their own fallback implementation of [SourceSetMappedFragmentLocator] in case no other
 * locator has been registered or none could locate the fragment.
 */
interface CompositeSourceSetMappedFragmentLocator : SourceSetMappedFragmentLocator {
    fun registerLocator(locator: SourceSetMappedFragmentLocator)
}

/**
 * Calls each of the [childLocators] and then chooses the result according to the following logic:
 * * If there's at least result with non-`main` module, choose that result (or first of them if there are multiple)
 * * Otherwise, choose the first not-null result
 * The [fallbackLocator] is used as the *last* locator in addition to the [childLocators].
 */
internal class DefaultCompositeLocator(private val fallbackLocator: SourceSetMappedFragmentLocator?) :
    CompositeSourceSetMappedFragmentLocator {
    private val childLocators = mutableListOf<SourceSetMappedFragmentLocator>()

    override fun registerLocator(locator: SourceSetMappedFragmentLocator) {
        childLocators.add(locator)
    }

    override fun locateFragmentForSourceSet(sourceSetName: String): SourceSetMappedFragmentLocator.FragmentLocation? {
        val locators = listOfNotNull(*childLocators.toTypedArray(), fallbackLocator)
        val results = locators.mapTo(mutableListOf<SourceSetMappedFragmentLocator.FragmentLocation?>()) { null }

        locators.forEachIndexed { index, locator ->
            val result = locator.locateFragmentForSourceSet(sourceSetName)
            if (result != null && result.moduleName != KotlinGradleModule.MAIN_MODULE_NAME)
                return result
            // Otherwise, remember it for the loop below
            results[index] = result
        }

        return results.firstOrNull { it != null }
    }
}

internal class AndroidTestFragmentLocator(private val androidTarget: KotlinAndroidTarget) : SourceSetMappedFragmentLocator {
    override fun locateFragmentForSourceSet(sourceSetName: String): SourceSetMappedFragmentLocator.FragmentLocation? {
        val targetName = androidTarget.name
        val androidSourceSetName = sourceSetName.removePrefix(targetName).takeIf { it != sourceSetName }
            ?: return null

        val moduleName = when {
            "Test" in androidSourceSetName -> KotlinGradleModule.TEST_MODULE_NAME
            else -> return null // should use the general logic then
        }

        val fragmentName =
            if (sourceSetName.endsWith(moduleName, ignoreCase = true)) sourceSetName.dropLast(moduleName.length) else sourceSetName

        return SourceSetMappedFragmentLocator.FragmentLocation(moduleName, fragmentName)
    }
}

internal class MultiplatformSourceSetMappedFragmentLocator(private val project: Project) : SourceSetMappedFragmentLocator {

    override fun locateFragmentForSourceSet(sourceSetName: String): SourceSetMappedFragmentLocator.FragmentLocation {
        val camelCaseParts = sourceSetName.camelCaseParts()
        if (camelCaseParts.size < 2) {
            return SourceSetMappedFragmentLocator.FragmentLocation(KotlinGradleModule.MAIN_MODULE_NAME, sourceSetName)
        }

        val candidateModuleNames =
            (1 until camelCaseParts.size).asSequence().map { camelCaseParts.takeLast(it).joinToString("").decapitalize() }

        val moduleName =
            candidateModuleNames.lastOrNull { project.kpmModules.findByName(it) != null }
                ?: KotlinGradleModule.MAIN_MODULE_NAME

        val fragmentName =
            if (sourceSetName.endsWith(moduleName, ignoreCase = true))
                sourceSetName.dropLast(moduleName.length).takeIf { it.isNotEmpty() }
                    ?: run {
                        check(sourceSetName == moduleName)
                        // TODO Not sure what to do here. Maybe return "common"?
                        sourceSetName
                    }
            else sourceSetName

        return SourceSetMappedFragmentLocator.FragmentLocation(moduleName, fragmentName)
    }

    private fun String.camelCaseParts(): List<String> {
        val capitalizedMatches = Regex("[A-Z][^A-Z]*").findAll(this).toList()
        val firstNonCapitalizedWord =
            capitalizedMatches.firstOrNull()?.takeIf { it.range.first != 0 }
                ?.let { substring(0, it.range.first) }
                ?: this
        return listOf(firstNonCapitalizedWord) + capitalizedMatches.map { it.value }
    }
}

private class SingleTargetSourceSetMappedFragmentLocator : SourceSetMappedFragmentLocator {
    override fun locateFragmentForSourceSet(sourceSetName: String): SourceSetMappedFragmentLocator.FragmentLocation? {
        return SourceSetMappedFragmentLocator.FragmentLocation(sourceSetName, SINGLE_PLATFORM_FRAGMENT_NAME)
    }
}

internal const val SINGLE_PLATFORM_FRAGMENT_NAME = "common"