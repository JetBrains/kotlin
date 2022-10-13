/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.compilation

import org.jetbrains.kotlin.native.interop.gen.jvm.InternalInteropOptions
import org.jetbrains.kotlin.native.interop.gen.jvm.interop
import java.io.File

internal fun invokeCInterop(inputDef: File, outputLib: File, extraArgs: Array<String>): Array<String>? {
    val args = arrayOf("-o", outputLib.canonicalPath, "-def", inputDef.canonicalPath)
    val buildDir = org.jetbrains.kotlin.konan.file.File("${outputLib.canonicalPath}-build")
    val generatedDir = org.jetbrains.kotlin.konan.file.File(buildDir, "kotlin")
    val nativesDir = org.jetbrains.kotlin.konan.file.File(buildDir, "natives")
    val manifest = org.jetbrains.kotlin.konan.file.File(buildDir, "manifest.properties")
    val cstubsName = "cstubs"

    return interop(
        "native",
        args + extraArgs,
        InternalInteropOptions(generatedDir.absolutePath, nativesDir.absolutePath, manifest.path, cstubsName),
        false
    )
}
