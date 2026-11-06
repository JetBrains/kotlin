/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode

internal data class PropertyLine(
    val rangeStart: String,
    val rangeEnd: String,
    val property: String,
) {

    fun intRange(): IntRange {
        return rangeStart.hexToInt()..rangeEnd.hexToInt()
    }

    fun hexIntRangeLiteral(): String {
        return "${rangeStart.hexToInt().toHexIntLiteral()}..${rangeEnd.hexToInt().toHexIntLiteral()}"
    }

    companion object {
        fun parse(line: String): PropertyLine {
            val properties = line.substringBefore('#').split(";").map { it.trim() }
            require(properties.size in 2..3) { "Invalid property line: $line" }
            val range = properties[0].split("..")
            return PropertyLine(range.first(), range.last(), properties[1])
        }
    }
}
