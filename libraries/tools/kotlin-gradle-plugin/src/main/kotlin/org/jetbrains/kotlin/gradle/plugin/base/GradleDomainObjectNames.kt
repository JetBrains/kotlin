/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.base

import org.jetbrains.kotlin.gradle.dsl.KotlinPlatformExtension
import org.jetbrains.kotlin.gradle.dsl.disambiguateName
import kotlin.reflect.KProperty

private val sourceSetNameFromPlatformExtension = NameFromKotlinExtensionDelegate("SourceSetName")
private val configurationNameFromPlatformExtension = NameFromKotlinExtensionDelegate("ConfigurationName")
private val taskNameFromPlatformExtension = NameFromKotlinExtensionDelegate("TaskName")

internal val KotlinPlatformExtension.mainSourceSetName: String by sourceSetNameFromPlatformExtension
internal val KotlinPlatformExtension.testSourceSetName: String by sourceSetNameFromPlatformExtension

internal val KotlinPlatformExtension.defaultConfigurationName by configurationNameFromPlatformExtension
internal val KotlinPlatformExtension.compileConfigurationName by configurationNameFromPlatformExtension
internal val KotlinPlatformExtension.testCompileConfigurationName by configurationNameFromPlatformExtension

internal val KotlinPlatformExtension.compileClasspathConfigurationName by configurationNameFromPlatformExtension
internal val KotlinPlatformExtension.testCompileClasspathConfigurationName by configurationNameFromPlatformExtension

internal val KotlinPlatformExtension.apiElementsConfigurationName by configurationNameFromPlatformExtension
internal val KotlinPlatformExtension.runtimeElementsConfigurationName by configurationNameFromPlatformExtension
internal val KotlinPlatformExtension.implementationConfigurationName by configurationNameFromPlatformExtension
internal val KotlinPlatformExtension.testImplementationConfigurationName by configurationNameFromPlatformExtension
internal val KotlinPlatformExtension.runtimeConfigurationName by configurationNameFromPlatformExtension
internal val KotlinPlatformExtension.runtimeOnlyConfigurationName by configurationNameFromPlatformExtension
internal val KotlinPlatformExtension.testRuntimeConfigurationName by configurationNameFromPlatformExtension
internal val KotlinPlatformExtension.testRuntimeOnlyConfigurationName by configurationNameFromPlatformExtension

internal val KotlinPlatformExtension.jarTaskName by taskNameFromPlatformExtension
internal val KotlinPlatformExtension.processResourcesTaskName by taskNameFromPlatformExtension

private class NameFromKotlinExtensionDelegate(val propertySuffix: String) {
    operator fun getValue(thisRef: KotlinPlatformExtension, property: KProperty<*>): String {
        require(property.name.endsWith(propertySuffix))
        val nameToDisambiguate = property.name.substringBeforeLast(propertySuffix)
            .also { require(it.isNotEmpty()) }
        return thisRef.disambiguateName(nameToDisambiguate)
    }
}