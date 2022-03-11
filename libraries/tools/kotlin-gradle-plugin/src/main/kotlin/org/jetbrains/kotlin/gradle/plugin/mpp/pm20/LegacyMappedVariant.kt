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
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.FragmentNameDisambiguationOmittingMain
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.mpp.publishedConfigurationName
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultLanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.plugin.sources.kpm.FragmentMappedKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.kpm.SourceSetMappedFragmentLocator
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.utils.filesProvider
import org.jetbrains.kotlin.project.model.KotlinAttributeKey
import org.jetbrains.kotlin.project.model.KotlinModuleDependency

internal open class LegacyMappedVariant(
    override val fragmentName: String,
    override val containingModule: KotlinGradleModule,
    internal val compilation: KotlinCompilation<*>,
    dependencyConfigurations: KotlinFragmentDependencyConfigurations,
) : KotlinGradleVariant, KotlinFragmentDependencyConfigurations by dependencyConfigurations {

    override fun toString(): String = "variant mapped to $compilation"

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
            val compilationUsages = allTargetUsages.filterIsInstance<DefaultKotlinUsageContext>().filter {
                val usageCompilation = it.compilation
                val target = usageCompilation.target
                usageCompilation == compilation ||
                        (target as? KotlinJsIrTarget)?.legacyTarget?.compilations?.findByName(usageCompilation.name) == compilation
            }
            return compilationUsages.filter { it.includeIntoProjectStructureMetadata }
                .flatMap { listOf(it.dependencyConfigurationName, publishedConfigurationName(it.dependencyConfigurationName)) }
                .toSet()
        }

    override val kotlinSourceRoots: SourceDirectorySet by lazy {
        project.objects.sourceDirectorySet(
            "$fragmentName.kotlin", "Kotlin sources for fragment $fragmentName"
        )
    }

    override val languageSettings: LanguageSettingsBuilder by lazy { DefaultLanguageSettingsBuilder() }

    override fun refines(other: KotlinGradleFragment) {
        refinesContainer.refines(other)
    }

    override fun refines(other: NamedDomainObjectProvider<KotlinGradleFragment>) {
        refinesContainer.refines(other)
    }

    private val refinesContainer by lazy { RefinesContainer(this) }

    override fun dependencies(configure: KotlinDependencyHandler.() -> Unit) {
        compilation.dependencies(configure)
    }

    override val directRefinesDependencies: Iterable<KotlinGradleFragment>
        get() = refinesContainer.directRefinesDependencies

    override val declaredModuleDependencies: Iterable<KotlinModuleDependency>
        get() = FragmentDeclaredModuleDependenciesBuilder().buildDeclaredModuleDependencies(this)

    override val apiConfigurationName: String
        get() = compilation.apiConfigurationName
    override val implementationConfigurationName: String
        get() = compilation.implementationConfigurationName
    override val compileOnlyConfigurationName: String
        get() = compilation.compileOnlyConfigurationName
    override val runtimeOnlyConfigurationName: String
        get() = compilation.runtimeOnlyConfigurationName

    override val variantAttributes: Map<KotlinAttributeKey, String>
        // TODO handle user attributes
        get() = compilation.attributes.keySet().filter { it.name.startsWith("org.jetbrains.kotlin") }.associate {
            val value = compilation.attributes.getAttribute(it)
            KotlinAttributeKey(it.name) to ((value as? Named)?.name ?: value.toString())
        }
}

internal class LegacyMappedVariantWithRuntime(
    fragmentName: String,
    containingModule: KotlinGradleModule,
    dependencyConfigurations: KotlinFragmentDependencyConfigurations,
    private val compilationWithRuntime: KotlinCompilationToRunnableFiles<*>,
) :
    LegacyMappedVariant(fragmentName, containingModule, compilationWithRuntime, dependencyConfigurations),
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

internal enum class PublicationRegistrationMode {
    IMMEDIATE, WHEN_EVALUATED
}

