/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.currentBuildId
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.matchesModule
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.representsProject
import org.jetbrains.kotlin.gradle.plugin.sources.getVisibleSourceSetsFromAssociateCompilations
import org.jetbrains.kotlin.project.model.*

class ProjectStructureMetadataModuleBuilder {
    private val modulesCache = mutableMapOf<ModuleIdentifier, KotlinModule>()

    private fun buildModuleFromProjectStructureMetadata(
        moduleIdentifier: ModuleIdentifier,
        metadata: KotlinProjectStructureMetadata
    ): KotlinModule =
        BasicKotlinModule(moduleIdentifier).apply {
            metadata.sourceSetNamesByVariantName.keys.forEach { variantName ->
                fragments.add(BasicKotlinModuleVariant(this@apply, variantName))
            }
            metadata.sourceSetNamesByVariantName.forEach { (variantName, sourceSets) ->
                val variant = fragmentByName(variantName)
                sourceSets.forEach { sourceSetName ->
                    if (fragments.none { it.fragmentName == sourceSetName })
                        fragments.add(BasicKotlinModuleFragment(this@apply, sourceSetName))
                    val fragment = fragmentByName(sourceSetName)
                    variant.directRefinesDependencies.add(fragment)
                }
            }
            metadata.sourceSetsDependsOnRelation.forEach { (depending, dependencies) ->
                val dependingFragment = fragmentByName(depending)
                dependencies.forEach { dependency ->
                    if (fragments.none { it.fragmentName == dependency })
                        fragments.add(BasicKotlinModuleFragment(this@apply, dependency))
                    val fragment = fragmentByName(dependency)
                    dependingFragment.directRefinesDependencies.add(fragment)
                }
            }
        }

    fun getModule(moduleIdentifier: ModuleIdentifier, projectStructureMetadata: KotlinProjectStructureMetadata): KotlinModule {
        return modulesCache.getOrPut(moduleIdentifier) {
            buildModuleFromProjectStructureMetadata(
                moduleIdentifier,
                projectStructureMetadata
            )
        }
    }
}

@Suppress("unused")
class GradleProjectModuleBuilder(private val addInferredSourceSetVisibilityAsExplicit: Boolean) {
    fun buildModuleFromProject(project: Project): KotlinModule? {
        val extension = project.multiplatformExtensionOrNull
            ?: project.kotlinExtension
            ?: return null

        val targets = when (extension) {
            is KotlinMultiplatformExtension -> extension.targets.filter { it.name != "metadata" }
            is KotlinSingleTargetExtension -> listOf(extension.target)
            else -> return null
        }

        return BasicKotlinModule(LocalModuleIdentifier(project.currentBuildId().name, project.path)).apply {
            val variantToCompilation = mutableMapOf<KotlinModuleFragment, KotlinCompilation<*>>()

            targets.forEach { target ->
                val publishedVariantsByCompilation = (target as? AbstractKotlinTarget)?.kotlinComponents.orEmpty()
                    .flatMap { component -> (component as? KotlinVariant)?.usages.orEmpty() }
                    .groupBy { it.compilation }

                target.compilations.forEach { compilation ->
                    val names =
                        publishedVariantsByCompilation[compilation].orEmpty()
                            .filter { it.includeIntoProjectStructureMetadata }.map { it.name }

                    names.forEach { variantName ->
                        val variant = BasicKotlinModuleVariant(this@apply, variantName)
                        variantToCompilation[variant] = compilation
                        fragments.add(variant)

                        // TODO The attributes from the compile dependencies configuration might differ from exposed attributes
                        val compileDependenciesConfiguration =
                            project.configurations.getByName(compilation.compileDependencyConfigurationName)
                        compileDependenciesConfiguration.attributes.keySet().forEach { key ->
                            variant.variantAttributes[KotlinAttributeKey(key.name)] =
                                attributeString(compileDependenciesConfiguration.attributes, key)
                        }
                        variant.isExported = compilation.isMain()
                    }
                }
            }
            extension.sourceSets.forEach { sourceSet ->
                val existingVariant = fragments.filterIsInstance<BasicKotlinModuleVariant>().find { it.fragmentName == sourceSet.name }
                val fragment = existingVariant ?: BasicKotlinModuleFragment(this@apply, sourceSet.name).also { fragments.add(it) }
                fragment.kotlinSourceDirectories = sourceSet.kotlin.sourceDirectories.toList()

                // FIXME: Kotlin/Native implementation-effective-api dependencies are missing here. Introduce dependency scopes
                project.configurations.getByName(sourceSet.apiConfigurationName).allDependencies.forEach {
                    val moduleDependency = it.toModuleDependency(project)
                    fragment.declaredModuleDependencies.add(moduleDependency)
                }
            }

            // Once all fragments are created, add dependencies between them

            fragments.forEach { fragment ->
                val sourceSet = extension.sourceSets.findByName(fragment.fragmentName)
                    ?: variantToCompilation.getValue(fragment).defaultSourceSet
                sourceSet.dependsOn.forEach { dependency ->
                    val dependencyFragment = fragmentByName(dependency.name)
                    fragment.directRefinesDependencies.add(dependencyFragment)
                }
            }
            targets.forEach { target ->
                target.compilations.forEach { compilation ->
                    val fragment = fragmentByName(compilation.defaultSourceSetName)
                    compilation.associateWith.forEach { associate ->
                        val dependencyFragment = fragmentByName(associate.defaultSourceSetName)
                        fragment.declaredContainingModuleFragmentDependencies.add(dependencyFragment)
                    }
                }
            }
            if (addInferredSourceSetVisibilityAsExplicit) {
                extension.sourceSets.forEach { sourceSet ->
                    val fragment = fragmentByName(sourceSet.name)
                    getVisibleSourceSetsFromAssociateCompilations(project, sourceSet).forEach { dependency ->
                        val dependencyFragment = fragmentByName(dependency.name)
                        fragment.declaredContainingModuleFragmentDependencies.add(dependencyFragment)
                    }
                }
            }
        }
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
) = when (val dependency = this) {
    is ProjectDependency ->
        ModuleDependency(LocalModuleIdentifier(project.currentBuildId().name, dependency.dependencyProject.path))
    else ->
        ModuleDependency(MavenModuleIdentifier(dependency.group.orEmpty(), dependency.name))
}

private fun BasicKotlinModule.fragmentByName(name: String) =
    fragments.single { it.fragmentName == name }

class GradleModuleVariantResolver(val project: Project) : ModuleVariantResolver {
    private val resolvedVariantProvider = ResolvedMppVariantsProvider.get(project)

