/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaCompilation
import org.jetbrains.kotlin.gradle.utils.providerWithLazyConvention
import org.jetbrains.kotlin.gradle.utils.withType

internal const val KOTLIN_MODULE_GROUP = "org.jetbrains.kotlin"
internal const val KOTLIN_COMPILER_EMBEDDABLE = "kotlin-compiler-embeddable"
internal const val KOTLIN_BUILD_TOOLS_API_IMPL = "kotlin-build-tools-impl"
internal const val PLATFORM_INTEGERS_SUPPORT_LIBRARY = "platform-integers"

internal val CustomizeKotlinDependenciesSetupAction = KotlinProjectSetupAction {
    val kotlinExtension = project.kotlinExtension
    val propertiesProvider = PropertiesProvider(project)
    val coreLibrariesVersion = project.objects.providerWithLazyConvention {
        kotlinExtension.coreLibrariesVersion
    }

    if (propertiesProvider.stdlibDefaultDependency)
        project.configureStdlibDefaultDependency(kotlinExtension, coreLibrariesVersion)

    if (propertiesProvider.kotlinTestInferJvmVariant) { // TODO: extend this logic to PM20
        project.configureKotlinTestDependency(
            kotlinExtension,
            coreLibrariesVersion,
        )
    }

    if (propertiesProvider.stdlibDomApiIncluded) {
        project.configureKotlinDomApiDefaultDependency(kotlinExtension, coreLibrariesVersion)
    }

    project.configurations.configureDefaultVersionsResolutionStrategy(coreLibrariesVersion)

    if (propertiesProvider.stdlibJdkVariantsVersionAlignment) {
        project.configurations.configureStdlibVersionAlignment()
    }

    excludeStdlibAndKotlinTestCommonFromPlatformCompilations(project)
}

private fun ConfigurationContainer.configureDefaultVersionsResolutionStrategy(
    coreLibrariesVersion: Provider<String>,
) = all { configuration ->
    configuration.withDependencies { dependencySet ->
        dependencySet
            .withType<ExternalDependency>()
            .configureEach { dependency ->
                if (dependency.group == KOTLIN_MODULE_GROUP &&
                    dependency.version.isNullOrEmpty()
                ) {
                    dependency.version {
                        it.require(coreLibrariesVersion.get())
                    }
                }
            }
    }
}

private fun excludeStdlibAndKotlinTestCommonFromPlatformCompilations(project: Project) {
    val multiplatformExtension = project.multiplatformExtensionOrNull ?: return

    multiplatformExtension.targets.matching { it !is KotlinMetadataTarget }.configureEach {
        it.excludeStdlibAndKotlinTestCommonFromPlatformCompilations()
    }
}

// there several JVM-like targets, like KotlinWithJava, or KotlinAndroid, and they don't have common supertype
// aside from KotlinTarget
@Suppress("DEPRECATION") // KT-58227, KT-64273
private fun KotlinTarget.excludeStdlibAndKotlinTestCommonFromPlatformCompilations() {
    compilations.all {
        listOfNotNull(
            it.compileDependencyConfigurationName,
            it.defaultSourceSet.apiMetadataConfigurationName,
            it.defaultSourceSet.implementationMetadataConfigurationName,
            it.defaultSourceSet.compileOnlyMetadataConfigurationName,
            (it as? org.jetbrains.kotlin.gradle.plugin.KotlinCompilationToRunnableFiles<*>)?.runtimeDependencyConfigurationName,

            // Additional configurations for (old) jvmWithJava-preset. Remove it when we drop it completely
            (it as? KotlinWithJavaCompilation<*, *>)?.apiConfigurationName
        ).forEach { configurationName ->
            project.configurations.getByName(configurationName).apply {
                exclude(mapOf("group" to "org.jetbrains.kotlin", "module" to "kotlin-stdlib-common"))
                exclude(mapOf("group" to "org.jetbrains.kotlin", "module" to "kotlin-test-common"))
                exclude(mapOf("group" to "org.jetbrains.kotlin", "module" to "kotlin-test-annotations-common"))
            }
        }
    }
}

internal fun DependencyHandler.kotlinDependency(moduleName: String, versionOrNull: String?) =
    create("$KOTLIN_MODULE_GROUP:$moduleName${versionOrNull?.prependIndent(":").orEmpty()}")

internal fun Configuration.allNonProjectDependencies() = allDependencies.matching { it !is ProjectDependency }
