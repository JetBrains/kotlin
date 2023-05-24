/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.*
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinNativeTargetConfigurator.NativeArtifactFormat
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.AfterFinaliseDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.internal.artifactTypeAttribute
import org.jetbrains.kotlin.gradle.plugin.launchInStage
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.copyAttributes
import org.jetbrains.kotlin.gradle.plugin.usesPlatformOf
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropKlibLibraryElements.cinteropKlibLibraryElements
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess

internal fun createCInteropApiElementsKlibArtifact(
    target: KotlinNativeTarget,
    settings: DefaultCInteropSettings,
    interopTask: TaskProvider<out CInteropProcess>,
) {
    val project = target.project
    val configurationName = cInteropApiElementsConfigurationName(target)
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
): Configuration {
    configurations.findByName(compilation.cInteropDependencyConfigurationName)?.let { return it }

    val compileOnlyConfiguration = configurations.getByName(compilation.compileOnlyConfigurationName)
    val implementationConfiguration = configurations.getByName(compilation.implementationConfigurationName)

    return configurations.create(compilation.cInteropDependencyConfigurationName).apply {
        extendsFrom(compileOnlyConfiguration, implementationConfiguration)
        isVisible = false
        isCanBeResolved = true
        isCanBeConsumed = false

        /* Deferring attributes to wait for compilation.attributes to be configured  by user*/
        launchInStage(AfterFinaliseDsl) {
            usesPlatformOf(compilation.target)
            copyAttributes(compilation.attributes, attributes)
            attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, cinteropKlibLibraryElements())
            attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, KotlinUsages.KOTLIN_CINTEROP))
            attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
            description = "CInterop dependencies for compilation '${compilation.name}')."
        }
    }
}

internal val KotlinNativeCompilation.cInteropDependencyConfigurationName: String
    get() = compilation.disambiguateName("CInterop")

internal fun Project.locateOrCreateCInteropApiElementsConfiguration(target: KotlinTarget): Configuration {
    val configurationName = cInteropApiElementsConfigurationName(target)
    configurations.findByName(configurationName)?.let { return it }

    return configurations.create(configurationName).apply {
        isCanBeResolved = false
        isCanBeConsumed = true

        /* Deferring attributes to wait for target.attributes to be configured by user */
        launchInStage(AfterFinaliseDsl) {
            usesPlatformOf(target)
            copyAttributes(target.attributes, attributes)
            attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, cinteropKlibLibraryElements())
            attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, KotlinUsages.KOTLIN_CINTEROP))
            attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
            attributes.attribute(artifactTypeAttribute, NativeArtifactFormat.KLIB)
        }
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

