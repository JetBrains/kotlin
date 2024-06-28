/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.properties

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import java.io.File

internal val Project.nativeProperties: NativeProperties
    get() = NativePropertiesLoader(this)

internal interface NativeProperties {
    val isUseXcodeMessageStyleEnabled: Provider<Boolean>
    val kotlinNativeVersion: Provider<String>
    val jvmArgs: Provider<List<String>>
    val forceDisableRunningInProcess: Provider<Boolean>
    val konanDataDir: Provider<File?>
    val downloadFromMaven: Provider<Boolean>
    val isToolchainEnabled: Provider<Boolean>
    val isUseEmbeddableCompilerJar: Provider<Boolean>

    /**
     * Value of 'kotlin.native.home' property.
     *
     * It may be empty.
     */
    val userProvidedNativeHome: Provider<String>

    /**
     * Actual Kotlin Native home directory calculated based on user configuration, host os and other inputs.
     *
     * Provider should always be present.
     */
    val actualNativeHomeDirectory: Provider<File>

    companion object {
        /**
         * Allows a user to provide a local Kotlin/Native distribution instead of a downloaded one.
         */
        internal val NATIVE_HOME = PropertiesBuildService.NullableStringGradleProperty(
            name = "kotlin.native.home"
        )
    }
}

private class NativePropertiesLoader(project: Project) : NativeProperties {

    private val propertiesService = project.propertiesService

    override val isUseXcodeMessageStyleEnabled: Provider<Boolean> = propertiesService.flatMap {
        it.property(USE_XCODE_MESSAGE_STYLE, project)
    }

    override val kotlinNativeVersion: Provider<String> = propertiesService.flatMap {
        it.propertyWithDeprecatedName(NATIVE_VERSION, NATIVE_VERSION_DEPRECATED, project)
            .orElse(NativeCompilerDownloader.DEFAULT_KONAN_VERSION)
    }

    override val jvmArgs: Provider<List<String>> = project.propertiesService.flatMap { propertiesService ->
        propertiesService.propertyWithDeprecatedName(NATIVE_JVM_ARGS, NATIVE_JVM_ARGS_DEPRECATED, project)
            .map {
                @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
                it!!.split("\\s+".toRegex())
            }
            .orElse(emptyList())
    }

    override val forceDisableRunningInProcess: Provider<Boolean> = propertiesService.flatMap {
        it.property(NATIVE_FORCE_DISABLE_IN_PROCESS, project)
    }

    private val konanDataDirProperty = propertiesService.flatMap { service ->
        service.property(KONAN_DATA_DIR, project).map { File(it) }
    }

    override val konanDataDir: Provider<File?> = konanDataDirProperty

    override val downloadFromMaven: Provider<Boolean> = propertiesService.flatMap {
        it.property(NATIVE_DOWNLOAD_FROM_MAVEN, project)
    }

    override val isToolchainEnabled: Provider<Boolean> = propertiesService.flatMap {
        it.property(NATIVE_TOOLCHAIN_ENABLED, project).zip(downloadFromMaven) { isToolchainEnabled, isDownloadFromMavenEnabled ->
            isToolchainEnabled && isDownloadFromMavenEnabled
        }
    }

    override val userProvidedNativeHome: Provider<String> = propertiesService.flatMap { service ->
        service.propertyWithDeprecatedName(NativeProperties.NATIVE_HOME, NATIVE_HOME_DEPRECATED, project)
    }

    override val actualNativeHomeDirectory: Provider<File> = konanDataDirProperty
        .map { NativeCompilerDownloader.getOsSpecificCompilerDirectory(project, it) }
        .orElse(
            userProvidedNativeHome
                .map { File(it) }
                .orElse(
                    project.providers.provider {
                        NativeCompilerDownloader.getDefaultCompilerDirectory(project)
                    }
                )
        )

    override val isUseEmbeddableCompilerJar: Provider<Boolean> = propertiesService.flatMap {
        it.property(NATIVE_USE_EMBEDDABLE_COMPILER_JAR, project)
    }

    companion object {
        private const val PROPERTIES_PREFIX = "kotlin.native"

        /**
         * Forces K/N compiler to print messages which could be parsed by Xcode
         */
        private val USE_XCODE_MESSAGE_STYLE = PropertiesBuildService.NullableBooleanGradleProperty(
            name = "$PROPERTIES_PREFIX.useXcodeMessageStyle",
        )

        private val NATIVE_VERSION = PropertiesBuildService.NullableStringGradleProperty(
            name = "$PROPERTIES_PREFIX.version"
        )

        private val NATIVE_VERSION_DEPRECATED = PropertiesBuildService.NullableStringGradleProperty(
            name = "org.jetbrains.kotlin.native.version"
        )

        /**
         * Allows a user to specify additional arguments of a JVM executing a K/N compiler.
         */
        private val NATIVE_JVM_ARGS = PropertiesBuildService.NullableStringGradleProperty(
            name = "$PROPERTIES_PREFIX.jvmArgs",
        )

        private val NATIVE_JVM_ARGS_DEPRECATED = PropertiesBuildService.NullableStringGradleProperty(
            name = "org.jetbrains.kotlin.native.jvmArgs",
        )

        /**
         * Forces to run a compilation in a separate JVM.
         */
        private val NATIVE_FORCE_DISABLE_IN_PROCESS = PropertiesBuildService.BooleanGradleProperty(
            name = "$PROPERTIES_PREFIX.disableCompilerDaemon",
            defaultValue = false
        )

        /**
         * Allows the user to specify a custom location for the Kotlin/Native distribution.
         * This property takes precedence over the 'KONAN_DATA_DIR' environment variable.
         */
        private val KONAN_DATA_DIR = PropertiesBuildService.NullableStringGradleProperty(
            name = "konan.data.dir"
        )

        private val NATIVE_HOME_DEPRECATED = PropertiesBuildService.NullableStringGradleProperty(
            name = "org.jetbrains.kotlin.native.home"
        )

        /**
         * Allows downloading Kotlin/Native distribution with maven.
         *
         * Makes downloader search for bundles in maven repositories specified in the project.
         */
        private val NATIVE_DOWNLOAD_FROM_MAVEN = PropertiesBuildService.BooleanGradleProperty(
            name = "$PROPERTIES_PREFIX.distribution.downloadFromMaven",
            defaultValue = true
        )

        /**
         * Enables kotlin native toolchain in native projects.
         */
        private val NATIVE_TOOLCHAIN_ENABLED = PropertiesBuildService.BooleanGradleProperty(
            name = "$PROPERTIES_PREFIX.toolchain.enabled",
            defaultValue = true
        )

        /**
         * Switches Kotlin/Native tasks to using embeddable compiler jar,
         * allowing to apply backend-agnostic compiler plugin artifacts.
         * Will be default after proper migration.
         */
        private val NATIVE_USE_EMBEDDABLE_COMPILER_JAR = PropertiesBuildService.BooleanGradleProperty(
            name = "$PROPERTIES_PREFIX.useEmbeddableCompilerJar",
            defaultValue = true
        )
    }
}
