/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.dependencies

import org.gradle.api.Buildable
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.PlatformManagerPlugin
import org.jetbrains.kotlin.konan.target.*
import java.io.File
import java.nio.file.Paths
import javax.inject.Inject
import kotlin.io.path.isSameFileAs

private fun File.matchesDependency(dependency: String): Boolean {
    val path = Paths.get(dependency)
    return if (path.isAbsolute) {
        path.isSameFileAs(this.toPath())
    } else {
        this.name == dependency
    }
}

private val Iterable<File>.commonAncestor: File
    get() = map { it.canonicalFile.normalize() }.reduce { acc, file ->
        // Both `file` and `acc` are fully normalized. If `file` and `acc` share the root (which we assume they do), then
        // the path of `file` relative to `acc` will start with a sequence of `..` until the common ancestor is reached, and
        // after that there will be no `..` in the rest of the path.
        val relativePath = file.toRelativeString(acc)
        val goUpPrefix = ".."
        var startIndex = 0
        var count = 0
        while (true) {
            val index = relativePath.indexOf(goUpPrefix, startIndex)
            if (index == -1) {
                break
            }
            startIndex = index + goUpPrefix.length
            count++
        }
        // `count` contains the number of times one must go up from `acc` to get to the common ancestor of `file` and `acc`.
        var result = acc
        repeat(count) {
            result = result.parentFile
        }
        // Doing `parentFile` from a normalized canonical path keeps the path normalized.
        result
    }

/**
 * Consuming native dependencies.
 *
 * Creates resolvable [nativeDependencies] configuration with default dependency
 * on `:kotlin-native:dependencies` (that project must provide native dependencies by using [NativeDependenciesDownloaderPlugin])
 *
 * [llvmPath], [libffiPath] and [hostPlatform] are accessors that can be used during configuration
 * phase and do not anyhow force the actual dependencies to be downloaded. So, anytime these
 * accessors are used, make sure that execution of related tasks (or configurations resolving)
 * depend on [llvmDependency], [libffiDependency] and [hostPlatformDependency] respectively.
 *
 * Apply [plugin][NativeDependenciesPlugin] to create this extension.
 * The extension name is `nativeDependencies`.
 *
 * @see NativeDependenciesPlugin
 */
abstract class NativeDependenciesExtension @Inject constructor(private val project: Project) {
    private val platformManager = project.extensions.getByType<PlatformManager>()

    val nativeDependencies: Configuration by project.configurations.creating {
        description = "Native dependencies"
        isCanBeConsumed = false
        isCanBeResolved = true
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(NativeDependenciesUsage.NATIVE_DEPENDENCY))
        }
        defaultDependencies {
            add(project.dependencies.project(":kotlin-native:dependencies"))
        }
    }

    /**
     * The root folder, where all dependencies are placed.
     *
     * The assumption is made, that the paths of each dependency relative to this root is stable across different machines.
     * Note: this will not cause the dependencies to actually download.
     */
    val nativeDependenciesRoot: File
        get() = nativeDependencies.incoming.artifacts.artifactFiles.commonAncestor

    private val llvmFileCollection: FileCollection = nativeDependencies.incoming.artifacts.artifactFiles.filter {
        it.matchesDependency(platformManager.hostPlatform.llvmHome!!)
    }

    private val libffiFileCollection: FileCollection = nativeDependencies.incoming.artifacts.artifactFiles.filter {
        it.matchesDependency(platformManager.hostPlatform.libffiDir!!)
    }

    /**
     * Dependency on host LLVM.
     */
    val llvmDependency: Buildable by ::llvmFileCollection

    /**
     * Absolute path to host LLVM. Can be used during configuration.
     * Note: this will not force LLVM to be downloaded. Make sure to depend
     * on [llvmDependency] in tasks that need it.
     */
    val llvmPath: String
        get() = llvmFileCollection.singleFile.canonicalPath

    /**
     * Dependency on host libffi.
     */
    val libffiDependency: Buildable by ::libffiFileCollection

    /**
     * Absolute path to host libffi. Can be used during configuration.
     * Note: this will not force libffi to be downloaded. Make sure to depend
     * on [libffiDependency] in tasks that need it.
     */
    val libffiPath: String
        get() = libffiFileCollection.singleFile.canonicalPath

    /**
     * Dependency on host platform.
     *
     * Equivalent to calling [targetDependency] with host target
     */
    val hostPlatformDependency: Buildable
        get() = targetDependency()

    /**
     * [Platform] for host. Can be used during configuration.
     * Note: this will not force platform dependency to be downloaded. Make sure to depend
     * on [hostPlatformDependency] in tasks that need it.
     */
    val hostPlatform: Platform
        get() = platformManager.hostPlatform

    /**
     * Dependency on [target] platform.
     */
    fun targetDependency(target: TargetWithSanitizer = TargetWithSanitizer.host): Buildable =
            nativeDependencies.incoming.artifactView {
                attributes {
                    attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, target)
                }
            }.files

    /**
     * Dependency on [target] platform.
     */
    // TODO: Remove when all build.gradle are gone.
    fun targetDependency(target: KonanTarget): Buildable = targetDependency(target.withSanitizer())
}

/**
 * Consuming native dependencies.
 *
 * Provides [extension][NativeDependenciesExtension] named `nativeDependencies` that manages native dependencies.
 *
 * To provide native dependencies use [NativeDependenciesDownloaderPlugin].
 *
 * @see NativeDependenciesDownloaderPlugin
 * @see NativeDependenciesExtension
 */
class NativeDependenciesPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.apply<PlatformManagerPlugin>()
        project.apply<NativeDependenciesBasePlugin>()
        project.extensions.create<NativeDependenciesExtension>("nativeDependencies", project)
    }
}
