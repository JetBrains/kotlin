/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_IMPORT_ENABLE_KGP_DEPENDENCY_RESOLUTION

internal fun Project.enableKgpDependencyResolution(isEnabled: Boolean) {
    extensions.extraProperties.set(KOTLIN_MPP_IMPORT_ENABLE_KGP_DEPENDENCY_RESOLUTION, "$isEnabled") // IJ property parser expects a String
}
