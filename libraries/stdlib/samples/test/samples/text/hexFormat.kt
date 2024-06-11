/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.text

import samples.*
import kotlin.test.*

@RunWith(Enclosed::class)
class HexFormats {

    class Numbers {
        @Sample
        fun prefix() {
            // By default, `prefix` is an empty string.
            assertPrints(0x3a.toHexString(), "0000003a")
            assertPrints("0000003a".hexToInt(), "58")

            val prefixFormat = HexFormat { number.prefix = "0x" }

            // `prefix` is placed before the hex representation.
            assertPrints(0x3a.toHexString(prefixFormat), "0x0000003a")
            assertPrints("0x0000003a".hexToInt(prefixFormat), "58")

            // Parsing `prefix` is conducted in a case-insensitive manner.
            assertPrints("0X0000003a".hexToInt(prefixFormat), "58")

            // Parsing fails if the input string does not start with the specified prefix.
            assertFailsWith<IllegalArgumentException> {
                "0000003a".hexToInt(prefixFormat)
            }
        }

        @Sample
        fun suffix() {
            // By default, `suffix` is an empty string
            assertPrints(0x3a.toHexString(), "0000003a")
            assertPrints("0000003a".hexToInt(), "58")

            val suffixFormat = HexFormat { number.suffix = "h" }

            // `suffix` is placed after the hex representation
            assertPrints(0x3a.toHexString(suffixFormat), "0000003ah")
            assertPrints("0000003ah".hexToByte(suffixFormat), "58")

            // Parsing `suffix` is conducted in a case-insensitive manner
            assertPrints("0000003aH".hexToInt(suffixFormat), "58")

            // Parsing fails if the input string does not end with the specified suffix.
            assertFailsWith<IllegalArgumentException> {
                "0000003a".hexToInt(suffixFormat)
            }
        }

        @Sample
        fun removeLeadingZeros() {
            // By default, removeLeadingZeroes is `false`
            assertPrints(0x3a.toHexString(), "0000003a")

            val removeLeadingZerosFormat = HexFormat { number.removeLeadingZeros = true }

            // If there are no leading zeros, removeLeadingZeroes has no effect
            assertPrints(0x3a.toByte().toHexString(removeLeadingZerosFormat), "3a")

            // The leading zeros in the hex representation are removed until minLength is reached.
            // By default, minLength is 1.
            assertPrints(0x3a.toHexString(removeLeadingZerosFormat), "3a")
            assertPrints(0.toHexString(removeLeadingZerosFormat), "0")

            // Here minLength is set to 6.
            val shorterLengthFormat = HexFormat {
                number.removeLeadingZeros = true
                number.minLength = 6
            }
            assertPrints(0x3a.toHexString(shorterLengthFormat), "00003a")

            // When minLength is longer than the hex representation, the hex representation is padded with zeros.
            // removeLeadingZeros is ignored in this case.
            val longerLengthFormat = HexFormat {
                number.removeLeadingZeros = true
                number.minLength = 12
            }
            assertPrints(0x3a.toHexString(longerLengthFormat), "00000000003a")

            // When parsing, removeLeadingZeros is ignored
            assertPrints("0000003a".hexToInt(removeLeadingZerosFormat), "58")
        }

        @Sample
        fun minLength() {
            // By default, minLength is 1 and removeLeadingZeros is false.
            assertPrints(0x3a.toHexString(), "0000003a")

            // Specifying a minLength shorter than the hex representation with removeLeadingZeros set to false.
            assertPrints(0x3a.toHexString(HexFormat { number.minLength = 4 }), "0000003a")

            // Specifying a minLength shorter than the hex representation with removeLeadingZeros set to true.
            val shorterLengthFormat = HexFormat {
                number.removeLeadingZeros = true
                number.minLength = 4
            }
            assertPrints(0x3a.toHexString(shorterLengthFormat), "003a")
            assertPrints(0xff80ed.toHexString(shorterLengthFormat), "ff80ed")

            // Specifying a minLength longer than the hex representation.
            // removeLeadingZeros is ignored in this case.
            val longerLengthFormat = HexFormat {
                number.removeLeadingZeros = true
                number.minLength = 12
            }
            assertPrints(0x3a.toHexString(longerLengthFormat), "00000000003a")

            // When parsing, minLength is ignored.
            assertPrints("3a".hexToInt(longerLengthFormat), "58")

            // The number of hex digits can be greater than what can fit into the type.
            assertPrints("00000000003a".hexToInt(), "58")
            assertPrints("0000ffffffff".hexToInt(), "-1")
            // But excess leading digits must be zeros.
            assertFailsWith<IllegalArgumentException> { "000100000000".hexToInt() }
        }
    }
}