/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nativeDistribution

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.project
import org.jetbrains.kotlin.konan.target.TargetWithSanitizer
import javax.inject.Inject

open class BuiltNativeDistribution @Inject constructor(
        @get:Internal("tracked via files and filesRelativePaths")
        val dist: NativeDistribution,
        objectFactory: ObjectFactory,
) {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE) // computed manually
    internal val files: ConfigurableFileCollection = objectFactory.fileCollection()

    @get:Input
    @Suppress("UNUSED") // used by Gradle via reflection
    protected val filesRelativePaths = files.elements.map { files ->
        val root = dist.root.asFile
        files.map {
            it.asFile.toRelativeString(root)
        }.sorted()
    }
}

private fun Project.distributionComponent(component: String, target: TargetWithSanitizer? = null): Configuration = configurations.detachedConfiguration(
        dependencies.project(":kotlin-native:prepare:kotlin-native-distribution")
).apply {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("native-distribution-component"))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(component))
        target?.let {
            attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, it)
        }
    }
}

val Project.nativeDistributionWithCompiler: BuiltNativeDistribution
    get() = objects.newInstance<BuiltNativeDistribution>(nativeDistribution.get()).apply {
        files.from(distributionComponent("compiler"))
    }

val Project.nativeDistributionWithStdlib: BuiltNativeDistribution
    get() = objects.newInstance<BuiltNativeDistribution>(nativeDistribution.get()).apply {
        files.from(distributionComponent("compiler"))
        files.from(distributionComponent("stdlib"))
    }

fun Project.nativeDistributionWithStdlibAndRuntime(target: TargetWithSanitizer? = TargetWithSanitizer.host): BuiltNativeDistribution = objects.newInstance<BuiltNativeDistribution>(nativeDistribution.get()).apply {
    files.from(distributionComponent("compiler"))
    files.from(distributionComponent("stdlib"))
    files.from(distributionComponent("runtime", target))
}