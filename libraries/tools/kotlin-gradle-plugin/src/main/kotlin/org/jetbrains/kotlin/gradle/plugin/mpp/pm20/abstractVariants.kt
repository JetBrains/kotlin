/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationOutput
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultKotlinCompilationOutput
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.fromPlatform
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.project.model.KotlinAttributeKey
import org.jetbrains.kotlin.project.model.KotlinModuleVariant
import org.jetbrains.kotlin.project.model.KotlinPlatformTypeAttribute

abstract class KotlinGradleVariant(
    containingModule: KotlinGradleModule,
    override val fragmentName: String
) : KotlinGradleFragment(containingModule, fragmentName), KotlinModuleVariant {

    abstract val platformType: KotlinPlatformType

    override val variantAttributes: Map<KotlinAttributeKey, String>
        get() = mapOf(KotlinPlatformTypeAttribute to KotlinPlatformTypeAttribute.fromPlatform(platformType)) // TODO user attributes

    // TODO generalize with KotlinCompilation?
    val compileDependencyConfigurationName: String
        get() = disambiguateName("CompileDependencies")

    open lateinit var compileDependencyFiles: FileCollection

    // TODO rewrite using our own artifacts API?
    val compilationOutputs: KotlinCompilationOutput =
        DefaultKotlinCompilationOutput(
            project,
            project.provider { project.buildDir.resolve("processedResources/${containingModule.name}/${fragmentName}") }
        )

    // TODO rewrite using our own artifacts API
    open val sourceArchiveTask: TaskProvider<AbstractArchiveTask>
        get() = project.tasks.withType<AbstractArchiveTask>().named(defaultSourceArtifactTaskName)

    // TODO generalize exposing outputs: what if a variant has more than one such configurations or none?
    val apiElementsConfigurationName: String
        get() = disambiguateName("apiElements")
}

// TODO: rewrite with the artifacts API
internal val KotlinGradleVariant.defaultSourceArtifactTaskName: String
    get() = disambiguateName("sourcesJar")

abstract class KotlinGradleVariantWithRuntimeDependencies(
    containingModule: KotlinGradleModule,
    fragmentName: String
) : KotlinGradleVariant(containingModule, fragmentName) {
    // TODO deduplicate with KotlinCompilation?
    val runtimeDependencyConfigurationName: String
        get() = disambiguateName("RuntimeDependencies")

    open lateinit var runtimeDependencyFiles: FileCollection

    open val runtimeFiles: ConfigurableFileCollection by lazy {
        project.files(compilationOutputs.allOutputs, runtimeDependencyFiles)
    }

    // TODO generalize exposing outputs: what if a variant has more than one such configurations or none?
    val runtimeElementsConfigurationName: String
        get() = disambiguateName("runtimeElements")
}