/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package library.sample

import kotlin.js.Date

public fun pairAdd(p: Pair<Int, Int>): Int = p.first + p.second

public fun pairMul(p: Pair<Int, Int>): Int = p.first * p.second

public data class IntHolder(val value: Int)

public fun Date.extFun(): Int = 1000