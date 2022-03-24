/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.GradleModuleVariantResolver
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.allDependencyModules
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.disambiguateName
import org.jetbrains.kotlin.gradle.utils.filesProvider
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.project.model.VariantResolution
import org.jetbrains.kotlin.project.model.withRefinesClosure

interface KotlinVariantCompilationDataInternal<T : KotlinCommonOptions> : KotlinVariantCompilationData<T> {
    override val compileKotlinTaskName: String
        get() = lowerCamelCaseName("compile", compilationPurpose.takeIf { it != "main" }, "Kotlin", compilationClassifier)

    override val compileAllTaskName: String
        get() = owner.disambiguateName("classes")

    override val kotlinSourceDirectoriesByFragmentName: Map<String, SourceDirectorySet>
        get() = owner.withRefinesClosure.filterIsInstance<KotlinGradleVariant>().associate { it.disambiguateName("") to it.kotlinSourceRoots }

    override val friendPaths: Iterable<FileCollection>
        get() {
            // TODO note for Android: see the friend artifacts code in KotlinAndroidCompilation; should we port it here?
            return listOf(
                project.filesProvider {
                    val friendVariants = resolveFriendVariants()
                    val friendModuleClassifiers = friendVariants.map { it.containingModule.moduleClassifier }.toSet()
                    owner.compileDependenciesConfiguration
                        .incoming.artifactView { view ->
                            view.componentFilter { id ->
                                // FIXME rewrite using the proper module resolution after those changes are merged
                                val asProject = id as? ProjectComponentIdentifier
                                asProject?.build?.isCurrentBuild == true &&
                                        asProject.projectPath == owner.project.path
                            }
                        }.artifacts.filter {
                            // FIXME rewrite using the proper module resolution after those changes are merged
                            moduleClassifiersFromCapabilities(it.variant.capabilities).any { it in friendModuleClassifiers }
                        }.map { it.file }
                }
            )
        }

    override val moduleName: String
        get() = // TODO accurate module names that don't rely on all variants having a main counterpart
            owner.containingModule.project.kpmModules
                .getByName(KotlinGradleModule.MAIN_MODULE_NAME).variants.findByName(owner.name)?.ownModuleName() ?: ownModuleName

    override val ownModuleName: String
        get() = owner.ownModuleName()

    private fun resolveFriendVariants(): Iterable<KotlinGradleVariant> {
        val moduleResolver = GradleModuleDependencyResolver.getForCurrentBuild(project)
        val variantResolver = GradleModuleVariantResolver.getForCurrentBuild(project)
        val dependencyGraphResolver = GradleKotlinDependencyGraphResolver(moduleResolver)

        val friendModules =
            ((dependencyGraphResolver.resolveDependencyGraph(owner.containingModule) as? GradleDependencyGraph)
                ?: error("Failed to resolve dependencies of ${owner.containingModule}"))
                .allDependencyModules
                .filterIsInstance<KotlinGradleModule>()
                .filter { dependencyModule ->
                    // the module comes from the same Gradle project // todo: extend to other friends once supported
                    dependencyModule.project == owner.containingModule.project
                }

        return friendModules
            .map { friendModule -> variantResolver.getChosenVariant(owner, friendModule) }
            // also, important to check that the owner variant really requests this module:
            .filterIsInstance<VariantResolution.VariantMatch>()
            .mapNotNull { variantMatch -> variantMatch.chosenVariant as? KotlinGradleVariant }
    }
}

fun KotlinCompilationData<*>.isMainCompilationData(): Boolean = when (this) {
    is KotlinCompilation<*> -> isMain()
    else -> compilationPurpose == KotlinGradleModule.MAIN_MODULE_NAME
}
