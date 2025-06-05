/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.gcfuzzing.dsl

import org.jetbrains.kotlin.konan.test.gcfuzzing.translation.CInteropConfig
import org.jetbrains.kotlin.konan.test.gcfuzzing.translation.KotlinConfig
import org.jetbrains.kotlin.konan.test.gcfuzzing.translation.ObjCConfig
import org.jetbrains.kotlin.konan.test.gcfuzzing.translation.headerFilename
import org.jetbrains.kotlin.konan.test.gcfuzzing.translation.produceCInterop
import org.jetbrains.kotlin.konan.test.gcfuzzing.translation.produceKotlin
import org.jetbrains.kotlin.konan.test.gcfuzzing.translation.produceObjC

fun Program.translate(): Output {
    val cinteropConfig = CInteropConfig(moduleName = "cinterop")
    val kotlinConfig = KotlinConfig(
        cinteropModuleName = cinteropConfig.moduleName,
        maximumStackDepth = 100,
        kotlinFrameworkName = "KotlinObjCFramework"
    )
    val objcConfig = ObjCConfig(
        cinteropHeaderFilename = cinteropConfig.headerFilename,
        kotlinHeaderFilename = "${kotlinConfig.kotlinFrameworkName}.h",
        kotlinIdentifierPrefix = "KOCF",
        kotlinGlobalClass = "KOCFLibKt",
        maximumStackDepth = kotlinConfig.maximumStackDepth,
    )
    val cinterop = produceCInterop(cinteropConfig)
    val kotlin = produceKotlin(kotlinConfig)
    val objc = produceObjC(objcConfig)
    return Output(kotlin, cinterop, objc)
}