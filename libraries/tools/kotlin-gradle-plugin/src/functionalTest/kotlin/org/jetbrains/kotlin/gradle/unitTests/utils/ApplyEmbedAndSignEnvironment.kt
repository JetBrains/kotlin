/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.utils

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XcodeEnvironment
import org.jetbrains.kotlin.gradle.utils.getFile

fun Project.applyEmbedAndSignEnvironment(
    configuration: String,
    sdk: String,
    archs: String,
    builtProductsDirectory: String = layout.buildDirectory.dir("products").getFile().canonicalPath,
    targetBuildDirectory: String = layout.buildDirectory.dir("buildDir").getFile().canonicalPath,
) {
    extensions.extraProperties.set(
        "${XcodeEnvironment.XCODE_ENVIRONMENT_OVERRIDE_KEY}.CONFIGURATION",
        configuration
    )
    extensions.extraProperties.set(
        "${XcodeEnvironment.XCODE_ENVIRONMENT_OVERRIDE_KEY}.SDK_NAME",
        sdk
    )
    extensions.extraProperties.set(
        "${XcodeEnvironment.XCODE_ENVIRONMENT_OVERRIDE_KEY}.ARCHS",
        archs
    )
    extensions.extraProperties.set(
        "${XcodeEnvironment.XCODE_ENVIRONMENT_OVERRIDE_KEY}.BUILT_PRODUCTS_DIR",
        builtProductsDirectory
    )
    extensions.extraProperties.set(
        "${XcodeEnvironment.XCODE_ENVIRONMENT_OVERRIDE_KEY}.TARGET_BUILD_DIR",
        targetBuildDirectory,
    )
    extensions.extraProperties.set(
        "${XcodeEnvironment.XCODE_ENVIRONMENT_OVERRIDE_KEY}.FRAMEWORKS_FOLDER_PATH",
        "Frameworks"
    )
    extensions.extraProperties.set(
        "${XcodeEnvironment.XCODE_ENVIRONMENT_OVERRIDE_KEY}.EXPANDED_CODE_SIGN_IDENTITY",
        "-"
    )
    extensions.extraProperties.set(
        "${XcodeEnvironment.XCODE_ENVIRONMENT_OVERRIDE_KEY}.ENABLE_USER_SCRIPT_SANDBOXING",
        "NO"
    )
}