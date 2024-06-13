/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.properties

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader

internal val Project.nativeProperties: NativeProperties
    get() = NativePropertiesLoader(this)

internal interface NativeProperties {
    val isUseXcodeMessageStyleEnabled: Provider<Boolean>
    val kotlinNativeVersion: Provider<String>
    val jvmArgs: Provider<List<String>>
    val forceDisableRunningInProcess: Provider<Boolean>
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
    }
}
