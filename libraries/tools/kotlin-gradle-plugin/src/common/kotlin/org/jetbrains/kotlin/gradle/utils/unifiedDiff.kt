/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import java.io.File

/**
 * Builds a unified diff between [expected] and [actual] files, ignoring line ending differences.
 *
 * @see unifiedDiff
 */
internal fun unifiedDiff(
    expected: File,
    actual: File,
    contextLines: Int = 3,
    maxOutputLines: Int = 200,
): String = unifiedDiff(
    expectedLines = expected.readLines(),
    actualLines = actual.readLines(),
    expectedName = expected.path,
    actualName = actual.path,
    contextLines = contextLines,
    maxOutputLines = maxOutputLines,
)

/**
 * Builds a line-based unified diff (`diff -u` like format) between [expectedLines] and [actualLines].
 *
 * Returns an empty string if there is no difference.
 * Output is truncated to [maxOutputLines] lines (headers included).
 */
internal fun unifiedDiff(
    expectedLines: List<String>,
    actualLines: List<String>,
    expectedName: String,
    actualName: String,
    contextLines: Int = 3,
    maxOutputLines: Int = 200,
): String {
    require(contextLines >= 0) { "contextLines must be non-negative, but was $contextLines" }
    require(maxOutputLines > 0) { "maxOutputLines must be positive, but was $maxOutputLines" }

    val edits = computeEdits(expectedLines, actualLines)
    if (edits.none { it.type != EditType.EQUAL }) return ""

    val output = mutableListOf<String>()
    output += "--- $expectedName"
    output += "+++ $actualName"

    for (hunk in groupIntoHunks(edits, contextLines)) {
        val first = edits[hunk.first]
        val expectedCount = hunk.count { edits[it].type != EditType.INSERT }
        val actualCount = hunk.count { edits[it].type != EditType.DELETE }
        val expectedStart = if (expectedCount == 0) first.expectedIndex else first.expectedIndex + 1
        val actualStart = if (actualCount == 0) first.actualIndex else first.actualIndex + 1
        output += "@@ -$expectedStart,$expectedCount +$actualStart,$actualCount @@"
        for (i in hunk) {
            val edit = edits[i]
            output += when (edit.type) {
                EditType.EQUAL -> " " + expectedLines[edit.expectedIndex]
                EditType.DELETE -> "-" + expectedLines[edit.expectedIndex]
                EditType.INSERT -> "+" + actualLines[edit.actualIndex]
            }
        }
    }

    val result = if (output.size > maxOutputLines) {
        output.subList(0, maxOutputLines) + "... (${output.size - maxOutputLines} more lines)"
    } else {
        output
    }
    return result.joinToString("\n")
}

private enum class EditType { EQUAL, DELETE, INSERT }

/**
 * [expectedIndex] and [actualIndex] are positions in the corresponding sequences at the moment of this edit.
 */
private class Edit(val type: EditType, val expectedIndex: Int, val actualIndex: Int)

/**
 * Upper bound of the edit distance for which Myers' algorithm is used.
 * Memory consumption of backtracking is O(D^2), so for larger distances
 * a trivial "delete everything, insert everything" script is produced instead.
 */
private const val MAX_EDIT_DISTANCE = 2000

/**
 * Myers' O((N+M)D) difference algorithm.
 */
private fun computeEdits(a: List<String>, b: List<String>): List<Edit> {
    val n = a.size
    val m = b.size
    val max = n + m
    if (max == 0) return emptyList()

    val offset = max + 1
    val v = IntArray(2 * max + 3)
    // trace[d] keeps the state of `v` for diagonals in [-(d + 1), d + 1] before step d
    val trace = mutableListOf<IntArray>()

    for (d in 0..minOf(max, MAX_EDIT_DISTANCE)) {
        trace += v.copyOfRange(offset - d - 1, offset + d + 2)
        for (k in -d..d step 2) {
            var x = if (k == -d || (k != d && v[offset + k - 1] < v[offset + k + 1])) {
                v[offset + k + 1]
            } else {
                v[offset + k - 1] + 1
            }
            var y = x - k
            while (x < n && y < m && a[x] == b[y]) {
                x++
                y++
            }
            v[offset + k] = x
            if (x >= n && y >= m) {
                return backtrack(trace, n, m)
            }
        }
    }

    return List(n) { Edit(EditType.DELETE, it, 0) } + List(m) { Edit(EditType.INSERT, n, it) }
}

private fun backtrack(trace: List<IntArray>, n: Int, m: Int): List<Edit> {
    val edits = ArrayList<Edit>()
    var x = n
    var y = m
    for (d in trace.indices.reversed()) {
        val v = trace[d]
        fun at(k: Int) = v[k + d + 1]

        val k = x - y
        val prevK = if (k == -d || (k != d && at(k - 1) < at(k + 1))) k + 1 else k - 1
        val prevX = at(prevK)
        val prevY = prevX - prevK

        while (x > prevX && y > prevY) {
            x--
            y--
            edits += Edit(EditType.EQUAL, x, y)
        }
        if (d > 0) {
            if (x == prevX) {
                y--
                edits += Edit(EditType.INSERT, x, y)
            } else {
                x--
                edits += Edit(EditType.DELETE, x, y)
            }
        }
        x = prevX
        y = prevY
    }
    edits.reverse()
    return edits
}

/**
 * Groups indices of [edits] into hunks: changed edits surrounded by up to [contextLines] equal edits.
 * Changes separated by not more than `2 * contextLines` equal lines are merged into one hunk.
 */
private fun groupIntoHunks(edits: List<Edit>, contextLines: Int): List<IntRange> {
    val changeIndices = edits.indices.filter { edits[it].type != EditType.EQUAL }
    val hunks = mutableListOf<IntRange>()
    var start = changeIndices.first()
    var end = start
    for (index in changeIndices.drop(1)) {
        if (index - end - 1 > 2 * contextLines) {
            hunks += maxOf(0, start - contextLines)..minOf(edits.lastIndex, end + contextLines)
            start = index
        }
        end = index
    }
    hunks += maxOf(0, start - contextLines)..minOf(edits.lastIndex, end + contextLines)
    return hunks
}
