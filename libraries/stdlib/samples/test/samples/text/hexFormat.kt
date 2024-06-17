/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.text

import samples.*
import kotlin.test.*

@RunWith(Enclosed::class)
class HexFormats {

    class HexFormatClass {
        @Sample
        fun hexFormatBuilderFunction() {
            // Specifying format options for numeric values.
            assertPrints(58.toHexString(HexFormat { number.removeLeadingZeros = true }), "3a")
            assertPrints("0x3a".hexToInt(HexFormat { number.prefix = "0x" }), "58")

            // Specifying format options for byte arrays.
            val macAddressFormat = HexFormat {
                upperCase = true
                bytes {
                    bytesPerGroup = 2
                    groupSeparator = "."
                }
            }
            val macAddressBytes = byteArrayOf(0x00, 0x1b, 0x63, 0x84.toByte(), 0x45, 0xe6.toByte())
            assertPrints(macAddressBytes.toHexString(macAddressFormat), "001B.6384.45E6")
            assertTrue("001B.6384.45E6".hexToByteArray(macAddressFormat).contentEquals(macAddressBytes))

            // Creating a format that defines options for both byte arrays and numeric values.
            val customHexFormat = HexFormat {
                upperCase = true
                number {
                    removeLeadingZeros = true
                    prefix = "0x"
                }
                bytes {
                    bytesPerGroup = 2
                    groupSeparator = "."
                }
            }
            // Formatting numeric values utilizes the `upperCase` and `number` options.
            assertPrints(58.toHexString(customHexFormat), "0x3A")
            // Formatting byte arrays utilizes the `upperCase` and `bytes` options.
            assertPrints(macAddressBytes.toHexString(customHexFormat), "001B.6384.45E6")
        }

        @Sample
        fun upperCase() {
            // By default, upperCase is set to false, so lower-case hexadecimal digits are used when formatting.
            assertPrints(58.toHexString(), "0000003a")
            assertPrints(58.toHexString(HexFormat.Default), "0000003a")

            // Setting upperCase to true changes the hexadecimal digits to upper-case.
            assertPrints(58.toHexString(HexFormat { upperCase = true }), "0000003A")
            assertPrints(58.toHexString(HexFormat.UpperCase), "0000003A")

            // The upperCase option affects only the case of hexadecimal digits.
            val format = HexFormat {
                upperCase = true
                number.prefix = "0x"
            }
            assertPrints(58.toHexString(format), "0x0000003A")

            // The upperCase option also affects how byte arrays are formatted.
            assertPrints(byteArrayOf(0x1b, 0xe6.toByte()).toHexString(format), "1BE6")

            // The upperCase option does not affect parsing; parsing is always case-insensitive.
            assertPrints("0x0000003a".hexToInt(format), "58")
            assertTrue("1BE6".hexToByteArray(format).contentEquals(byteArrayOf(0x1b, 0xe6.toByte())))
        }
    }

    class ByteArrays {
        @Sample
        fun bytesPerLine() {
            val data = ByteArray(7) { it.toByte() }

            // By default, bytesPerLine is set to Int.MAX_VALUE, which exceeds data.size.
            // Therefore, all bytes are formatted as a single line without any line breaks.
            assertPrints(data.toHexString(), "00010203040506")
            assertTrue("00010203040506".hexToByteArray().contentEquals(data))

            // Setting bytesPerLine to 3 splits the output into 3 lines with 3, 3, and 1 bytes, respectively.
            // Each line is separated by a line feed (LF) character.
            val threePerLineFormat = HexFormat {
                bytes.bytesPerLine = 3
            }
            assertPrints(data.toHexString(threePerLineFormat), "000102\n030405\n06")

            // When parsing, any of the line separators CRLF, LF, and CR are accepted.
            assertTrue("000102\n030405\r\n06".hexToByteArray(threePerLineFormat).contentEquals(data))

            // Parsing fails if the input string does not conform to specified format.
            // In this case, lines do not consist of the expected number of bytes.
            assertFailsWith<IllegalArgumentException> {
                "0001\n0203\n0405\n06".hexToByteArray(threePerLineFormat)
            }
        }

