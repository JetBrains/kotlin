/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport.sx

import org.jetbrains.kotlin.backend.konan.objcexport.SXClangModule
import org.jetbrains.kotlin.backend.konan.objcexport.SXObjCHeader
import org.jetbrains.kotlin.konan.file.File

class SXClangModuleDumper(
        private val shouldExportKDoc: Boolean,
) {
    fun dumpHeaders(
            module: SXClangModule,
            headersDirectory: File,
            umbrellaHeaderName: String,
    ) {
        val headerNames: Map<SXObjCHeader, String> = module.headers.associateWith {
            umbrellaHeaderName
        }
        module.headers.forEach {
            val headerFile = headersDirectory.child(headerNames.getValue(it))
            val headerLines = SXObjCHeaderTextRenderer(shouldExportKDoc, headerNames::getValue).render(it)
            headerFile.writeLines(headerLines)
        }
    }
}