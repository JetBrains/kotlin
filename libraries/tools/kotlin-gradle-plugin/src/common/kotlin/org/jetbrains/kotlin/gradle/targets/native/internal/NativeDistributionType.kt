/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnosticOncePerBuild
import org.jetbrains.kotlin.gradle.targets.native.internal.NativeDistributionType.LIGHT
import org.jetbrains.kotlin.gradle.targets.native.internal.NativeDistributionType.PREBUILT
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

internal enum class NativeDistributionType(val suffix: String?, val mustGeneratePlatformLibs: Boolean) {
    LIGHT(null, true),
    PREBUILT("prebuilt", false),
}

internal class NativeDistributionTypeProvider(private val project: Project) {
    private val propertiesProvider = PropertiesProvider(project)

    fun getDistributionType(): NativeDistributionType {
        return when (val type = propertiesProvider.nativeDistributionType?.toLowerCaseAsciiOnly()) {
            null -> PREBUILT
            "prebuilt" -> PREBUILT
            "light" -> LIGHT
            else -> {
                project.reportDiagnosticOncePerBuild(KotlinToolingDiagnostics.UnrecognizedKotlinNativeDistributionType(type))
                PREBUILT
            }
        }
    }
}
