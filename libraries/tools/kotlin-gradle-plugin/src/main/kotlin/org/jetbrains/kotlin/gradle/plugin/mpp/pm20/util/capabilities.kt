/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util

import org.gradle.api.Project
import org.gradle.api.capabilities.Capability
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KOTLIN_AUXILIARY_MODULE_CAPABILITY_INFIX_PART
import org.jetbrains.kotlin.gradle.utils.getValue

internal class ComputedCapability(
    val groupProvider: Provider<String>,
    val nameProvider: Provider<String>,
    val versionProvider: Provider<String>,
    val kotlinModuleClassifier: String
) : Capability {
    override fun getGroup(): String = groupProvider.get()

    override fun getName(): String =
        nameProvider.get() +
                kotlinModuleClassifier.let { "$KOTLIN_AUXILIARY_MODULE_CAPABILITY_INFIX_PART$it" }

    override fun getVersion(): String? = versionProvider.get()

    companion object {
        /**
         * Capability that should be used to point to an auxiliary [module] using a `project("...")` dependency. Unlike published modules,
         * this capability is always used for dependencies on auxiliary modules, even if they are published as [Standalone] Maven modules.
         * However, as `main` modules use the "default", implicit capability, depending on or exposing a `main` module should not involve
         * the capability, so this function returns `null` for `main` modules.
         */
        fun forProjectDependenciesOnModule(module: KotlinGradleModule): ComputedCapability? {
            if (module.isMain)
                return null

            val project = module.project
            return ComputedCapability(
                project.provider { project.group.toString() },
                project.provider { project.name },
                project.provider { project.version.toString() },
                // With project-to-project dependencies, we always use the module classifier capability, even with standalone-published modules
                checkNotNull(module.moduleClassifier) // because this is not a main module
            )
        }

        /**
         * Capability that should be used to depend on a published [module]. If a module is published as a [Standalone] Maven module, no
         * capability is necessary to depend on it, so this function returns `null`. A `main` module does not need any explicit capability
         * to depend on it, so this function returns `null` for `main` modules.
         *
         * If the [module] is not yet set up for publishing with [KotlinGradleModule.makePublic], then this function can't decide
         * whether a capability is needed, so it throws an [IllegalStateException].
         */
        fun forPublishedModule(module: KotlinGradleModule): ComputedCapability? {
            check(module.isPublic)

            if (module.isMain || module.publicationMode is Standalone)
                return null

            val project = module.project
            val publicationHolder by project.provider {
                module.publicationHolder()
            }

            return ComputedCapability(
                project.provider { publicationHolder?.publishedMavenModuleCoordinates?.group ?: project.group.toString() },
                project.provider { publicationHolder?.publishedMavenModuleCoordinates?.name ?: project.name },
                project.provider { publicationHolder?.publishedMavenModuleCoordinates?.version ?: project.version.toString() },
                checkNotNull(module.moduleClassifier) // because this is not a main module
            )
        }

        /**
         * Capability for a known dependency on a Maven module hosting an [Embedded] auxiliary module.
         */
        fun forAuxiliaryModuleByCoordinatesAndName(
            project: Project,
            moduleIdentifier: VersionedMavenModuleIdentifier
        ): ComputedCapability {
            return ComputedCapability(
                project.provider { moduleIdentifier.moduleId.group },
                project.provider { moduleIdentifier.moduleId.name },
                project.provider { moduleIdentifier.version },
                checkNotNull(moduleIdentifier.moduleId.moduleClassifier)
            )
        }

        /**
         * Capability that should be used to point to a published [variant]. If the variant's [KotlinGradleVariant.containingModule] is
         * published as a [Standalone] Maven module, then no capability is necessary to point to the variant, so this function returns
         * `null`.
         *
         * Unlike depending on the whole module (in the sense of KPM module dependencies), which just has one single capability, each of
         * the platform variants must have a separate capability, so that the variant from the platform module doesn't conflict with the
         * "umbrella" variant from the root module (Gradle prohibits duplicate capability across the dependency graph). So to publish (or
         * depend on) a particular platform variant in a platform module, one unique capabilities per platform module.
         *
         * A `main` module's variants don't not need any explicit capability, so this function returns `null` for variants of `main` modules.
         *
         * If the variant's containing module is not yet set up for publishing with [KotlinGradleModule.makePublic], then this
         * function can't decide whether or not a capability is needed, so it throws an [IllegalStateException].
         */
        fun forPublishedPlatformVariant(
            variant: KotlinGradleVariant,
            publication: SingleMavenPublishedModuleHolder
        ): ComputedCapability? {
            val module = variant.containingModule

            check(module.isPublic)

            if (module.isMain || module.publicationMode is Standalone)
                return null

            val project = module.project
            val coordinates = publication.publishedMavenModuleCoordinates
            return ComputedCapability(
                project.provider { coordinates.group },
                project.provider { coordinates.name },
                project.provider { coordinates.version },
                checkNotNull(module.moduleClassifier) // because this is not a main module
            )
        }
    }

    fun notation(): String = "$group:$name:$version"
}