/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.targets.native.internal.NativeDistributionType.*
import org.jetbrains.kotlin.gradle.utils.SingleWarningPerBuild
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

internal enum class NativeDistributionType(val suffix: String?, val mustGeneratePlatformLibs: Boolean) {
    LIGHT(null, true),
    PREBUILT("prebuilt", false),
}

internal class NativeDistributionTypeProvider(private val project: Project) {
    private val propertiesProvider = PropertiesProvider(project)

    private fun warning(message: String) = SingleWarningPerBuild.show(project, "Warning: $message")

    fun getDistributionType(): NativeDistributionType {
        return when (propertiesProvider.nativeDistributionType?.toLowerCaseAsciiOnly()) {
            null -> PREBUILT
            "prebuilt" -> PREBUILT
            "light" -> LIGHT
            else -> {
                warning("Unknown Kotlin/Native distribution type: ${propertiesProvider.nativeDistributionType?.toLowerCaseAsciiOnly()}. Available values: prebuilt, light")
                PREBUILT
            }
        }
    }
}
