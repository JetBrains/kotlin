/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.phases

import org.jetbrains.kotlin.backend.konan.serialization.KonanIdSignaturer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.konan.file.use

interface Resource<T> {
    val value: T

    fun close()
}

inline fun <T, R> Resource<T>.use(block: (T) -> R): R {
    try {
        return block(value)
    } finally {
        close()
    }
}

class SymbolTableResource : Resource<SymbolTable> {
    override val value: SymbolTable by lazy {
        SymbolTable(KonanIdSignaturer(KonanManglerDesc), IrFactoryImpl)
    }

    override fun close() {
        // TODO: Invalidate
    }
}

