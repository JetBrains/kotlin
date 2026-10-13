/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHost
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.konan.target.KonanTarget

/**
 * [KotlinNativeHost.targetName] as a [KonanTarget], or `null` if the host is not supported.
 *
 * @since 2.5.20
 */
@ExperimentalKotlinGradlePluginApi
val KotlinNativeHost.konanTarget: KonanTarget?
    get() = targetName?.let { KonanTarget.predefinedTargets[it] }

/**
 * Whether this host can link binaries for [target]. See [KotlinNativeHost.canLinkBinaries].
 *
 * @since 2.5.20
 */
@ExperimentalKotlinGradlePluginApi
fun KotlinNativeHost.canLinkBinaries(target: KonanTarget): Boolean = canLinkBinaries(target.name)

/**
 * Whether binaries of [target] can run on this host. See [KotlinNativeHost.canRunBinaries].
 *
 * @since 2.5.20
 */
@ExperimentalKotlinGradlePluginApi
fun KotlinNativeHost.canRunBinaries(target: KonanTarget): Boolean = canRunBinaries(target.name)
