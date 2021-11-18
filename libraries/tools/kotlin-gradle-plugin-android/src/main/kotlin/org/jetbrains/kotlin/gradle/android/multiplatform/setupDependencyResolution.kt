/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android.multiplatform

import com.android.build.api.attributes.BuildTypeAttr
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.publishing.AndroidArtifacts
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Category
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.external.KotlinExternalTargetCompilationHandle
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation
import org.jetbrains.kotlin.gradle.utils.isGradleVersionAtLeast

fun setUpDependencyResolution(
    variant: BaseVariant, compilation: KotlinExternalTargetCompilationHandle
) {
    val project = compilation.project

    compilation.setCompileDependencyFilesConfiguration(variant.compileConfiguration.apply {
        attributes.attribute(KotlinPlatformType.attribute, compilation.target.platformType)
        attributes.attribute(AndroidArtifacts.ARTIFACT_TYPE, AndroidArtifacts.ArtifactType.PROCESSED_JAR.type)
        setJavaTargetEnvironmentAttributeIfSupported(project, "android")
    })

    compilation.setRuntimeDependencyFilesConfiguration(variant.runtimeConfiguration.apply {
        attributes.attribute(KotlinPlatformType.attribute, compilation.target.platformType)
        attributes.attribute(AndroidArtifacts.ARTIFACT_TYPE, AndroidArtifacts.ArtifactType.PROCESSED_JAR.type)
        setJavaTargetEnvironmentAttributeIfSupported(project, "android")
    })

    val buildTypeAttrValue = project.objects.named(BuildTypeAttr::class.java, variant.buildType.name)
    listOfNotNull(compilation.runtimeDependencyConfiguration, compilation.compileDependencyConfiguration).forEach {
        it.attributes.attribute(Attribute.of(BuildTypeAttr::class.java), buildTypeAttrValue)
    }

    // TODO this code depends on the convention that is present in the Android plugin as there's no public API
    // We should request such API in the Android plugin
    val apiElementsConfigurationName = "${variant.name}ApiElements"
    val runtimeElementsConfigurationName = "${variant.name}RuntimeElements"

    // KT-29476, the Android *Elements configurations need Kotlin MPP dependencies:
    if (project.configurations.findByName(apiElementsConfigurationName) != null) {
        project.addExtendsFromRelation(apiElementsConfigurationName, compilation.apiConfiguration.name)
    }
    if (project.configurations.findByName(runtimeElementsConfigurationName) != null) {
        project.addExtendsFromRelation(runtimeElementsConfigurationName, compilation.implementationConfiguration.name)
        project.addExtendsFromRelation(runtimeElementsConfigurationName, compilation.runtimeOnlyConfiguration.name)
    }

    listOf(apiElementsConfigurationName, runtimeElementsConfigurationName).forEach { outputConfigurationName ->
        project.configurations.findByName(outputConfigurationName)?.let { configuration ->
            configuration.attributes.attribute(KotlinPlatformType.attribute, compilation.target.platformType)
            configuration.setJavaTargetEnvironmentAttributeIfSupported(project, "android")
            configuration.attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        }
    }
}

internal fun Project.categoryByName(categoryName: String): Category =
    objects.named(Category::class.java, categoryName)

internal fun Configuration.setJavaTargetEnvironmentAttributeIfSupported(project: Project, value: String) {
    if (isGradleVersionAtLeast(7, 0)) {
        @Suppress("UNCHECKED_CAST")
        val attributeClass = Class.forName("org.gradle.api.attributes.java.TargetJvmEnvironment") as Class<out Named>

        @Suppress("UNCHECKED_CAST")
        val attributeKey = attributeClass.getField("TARGET_JVM_ENVIRONMENT_ATTRIBUTE").get(null) as Attribute<Named>

        val attributeValue = project.objects.named(attributeClass, value)
        attributes.attribute(attributeKey, attributeValue)
    }
}
