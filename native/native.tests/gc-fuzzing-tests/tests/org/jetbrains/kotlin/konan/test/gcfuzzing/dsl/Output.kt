/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.gcfuzzing.dsl

import org.jetbrains.kotlin.konan.test.gcfuzzing.translation.CInteropOutput
import org.jetbrains.kotlin.konan.test.gcfuzzing.translation.KotlinOutput
import org.jetbrains.kotlin.konan.test.gcfuzzing.translation.ObjCOutput
import java.io.File

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