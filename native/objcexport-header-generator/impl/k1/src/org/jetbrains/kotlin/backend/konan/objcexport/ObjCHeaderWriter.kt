/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import java.nio.file.Path
import kotlin.io.path.writeLines

// For now, the object is pretty dumb.
// Later it will accept an object with ObjC declarations instead of lines.
class ObjCHeaderWriter {
    fun write(
        headerName: String,
        headerLines: List<String>,
        headersDirectory: Path,
    ) {
        headersDirectory.resolve(headerName).writeLines(headerLines)
    }
}
