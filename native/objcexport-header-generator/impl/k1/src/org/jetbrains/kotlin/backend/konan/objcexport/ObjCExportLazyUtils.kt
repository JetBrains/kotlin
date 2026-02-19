/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.psi.KtFile

@InternalKotlinNativeApi
fun ObjCExportLazy.dumpObjCHeader(files: Collection<KtFile>, outputFile: String, shouldExportKDoc: Boolean) {
    val lines = (this.generateBase() + files.flatMap { this.translate(it) })
        .flatMap { StubRenderer.render(it, shouldExportKDoc) + listOf("") }

    File(outputFile).writeLines(lines)
}