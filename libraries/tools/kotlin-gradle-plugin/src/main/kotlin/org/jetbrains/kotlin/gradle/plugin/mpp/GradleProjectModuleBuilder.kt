/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultLanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope
import org.jetbrains.kotlin.gradle.plugin.sources.getVisibleSourceSetsFromAssociateCompilations
import org.jetbrains.kotlin.gradle.utils.getOrPutRootProjectProperty
import org.jetbrains.kotlin.project.model.*

class ProjectStructureMetadataModuleBuilder {
    private val modulesCache = mutableMapOf<KotlinModuleIdentifier, KotlinModule>()

    private fun buildModuleFromProjectStructureMetadata(
        component: ResolvedComponentResult,
        metadata: KotlinProjectStructureMetadata
    ): KotlinModule {
        val moduleData = BasicKotlinModule(component.toSingleModuleIdentifier()).apply {
            metadata.sourceSetNamesByVariantName.keys.forEach { variantName ->
                fragments.add(BasicKotlinModuleVariant(this@apply, variantName))
            }
            fun fragment(sourceSetName: String): BasicKotlinModuleFragment {
                if (fragments.none { it.fragmentName == sourceSetName })
                    fragments.add(BasicKotlinModuleFragment(this@apply, sourceSetName))
                return fragmentByName(sourceSetName)
            }
            metadata.sourceSetNamesByVariantName.forEach { (variantName, sourceSets) ->
                val variant = fragmentByName(variantName)
                sourceSets.forEach { sourceSetName ->
                    variant.directRefinesDependencies.add(fragment(sourceSetName))
                }
            }
            metadata.sourceSetModuleDependencies.forEach { (sourceSetName, dependencies) ->
                val fragment = fragment(sourceSetName)
                dependencies.forEach { dependency ->
                    fragment.declaredModuleDependencies.add(
                        KotlinModuleDependency(
                            MavenModuleIdentifier(
                                dependency.groupId.orEmpty(),
                                dependency.moduleId,
                                null /* TODO */
                            )
                        )
                    )
                }
            }

            metadata.sourceSetsDependsOnRelation.forEach { (depending, dependencies) ->
                val dependingFragment = fragment(depending)
                dependencies.forEach { dependency ->
                    dependingFragment.directRefinesDependencies.add(fragment(dependency))
                }
            }
        }
        return ExternalImportedKotlinModule(
            moduleData,
            metadata,
            moduleData.fragments.filterTo(mutableSetOf()) { it.fragmentName in metadata.hostSpecificSourceSets }
        )
    }

    fun getModule(component: ResolvedComponentResult, projectStructureMetadata: KotlinProjectStructureMetadata): KotlinModule {
        val moduleId = component.toSingleModuleIdentifier()
        return modulesCache.getOrPut(moduleId) {
            buildModuleFromProjectStructureMetadata(
                component,
                projectStructureMetadata
            )
        }
    }
}

private fun detectModules(targets: Iterable<KotlinTarget>, sourceSets: Iterable<KotlinSourceSet>): Map<String, List<KotlinCompilation<*>>> {
    // DSU-like approach: all compilations and source sets that are reachable via dependsOn edges are considered a single module

    val compilations = targets.flatMap { it.compilations }

    val dsu = mutableMapOf<Any, Any>().apply {
        compilations.forEach { put(it, it) }
        sourceSets.forEach { put(it, it) }
    }

    fun get(item: Any): Any =
        dsu.getValue(item).let { leader -> if (leader === item) leader else get(leader).also { dsu[item] = it } }

    fun union(item: Any, other: Any) = dsu.put(get(item), get(other))

    sourceSets.forEach { sourceSet ->
        sourceSet.dependsOn.forEach { other -> union(sourceSet, other) }
    }
    compilations.forEach { compilation ->
        compilation.kotlinSourceSets.forEach { union(compilation, it) }
    }
    val uniqueCompilationNamesCounter = mutableMapOf<Set<String>, Int>()

    fun moduleName(compilations: Iterable<KotlinCompilation<*>>): String {
        val names = compilations.map { it.name }.toSortedSet()
        val uniqueNumber = uniqueCompilationNamesCounter.put(names, uniqueCompilationNamesCounter[names]?.plus(1) ?: 0)
        return names.joinToString("-") + (uniqueNumber?.let { "-$it" } ?: "")
    }

    return compilations.groupBy { get(it) }.values.associateBy { moduleName(it) }
}

@Suppress("unused")
class GradleProjectModuleBuilder(private val addInferredSourceSetVisibilityAsExplicit: Boolean) {
    private fun getModulesFromPm20Project(project: Project) = project.pm20Extension.modules.toList()

