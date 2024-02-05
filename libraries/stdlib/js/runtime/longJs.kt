/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Copyright 2009 The Closure Library Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS-IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

package kotlin.js

import kotlin.internal.InlineOnly

internal fun longHashCode(l: Long): Int =
    getBigIntHashCode(l)

internal fun low(l: Long): Int =
    (l and TWO_PWR_32_MINUS_1).toInt()

internal fun high(l: Long): Int =
    ((l shr 32) and TWO_PWR_32_MINUS_1).toInt()

@Suppress("UNUSED_PARAMETER", "UNUSED_VARIABLE")
internal fun shiftRightUnsigned(value: Long, numBits: Int): Long {
    val mask = TWO_PWR_64_MINUS_1
    return js("((value & mask) >> BigInt(numBits)) & mask")
}

private val TWO_PWR_32_MINUS_1 = 0xFFFFFFFFL
private val TWO_PWR_64_MINUS_1 = js("~(BigInt(1) << BigInt(63))")