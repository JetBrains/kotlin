/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir.source.location

class SourceLocationMapping(
    // Offsets in generating binary, initialized lazily. Since blocks has as a prefix variable length number encoding its size 
    // we can't calculate absolute offsets inside those blocks until we generate whole block and generate size.
    private val offsets: List<Box>,
    val sourceLocation: SourceLocation
) {
    val offset by lazy {
        offsets.sumOf {
            assert(it.value >= 0) { "Offset must be >=0 but ${it.value}" }
            it.value
        }
    }
}
