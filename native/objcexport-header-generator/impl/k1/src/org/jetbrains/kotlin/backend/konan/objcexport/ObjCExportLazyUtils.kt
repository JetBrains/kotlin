/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi
import org.jetbrains.kotlin.psi.KtFile
import kotlin.io.path.Path
import kotlin.io.path.writeLines

@InternalKotlinNativeApi
fun ObjCExportLazy.dumpObjCHeader(files: Collection<KtFile>, outputFile: String, shouldExportKDoc: Boolean) {
    val lines = (this.generateBase() + files.flatMap { this.translate(it) })
        .flatMap { StubRenderer.render(it, shouldExportKDoc) + listOf("") }

    Path(outputFile).writeLines(lines)
}
