/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.dependencies

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Usage
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.PlatformManagerPlugin
import org.jetbrains.kotlin.XcodeProvisioningSpec
import org.jetbrains.kotlin.isInternalServerEnabled
import org.jetbrains.kotlin.isWholeXcodeProvisioningEnabled
import org.jetbrains.kotlin.konan.properties.KonanPropertiesLoader
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.PlatformManager
import org.jetbrains.kotlin.konan.target.TargetDomainObjectContainer
import org.jetbrains.kotlin.konan.target.TargetWithSanitizer
import org.jetbrains.kotlin.konan.util.DependencyProcessor
import org.jetbrains.kotlin.utils.capitalized
import java.io.File
import java.nio.file.Paths
import javax.inject.Inject
import kotlin.io.path.name

/**
 * Downloading native dependencies.
 *
 * An embedder must provide [dependenciesDirectory] and [repositoryURL] and then provide a list
 * of targets.
 * To configure for all targets:
 * ```
 * nativeDependenciesDownloader {
 *     dependenciesDir.set(…)
 *     baseUrl.set(…)
 *     allTargets {}
 * }
 * ```
 * To download for host and `someTarget` only:
 * ```
 * nativeDependenciesDownloader {
 *     dependenciesDir.set(…)
 *     baseUrl.set(…)
 *     hostTarget {}
 *     target(someTarget) {}
 * }
 * ```
 *
 * Apply [plugin][NativeDependenciesDownloaderPlugin] to create this extension.
 * The extension name is `nativeDependenciesDownloader`.
 *
 * @see NativeDependenciesDownloaderPlugin
 */
abstract class NativeDependenciesDownloaderExtension @Inject constructor(private val project: Project) : TargetDomainObjectContainer<NativeDependenciesDownloaderExtension.Target>(project) {
    init {
        this.factory = { target ->
            project.objects.newInstance<Target>(this, target)
        }
    }

    /**
     * Outgoing configuration with all native dependencies.
     */
    val nativeDependenciesElements: Configuration by project.configurations.creating {
        description = "Native dependencies"
        isCanBeConsumed = true
        isCanBeResolved = false
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(NativeDependenciesUsage.NATIVE_DEPENDENCY))
        }
    }

    /**
     * Root directory where to place all dependencies.
     */
    abstract val dependenciesDirectory: DirectoryProperty

    /**
     * Dependency repository.
     */
    abstract val repositoryURL: Property<String>

    private val platformManager = project.extensions.getByType<PlatformManager>()

    private val expectedXcode: XcodeProvisioningSpec? by lazy {
        if (!project.isWholeXcodeProvisioningEnabled()) return@lazy null
        val loader = platformManager.loader(HostManager.host)
        require(loader is KonanPropertiesLoader) {
            "loader for ${HostManager.host} must implement ${KonanPropertiesLoader::class}"
        }
        fun requiredProperty(name: String) = loader.properties.getProperty(name)
                ?: error("whole-Xcode provisioning is on but '$name' is missing from konan.properties")
        XcodeProvisioningSpec(
                konanDataDir = project.providers.gradleProperty("konan.data.dir").orNull,
                version = requiredProperty("xcodeVersion"),
                build = requiredProperty("xcodeBuild"),
                artifactUrl = requiredProperty("xcodeArtifactUrl"),
                serverEnabled = project.isInternalServerEnabled(),
                isTeamCity = project.providers.environmentVariable("TEAMCITY_VERSION").isPresent,
        )
    }

    internal fun xcodeProvisioningFor(target: KonanTarget): XcodeProvisioningSpec? =
            expectedXcode?.takeIf { target.family.isAppleFamily }

    val hostXcodeApp: File? by lazy {
        expectedXcode?.let { dependenciesDirectory.get().asFile.resolve("xcode_${it.version}_${it.build}") }
    }

    abstract class Target @Inject constructor(
            private val owner: NativeDependenciesDownloaderExtension,
            private val _target: TargetWithSanitizer,
    ) {
        val target by _target::target
        val sanitizer by _target::sanitizer

        private val project by owner::project

        private val platformManager = project.extensions.getByType<PlatformManager>()

        private val loader: KonanPropertiesLoader = platformManager.loader(target).let {
            require(it is KonanPropertiesLoader) {
                "loader for $target must implement ${KonanPropertiesLoader::class}"
            }
            it
        }

        private val dependencyProcessor by lazy {
            DependencyProcessor(
                    owner.dependenciesDirectory.apply { finalizeValue() }.asFile.get(),
                    loader,
                    owner.repositoryURL.apply { finalizeValue() }.get(),
                    keepUnstable = false) { _, _, _ ->
                error("This is used as dependency resolver only, downloading cannot be performed")
            }
        }

        private val dependencies by loader::dependencies

        val task = project.tasks.register<NativeDependenciesDownloader>("nativeDependencies${_target.name.capitalized}") {
            description = "Download dependencies for $_target"
            group = "native dependencies"
            target.set(this@Target.target)
            dependenciesDirectory.set(owner.dependenciesDirectory)
            repositoryURL.set(owner.repositoryURL)
            owner.xcodeProvisioningFor(this@Target.target)?.let { xcodeProvisioning.set(it) }
        }

        init {
            owner.nativeDependenciesElements.outgoing {
                variants {
                    create("$_target") {
                        attributes {
                            attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, _target)
                        }
                        dependencies.forEach { dependency ->
                            artifact(dependencyProcessor.resolve(dependency)) {
                                name = dependency
                                type = "directory"
                                extension = ""
                                builtBy(task)
                            }
                        }
                        // Check for overridden distributions. They'll be absent from `depedencies` and
                        // so should be added manually.
                        listOf(loader.llvmHome!!, loader.libffiDir!!).forEach { dependency ->
                            val dependencyPath = Paths.get(dependency)
                            // If dependency is an absolute path - it's overridden.
                            if (dependencyPath.isAbsolute) {
                                artifact(dependencyPath.toFile()) {
                                    name = dependencyPath.name
                                    type = "directory"
                                    extension = ""
                                }
                            }
                        }
                        if (target.family.isAppleFamily) {
                            owner.hostXcodeApp?.let { xcodeApp ->
                                artifact(xcodeApp) {
                                    name = xcodeApp.name
                                    type = "directory"
                                    extension = ""
                                    builtBy(task)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Downloading native dependencies.
 *
 * Provides [extension][NativeDependenciesDownloaderExtension] named `nativeDependenciesDownloader`
 * that configures native dependencies.
 *
 * To consume native dependencies use [NativeDependenciesPlugin].
 *
 * @see NativeDependenciesPlugin
 * @see NativeDependenciesDownloaderExtension
 */
open class NativeDependenciesDownloaderPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.apply<PlatformManagerPlugin>()
        project.apply<NativeDependenciesBasePlugin>()
        project.extensions.create<NativeDependenciesDownloaderExtension>("nativeDependenciesDownloader", project)
    }
}
