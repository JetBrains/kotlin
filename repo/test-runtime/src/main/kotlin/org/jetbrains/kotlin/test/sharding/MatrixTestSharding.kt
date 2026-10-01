/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.sharding

/**
 * Assigns the rows of a test matrix (one row per invocation, one value per argument) to shards,
 * assuming that every invocation takes the same time.
 *
 * The rows are sorted by their values, column by column (see [compareKeys]), and laid out next to each other on the line `[0, totalShards)`,
 * each row taking an equal part of it. Shard `s` covers `[s - 1, s)` of the line.
 * Every row is placed at the same relative offset within its part, and the offset is derived from [salt].
 *
 * - Balance: each shard receives `floor(rows / totalShards)` or `ceil(rows / totalShards)` rows.
 * - Locality: rows sharing their leading values (e.g., the same first value) are adjacent, so they occupy neighbouring shards.
 *   The same set of rows is placed on the same range of shards in every matrix with the same [totalShards],
 *   and on the very same shards in every matrix with the same [salt] as well.
 * - Different salts (e.g., different test classes, or different test templates when sharding by method) shift the rows within their parts,
 *   so matrices with fewer rows than shards still reach all shards of their range.
 *
 * The shard of a row depends on its values, not on its position in the matrix (identical rows are placed next to each other).
 * The values must therefore be identical on all shards: either [Comparable] or with a stable `toString()`.
 * Changing [totalShards] moves most of the rows.
 *
 * Example: 4 rows `(A, x)`, `(A, y)`, `(B, x)`, `(B, y)` on 3 shards.
 * Each row takes a part of length `3 / 4` of the line `[0, 3)`:
 *
 * ```
 *   rows:   0        0.75     1.5      2.25     3
 *           |  A/x   |  A/y   |  B/x   |  B/y   |
 *   shards: |     1     |     2     |     3     |
 *           0           1           2           3
 * ```
 *
 * With an offset of `0.5` (half of each part), the points are `0.375`, `1.125`, `1.875` and `2.625`,
 * so the rows land on the shards 1, 2, 2 and 3. The rows with the first value `A` stay on the shards 1 and 2,
 * the rows with `B` on the shards 2 and 3: only shard 2, at the border of both groups, receives both.
 * Another salt shifts all four points by the same amount, e.g., an offset of `0.1` gives the shards 1, 1, 2 and 3.
 */
