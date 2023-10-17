/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.isTest
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope
import org.jetbrains.kotlin.gradle.plugin.sources.android.AndroidBaseSourceSetName
import org.jetbrains.kotlin.gradle.plugin.sources.android.AndroidVariantType
import org.jetbrains.kotlin.gradle.plugin.sources.android.androidSourceSetInfoOrNull
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.plugin.sources.sourceSetDependencyConfigurationByScope
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.npm.SemVer
import org.jetbrains.kotlin.gradle.utils.forAllTargets
import org.jetbrains.kotlin.gradle.utils.withType

internal const val KOTLIN_STDLIB_COMMON_MODULE_NAME = "kotlin-stdlib-common"
internal const val KOTLIN_STDLIB_MODULE_NAME = "kotlin-stdlib"
internal const val KOTLIN_STDLIB_JDK7_MODULE_NAME = "kotlin-stdlib-jdk7"
internal const val KOTLIN_STDLIB_JDK8_MODULE_NAME = "kotlin-stdlib-jdk8"
internal const val KOTLIN_STDLIB_JS_MODULE_NAME = "kotlin-stdlib-js"
internal const val KOTLIN_ANDROID_JVM_STDLIB_MODULE_NAME = KOTLIN_STDLIB_MODULE_NAME

internal fun Project.configureStdlibDefaultDependency(
    kotlinExtension: KotlinProjectExtension,
    coreLibrariesVersion: Provider<String>,
) {
    kotlinExtension.forAllTargets { target ->
        target.addStdlibDependency(
            configurations,
            dependencies,
            coreLibrariesVersion,
            isMppProject = kotlinExtension is KotlinMultiplatformExtension,
        )
    }
}

/**
 * Aligning kotlin-stdlib-jdk8 and kotlin-stdlib-jdk7 dependencies versions with kotlin-stdlib (or kotlin-stdlib-jdk7)
 * when project stdlib version is >= 1.8.0
 */
internal fun ConfigurationContainer.configureStdlibVersionAlignment() = all { configuration ->
    configuration.withDependencies { dependencySet ->
        dependencySet
            .withType<ExternalDependency>()
            .configureEach { dependency ->
                if (dependency.group == KOTLIN_MODULE_GROUP &&
                    (dependency.name == KOTLIN_STDLIB_MODULE_NAME || dependency.name == KOTLIN_STDLIB_JDK7_MODULE_NAME) &&
                    dependency.version != null &&
                    SemVer.fromGradleRichVersion(dependency.version!!).let { it >= kotlin180Version && it < kotlin1920Version }
                ) {
                    if (configuration.isCanBeResolved) configuration.alignStdlibJvmVariantVersions(dependency)

                    // dependency substitution only works for resolvable configuration,
                    // so we need to find all configuration that extends current one
                    filter {
                        it.isCanBeResolved && it.hierarchy.contains(configuration)
                    }.forEach {
                        it.alignStdlibJvmVariantVersions(dependency)
                    }
                }
            }
    }
}

private fun Configuration.alignStdlibJvmVariantVersions(
    kotlinStdlibDependency: ExternalDependency,
) {
    resolutionStrategy.dependencySubstitution {
        if (kotlinStdlibDependency.name != KOTLIN_STDLIB_JDK7_MODULE_NAME) {
            it.substitute(it.module("org.jetbrains.kotlin:kotlin-stdlib-jdk7"))
                .using(it.module("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${kotlinStdlibDependency.version}"))
                .because("kotlin-stdlib-jdk7 is now part of kotlin-stdlib")
        }

        it.substitute(it.module("org.jetbrains.kotlin:kotlin-stdlib-jdk8"))
            .using(it.module("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinStdlibDependency.version}"))
            .because("kotlin-stdlib-jdk8 is now part of kotlin-stdlib")
    }
}