    override fun getChosenVariant(requestingVariant: KotlinModuleVariant, dependencyModule: KotlinModule): VariantResolution {
        // TODO maybe improve this behavior? Currently it contradicts dependency resolution in that it may return a chosen variant for an
        //  unrequested dependency. This workaround is needed for synthetic modules which were not produced from module metadata, so maybe
        //  those modules should be marked somehow
        dependencyModule.variants.singleOrNull()
            ?.let { return VariantResolution.fromMatchingVariants(requestingVariant, dependencyModule, listOf(it)) }

        val module = requestingVariant.containingModule

        // This implementation can only resolve variants for the current project's KotlinModule
        require(module.representsProject(project))

        val targets =
            project.multiplatformExtensionOrNull?.targets ?: listOf((project.kotlinExtension as KotlinSingleTargetExtension).target)

        val compilation =
            targets.filterIsInstance<AbstractKotlinTarget>()
                .flatMap { it.kotlinComponents.filterIsInstance<KotlinVariant>() }
                .flatMap { it.usages }
                .firstOrNull { it.name == requestingVariant.fragmentName }
                ?.compilation
                ?: return VariantResolution.NotRequested(requestingVariant, dependencyModule)

        val compileClasspath = project.configurations.getByName(compilation.compileDependencyConfigurationName)

        // TODO optimize O(n) search, store the mapping
        val component = compileClasspath.incoming.resolutionResult.allComponents.find { it.id.matchesModule(dependencyModule) }
            ?: return VariantResolution.NotRequested(requestingVariant, dependencyModule)

        val dependencyModuleId = ModuleIds.fromComponent(project, component)
        // FIXME check composite builds, it's likely that resolvedVariantProvider fails on them?
        val variantName = resolvedVariantProvider.getResolvedVariantName(dependencyModuleId, compileClasspath)

        val resultVariant = dependencyModule.variants.singleOrNull { it.fragmentName == variantName }

        return if (resultVariant == null)
            VariantResolution.NoVariantMatch(requestingVariant, dependencyModule)
        else
            VariantResolution.VariantMatch(requestingVariant, dependencyModule, resultVariant)
    }
}