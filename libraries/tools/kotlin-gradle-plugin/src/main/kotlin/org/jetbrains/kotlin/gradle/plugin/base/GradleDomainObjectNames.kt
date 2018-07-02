/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.base

import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import kotlin.reflect.KProperty

private val configurationNameFromPlatformExtension = NameFromKotlinTargetDelegate("ConfigurationName")
private val taskNameFromPlatformExtension = NameFromKotlinTargetDelegate("TaskName")

//internal val KotlinTarget.defaultConfigurationName by configurationNameFromPlatformExtension
//internal val KotlinTarget.compileConfigurationName by configurationNameFromPlatformExtension
//internal val KotlinTarget.testCompileConfigurationName by configurationNameFromPlatformExtension
//
//internal val KotlinTarget.compileClasspathConfigurationName by configurationNameFromPlatformExtension
//internal val KotlinTarget.testCompileClasspathConfigurationName by configurationNameFromPlatformExtension
//internal val KotlinTarget.testRuntimeClasspathConfigurationName by configurationNameFromPlatformExtension
//
//internal val KotlinTarget.apiElementsConfigurationName by configurationNameFromPlatformExtension
//internal val KotlinTarget.runtimeElementsConfigurationName by configurationNameFromPlatformExtension
//internal val KotlinTarget.implementationConfigurationName by configurationNameFromPlatformExtension
//internal val KotlinTarget.testImplementationConfigurationName by configurationNameFromPlatformExtension
//internal val KotlinTarget.runtimeConfigurationName by configurationNameFromPlatformExtension
//internal val KotlinTarget.runtimeOnlyConfigurationName by configurationNameFromPlatformExtension
//internal val KotlinTarget.testRuntimeConfigurationName by configurationNameFromPlatformExtension
//internal val KotlinTarget.testRuntimeOnlyConfigurationName by configurationNameFromPlatformExtension
//
//internal val KotlinTarget.classesTaskName by taskNameFromPlatformExtension
//internal val KotlinTarget.jarTaskName by taskNameFromPlatformExtension
//internal val KotlinTarget.processResourcesTaskName by taskNameFromPlatformExtension

private class NameFromKotlinTargetDelegate(val propertySuffix: String) {
    operator fun getValue(thisRef: KotlinTarget, property: KProperty<*>): String {
        require(property.name.endsWith(propertySuffix))
        val nameToDisambiguate = property.name.substringBeforeLast(propertySuffix)
            .also { require(it.isNotEmpty()) }
        return thisRef.disambiguateName(nameToDisambiguate)
    }
}