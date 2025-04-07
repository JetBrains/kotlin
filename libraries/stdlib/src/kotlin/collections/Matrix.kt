/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

import kotlin.js.JsName

/**
 * A generic 2D matrix backed by a 1D array for memory efficiency.
 *
 * The matrix provides O(1) access to elements by row and column indices.
 *
 * @param T the type of elements contained in the matrix.
 * @property rows number of rows in the matrix.
 * @property columns number of columns in the matrix.
 * @constructor Creates a new matrix with the specified [rows] and [columns], initialized by the [init] function.
 */
@SinceKotlin("1.9")
public class Matrix<T> private constructor(
    public val rows: Int,
    public val columns: Int,
    private val data: Array<Any?>
) {
    /**
     * Creates a new matrix with the specified [rows] and [columns], initialized by the [init] function.
     *
     * @throws IllegalArgumentException if [rows] or [columns] are negative.
     */
    public constructor(
        rows: Int,
        columns: Int,
        init: (Int, Int) -> T
    ) : this(
        rows,
        columns,
        Array(rows * columns) { i -> init(i / columns, i % columns) }
    ) {
        require(rows >= 0) { "Rows must be non-negative: $rows" }
        require(columns >= 0) { "Columns must be non-negative: $columns" }
    }

    /**
     * Returns the element at the specified [row] and [column] in the matrix.
     *
     * @throws IndexOutOfBoundsException if [row] or [column] is out of bounds.
     */
    @Suppress("UNCHECKED_CAST")
    public operator fun get(row: Int, column: Int): T {
        checkIndices(row, column)
        return data[index(row, column)] as T
    }

    /**
     * Sets the element at the specified [row] and [column] in the matrix to the given [value].
     *
     * @throws IndexOutOfBoundsException if [row] or [column] is out of bounds.
     */
    public operator fun set(row: Int, column: Int, value: T) {
        checkIndices(row, column)
        data[index(row, column)] = value
    }

    private fun index(row: Int, column: Int): Int = row * columns + column

    private fun checkIndices(row: Int, column: Int) {
        if (row !in 0 until rows) throw IndexOutOfBoundsException("Row index out of range: $row")
        if (column !in 0 until columns) throw IndexOutOfBoundsException("Column index out of range: $column")
    }

    /**
     * Returns a string representation of the matrix.
     */
    override fun toString(): String = buildString {
        for (r in 0 until rows) {
            for (c in 0 until columns) {
                append(get(r, c)).append('\t')
            }
            appendLine()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Matrix<*>) return false
        if (rows != other.rows || columns != other.columns) return false

        for (r in 0 until rows) {
            for (c in 0 until columns) {
                if (get(r, c) != other.get(r, c)) return false
            }
        }
        return true
    }

    override fun hashCode(): Int {
        var result = rows
        result = 31 * result + columns
        for (element in data) {
            result = 31 * result + (element?.hashCode() ?: 0)
        }
        return result
    }

    /**
     * Returns new array of type `Array<Any?>` with the elements of this matrix.
     */
    @JsName("toArray")
    protected open fun toArray(): Array<Any?> = data.copyOf()

    /**
     * Creates an iterator over the elements of the matrix in row-major order.
     */
    public operator fun iterator(): Iterator<T> = object : Iterator<T> {
        private var index = 0
        override fun hasNext(): Boolean = index < data.size
        override fun next(): T {
            if (!hasNext()) throw NoSuchElementException()
            @Suppress("UNCHECKED_CAST")
            return data[index++] as T
        }
    }
}