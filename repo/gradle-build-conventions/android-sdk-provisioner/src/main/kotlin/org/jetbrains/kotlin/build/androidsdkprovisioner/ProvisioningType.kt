/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.androidsdkprovisioner

internal enum class ProvisionedFileType {
    FILE,
    DIRECTORY,
}

enum class ProvisioningType(
    val defaultSystemPropertyName: String,
    internal val type: ProvisionedFileType,
    internal val configurationName: String,
) {
    SDK("android.sdk", ProvisionedFileType.DIRECTORY, "androidSdk"),
    PLATFORM_JAR("android.jar", ProvisionedFileType.FILE, "androidJar"),
    SDK_WITH_EMULATOR("android.sdk", ProvisionedFileType.DIRECTORY, "androidEmulator"),
}