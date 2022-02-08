/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode

internal class PropertyLine(properties: List<String>) {
    init {
        require(properties.size == 2)
    }

    val rangeStart: String = properties[0].split("..").first()
    val rangeEnd: String = properties[0].split("..").last()
    val property: String = properties[1].takeWhile { it != ' ' }

    fun intRange(): IntRange {
        return rangeStart.hexToInt()..rangeEnd.hexToInt()
    }

    fun hexIntRangeLiteral(): String {
        return "${rangeStart.hexToInt().toHexIntLiteral()}..${rangeEnd.hexToInt().toHexIntLiteral()}"
    }

    override fun toString(): String {
        return "PropertyLine{rangeStart=$rangeStart" +
                ", rangeEnd=$rangeEnd" +
                ", property=$property" +
                "}"
    }
}
