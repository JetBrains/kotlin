/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal

import kotlin.jvm.JvmField

class VArrayWrapperPerSize(
    @JvmField val ones: ByteArray?,
    @JvmField val twos: ShortArray?,
    @JvmField val fours: IntArray?,
    @JvmField val eights: LongArray?,
    @JvmField val refs: Array<Any?>?,
    @JvmField val size: Int
)

class VArrayWrapperTwoArrays(
    @JvmField val longs: LongArray?,
    @JvmField val refs: Array<Any?>?,
    @JvmField val size: Int
)