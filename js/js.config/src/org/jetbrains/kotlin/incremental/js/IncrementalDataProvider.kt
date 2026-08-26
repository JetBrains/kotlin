/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.js

import java.io.File

// byte arrays are used to simplify passing to different classloaders
interface IncrementalDataProvider {
    /** gets non-dirty package parts data from previous compilation */
    val compiledPackageParts: Map<File, TranslationResultValue>

    val serializedIrFiles: Map<File, IrTranslationResultValue>
}
