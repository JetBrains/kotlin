/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.ranges

public infix fun Int.until(to: Int): IntRange {
    TODO("stub")
}

public class IntRange(public val start: Int, public val endInclusive: Int) {
    public operator fun iterator(): Iterator<Int> {
        TODO("stub")
    }
}
