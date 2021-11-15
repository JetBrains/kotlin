/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import com.android.build.api.attributes.BuildTypeAttr
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Category
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.usesPlatformOf
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmExternalCompilation
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation

fun setUpDependencyResolution(variant: BaseVariant, compilation: KotlinJvmExternalCompilation) {
    val project = compilation.target.project

    compilation.compileDependencyFiles = variant.compileConfiguration.apply {
        usesPlatformOf(compilation.target)
        attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm) // TODO NOW; Remove
        project.addExtendsFromRelation(name, compilation.compileDependencyConfigurationName)
    }

    compilation.runtimeDependencyFiles = variant.runtimeConfiguration.apply {
        usesPlatformOf(compilation.target)
        attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm) // TODO NOW; Remove
        project.addExtendsFromRelation(name, compilation.runtimeDependencyConfigurationName)
    }

    val buildTypeAttrValue = project.objects.named(BuildTypeAttr::class.java, variant.buildType.name)
    listOf(compilation.compileDependencyConfigurationName, compilation.runtimeDependencyConfigurationName).forEach {
        project.configurations.findByName(it)?.attributes?.attribute(Attribute.of(BuildTypeAttr::class.java), buildTypeAttrValue)
    }

    // TODO this code depends on the convention that is present in the Android plugin as there's no public API
    // We should request such API in the Android plugin
    val apiElementsConfigurationName = "${variant.name}ApiElements"
    val runtimeElementsConfigurationName = "${variant.name}RuntimeElements"

    // KT-29476, the Android *Elements configurations need Kotlin MPP dependencies:
    if (project.configurations.findByName(apiElementsConfigurationName) != null) {
        project.addExtendsFromRelation(apiElementsConfigurationName, compilation.apiConfigurationName)
    }
    if (project.configurations.findByName(runtimeElementsConfigurationName) != null) {
        project.addExtendsFromRelation(runtimeElementsConfigurationName, compilation.implementationConfigurationName)
        project.addExtendsFromRelation(runtimeElementsConfigurationName, compilation.runtimeOnlyConfigurationName)
    }

    listOf(apiElementsConfigurationName, runtimeElementsConfigurationName).forEach { outputConfigurationName ->
        project.configurations.findByName(outputConfigurationName)?.let { configuration ->
            configuration.usesPlatformOf(compilation.target)
            configuration.attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        }
    }
}

internal fun Project.categoryByName(categoryName: String): Category =
    objects.named(Category::class.java, categoryName)
