/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.utils.SingleWarningPerBuild

internal object MissingNativeStdlibWarning {
    const val NO_NATIVE_STDLIB_WARNING =
        "The Kotlin/Native distribution used in this build does not provide the standard library. "

    const val NO_NATIVE_STDLIB_PROPERTY_WARNING =
        "Make sure that the '${PropertiesProvider.KOTLIN_NATIVE_HOME}' property points to a valid Kotlin/Native distribution."

     fun Project.showMissingNativeStdlibWarning() {
        if (!project.hasProperty("kotlin.native.nostdlib")) {
            SingleWarningPerBuild.show(
                project, buildString {
                    append(NO_NATIVE_STDLIB_WARNING)
                    if (PropertiesProvider(project).nativeHome != null)
                        append(NO_NATIVE_STDLIB_PROPERTY_WARNING)
                }
            )
        }
    }
}