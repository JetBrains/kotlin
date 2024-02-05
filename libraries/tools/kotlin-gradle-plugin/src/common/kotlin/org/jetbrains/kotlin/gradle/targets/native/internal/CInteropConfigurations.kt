/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.*
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinNativeTargetConfigurator.NativeArtifactFormat
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.AfterFinaliseDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.internal.artifactTypeAttribute
import org.jetbrains.kotlin.gradle.plugin.launchInStage
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.usesPlatformOf
import org.jetbrains.kotlin.gradle.targets.KotlinTargetSideEffect
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropKlibLibraryElements.cinteropKlibLibraryElements
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.utils.createConsumable
import org.jetbrains.kotlin.gradle.utils.findConsumable

internal fun createCInteropApiElementsKlibArtifact(
    target: KotlinNativeTarget,
    settings: DefaultCInteropSettings,
    interopTask: TaskProvider<out CInteropProcess>,
) {
    val project = target.project
    val configurationName = cInteropApiElementsConfigurationName(target)
    val configuration = project.configurations.getByName(configurationName)
    project.artifacts.add(configuration.name, interopTask.flatMap { it.outputFileProvider }) { artifact ->
        artifact.extension = "klib"
        artifact.type = "klib"
        artifact.classifier = "cinterop-${settings.name}"
        artifact.builtBy(interopTask)
    }
}

internal fun Project.locateOrCreateCInteropDependencyConfiguration(
    compilation: KotlinNativeCompilation,
): Configuration {
    configurations.findResolvable(compilation.cInteropDependencyConfigurationName)?.let { return it }

    val compileOnlyConfiguration = configurations.getByName(compilation.compileOnlyConfigurationName)
    val implementationConfiguration = configurations.getByName(compilation.implementationConfigurationName)

    return configurations.createResolvable(compilation.cInteropDependencyConfigurationName).apply {
        extendsFrom(compileOnlyConfiguration, implementationConfiguration)
        isVisible = false

        /* Deferring attributes to wait for compilation.attributes to be configured  by user*/
        launchInStage(AfterFinaliseDsl) {
            usesPlatformOf(compilation.target)
            compilation.copyAttributesTo(project, dest = attributes)
            attributes.setAttribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, cinteropKlibLibraryElements())
            attributes.setAttribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_CINTEROP))
            attributes.setAttribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
            description = "CInterop dependencies for compilation '${compilation.name}')."
        }
    }
}

internal val KotlinNativeCompilation.cInteropDependencyConfigurationName: String
    get() = compilation.disambiguateName("CInterop")

internal val SetupCInteropApiElementsConfigurationSideEffect = KotlinTargetSideEffect<KotlinNativeTarget> { target ->
    target.project.locateOrCreateCInteropApiElementsConfiguration(target)
}

internal fun Project.locateOrCreateCInteropApiElementsConfiguration(target: KotlinTarget): Configuration {
    val configurationName = cInteropApiElementsConfigurationName(target)
    configurations.findConsumable(configurationName)?.let { return it }

    return configurations.createConsumable(configurationName).apply {
        /* Deferring attributes to wait for target.attributes to be configured by user */
        launchInStage(AfterFinaliseDsl) {
            usesPlatformOf(target)
            target.copyAttributesTo(project, dest = attributes)
            attributes.setAttribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, cinteropKlibLibraryElements())
            attributes.setAttribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_CINTEROP))
            attributes.setAttribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
            attributes.setAttribute(artifactTypeAttribute, NativeArtifactFormat.KLIB)

            /* Expose api dependencies */
            target.compilations.findByName(MAIN_COMPILATION_NAME)?.let { compilation ->
                extendsFrom(compilation.internal.configurations.apiConfiguration)
            }
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