    fun buildModulesFromProject(project: Project): List<KotlinModule> {
        if (project.topLevelExtension is KotlinPm20ProjectExtension)
            return getModulesFromPm20Project(project)

        val extension = project.multiplatformExtensionOrNull
            ?: project.kotlinExtension
            ?: return emptyList()

        val targets = when (extension) {
            is KotlinMultiplatformExtension -> extension.targets.filter { it.name != KotlinMultiplatformPlugin.METADATA_TARGET_NAME }
            is KotlinSingleTargetExtension -> listOf(extension.target)
            else -> return emptyList()
        }

        val moduleCompilationCluster = detectModules(targets, extension.sourceSets)

        val publishedVariantsByCompilation = targets.flatMap { target ->
            (target as? AbstractKotlinTarget)?.kotlinComponents.orEmpty()
                .flatMap { component -> (component as? KotlinVariant)?.usages.orEmpty() }
        }.groupBy { it.compilation }

        val moduleByFragment = mutableMapOf<KotlinModuleFragment, KotlinModule>()

        val result = moduleCompilationCluster.entries.map { (classifier, compilationsToInclude) ->
            val sourceSetsToInclude = compilationsToInclude.flatMapTo(mutableSetOf()) { it.allKotlinSourceSets }

            val moduleIdentifier = LocalModuleIdentifier(
                project.currentBuildId().name,
                project.path,
                classifier.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME }
            )

            BasicKotlinModule(moduleIdentifier).apply {
                val variantToCompilation = mutableMapOf<BasicKotlinModuleFragment, KotlinCompilation<*>>()

                compilationsToInclude.forEach { compilation ->
                    // A compilation may be exposed as more than one variant, so we collect all of its names
                    val variantNames =
                        publishedVariantsByCompilation[compilation]?.filter { it.includeIntoProjectStructureMetadata }?.map { it.name }
                            ?: listOf(compilation.defaultSourceSetName)

                    variantNames.forEach { variantName ->
                        val variant = BasicKotlinModuleVariant(this@apply, variantName, DefaultLanguageSettingsBuilder(project))
                        moduleByFragment[variant] = this@apply
                        variantToCompilation[variant] = compilation
                        fragments.add(variant)

                        // TODO The attributes from the compile dependencies configuration might differ from exposed attributes
                        val compileDependenciesConfiguration =
                            project.configurations.getByName(compilation.compileDependencyConfigurationName)
                        compileDependenciesConfiguration.attributes.keySet().forEach { key ->
                            variant.variantAttributes[KotlinAttributeKey(key.name)] =
                                attributeString(compileDependenciesConfiguration.attributes, key)
                        }
                    }
                }
                // Once all fragments are created, add dependencies between them
                sourceSetsToInclude.forEach { sourceSet ->
                    val existingVariant = fragments.filterIsInstance<BasicKotlinModuleVariant>().find { it.fragmentName == sourceSet.name }
                    val fragment = existingVariant ?: BasicKotlinModuleFragment(this@apply, sourceSet.name, sourceSet.languageSettings).also { fragments.add(it) }
                    moduleByFragment[fragment] = this@apply
                    fragment.kotlinSourceRoots = sourceSet.kotlin.sourceDirectories.toList()

                    // FIXME: Kotlin/Native implementation-effective-api dependencies are missing here. Introduce dependency scopes
                    requestedDependencies(project, sourceSet, listOf(KotlinDependencyScope.API_SCOPE)).forEach {
                        val moduleDependency = it.toModuleDependency(project)
                        fragment.declaredModuleDependencies.add(moduleDependency)
                    }
                }
                fragments.forEach { fragment ->
                    val sourceSet = extension.sourceSets.findByName(fragment.fragmentName)
                        ?: variantToCompilation.getValue(fragment).defaultSourceSet
                    sourceSet.dependsOn.forEach { dependency ->
                        val dependencyFragment = fragmentByName(dependency.name)
                        fragment.directRefinesDependencies.add(dependencyFragment)
                    }
                }
            }
        }

        fun fragmentByName(name: String) =
            result.asSequence().flatMap { it.fragments.asSequence() }.first { it.fragmentName == name }

        targets.flatMap { it.compilations }.forEach { compilation ->
            val variant = fragmentByName(compilation.defaultSourceSetName)
            compilation.associateWith.forEach { associate ->
                val associateVariant = fragmentByName(associate.defaultSourceSetName)
                variant.declaredModuleDependencies.add(KotlinModuleDependency(associateVariant.containingModule.moduleIdentifier))
            }
        }

        if (addInferredSourceSetVisibilityAsExplicit) {
            project.kotlinExtension.sourceSets.forEach { sourceSet ->
                val fragment = fragmentByName(sourceSet.name)
                getVisibleSourceSetsFromAssociateCompilations(project, sourceSet).forEach { dependency ->
                    val dependencyFragment = fragmentByName(dependency.name)
                    fragment.declaredModuleDependencies.add(KotlinModuleDependency(dependencyFragment.containingModule.moduleIdentifier))
                }
            }
        }

        return result
    }

    private fun <T : Any> attributeString(container: AttributeContainer, attributeKey: Attribute<T>): String {
        val value = container.getAttribute(attributeKey)
        return when (value) {
            is Named -> value.name
            else -> value.toString()
        }
    }
}