internal class MatrixTestSharding(
    matrix: List<List<Any?>>,
    salt: ByteArray = byteArrayOf(),
    totalShards: Int,
    shardSeed: Int = 0,
) {

    val rows = matrix.size

    init {
        if (totalShards < 1) error("Invalid 'totalShards': $totalShards; Expected >= 1")
        if (rows == 0) error("Matrix should have at least one row")
        /*
         * The placement below uses fixed-point numbers with 32 fractional bits in a Long.
         * Keeping rows * totalShards below 2^31 keeps all intermediate values below 2^63, so they cannot overflow.
         */
        if (rows.toLong() * totalShards > Int.MAX_VALUE) error("Matrix is too large: $rows rows on $totalShards shards")
    }

    val columns = matrix[0].size

    init {
        if (columns == 0) error("Matrix should have at least one column")
        for (row in matrix) {
            if (row.size != columns) error("Matrix should have only size of $columns columns")
        }
    }

    /* Shard (1-based) of each row, in the order of the given matrix */
    private val rowShards = IntArray(rows)

    init {
        /*
         * The relative position of each row's point within its part, as the fraction offset / 2^32 (in [0, 1)).
         * It is derived from the salt only, so it is the same for all rows of this matrix:
         * shifting all points by the same amount keeps them equally spaced, which is what keeps the shards balanced.
         * A random offset per row would instead let points pile up on one shard.
         * The upper 32 bits of the hash are used as a uniformly distributed fraction.
         * calculateHash includes the seed ('tests.shardSeed'), so changing the seed shifts the rows as well.
         */
        val offset = (calculateHash(salt, shardSeed) shr 32).toLong()

        /*
         * Sort the rows by their values, so that
         * - the placement does not depend on the order in which the rows were provided, and
         * - rows sharing their leading values (e.g., the same first value) are adjacent and therefore land on neighbouring shards.
         * Only the adjacency matters: the order itself (e.g., the string "10" sorting before "9") is irrelevant.
         * Sorting is stable, so identical rows keep their relative order.
         */
        val sortedRows = (0 until rows).sortedWith { a, b -> compareRows(matrix[a], matrix[b]) }

        sortedRows.forEachIndexed { position, row ->
            /*
             * The row at 'position' covers the part [position, position + 1) * totalShards / rows of the line [0, totalShards).
             * Its point is at the offset within that part:
             *     point = (position + offset / 2^32) * totalShards / rows
             * Shard s covers [s - 1, s), so the (1-based) shard of the point is floor(point) + 1.
             *
             * To stay in integer arithmetic, the numerator is computed with 32 fractional bits:
             *     numerator   = (position * 2^32 + offset) * totalShards
             *     denominator = rows * 2^32
             * Both are below 2^63 because rows * totalShards < 2^31 (see the check above).
             * Integer division rounds down, which gives floor(point) without any floating-point rounding issues.
             */
            val numerator = ((position.toLong() * totalShards) shl 32) + (offset * totalShards)
            val denominator = rows.toLong() shl 32
            rowShards[row] = (numerator / denominator).toInt() + 1
        }

        /*
         * Why this is balanced: consecutive points are exactly totalShards / rows apart,
         * and every shard is a part of length 1 of the line.
         * A part of length 1 contains either floor(rows / totalShards) or ceil(rows / totalShards) of such equally spaced points.
         */
    }

    /**
     * Returns the shard (1-based) of the row at index [row] of the matrix passed to the constructor.
     */
    fun getShardOfRow(row: Int): Int {
        return rowShards[row]
    }

    /**
     * Compares two rows by their values, column by column (lexicographically), like sorting by the first value,
     * then by the second value, and so on. This is what groups rows with the same leading values.
     */
    private fun compareRows(a: List<Any?>, b: List<Any?>): Int {
        for (column in 0 until columns) {
            val result = compareKeys(a[column], b[column])
            if (result != 0) return result
        }
        return 0
    }

    /**
     * Compares two values of the same column:
     * - `null` before any other value,
     * - as equal, if they are equal,
     * - by their natural order, if one of them is [Comparable] and the other one is an instance of its class
     *   (for enum constants, of their enum class, so that constants with a body are ordered by their ordinal as well),
     * - otherwise, by their `toString()`.
     * The order only depends on the values, so it is identical on all shards and machines,
     * as long as the values are [Comparable] or have a stable `toString()` (not an identity-based one).
     */
    private fun compareKeys(a: Any?, b: Any?): Int {
        if (a == null && b == null) return 0
        if (a == null) return -1
        if (b == null) return 1

        /* Use regular equality */
        if (a == b) return 0

        /* Respect types which are inherently comparable */
        @Suppress("UNCHECKED_CAST")
        if (a is Comparable<*> && a.comparableClass.isInstance(b)) {
            return (a as Comparable<Any?>).compareTo(b as Comparable<*>)
        }

        @Suppress("UNCHECKED_CAST")
        if (b is Comparable<*> && b.comparableClass.isInstance(a)) {
            return -(b as Comparable<Any?>).compareTo(a)
        }

        /* Tie-breaker: Use string representation */
        return a.toString().compareTo(b.toString())
    }

    /**
     * The class whose instances can be compared with this value by its natural order.
     * An enum constant with a body is an instance of an anonymous subclass of its enum class,
     * yet it is comparable to all constants of the enum class.
     */
    private val Any.comparableClass: Class<*>
        get() = if (this is Enum<*>) declaringJavaClass else javaClass
}
