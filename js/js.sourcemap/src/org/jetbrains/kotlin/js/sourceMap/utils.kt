/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.sourceMap

import org.jetbrains.kotlin.js.util.TextOutput
import java.io.File

fun TextOutput.addSourceMappingURL(outputJsFile: File) {
    print("\n//# sourceMappingURL=")
    print(outputJsFile.name)
    print(".map\n")
}
