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
}

private class NativePropertiesLoader(project: Project) : NativeProperties {

    override val isUseXcodeMessageStyleEnabled: Provider<Boolean> = project.propertiesService.flatMap {
        it.property(USE_XCODE_MESSAGE_STYLE, project)
    }

    override val kotlinNativeVersion: Provider<String> = project.propertiesService.flatMap {
        it.propertyWithDeprecatedName(NATIVE_VERSION, NATIVE_VERSION_DEPRECATED, project)
            .orElse(NativeCompilerDownloader.DEFAULT_KONAN_VERSION)
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
    }
}
