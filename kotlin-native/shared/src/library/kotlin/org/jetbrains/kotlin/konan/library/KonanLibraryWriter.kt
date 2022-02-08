/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.library.BaseWriter
import org.jetbrains.kotlin.library.IrWriter
import org.jetbrains.kotlin.library.MetadataWriter

interface TargetedWriter {
    fun addIncludedBinary(library: String)
}

interface BitcodeWriter : TargetedWriter {
    fun addNativeBitcode(library: String)
}

interface KonanLibraryWriter : MetadataWriter, BaseWriter, IrWriter, BitcodeWriter