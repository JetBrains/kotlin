/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.utils.named

/**
 * Resolves dependencies of jvm and Android source sets from the perspective jvm
 */
internal fun IdeJvmAndAndroidPlatformBinaryDependencyResolver(project: Project): IdeDependencyResolver =
    IdePlatformDependencyResolver(
        binaryType = IdeaKotlinDependency.CLASSPATH_BINARY_TYPE,
        artifactResolutionStrategy = IdePlatformDependencyResolver.ArtifactResolutionStrategy.PlatformLikeSourceSet(
            setupPlatformResolutionAttributes = {
                attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(Usage.JAVA_API))
                attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.LIBRARY))
                attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
                attributes.attribute(
                    TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                    project.objects.named(TargetJvmEnvironment.STANDARD_JVM)
                )
            },
        )
    )
