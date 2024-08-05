/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver

enum class UniqueLevel(val level: Int) {
    Bottom(0),
    Unique(1),
    BorrowedUnique(2),
    Shared(2),
    BorrowedShared(3),
    Top(4),
}

fun UniqueLevel.lessThanOrEqual(other: UniqueLevel): Boolean = this.level < other.level || this == other

fun UniqueLevel.leastUpperBound(other: UniqueLevel): UniqueLevel =
    UniqueLevel.entries.first { this.lessThanOrEqual(it) && other.lessThanOrEqual(it) }

fun Set<UniqueLevel>.leastUpperBound(): UniqueLevel = this.fold(UniqueLevel.Bottom) { acc, uniqueLevel -> acc.leastUpperBound(uniqueLevel) }

fun Set<UniqueLevel>.lessThanOrEqual(uniqueLevel: UniqueLevel): Boolean = this.leastUpperBound().lessThanOrEqual(uniqueLevel)