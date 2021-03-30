/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import org.jetbrains.kotlin.backend.common.serialization.cityHash64

@OptIn(ExperimentalUnsignedTypes::class)
internal fun localHash(data: ByteArray): Long {
    return cityHash64(data).toLong()
}

internal class LocalHash(val value: Long) : ConstValue by Int64(value)
