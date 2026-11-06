/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider

fun Project.computeTransformedLibraryVersionChecksum(enable: Boolean = true) {
    extensions.extraProperties.set(
        PropertiesProvider.PropertyNames.KOTLIN_MPP_COMPUTE_TRANSFORMED_LIBRARY_VERSION_CHECKSUM,
        enable.toString(),
    )
}
