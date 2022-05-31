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
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.mpp.publishedConfigurationName
import org.jetbrains.kotlin.gradle.plugin.sources.kpm.FragmentMappedKotlinSourceSet
import org.jetbrains.kotlin.gradle.utils.filesProvider
import org.jetbrains.kotlin.gradle.utils.withType
import org.jetbrains.kotlin.project.model.KotlinAttributeKey
import org.jetbrains.kotlin.project.model.KpmModuleDependency
import org.jetbrains.kotlin.tooling.core.MutableExtras
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf

internal open class GradleKpmLegacyMappedVariant(
    internal val compilation: KotlinCompilation<*>,
) : GradleKpmVariant {
    override fun toString(): String = "variant mapped to $compilation"

    private val fragmentForDefaultSourceSet =
        (compilation.defaultSourceSet as FragmentMappedKotlinSourceSet).underlyingFragment

    override val containingModule: GradleKpmModule get() = fragmentForDefaultSourceSet.containingModule

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
        get() {
            val allTargetComponents = (compilation.target as AbstractKotlinTarget).kotlinComponents
            val allTargetUsages = allTargetComponents.flatMap {
                when (it) {
                    is KotlinVariant -> it.usages
                    is JointAndroidKotlinTargetComponent -> it.usages
                    else -> emptyList()
                }
            }
            val compilationUsages = allTargetUsages.filterIsInstance<DefaultKotlinUsageContext>().filter { it.compilation == compilation }
            return compilationUsages.filter { it.includeIntoProjectStructureMetadata }
                .flatMap { listOf(it.dependencyConfigurationName, publishedConfigurationName(it.dependencyConfigurationName)) }
                .toSet()
        }

    override val kotlinSourceRoots: SourceDirectorySet
        get() = compilation.defaultSourceSet.kotlin

    override val languageSettings: LanguageSettingsBuilder
        get() = compilation.defaultSourceSet.languageSettings

    override val extras: MutableExtras = mutableExtrasOf()

    override fun refines(other: GradleKpmFragment) {
        fragmentForDefaultSourceSet.refines(other)
    }

    override fun refines(other: NamedDomainObjectProvider<GradleKpmFragment>) {
        fragmentForDefaultSourceSet.refines(other)
    }

    override fun dependencies(configure: KotlinDependencyHandler.() -> Unit) {
        fragmentForDefaultSourceSet.dependencies(configure)
    }

    //FIXME map to the original name, require that the fragment for source set does not exist yet?
    override val fragmentName: String
        get() = fragmentForDefaultSourceSet.fragmentName + "Variant"

    override val declaredRefinesDependencies: Iterable<GradleKpmFragment>
        get() = fragmentForDefaultSourceSet.declaredRefinesDependencies

    override val declaredModuleDependencies: Iterable<KpmModuleDependency>
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

internal class GradleKpmLegacyMappedVariantWithRuntime(private val compilationWithRuntime: KotlinCompilationToRunnableFiles<*>) :
    GradleKpmLegacyMappedVariant(compilationWithRuntime),
    GradleKpmVariantWithRuntime {

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

internal enum class PublicationRegistrationMode {
    IMMEDIATE, AFTER_EVALUATE
}

internal fun mapTargetCompilationsToKpmVariants(target: AbstractKotlinTarget, publicationRegistration: PublicationRegistrationMode) {
    target.compilations.all { compilation ->
        val variant = if (compilation is KotlinCompilationToRunnableFiles)
            GradleKpmLegacyMappedVariantWithRuntime(compilation)
        else GradleKpmLegacyMappedVariant(compilation)

        val defaultSourceSetFragment = (compilation.defaultSourceSet as FragmentMappedKotlinSourceSet).underlyingFragment
        variant.refines(defaultSourceSetFragment)

        val module = defaultSourceSetFragment.containingModule
        module.fragments.add(variant)
    }

    fun whenPublicationShouldRegister(action: () -> Unit) =
        when (publicationRegistration) {
            PublicationRegistrationMode.IMMEDIATE -> action()
            PublicationRegistrationMode.AFTER_EVALUATE -> target.project.whenEvaluated { action() }
        }

    whenPublicationShouldRegister {
        val mainModule = target.project.kpmModules.getByName(GradleKpmModule.MAIN_MODULE_NAME)
        target.kotlinComponents.forEach { kotlinComponent ->
            val moduleHolder = GradleKpmDefaultSingleMavenPublishedModuleHolder(
                mainModule,
                kotlinComponent.defaultArtifactId.removePrefix(target.project.name.toLowerCase() + "-")
            )
            val usages = when (kotlinComponent) { // unfortunately, there's no common supertype with `usages`
                is KotlinVariant -> kotlinComponent.usages
                is JointAndroidKotlinTargetComponent -> kotlinComponent.usages
                else -> error("unexpected type of kotlinComponent in legacy variant mapping: ${kotlinComponent.javaClass}")
            }

            moduleHolder.whenPublicationAssigned { publication ->
                // Include Sources
                kotlinComponent.sourcesArtifacts.forEach {
                    publication.artifact(it)
                }

                // Support the `mavenPublication { ... }` DSL in target:
                target.publicationConfigureActions.all { action ->
                    action.execute(publication)
                }
            }

            // FIXME: include additional variants into project structure metadata?

            val request = GradleKpmBasicPlatformPublicationToMavenRequest(
                kotlinComponent.name,
                mainModule,
                moduleHolder,
                usages.map { usage ->
                    val variant = mainModule.variants.withType<GradleKpmLegacyMappedVariant>().single {
                        it.compilation == usage.compilation
                    }
                    (usage as? DefaultKotlinUsageContext) ?: error("unexpected KotlinUsageContext type: ${usage.javaClass}")
                    KpmGradleAdvancedConfigurationPublicationRequest(
                        variant,
                        target.project.configurations.getByName(usage.dependencyConfigurationName),
                        usage.overrideConfigurationAttributes,
                        usage.overrideConfigurationArtifacts,
                        usage.includeIntoProjectStructureMetadata
                    )
                }
            )

            GradleKpmVariantPublishingConfigurator.get(target.project).configurePublishing(request)
        }
    }
}
