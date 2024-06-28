/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.platform.wasm

import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.toTargetPlatform
import org.jetbrains.kotlin.platform.WasmPlatform as CoreWasmPlatform

abstract class WasmPlatform(platformName: String) : CoreWasmPlatform(platformName) {
    override val oldFashionedDescription: String
        get() = "Wasm"
}

object WasmPlatformUnspecifiedTarget : WasmPlatform("Wasm") {
    override val targetName: String
        get() = "general"
}

class WasmPlatformWithTarget(val target: WasmTarget) : WasmPlatform(target.alias) {
    override val targetName: String
        get() = target.name
}

object WasmPlatforms {
    private val platforms: Map<WasmTarget, TargetPlatform> =
        WasmTarget.entries.associateWith { WasmPlatformWithTarget(it).toTargetPlatform() }

    @Suppress("DEPRECATION_ERROR")
    val unspecifiedWasmPlatform: TargetPlatform
        get() = Default

    val wasmJs = platforms[WasmTarget.JS]!!
    val wasmWasi = platforms[WasmTarget.WASI]!!

    fun wasmPlatformByTargetVersion(targetVersion: WasmTarget): TargetPlatform =
        platforms[targetVersion]!!

    val allWasmPlatforms: List<TargetPlatform> = listOf(unspecifiedWasmPlatform) + platforms.values

    fun wasmPlatformByTargets(targets: Collection<WasmTarget>): TargetPlatform {
        val platforms = targets.map { wasmPlatformByTargetVersion(it) }
        return when (platforms.size) {
            0 -> wasmJs
            1 -> platforms.first()
            else -> TargetPlatform(platforms.flatMap { it.componentPlatforms }.toSet())
        }
    }

    fun wasmPlatformByTargetNames(targets: Collection<String>): TargetPlatform =
        wasmPlatformByTargets(targets.mapNotNull { WasmTarget.fromName(it) })

    object Default : TargetPlatform(setOf(WasmPlatformUnspecifiedTarget))
}

fun TargetPlatform?.isWasm(): Boolean = this?.singleOrNull() is WasmPlatform

fun TargetPlatform?.isWasmJs(): Boolean {
    val platform = this?.singleOrNull()
    return platform is WasmPlatformWithTarget && platform.target == WasmTarget.JS
}
fun TargetPlatform?.isWasmWasi(): Boolean {
    val platform = this?.singleOrNull()
    return platform is WasmPlatformWithTarget && platform.target == WasmTarget.WASI
}