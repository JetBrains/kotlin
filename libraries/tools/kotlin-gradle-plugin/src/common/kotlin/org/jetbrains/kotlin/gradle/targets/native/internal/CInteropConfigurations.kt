/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.*
import org.gradle.api.internal.artifacts.ArtifactAttributes
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinNativeTargetConfigurator.NativeArtifactFormat
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultCInteropSettings
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropKlibLibraryElements.cinteropKlibLibraryElements
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess

internal fun createCInteropApiElementsKlibArtifact(
    settings: DefaultCInteropSettings,
    interopTask: TaskProvider<out CInteropProcess>
) {
    val project = settings.compilation.project
    val configurationName = cInteropApiElementsConfigurationName(settings.target ?: return)
    val configuration = project.configurations.getByName(configurationName)
    project.artifacts.add(configuration.name, interopTask.map { it.outputFile }) { artifact ->
        artifact.extension = "klib"
        artifact.type = "klib"
        artifact.classifier = "cinterop-${settings.name}"
        artifact.builtBy(interopTask)
    }
}

internal fun Project.locateOrCreateCInteropDependencyConfiguration(
    compilation: KotlinNativeCompilation,
    cinterop: CInteropSettings,
    target: KotlinTarget
): Configuration {
    val compileOnlyConfiguration = configurations.getByName(compilation.compileOnlyConfigurationName)
    val implementationConfiguration = configurations.getByName(compilation.implementationConfigurationName)

    return configurations.maybeCreate(cinterop.dependencyConfigurationName).apply {
        extendsFrom(compileOnlyConfiguration, implementationConfiguration)
        isVisible = false
        isCanBeResolved = true
        isCanBeConsumed = false

        usesPlatformOf(target)
        attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, cinteropKlibLibraryElements())
        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, KotlinUsages.KOTLIN_CINTEROP))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        description = "Dependencies for cinterop '${cinterop.name}' (compilation '${compilation.name}')."
    }
}

internal fun Project.locateOrCreateCInteropApiElementsConfiguration(target: KotlinTarget): Configuration {
    val configurationName = cInteropApiElementsConfigurationName(target)
    configurations.findByName(configurationName)?.let { return it }

    return configurations.create(configurationName).apply {
        isCanBeResolved = false
        isCanBeConsumed = true

        usesPlatformOf(target)
        attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, cinteropKlibLibraryElements())
        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, KotlinUsages.KOTLIN_CINTEROP))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        attributes.attribute(ArtifactAttributes.ARTIFACT_FORMAT, NativeArtifactFormat.KLIB)
    }
}

private fun cInteropApiElementsConfigurationName(target: KotlinTarget): String {
    return target.name + "CInteropApiElements"
}

internal object CInteropKlibLibraryElements {
    const val CINTEROP_KLIB = "cinterop-klib"

    fun Project.cinteropKlibLibraryElements(): LibraryElements = objects.named(LibraryElements::class.java, CINTEROP_KLIB)

    fun setupAttributesMatchingStrategy(schema: AttributesSchema) {
        schema.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE) { strategy ->
            strategy.compatibilityRules.add(CInteropLibraryElementsCompatibilityRule::class.java)
        }
    }
}

private class CInteropLibraryElementsCompatibilityRule : AttributeCompatibilityRule<LibraryElements> {
    override fun execute(details: CompatibilityCheckDetails<LibraryElements>) {
        if (details.consumerValue?.name == CInteropKlibLibraryElements.CINTEROP_KLIB) {
            if (details.producerValue?.name == LibraryElements.JAR || details.producerValue?.name == LibraryElements.CLASSES) {
                details.compatible()
            }
        }
    }
}

