/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.targets.native.internal.NativeDistributionType.LIGHT
import org.jetbrains.kotlin.gradle.targets.native.internal.NativeDistributionType.PREBUILT
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

internal enum class NativeDistributionType(val suffix: String?, val mustGeneratePlatformLibs: Boolean) {
    LIGHT(null, true),
    PREBUILT("prebuilt", false),
}

internal class NativeDistributionTypeProvider(private val nativeDistributionType: String?) {
    companion object {
        private val log = Logging.getLogger(NativeDistributionTypeProvider::class.java)
    }

    fun getDistributionType(): NativeDistributionType {
        return when (val type = nativeDistributionType?.toLowerCaseAsciiOnly()) {
            null -> PREBUILT
            "prebuilt" -> PREBUILT
            "light" -> LIGHT
            else -> {
                log.warn("Unknown Kotlin/Native distribution type $type, defaulting to $PREBUILT")
                PREBUILT
            }
        }
    }
}
