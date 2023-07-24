/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.tcs.extras

import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryDependency
import org.jetbrains.kotlin.tooling.core.extrasKeyOf
import org.jetbrains.kotlin.tooling.core.readWriteProperty

val isIdeaProjectLevelKey = extrasKeyOf<Boolean>("isIdeaProjectLevel")

/**
 * Marks any binary dependency as Global in the sense of "this library is considered
 * the same, no matter in which Gradle project / IntelliJ module it will be used
 */
var IdeaKotlinBinaryDependency.isIdeaProjectLevel by isIdeaProjectLevelKey.readWriteProperty.notNull(true)


val isNativeDistributionKey = extrasKeyOf<Boolean>("isNativeDistribution")


/**
 * Marks this dependency as 'coming from the native distribution'
 */
var IdeaKotlinBinaryDependency.isNativeDistribution by isNativeDistributionKey.readWriteProperty.notNull(false)


val isNativeStdlibKey = extrasKeyOf<Boolean>("isNativeStdlib")

/**
 * Marks the dependency as the native stdlib (which is special in the native distribution)
 */
var IdeaKotlinBinaryDependency.isNativeStdlib by isNativeStdlibKey.readWriteProperty.notNull(false)


val isCommonizedKey = extrasKeyOf<Boolean>("isCommonized")

/**
 * Marks dependencies produced by the commonizer
 */
var IdeaKotlinBinaryDependency.isCommonized by isCommonizedKey.readWriteProperty.notNull(false)


/**
 * @see isOpaqueFileDependencyKey
 */
val isOpaqueFileDependencyKey = extrasKeyOf<Boolean>("isOpaqueFileDependencyKey")

/**
 * Marks a dependency as 'opaque' (meaning that we cannot really know about the actual binary coordiantes)
 *
 * Example: File dependencies are 'opaque'
 * ```kotlin
 *    kotlin {
 *        sourceSets.jvmMain.dependencies {
 *            implementation(files("libs/foo.jar")) // <- OPAQUE
 *            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2") // <- NOT OPAQUE
 *        }
 *    }
 * ```
 */
var IdeaKotlinBinaryDependency.isOpaqueFileDependency by isOpaqueFileDependencyKey.readWriteProperty.notNull(false)