internal fun mapTargetCompilationsToKpmVariants(target: AbstractKotlinTarget, publicationRegistration: PublicationRegistrationMode) {
    val project = target.project
    target.compilations.all { compilation ->
        val defaultSourceSetName = compilation.defaultSourceSetName
        val isSourceSetCreatedEarly = project.kotlinExtension.sourceSets.findByName(defaultSourceSetName) != null

        val variantLocation: SourceSetMappedFragmentLocator.FragmentLocation = if (isSourceSetCreatedEarly) {
            val underlyingFragment = (compilation.defaultSourceSet as FragmentMappedKotlinSourceSet).underlyingFragment
            SourceSetMappedFragmentLocator.FragmentLocation(
                underlyingFragment.containingModule.name,
                underlyingFragment.fragmentName + "Variant"
            )
        } else {
            val location = SourceSetMappedFragmentLocator.get(project).locateFragmentForSourceSet(defaultSourceSetName)
            checkNotNull(location) { "Unexpected failure to place the model entity (fragment) for $compilation" }
            location
        }
        val module = project.kpmModules.getByName(variantLocation.moduleName)

        val dependencyConfigurations = LegacyMappedVariantDependencyConfigurationsFactory(compilation)
            .create(module, FragmentNameDisambiguationOmittingMain(module, variantLocation.fragmentName))

        val variant = if (compilation is KotlinCompilationToRunnableFiles)
            LegacyMappedVariantWithRuntime(variantLocation.fragmentName, module, dependencyConfigurations, compilation)
        else LegacyMappedVariant(variantLocation.fragmentName, module, compilation, dependencyConfigurations)

        if (isSourceSetCreatedEarly) {
            val sourceSet = compilation.defaultSourceSet as FragmentMappedKotlinSourceSet
            val defaultSourceSetFragment = (sourceSet).underlyingFragment
            variant.refines(defaultSourceSetFragment)
            // TODO: deprecate and drop support for such cases or provide proper support by proper name derivation support in mapped variant
        } else {
            DefaultKotlinSourceDirectoriesConfigurator.configure(variant)
            val result = FragmentMappedKotlinSourceSet(defaultSourceSetName, variant)
            project.kotlinExtension.sourceSets.add(result)
        }

        DefaultKotlinSourceDirectoriesConfigurator.configure(variant)

        module.fragments.add(variant)
    }

    fun whenPublicationShouldRegister(action: () -> Unit) =
        when (publicationRegistration) {
            PublicationRegistrationMode.IMMEDIATE -> action()
            PublicationRegistrationMode.WHEN_EVALUATED -> target.project.whenEvaluated { action() }
        }

    whenPublicationShouldRegister {
        // The variants of JS IR targets in the "both" mode get propagated to the legacy target, registering them would lead to duplication errors
        if (target is KotlinJsIrTarget && target.legacyTarget != null)
            return@whenPublicationShouldRegister

        val mainModule = project.kpmModules.getByName(KotlinGradleModule.MAIN_MODULE_NAME)
        target.kotlinComponents.forEach { kotlinComponent ->
            publishKotlinComponent(mainModule, kotlinComponent)
        }
    }
}

private fun publishKotlinComponent(
    mainModule: KotlinGradleModule,
    kotlinComponent: KotlinTargetComponent,
) {
    val target = kotlinComponent.target as AbstractKotlinTarget
    val project = target.project

    val moduleHolder = DefaultSingleMavenPublishedModuleHolder(
        mainModule,
        kotlinComponent.defaultArtifactId.removePrefix(project.name.toLowerCase() + "-")
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

    val request = BasicPlatformPublicationToMavenRequest(
        kotlinComponent.name,
        mainModule,
        moduleHolder,
        usages.map { usage ->
            val publishCompilation = usage.compilation
            val variant = mainModule.variants.withType<LegacyMappedVariant>().run {
                singleOrNull { variant ->
                    variant.compilation == publishCompilation
                } ?: singleOrNull { variant ->
                    variant.compilation == (publishCompilation.target as? KotlinJsIrTarget)?.legacyTarget?.compilations?.findByName(
                        publishCompilation.name
                    )
                } ?: error("Couldn't find the variant to represent $publishCompilation for publishing")
            }
            (usage as? DefaultKotlinUsageContext) ?: error("unexpected KotlinUsageContext type: ${usage.javaClass}")
            AdvancedVariantPublicationRequest(
                variant,
                project.configurations.getByName(usage.dependencyConfigurationName),
                usage.overrideConfigurationAttributes,
                usage.overrideConfigurationArtifacts,
                usage.includeIntoProjectStructureMetadata
            )
        }
    )

    VariantPublishingConfigurator.get(project).configurePublishing(request)
}