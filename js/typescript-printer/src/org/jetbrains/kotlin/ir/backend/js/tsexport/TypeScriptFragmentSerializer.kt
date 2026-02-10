/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.tsexport

import java.io.File
import kotlin.io.writeText

public fun serializeTypeScriptFragment(input: File, fragment: TypeScriptDefinitionsFragment?) {
    if (fragment != null) {
        input.writeText(fragment.raw)
    } else {
        input.delete()
    }
}
