/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmModule
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinPm20ProjectExtension
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope
import org.jetbrains.kotlin.gradle.plugin.sources.sourceSetDependencyConfigurationByScope
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.npm.SemVer

private const val KOTLIN_DOM_API_MODULE_NAME = "kotlin-dom-api-compat"

private val Dependency.isKotlinDomApiDependency: Boolean
    get() = group == KOTLIN_MODULE_GROUP && (name == KOTLIN_DOM_API_MODULE_NAME)

private val kotlin1820Version = SemVer(1.toBigInteger(), 8.toBigInteger(), 20.toBigInteger())

private fun isAtLeast1_8_20(version: String) = SemVer.fromGradleRichVersion(version) >= kotlin1820Version

internal fun Project.configureKotlinDomApiDefaultDependency(
    topLevelExtension: KotlinTopLevelExtension,
    coreLibrariesVersion: Provider<String>
) {
    when (topLevelExtension) {
        is KotlinPm20ProjectExtension -> addKotlinDomApiToKpmProject(project, coreLibrariesVersion)

        is KotlinJsProjectExtension -> topLevelExtension.registerTargetObserver { target ->
            target?.addKotlinDomApiDependency(configurations, dependencies, coreLibrariesVersion)
        }

        is KotlinSingleTargetExtension<*> -> topLevelExtension
            .target
            .addKotlinDomApiDependency(configurations, dependencies, coreLibrariesVersion)

        is KotlinMultiplatformExtension -> topLevelExtension
            .targets
            .configureEach { target ->
                target.addKotlinDomApiDependency(configurations, dependencies, coreLibrariesVersion)
            }
    }
}

private fun addKotlinDomApiToKpmProject(
    project: Project,
    coreLibrariesVersion: Provider<String>
) {
    project.pm20Extension.modules.named(GradleKpmModule.MAIN_MODULE_NAME) { main ->
        main.variants.configureEach { variant ->
            when (variant.platformType) {
                KotlinPlatformType.common -> error("variants are not expected to be common")
                KotlinPlatformType.js -> {
                    val dependencyHandler = project.dependencies
                    variant.dependencies {
                        api(dependencyHandler.kotlinDomApiDependency(coreLibrariesVersion.get()))
                    }
                }
                else -> {}
            }
        }
    }
}

private fun KotlinTarget.addKotlinDomApiDependency(
    configurations: ConfigurationContainer,
    dependencies: DependencyHandler,
    coreLibrariesVersion: Provider<String>
) {
    compilations.configureEach { compilation ->
        compilation.allKotlinSourceSets.forEach { kotlinSourceSet ->
            val scopeConfiguration = configurations
                .sourceSetDependencyConfigurationByScope(kotlinSourceSet, KotlinDependencyScope.API_SCOPE)

            scopeConfiguration.withDependencies { dependencySet ->
                if (compilation.platformType != KotlinPlatformType.js) return@withDependencies
                if (compilation !is KotlinJsIrCompilation) return@withDependencies

                if (isKotlinDomApiAddedByUser(configurations, kotlinSourceSet)) return@withDependencies

                val stdlibDependency = KotlinDependencyScope.values()
                    .map { scope ->
                        configurations.sourceSetDependencyConfigurationByScope(kotlinSourceSet, scope)
                    }
                    .flatMap { it.allNonProjectDependencies() }
                    .singleOrNull { dependency ->
                        dependency.group == KOTLIN_MODULE_GROUP && dependency.name in stdlibModules
                    }

                if (stdlibDependency != null) {
                    val depVersion = stdlibDependency.version ?: coreLibrariesVersion.get()
                    if (!isAtLeast1_8_20(depVersion)) return@withDependencies

                    // Check if stdlib is directly added to SourceSet


                    dependencySet.addLater(
                        coreLibrariesVersion.map {
                            dependencies.kotlinDomApiDependency(it)
                        }
                    )
                }
            }
        }
    }
}

private fun isKotlinDomApiAddedByUser(
    configurations: ConfigurationContainer,
    vararg sourceSets: KotlinSourceSet
): Boolean {
    return sourceSets
        .asSequence()
        .flatMap { sourceSet ->
            KotlinDependencyScope.values().map { scope ->
                configurations.sourceSetDependencyConfigurationByScope(sourceSet, scope)
            }.asSequence()
        }
        .flatMap { it.allNonProjectDependencies().asSequence() }
        .any { it.isKotlinDomApiDependency }
}

internal fun DependencyHandler.kotlinDomApiDependency(versionOrNull: String?) =
    create("$KOTLIN_MODULE_GROUP:$KOTLIN_DOM_API_MODULE_NAME${versionOrNull?.prependIndent(":").orEmpty()}")
