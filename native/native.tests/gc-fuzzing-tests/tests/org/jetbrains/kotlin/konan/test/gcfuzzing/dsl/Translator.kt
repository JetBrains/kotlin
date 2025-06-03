/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.gcfuzzing.dsl

import org.jetbrains.kotlin.konan.test.gcfuzzing.translation.CInteropConfig
import org.jetbrains.kotlin.konan.test.gcfuzzing.translation.CInteropOutput
import org.jetbrains.kotlin.konan.test.gcfuzzing.translation.KotlinConfig
import org.jetbrains.kotlin.konan.test.gcfuzzing.translation.KotlinOutput
import org.jetbrains.kotlin.konan.test.gcfuzzing.translation.ObjCConfig
import org.jetbrains.kotlin.konan.test.gcfuzzing.translation.ObjCOutput
import org.jetbrains.kotlin.konan.test.gcfuzzing.translation.headerFilename
import org.jetbrains.kotlin.konan.test.gcfuzzing.translation.produceCInterop
import org.jetbrains.kotlin.konan.test.gcfuzzing.translation.produceKotlin
import org.jetbrains.kotlin.konan.test.gcfuzzing.translation.produceObjC
import java.io.File

class Config(
    val maximumStackDepth: Int,
    val mainLoopRepeatCount: Int,
    val maxThreadCount: Int,
) {
    companion object {
        val DEFAULT = Config(
            maximumStackDepth = 500,
            mainLoopRepeatCount = 100000,
            maxThreadCount = 100,
        )
    }
}

class Output(
    val kotlin: KotlinOutput,
    val cinterop: CInteropOutput,
    val objc: ObjCOutput,
) {
    fun save(root: File) {
        root.mkdirs()
        root.resolve(kotlin.filename).writeText(kotlin.contents)
        root.resolve(cinterop.defFilename).writeText(cinterop.defContents)
        root.resolve(cinterop.headerFilename).writeText(cinterop.headerContents)
        root.resolve(objc.filename).writeText(objc.contents)
    }
}

fun Program.translate(config: Config = Config.DEFAULT): Output {
    val cinteropConfig = CInteropConfig(moduleName = "cinterop")
    val kotlinConfig = KotlinConfig(
        cinteropModuleName = cinteropConfig.moduleName,
        maximumStackDepth = config.maximumStackDepth,
        moduleName = "ktlib"
    )
    val kotlinModuleCapitalized = kotlinConfig.moduleName.replaceFirstChar { it.uppercase() }
    val objcConfig = ObjCConfig(
        cinteropHeaderFilename = cinteropConfig.headerFilename,
        kotlinHeaderFilename = "${kotlinConfig.moduleName}.h",
        kotlinIdentifierPrefix = kotlinModuleCapitalized,
        kotlinGlobalClass = "${kotlinModuleCapitalized}Kt",
        maximumStackDepth = config.maximumStackDepth,
        maximumThreadCount = config.maxThreadCount,
        mainLoopRepeatCount = config.mainLoopRepeatCount,
        basename = "main"
    )
    val cinterop = produceCInterop(cinteropConfig)
    val kotlin = produceKotlin(kotlinConfig)
    val objc = produceObjC(objcConfig)
    return Output(kotlin, cinterop, objc)
}