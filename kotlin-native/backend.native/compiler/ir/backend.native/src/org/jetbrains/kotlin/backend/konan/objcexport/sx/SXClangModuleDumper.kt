/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport.sx

import org.jetbrains.kotlin.konan.file.File

class SXClangModuleDumper(
        private val shouldExportKDoc: Boolean,
) {
    fun dumpHeaders(
            module: SXClangModule,
            headersDirectory: File,
    ) {
        module.headers.forEach {
            val headerFile = headersDirectory.child(it.name)
            val headerLines = SXObjCHeaderTextRenderer(shouldExportKDoc).render(it)
            headerFile.writeLines(headerLines)
        }
    }
}