/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.text

import samples.*
import kotlin.test.*

@RunWith(Enclosed::class)
class HexFormats {

    @RunWith(Enclosed::class)
    class Numbers {

        class Formatting {

            @Sample
            fun prefix() {
                // Numeric values of different types
                val byteValue: Byte = 0x3a
                val intValue: Int = 0x3a

                // By default, `prefix` is an empty string
                assertPrints(byteValue.toHexString(), "3a")
                assertPrints(intValue.toHexString(), "0000003a")

                // `prefix` is placed before the hex representation
                assertPrints(byteValue.toHexString(HexFormat { number.prefix = "0x" }), "0x3a")
                assertPrints(intValue.toHexString(HexFormat { number.prefix = "#" }), "#0000003a")
            }

            @Sample
            fun suffix() {
                // Numeric values of different types
                val byteValue: Byte = 0x3a
                val intValue: Int = 0x3a

                // By default, `suffix` is an empty string
                assertPrints(byteValue.toHexString(), "3a")
                assertPrints(intValue.toHexString(), "0000003a")

                // `suffix` is placed after the hex representation
                assertPrints(byteValue.toHexString(HexFormat { number.suffix = "h" }), "3ah")
                assertPrints(intValue.toHexString(HexFormat { number.suffix = "-HEX" }), "0000003a-HEX")
            }

            @Sample
            fun removeLeadingZeros() {
                // Numeric values of different types
                val byteValue: Byte = 0x3a
                val intValue: Int = 0x3a

                // By default, removeLeadingZeroes is `false`
                assertPrints(intValue.toHexString(), "0000003a")

                // If there are no leading zeros, removeLeadingZeroes has no effect
                assertPrints(byteValue.toHexString(HexFormat { number.removeLeadingZeros = false }), "3a")
                assertPrints(byteValue.toHexString(HexFormat { number.removeLeadingZeros = true }), "3a")

                // The leading zeros in the hex representation are removed until minLength is reached.
                // By default, minLength is 1.
                assertPrints(intValue.toHexString(HexFormat { number.removeLeadingZeros = true }), "3a")
                assertPrints(0.toHexString(HexFormat { number.removeLeadingZeros = true }), "0")

                // Here minLength is set to 6.
                val smallerLengthFormat = HexFormat {
                    number.removeLeadingZeros = true
                    number.minLength = 6
                }
                assertPrints(intValue.toHexString(smallerLengthFormat), "00003a")

                // When minLength is greater than the hex representation, the hex representation is padded with zeros.
                // removeLeadingZeros is ignored in this case.
                val greaterLengthFormat = HexFormat {
                    number.removeLeadingZeros = true
                    number.minLength = 12
                }
                assertPrints(intValue.toHexString(greaterLengthFormat), "00000000003a")
            }

            @Sample
            fun minLength() {
                // minLength is less than the hex representation and removeLeadingZeros is false.
                // By default, minLength is 1 and removeLeadingZeros is false.
                assertPrints(0x3a.toHexString(), "0000003a")
                assertPrints(0x3a.toHexString(HexFormat { number.minLength = 4 }), "0000003a")

                // minLength is less than the hex representation and removeLeadingZeros is true.
                val smallerLengthFormat = HexFormat {
                    number.removeLeadingZeros = true
                    number.minLength = 4
                }
                assertPrints(0x3a.toHexString(smallerLengthFormat), "003a")
                assertPrints(0xff80ed.toHexString(smallerLengthFormat), "ff80ed")

                // When minLength is greater than the hex representation, the hex representation is padded with zeros.
                // removeLeadingZeros is ignored in this case.
                val greaterLengthFormat = HexFormat {
                    number.removeLeadingZeros = true
                    number.minLength = 12
                }
                assertPrints(0x3a.toHexString(greaterLengthFormat), "00000000003a")
            }
        }

        class Parsing {

            @Sample
            fun prefix() {
                // By default, `prefix` is an empty string
                assertPrints("3a".hexToByte(), "58")
                assertPrints("0000003a".hexToInt(), "58")

                // The string must start with the specified `prefix`
                assertPrints("0x3a".hexToByte(HexFormat { number.prefix = "0x" }), "58")
                // Parsing `prefix` is conducted in a case-insensitive manner
                assertPrints("0x0000003a".hexToInt(HexFormat { number.prefix = "0X" }), "58")
            }

            @Sample
            fun suffix() {
                // By default, `suffix` is an empty string
                assertPrints("3a".hexToByte(), "58")
                assertPrints("0000003a".hexToInt(), "58")

                // The string must end with the specified `suffix`
                assertPrints("3ah".hexToByte(HexFormat { number.suffix = "h" }), "58")
                // Parsing `suffix` is conducted in a case-insensitive manner
                assertPrints("0000003a-HEX".hexToInt(HexFormat { number.suffix = "-hex" }), "58")
            }

            @Sample
            fun removeLeadingZeros() {
                // When parsing, removeLeadingZeros is ignored
                assertPrints("0000003a".hexToInt(HexFormat { number.removeLeadingZeros = true }), "58")
            }

            @Sample
            fun minLength() {
                // When parsing, at least `minLength` hex digits are required.
                // By default, minLength is 1.
                assertPrints("3a".hexToInt(), "58")
                assertFailsWith<IllegalArgumentException> { "3a".hexToInt(HexFormat { number.minLength = 4 }) }

                // Number of hex digits can be greater than `minLength`,
                // but can't be greater than the width of the type being parsed at the same time.
                assertPrints("0000003a".hexToInt(HexFormat { number.minLength = 4 }), "58")
                assertFailsWith<IllegalArgumentException> { "00000000003a".hexToInt(HexFormat { number.minLength = 4 }) }

                // `minLength` can be greater than the width of the type being parsed.
                // The number of hex digits must be equal to `minLength` in this case.
                assertPrints("00000000003a".hexToInt(HexFormat { number.minLength = 12 }), "58")
            }
        }
    }
}