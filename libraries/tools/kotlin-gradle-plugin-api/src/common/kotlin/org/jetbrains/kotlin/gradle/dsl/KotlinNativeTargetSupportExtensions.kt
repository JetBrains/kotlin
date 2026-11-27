/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHost
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeTargetSupport
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.buildtools.api.arguments.enums.KonanTarget as BuildToolsKonanTarget

/**
 * The Kotlin/Native target of the [host] as a [KonanTarget], or `null` if the host is not supported.
 * See [KotlinNativeTargetSupport.target].
 *
 * @since 2.5.20
 */
@ExperimentalKotlinGradlePluginApi
fun KotlinNativeTargetSupport.konanTarget(host: KotlinNativeHost): KonanTarget? =
    target(host)?.let { KonanTarget.predefinedTargets[it.stringValue] }

/**
 * Whether the [host] can link binaries for the [target]. See [KotlinNativeTargetSupport.canLinkBinaries].
 *
 * @since 2.5.20
 */
@ExperimentalKotlinGradlePluginApi
fun KotlinNativeTargetSupport.canLinkBinaries(host: KotlinNativeHost, target: KonanTarget): Boolean =
    BuildToolsKonanTarget.entries.firstOrNull { it.stringValue == target.name }?.let { canLinkBinaries(host, it) } == true
