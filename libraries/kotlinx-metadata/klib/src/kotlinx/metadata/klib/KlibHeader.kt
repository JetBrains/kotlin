/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.klib

import kotlin.metadata.KmAnnotation

class KlibHeader(
    val moduleName: String,
    val file: List<KlibSourceFile>,
    val packageFragmentName: List<String>,
    val emptyPackage: List<String>,
    val annotation: List<KmAnnotation>
)
