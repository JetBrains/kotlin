/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.Project
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.DependencySubstitutions
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryDependency
import org.jetbrains.kotlin.gradle.internal.KOTLIN_ANDROID_JVM_STDLIB_MODULE_NAME
import org.jetbrains.kotlin.gradle.internal.KOTLIN_MODULE_GROUP
import org.jetbrains.kotlin.gradle.internal.KOTLIN_STDLIB_COMMON_MODULE_NAME
import org.jetbrains.kotlin.gradle.internal.addKotlinTestWithCapability
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.utils.named
import org.jetbrains.kotlin.gradle.utils.setAttribute

/**
 * Resolves dependencies of jvm and Android source sets from the perspective jvm
 */
internal fun IdeJvmAndAndroidPlatformBinaryDependencyResolver(project: Project): IdeDependencyResolver {
    val coreLibrariesVersion = project.provider { project.kotlinExtension.coreLibrariesVersion }
    return IdeBinaryDependencyResolver(
        binaryType = IdeaKotlinBinaryDependency.KOTLIN_COMPILE_BINARY_TYPE,
        artifactResolutionStrategy = IdeBinaryDependencyResolver.ArtifactResolutionStrategy.PlatformLikeSourceSet(
            setupPlatformResolutionAttributes = {
                attributes.setAttribute(Usage.USAGE_ATTRIBUTE, project.usageByName(Usage.JAVA_API))
                attributes.setAttribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.LIBRARY))
                attributes.setAttribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
                attributes.setAttribute(
                    TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                    project.objects.named(TargetJvmEnvironment.STANDARD_JVM)
                )
            },
            /*
            Prevent this resolver from running against project dependencies:
            Otherwise we would match the -jvm.jar from the dependency project which will result in
            matching the jvmMain source set as well (which is undesired)
             */
            componentFilter = { identifier -> identifier !is ProjectComponentIdentifier },
            dependencySubstitution = { substituteStdlibCommonWithAndroidJvm(it, coreLibrariesVersion) },
            withDependencies = DependencySet::addKotlinTestWithCapability,
        )
    )
}

/**
 * This is a replacement for propagation of stdlib-jvm in non-KGP-based IDE import for JVM+Android source sets.
 * It's possible to resolve regular dependencies of the source set with platform attributes to get the correct JVM variants.
 * But stdlib is a special case, kotlin-stdlib-common is not a common variant for the JVM stdlib w.r.t. publication.
 * Substituting kotlin-stdlib-common with the Android-JVM stdlib in requests workarounds the issue.
 */
internal fun substituteStdlibCommonWithAndroidJvm(dependencySubstitutions: DependencySubstitutions, coreLibrariesVersion: Provider<String>) {
    dependencySubstitutions.all { dependency ->
        val requested = dependency.requested
        if (requested is ModuleComponentSelector
            && requested.group == KOTLIN_MODULE_GROUP
            && requested.module == KOTLIN_STDLIB_COMMON_MODULE_NAME
        ) {
            val version = requested.version.ifBlank { coreLibrariesVersion.get() }
            dependency.useTarget(
                "$KOTLIN_MODULE_GROUP:$KOTLIN_ANDROID_JVM_STDLIB_MODULE_NAME:${version}",
                "Substituted by KGP for correct Dependencies scope in Android + JVM common source set"
            )
        }
    }
}
