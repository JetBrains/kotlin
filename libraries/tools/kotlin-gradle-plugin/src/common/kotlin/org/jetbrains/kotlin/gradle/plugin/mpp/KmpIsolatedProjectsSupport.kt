/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

/**
 * Enum representing the support level for Gradle Isolated Projects in Kotlin Multiplatform Gradle Plugin.
 *
 * The older version of Kotlin didn't support Isolated Projects.
 * And enabling support requires serious behavioral and structural changes in Kotlin Multiplatform Gradle Model.
 * I.e. some tasks, configurations, and other entities managed by Kotlin Gradle Plugin may behave and look different
 * when Isolated Projects support is enabled.
 *
 * The support modes are:
 *
 * - [ENABLE] (default mode): Explicitly enable support for Isolated Projects.
 * When Isolated Project support is enabled Kotlin Gradle Plugin will apply changes to its model to be compatible with Isolated Projects.
 * Please note that Isolated Projects still should be enabled on Gradle side.
 * Use this mode if you want to prepare your build scripts for migration to Isolated Projects friendly model.
 *
 * - [DISABLE]: Explicitly disable support for Isolated Projects. This mode keeps the Kotlin Multiplatform Gradle Plugin Model as it is.
 * Please note, in this mode Kotlin Gradle Plugin can't work with Isolated Projects.
 *
 * - [AUTO]: Automatically enables support for Isolated Projects feature when it is enabled on Gradle side.
 *
 * @since 2.1
 */
enum class KmpIsolatedProjectsSupport {
    ENABLE,
    DISABLE,
    AUTO;
}