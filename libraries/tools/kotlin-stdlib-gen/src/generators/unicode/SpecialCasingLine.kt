/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode

internal data class SpecialCasingLine(
    val char: String,
    val lowercaseMapping: List<String>,
    val titlecaseMapping: List<String>,
    val uppercaseMapping: List<String>,
    val conditionList: List<String>,
) {
    companion object {
        fun parse(line: String): SpecialCasingLine {
            val properties = line.substringBefore('#').split(";").map { it.trim() }
            require(properties.size in 5..6) { "Invalid special casing line: $line" }
            return SpecialCasingLine(
                char = properties[0],
                lowercaseMapping = properties[1].split(" "),
                titlecaseMapping = properties[2].split(" "),
                uppercaseMapping = properties[3].split(" "),
                conditionList = if (properties.size == 6) properties[4].split(" ") else emptyList()
            )
        }
    }
}