private fun KotlinTarget.addStdlibDependency(
    configurations: ConfigurationContainer,
    dependencies: DependencyHandler,
    coreLibrariesVersion: Provider<String>,
    isMppProject: Boolean,
) {
    compilations.configureEach { compilation ->
        compilation.internal.kotlinSourceSets.forAll { kotlinSourceSet ->
            val scope = if (compilation.isTest() ||
                (this is KotlinAndroidTarget && kotlinSourceSet.isRelatedToAndroidTestSourceSet())
            ) {
                KotlinDependencyScope.IMPLEMENTATION_SCOPE
            } else {
                KotlinDependencyScope.API_SCOPE
            }
            val scopeConfiguration = configurations
                .sourceSetDependencyConfigurationByScope(kotlinSourceSet, scope)

            scopeConfiguration.withDependencies { dependencySet ->
                // Check if stdlib is directly added to SourceSet
                /*
                In case of 'kotlin-stdlib-common being added: Ensure that stdlib is added also
                This will help with substituting kotlin-stdlib-common with kotlin-stdlib:
                Currently transformed metadata dependencies will not recognise the substitution otherwise.
                */
                if (isStdlibAddedByUser(configurations, stdlibModules - KOTLIN_STDLIB_COMMON_MODULE_NAME, kotlinSourceSet))
                    return@withDependencies

                val requestedStdlibVersion = coreLibrariesVersion.get()
                val stdlibVersion = SemVer.fromGradleRichVersion(requestedStdlibVersion)

                // Since 1.9.20 in MPP projects, we should add stdlib only for common dependencies
                // except standalone compilations which as not using 'common'
                if (isMppProject &&
                    stdlibVersion >= kotlin1920Version &&
                    kotlinSourceSet.dependsOn.isNotEmpty()
                ) return@withDependencies

                val stdlibModule = compilation.platformType.stdlibPlatformType(this, kotlinSourceSet, stdlibVersion >= kotlin1920Version)
                    ?: return@withDependencies

                dependencySet.addLater(
                    coreLibrariesVersion.map {
                        dependencies.kotlinDependency(stdlibModule, it)
                    }
                )
            }
        }
    }
}

internal fun isStdlibAddedByUser(
    configurations: ConfigurationContainer,
    stdlibModules: Set<String>,
    vararg sourceSets: KotlinSourceSet,
): Boolean {
    return sourceSets
        .asSequence()
        .flatMap { sourceSet ->
            KotlinDependencyScope.values().map { scope ->
                configurations.sourceSetDependencyConfigurationByScope(sourceSet, scope)
            }.asSequence()
        }
        .flatMap { it.allNonProjectDependencies().asSequence() }
        .any { dependency ->
            dependency.group == KOTLIN_MODULE_GROUP && dependency.name in stdlibModules
        }
}

internal fun KotlinPlatformType.stdlibPlatformType(
    kotlinTarget: KotlinTarget,
    kotlinSourceSet: KotlinSourceSet,
    isVersionWithGradleMetadata: Boolean,
): String? = when (this) {
    KotlinPlatformType.jvm -> if (isVersionWithGradleMetadata) KOTLIN_STDLIB_MODULE_NAME else KOTLIN_STDLIB_JDK8_MODULE_NAME
    KotlinPlatformType.androidJvm -> {
        if (kotlinTarget is KotlinAndroidTarget &&
            kotlinSourceSet.androidSourceSetInfoOrNull?.androidSourceSetName == AndroidBaseSourceSetName.Main.name
        ) {
            if (isVersionWithGradleMetadata) KOTLIN_ANDROID_JVM_STDLIB_MODULE_NAME else KOTLIN_STDLIB_JDK8_MODULE_NAME
        } else {
            null
        }
    }

    KotlinPlatformType.js -> if (isVersionWithGradleMetadata) KOTLIN_STDLIB_MODULE_NAME else KOTLIN_STDLIB_JS_MODULE_NAME
    KotlinPlatformType.wasm -> KOTLIN_STDLIB_MODULE_NAME
    KotlinPlatformType.native -> null
    KotlinPlatformType.common -> // there's no platform compilation that the source set is default for
        if (isVersionWithGradleMetadata) KOTLIN_STDLIB_MODULE_NAME else KOTLIN_STDLIB_COMMON_MODULE_NAME
}

private val androidTestVariants = setOf(AndroidVariantType.UnitTest, AndroidVariantType.InstrumentedTest)

private val kotlin180Version = SemVer(1.toBigInteger(), 8.toBigInteger(), 0.toBigInteger())
private val kotlin1920Version = SemVer(1.toBigInteger(), 9.toBigInteger(), 20.toBigInteger())

private fun KotlinSourceSet.isRelatedToAndroidTestSourceSet(): Boolean {
    val androidVariant = androidSourceSetInfoOrNull?.androidVariantType ?: return false
    return androidVariant in androidTestVariants
}

internal val stdlibModules = setOf(
    KOTLIN_STDLIB_COMMON_MODULE_NAME,
    KOTLIN_STDLIB_MODULE_NAME,
    KOTLIN_STDLIB_JDK7_MODULE_NAME,
    KOTLIN_STDLIB_JDK8_MODULE_NAME,
    KOTLIN_STDLIB_JS_MODULE_NAME,
)