internal fun Dependency.toModuleDependency(
    project: Project
): KotlinModuleDependency {
    return KotlinModuleDependency(
        when (this) {
            is ProjectDependency ->
                LocalModuleIdentifier(
                    project.currentBuildId().name,
                    dependencyProject.path,
                    moduleClassifiersFromCapabilities(requestedCapabilities).single() // FIXME multiple capabilities
                )
            is ModuleDependency ->
                MavenModuleIdentifier(
                    group.orEmpty(),
                    name,
                    moduleClassifiersFromCapabilities(requestedCapabilities).single() // FIXME multiple capabilities
                )
            else -> MavenModuleIdentifier(group.orEmpty(), name, null)
        }
    )
}

private fun BasicKotlinModule.fragmentByName(name: String) =
    fragments.single { it.fragmentName == name }

class CachingModuleVariantResolver(private val actualResolver: ModuleVariantResolver) : ModuleVariantResolver {
    private val resultCacheByRequestingVariant: MutableMap<KotlinModuleVariant, MutableMap<KotlinModule, VariantResolution>> = mutableMapOf()

    override fun getChosenVariant(requestingVariant: KotlinModuleVariant, dependencyModule: KotlinModule): VariantResolution {
        val resultCache = resultCacheByRequestingVariant.getOrPut(requestingVariant) { mutableMapOf() }
        return resultCache.getOrPut(dependencyModule) { actualResolver.getChosenVariant(requestingVariant, dependencyModule) }
    }
}

class GradleModuleVariantResolver : ModuleVariantResolver {
    override fun getChosenVariant(requestingVariant: KotlinModuleVariant, dependencyModule: KotlinModule): VariantResolution {
        // TODO maybe improve this behavior? Currently it contradicts dependency resolution in that it may return a chosen variant for an
        //  unrequested dependency. This workaround is needed for synthetic modules which were not produced from module metadata, so maybe
        //  those modules should be marked somehow
        if (dependencyModule is ExternalPlainKotlinModule) {
            return VariantResolution.fromMatchingVariants(
                requestingVariant,
                dependencyModule,
                listOf(dependencyModule.singleVariant)
            )
        }

        if (requestingVariant !is KotlinGradleVariant) {
            return VariantResolution.Unknown(requestingVariant, dependencyModule)
        }

        val module = requestingVariant.containingModule
        val project = module.project
        val resolvedVariantProvider = ResolvedMppVariantsProvider.get(project)

        // This implementation can only resolve variants for the current project's KotlinModule
        require(module.representsProject(project))

        val compileClasspath = getCompileDependenciesConfigurationForVariant(project, requestingVariant)

        val dependencyModuleId = dependencyModule.moduleIdentifier
        // FIXME check composite builds, it's likely that resolvedVariantProvider fails on them?
        val resolvedGradleVariantName = resolvedVariantProvider.getResolvedVariantName(dependencyModuleId, compileClasspath)
            ?: return VariantResolution.Unknown(requestingVariant, dependencyModule) // TODO NOW!

        val kotlinVariantName = when (dependencyModule) {
            is KotlinGradleModule -> dependencyModule.variants.single { resolvedGradleVariantName in it.gradleVariantNames }.name
            else -> resolvedGradleVariantName?.let(::kotlinVariantNameFromPublishedVariantName)
        }

        val resultVariant = dependencyModule.variants.singleOrNull { it.fragmentName == kotlinVariantName }

        return if (resultVariant == null)
            VariantResolution.NoVariantMatch(requestingVariant, dependencyModule)
        else
            VariantResolution.VariantMatch(requestingVariant, dependencyModule, resultVariant)
    }

    private fun getCompileDependenciesConfigurationForVariant(project: Project, requestingVariant: KotlinModuleVariant): Configuration =
        when (project.topLevelExtension) {
            is KotlinMultiplatformExtension, is KotlinSingleTargetExtension -> {
                val targets =
                    project.multiplatformExtensionOrNull?.targets ?: listOf((project.kotlinExtension as KotlinSingleTargetExtension).target)

                val compilation =
                    targets.filterIsInstance<AbstractKotlinTarget>()
                        .flatMap { it.kotlinComponents.filterIsInstance<KotlinVariant>() }
                        .flatMap { it.usages }
                        .firstOrNull { it.name == requestingVariant.fragmentName }
                        ?.compilation

                        ?: targets.asSequence().flatMap { it.compilations.asSequence() }.single {
                            it.defaultSourceSetName == requestingVariant.fragmentName
                        } // TODO: generalize the mapping PM2.0 <-> MPP

                        ?: error("could not find a compilation that produces the variant $requestingVariant in $project")

                project.configurations.getByName(compilation.compileDependencyConfigurationName)
            }
            is KotlinPm20ProjectExtension -> {
                (requestingVariant as KotlinGradleVariant).compileDependencyConfiguration
            }
            else -> error("could not find the compile dependencies configuration for variant $requestingVariant")
        }

    companion object {
        fun getForCurrentBuild(project: Project): ModuleVariantResolver {
            val extraPropertyName = "org.jetbrains.kotlin.dependencyResolution.variantResolver.${project.getKotlinPluginVersion()}"
            return project.getOrPutRootProjectProperty(extraPropertyName) {
                CachingModuleVariantResolver(GradleModuleVariantResolver())
            }
        }
    }
}
