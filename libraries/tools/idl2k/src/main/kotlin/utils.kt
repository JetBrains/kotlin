package org.jetbrains.idl2k.util

import java.util.*

fun List<List<*>>.mutationsCount() = if (isEmpty()) 0 else fold(1) { acc, e -> acc * e.size }

fun <T> List<List<T>>.mutations() : List<List<T>> {
    val indices = IntArray(size)
    val sizes = map { it.size }

    fun next() : Boolean {
        var carry = 1

        for (pos in size - 1 downTo 0) {
            var index = indices[pos]
            val size = sizes[pos]

            index += carry
            carry = (index - size + 1).coerceAtLeast(0)

            if (index >= size) {
                indices[pos] = index - size
            } else {
                indices[pos] = index
                return true
            }
        }

        return carry == 0
    }

    val count = mutationsCount()
    if (count == 0) {
        return emptyList()
    }

    val result = ArrayList<List<T>>(count)
    do {
        result.add(indices.mapIndexed { pos, index -> this[pos][index]  })
    } while (next())

    return result
}

fun mapEnumConstant(entry: String) = if (entry.isEmpty()) "EMPTY" else entry.toUpperCase().replace("-", "_")