        @Sample
        fun bytesPerGroup() {
            val data = ByteArray(7) { it.toByte() }

            // By default, both bytesPerLine and bytesPerGroup are set to Int.MAX_VALUE, which exceeds data.size.
            // Hence, all bytes are formatted as a single line and a single group.
            assertPrints(data.toHexString(), "00010203040506")
            assertTrue("00010203040506".hexToByteArray().contentEquals(data))

            // Setting bytesPerGroup to 2 with the default group separator, which is two spaces.
            val twoPerGroupFormat = HexFormat {
                bytes.bytesPerGroup = 2
            }
            assertPrints(data.toHexString(twoPerGroupFormat), "0001  0203  0405  06")
            assertTrue("0001  0203  0405  06".hexToByteArray(twoPerGroupFormat).contentEquals(data))

            // Specifying a custom group separator, a dot in this case.
            val dotGroupSeparatorFormat = HexFormat {
                bytes {
                    bytesPerGroup = 2
                    groupSeparator = "."
                }
            }
            assertPrints(data.toHexString(dotGroupSeparatorFormat), "0001.0203.0405.06")
            assertTrue("0001.0203.0405.06".hexToByteArray(dotGroupSeparatorFormat).contentEquals(data))

            // If bytesPerLine is less than or equal to bytesPerGroup, each line is treated as a single group,
            // hence no group separator is used.
            val lessBytesPerLineFormat = HexFormat {
                bytes {
                    bytesPerLine = 3
                    bytesPerGroup = 4
                    groupSeparator = "|"
                }
            }
            assertPrints(data.toHexString(lessBytesPerLineFormat), "000102\n030405\n06")
            assertTrue("000102\n030405\n06".hexToByteArray(lessBytesPerLineFormat).contentEquals(data))

            // When bytesPerLine is greater than bytesPerGroup, each line is split into multiple groups.
            val moreBytesPerLineFormat = HexFormat {
                bytes {
                    bytesPerLine = 5
                    bytesPerGroup = 2
                    groupSeparator = "."
                }
            }
            assertPrints(data.toHexString(moreBytesPerLineFormat), "0001.0203.04\n0506")
            assertTrue("0001.0203.04\n0506".hexToByteArray(moreBytesPerLineFormat).contentEquals(data))

            // Parsing fails due to incorrect group separator.
            assertFailsWith<IllegalArgumentException> {
                "0001  0203  04\n0506".hexToByteArray(moreBytesPerLineFormat)
            }
        }

        @Sample
        fun byteSeparator() {
            val data = ByteArray(7) { it.toByte() }

            // By default, the byteSeparator is an empty string, hence all bytes are concatenated without any separator.
            assertPrints(data.toHexString(), "00010203040506")
            assertTrue("00010203040506".hexToByteArray().contentEquals(data))

            // Specifying a custom byte separator, a colon in this case.
            val colonByteSeparatorFormat = HexFormat { bytes.byteSeparator = ":" }
            assertPrints(data.toHexString(colonByteSeparatorFormat), "00:01:02:03:04:05:06")
            assertTrue("00:01:02:03:04:05:06".hexToByteArray(colonByteSeparatorFormat).contentEquals(data))

            // Only adjacent bytes within a group are separated by the byteSeparator.
            val groupFormat = HexFormat {
                bytes.bytesPerGroup = 3
                bytes.byteSeparator = ":"
            }
            assertPrints(data.toHexString(groupFormat), "00:01:02  03:04:05  06")
            assertTrue("00:01:02  03:04:05  06".hexToByteArray(groupFormat).contentEquals(data))

            // Parsing fails due to incorrect byte separator.
            // In this case, the input string is lacking the necessary byte separators within groups.
            assertFailsWith<IllegalArgumentException> {
                "000102  030405  06".hexToByteArray(groupFormat)
            }
        }

        @Sample
        fun bytePrefix() {
            val data = ByteArray(4) { it.toByte() }

            // By default, the bytePrefix is an empty string, so bytes are formatted without any prefix.
            assertPrints(data.toHexString(), "00010203")
            assertTrue("00010203".hexToByteArray().contentEquals(data))

            // Specifying a custom byte prefix, "0x" in this case, to precede each byte.
            // A space is used as a byte separator for clarity in the output.
            val bytePrefixFormat = HexFormat {
                bytes.bytePrefix = "0x"
                bytes.byteSeparator = " "
            }
            assertPrints(data.toHexString(bytePrefixFormat), "0x00 0x01 0x02 0x03")
            assertTrue("0x00 0x01 0x02 0x03".hexToByteArray(bytePrefixFormat).contentEquals(data))

            // Parsing fails due to incorrect byte prefix.
            // In this case, the input string is lacking the necessary byte prefixes.
            assertFailsWith<IllegalArgumentException> {
                "00 01 02 03".hexToByteArray(bytePrefixFormat)
            }

        }

        @Sample
        fun byteSuffix() {
            val data = ByteArray(4) { it.toByte() }

            // By default, the byteSuffix is an empty string, so bytes are formatted without any suffix.
            assertPrints(data.toHexString(), "00010203")
            assertTrue("00010203".hexToByteArray().contentEquals(data))

            // Specifying a custom byte suffix, a semicolon in this case, to follow each byte.
            val byteSuffixFormat = HexFormat {
                bytes.byteSuffix = ";"
            }
            assertPrints(data.toHexString(byteSuffixFormat), "00;01;02;03;")
            assertTrue("00;01;02;03;".hexToByteArray(byteSuffixFormat).contentEquals(data))

            // Parsing fails due to incorrect byte suffix.
            // In this case, the input string is lacking the necessary byte suffixes.
            assertFailsWith<IllegalArgumentException> {
                "00010203".hexToByteArray(byteSuffixFormat)
            }
        }
    }

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