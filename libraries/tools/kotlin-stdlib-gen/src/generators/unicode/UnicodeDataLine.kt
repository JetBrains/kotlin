/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode

internal class UnicodeDataLine(properties: List<String>) {
    init {
        require(properties.size == 15)
    }

    val char: String = properties[0]
    val name: String = properties[1]
    val categoryCode: String = properties[2]
    val uppercaseMapping: String = properties[12]
    val lowercaseMapping: String = properties[13]
    val titlecaseMapping: String = properties[14]

    override fun toString(): String {
        return "UnicodeDataLine{char=$char" +
                ", categoryCode=$categoryCode" +
                ", uppercaseMapping=$uppercaseMapping" +
                ", lowercaseMapping=$lowercaseMapping" +
                ", titlecaseMapping=$titlecaseMapping" +
                ", name=$name" +
                "}"
    }
}