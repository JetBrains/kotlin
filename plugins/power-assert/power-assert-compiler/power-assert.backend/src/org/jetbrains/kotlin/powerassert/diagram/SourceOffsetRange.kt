/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert.diagram

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

class SourceOffsetRange(
    override val start: Int,
    val end: Int,
) : ClosedRange<Int> {
    override val endInclusive: Int
        get() = end

    fun expand(other: SourceOffsetRange): SourceOffsetRange {
        if (other.start < 0) return this
        return SourceOffsetRange(
            start = minOf(start, other.start),
            end = maxOf(end, other.end)
        )
    }
}

private var IrElement.sourceRangeAttribute: SourceOffsetRange? by irAttribute(copyByDefault = false)
val IrElement.sourceRange: SourceOffsetRange
    get() {
        sourceRangeAttribute?.let { return it }

        var range = SourceOffsetRange(startOffset, endOffset)
        acceptChildrenVoid(
            object : IrVisitorVoid() {
                override fun visitElement(element: IrElement) {
                    range = range.expand(element.sourceRange)
                }
            },
        )

        sourceRangeAttribute = range
        return range
    }
