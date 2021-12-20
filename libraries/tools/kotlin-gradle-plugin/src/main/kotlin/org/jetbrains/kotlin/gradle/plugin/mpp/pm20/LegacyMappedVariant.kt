/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.mpp.publishedConfigurationName
import org.jetbrains.kotlin.gradle.plugin.sources.kpm.FragmentMappedKotlinSourceSet
import org.jetbrains.kotlin.gradle.utils.filesProvider
import org.jetbrains.kotlin.project.model.KotlinAttributeKey
import org.jetbrains.kotlin.project.model.KotlinModuleDependency
import org.jetbrains.kotlin.project.model.KotlinModuleFragment

internal open class LegacyMappedVariant(
    private val compilation: KotlinCompilation<*>,
) : KotlinGradleVariant {
    private val fragmentForDefaultSourceSet =
        (compilation.defaultSourceSet as FragmentMappedKotlinSourceSet).underlyingFragment

    override val containingModule: KotlinGradleModule get() = fragmentForDefaultSourceSet.containingModule

    override val platformType: KotlinPlatformType
        get() = compilation.platformType
    override val compileDependenciesConfiguration: Configuration
        get() = project.configurations.getByName(compilation.compileDependencyConfigurationName)
    override var compileDependencyFiles: FileCollection
        get() = compilation.compileDependencyFiles
        set(value) {
            compilation.compileDependencyFiles = value
        }
    override val compilationOutputs: KotlinCompilationOutput
        get() = compilation.output
    override val sourceArchiveTaskName: String
        get() = defaultSourceArtifactTaskName // TODO: no such task yet

    override val apiElementsConfiguration: Configuration
        get() = if (compilation.isMain())
            project.configurations.getByName(compilation.target.apiElementsConfigurationName) else
            project.configurations.maybeCreate(disambiguateName("apiElements")).apply {
                isCanBeConsumed = false
                isCanBeResolved = false
            }

    override val gradleVariantNames: Set<String>
        get() = if (compilation.isMain()) {
            val apiElements = compilation.target.apiElementsConfigurationName
            setOf(apiElements, publishedConfigurationName(apiElements))
        } else emptySet()

    override val kotlinSourceRoots: SourceDirectorySet
        get() = compilation.defaultSourceSet.kotlin

    override val languageSettings: LanguageSettingsBuilder
        get() = compilation.defaultSourceSet.languageSettings

    override fun refines(other: KotlinGradleFragment) {
        fragmentForDefaultSourceSet.refines(other)
    }

    override fun refines(other: NamedDomainObjectProvider<KotlinGradleFragment>) {
        fragmentForDefaultSourceSet.refines(other)
    }

    override fun dependencies(configure: KotlinDependencyHandler.() -> Unit) {
        fragmentForDefaultSourceSet.dependencies(configure)
    }

    //FIXME map to the original name, require that the fragment for source set does not exist yet?
    override val fragmentName: String
        get() = fragmentForDefaultSourceSet.fragmentName + "Variant"

    override val directRefinesDependencies: Iterable<KotlinModuleFragment>
        get() = fragmentForDefaultSourceSet.directRefinesDependencies

    override val declaredModuleDependencies: Iterable<KotlinModuleDependency>
        get() = fragmentForDefaultSourceSet.declaredModuleDependencies

    /** This configuration includes the dependencies from the refines-parents */
    override val transitiveApiConfiguration: Configuration
        get() = TODO("Not yet implemented")

    /** This configuration includes the dependencies from the refines-parents */
    override val transitiveImplementationConfiguration: Configuration
        get() = TODO("Not yet implemented")

    override val transitiveRuntimeOnlyConfiguration: Configuration
        get() = TODO("Not yet implemented")

    override val apiConfiguration: Configuration
        get() = project.configurations.getByName(apiConfigurationName)
    override val implementationConfiguration: Configuration
        get() = project.configurations.getByName(implementationConfigurationName)
    override val compileOnlyConfiguration: Configuration
        get() = project.configurations.getByName(compileOnlyConfigurationName)
    override val runtimeOnlyConfiguration: Configuration
        get() = project.configurations.getByName(runtimeOnlyConfigurationName)

    override val apiConfigurationName: String
        get() = fragmentForDefaultSourceSet.apiConfigurationName
    override val implementationConfigurationName: String
        get() = fragmentForDefaultSourceSet.implementationConfigurationName
    override val compileOnlyConfigurationName: String
        get() = fragmentForDefaultSourceSet.compileOnlyConfigurationName
    override val runtimeOnlyConfigurationName: String
        get() = fragmentForDefaultSourceSet.runtimeOnlyConfigurationName

    override val variantAttributes: Map<KotlinAttributeKey, String>
        // TODO handle user attributes
        get() = compilation.attributes.keySet().filter { it.name.startsWith("org.jetbrains.kotlin") }.associate {
            val value = compilation.attributes.getAttribute(it)
            KotlinAttributeKey(it.name) to ((value as? Named)?.name ?: value.toString())
        }
}

internal class LegacyMappedVariantWithRuntime(private val compilationWithRuntime: KotlinCompilationToRunnableFiles<*>) :
    LegacyMappedVariant(compilationWithRuntime),
    KotlinGradleVariantWithRuntime {

    override val runtimeDependenciesConfiguration: Configuration
        get() = project.configurations.getByName(compilationWithRuntime.runtimeDependencyConfigurationName)

    override var runtimeDependencyFiles: FileCollection
        get() = compilationWithRuntime.runtimeDependencyFiles
        set(value) {
            compilationWithRuntime.runtimeDependencyFiles = value
        }

    override val runtimeFiles: ConfigurableFileCollection
        get() = project.filesProvider { listOf(runtimeFiles, compilationWithRuntime.output.allOutputs) }

    override val runtimeElementsConfiguration: Configuration
        get() = if (compilationWithRuntime.isMain())
            project.configurations.getByName(compilationWithRuntime.target.runtimeElementsConfigurationName)
        else project.configurations.maybeCreate(disambiguateName("runtimeElements"))
}

internal fun mapTargetCompilationsToKpmVariants(target: KotlinTarget) {
    target.compilations.all { compilation ->
        val variant = if (compilation is KotlinCompilationToRunnableFiles)
            LegacyMappedVariantWithRuntime(compilation)
        else LegacyMappedVariant(compilation)

        val defaultSourceSetFragment = (compilation.defaultSourceSet as FragmentMappedKotlinSourceSet).underlyingFragment
        variant.refines(defaultSourceSetFragment)

        val module = defaultSourceSetFragment.containingModule
        module.fragments.add(variant)

        if (compilation.isMain()) {
            val moduleHolder = DefaultSingleMavenPublishedModuleHolder(module, target.name)
            VariantPublishingConfigurator.get(target.project).configurePublishing(
                variant,
                moduleHolder,
                listOfNotNull(
                    // FIXME: use the target's kotlinComponents to build the configurations
                    variant.apiElementsConfiguration.name,
                    if (variant is KotlinGradleVariantWithRuntime) variant.runtimeElementsConfiguration.name else null
                )
            )
        }
    }
}