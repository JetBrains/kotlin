/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text

import kotlin.test.*

class BytesHexFormatTest {
    companion object {
        val fourBytes = ByteArray(4) { (10 + it).toByte() }
        val twentyBytes = ByteArray(20) { it.toByte() }
    }

    private fun testFormatAndParse(bytes: ByteArray, expected: String, format: HexFormat) {
        assertEquals(expected, bytes.toHexString(format))
        assertContentEquals(bytes, expected.hexToByteArray(format))

        assertEquals(expected, bytes.asUByteArray().toHexString(format))
        assertContentEquals(bytes.asUByteArray(), expected.hexToUByteArray(format))
    }

    @Test
    fun ignoreNumberFormat() {
        val format = HexFormat {
            number {
                prefix = "0x"
                suffix = "h"
                removeLeadingZeros = true
            }
        }

        testFormatAndParse(fourBytes, "0a0b0c0d", format)
    }

    @Test
    fun upperCase() {
        testFormatAndParse(fourBytes, "0A0B0C0D", HexFormat { upperCase = true })
        testFormatAndParse(fourBytes, "0A0B0C0D", HexFormat.UpperCase)
    }

    @Test
    fun lowerCase() {
        testFormatAndParse(fourBytes, "0a0b0c0d", HexFormat { upperCase = false })
    }

    @Test
    fun defaultCase() {
        testFormatAndParse(fourBytes, "0a0b0c0d", HexFormat.Default)
    }

    @Test
    fun byteSeparatorPrefixSuffix() {
        // byteSeparator
        testFormatAndParse(fourBytes, "0a 0b 0c 0d", HexFormat { bytes.byteSeparator = " " })
        // bytePrefix
        testFormatAndParse(fourBytes, "0x0a0x0b0x0c0x0d", HexFormat { bytes.bytePrefix = "0x" })
        // bytePrefix
        testFormatAndParse(fourBytes, "0a;0b;0c;0d;", HexFormat { bytes.byteSuffix = ";" })
        // all together
        testFormatAndParse(fourBytes, "0x0a; 0x0b; 0x0c; 0x0d;", HexFormat {
            bytes {
                byteSeparator = " "
                bytePrefix = "0x"
                byteSuffix = ";"
            }
        })
    }

    @Test
    fun bytesPerLine() {
        // Fewer bytes in the last line
        run {
            val format = HexFormat { bytes.bytesPerLine = 8 }
            val expected = "0001020304050607\n08090a0b0c0d0e0f\n10111213"
            testFormatAndParse(twentyBytes, expected, format)
        }

        // The last line is not ended by the line separator
        run {
            val format = HexFormat { bytes.bytesPerLine = 4 }
            val expected = "00010203\n04050607\n08090a0b\n0c0d0e0f\n10111213"
            testFormatAndParse(twentyBytes, expected, format)
        }
    }

    @Test
    fun bytesPerGroup() {
        // The default group separator, and fewer bytes in the last group
        run {
            val format = HexFormat { bytes.bytesPerGroup = 8 }
            val expected = "0001020304050607  08090a0b0c0d0e0f  10111213"
            testFormatAndParse(twentyBytes, expected, format)
        }

        // The specified group separator
        run {
            val format = HexFormat {
                bytes {
                    bytesPerGroup = 8
                    groupSeparator = "---"
                }
            }
            val expected = "0001020304050607---08090a0b0c0d0e0f---10111213"
            testFormatAndParse(twentyBytes, expected, format)
        }

        // The last group is not ended by the group separator
        run {
            val format = HexFormat { bytes.bytesPerGroup = 4 }
            val expected = "00010203  04050607  08090a0b  0c0d0e0f  10111213"
            testFormatAndParse(twentyBytes, expected, format)
        }
    }

    @Test
    fun bytesPerLineAndBytesPerGroup() {
        val format = HexFormat {
            upperCase = true
            bytes {
                bytesPerLine = 10
                bytesPerGroup = 4
                groupSeparator = "---"
                byteSeparator = " "
                bytePrefix = "#"
                byteSuffix = ";"
            }
        }

        val byteArray = ByteArray(31) { it.toByte() }

        val expected = """
            #00; #01; #02; #03;---#04; #05; #06; #07;---#08; #09;
            #0A; #0B; #0C; #0D;---#0E; #0F; #10; #11;---#12; #13;
            #14; #15; #16; #17;---#18; #19; #1A; #1B;---#1C; #1D;
            #1E;
        """.trimIndent()

        testFormatAndParse(byteArray, expected, format)
    }

    @Test
    fun macAddress() {
        val address = byteArrayOf(0x00, 0x1b, 0x63, 0x84.toByte(), 0x45, 0xe6.toByte())

        testFormatAndParse(
            address,
            "00:1b:63:84:45:e6",
            HexFormat { bytes.byteSeparator = ":" }
        )
        testFormatAndParse(
            address,
            "00-1B-63-84-45-E6",
            HexFormat { upperCase = true; bytes.byteSeparator = "-" }
        )
        testFormatAndParse(
            address,
            "001B.6384.45E6",
            HexFormat { upperCase = true; bytes.bytesPerGroup = 2; bytes.groupSeparator = "." }
        )
    }

    // Parsing strictness

    @Test
    fun parseRequiresTwoDigitsPerByte() {
        assertContentEquals(byteArrayOf(58), "3a".hexToByteArray())
        assertFailsWith<NumberFormatException> {
            "a".hexToByteArray()
        }
        assertFailsWith<NumberFormatException> {
            "03a".hexToByteArray()
        }

        val format = HexFormat { bytes.bytePrefix = "#"; bytes.byteSuffix = ";" }
        assertContentEquals(byteArrayOf(58), "#3a;".hexToByteArray(format))
        assertFailsWith<NumberFormatException> {
            "#a;".hexToByteArray(format)
        }
        assertFailsWith<NumberFormatException> {
            "#03a;".hexToByteArray(format)
        }

        assertFailsWith<NumberFormatException> {
            "0a0b0c0".hexToByteArray()
        }
    }

    @Test
    fun parseIgnoresCase() {
        assertContentEquals(
            twentyBytes,
            "000102030405060708090A0B0C0D0E0F10111213".hexToByteArray()
        )
        val hexString = "0x00H bs 0x01H bS 0x02H Bs 0x03H  gs  " +
                "0x04h BS 0x05h bs 0x06h Bs 0x07h  Gs  " +
                "0X08H bS 0X09H Bs 0X0aH bs 0X0bH  gS  " +
                "0X0Ch bs 0X0Dh BS 0X0Eh Bs 0X0Fh  GS  " +
                "0x10H Bs 0x11H bS 0x12H BS 0x13H"
        val format = HexFormat {
            upperCase = true
            bytes {
                bytesPerGroup = 4
                groupSeparator = "  gs  "
                byteSeparator = " bs "
                bytePrefix = "0x"
                byteSuffix = "h"
            }
        }
        assertContentEquals(
            twentyBytes,
            hexString.hexToByteArray(format)
        )
    }

    @Test
    fun parseAcceptsAllNewLineSequences() {
        assertContentEquals(
            twentyBytes,
            "00010203\n04050607\r\n08090a0b\r0c0d0e0f\r\n10111213".hexToByteArray(HexFormat { bytes.bytesPerLine = 4 })
        )
    }

    @Test
    fun parseMultipleNewLines() {
        assertFailsWith<NumberFormatException> {
            "00010203\n\r04050607\r\n08090a0b\r0c0d0e0f\r\n10111213".hexToByteArray(HexFormat { bytes.bytesPerLine = 4 })
        }
        assertFailsWith<NumberFormatException> {
            "00010203\n\n04050607\r\n08090a0b\r0c0d0e0f\r\n10111213".hexToByteArray(HexFormat { bytes.bytesPerLine = 4 })
        }
        assertFailsWith<NumberFormatException> {
            "00010203\n04050607\r\n\n08090a0b\r0c0d0e0f\r\n10111213".hexToByteArray(HexFormat { bytes.bytesPerLine = 4 })
        }
    }

    @Test
    fun parseNewLineAtEnd() {
        assertFailsWith<NumberFormatException> {
            "00010203\n04050607\r\n08090a0b\r0c0d0e0f\r\n10111213\n".hexToByteArray(HexFormat { bytes.bytesPerLine = 4 })
        }
    }

    // HexFormat corner-case options configuration

    @Test
    fun emptyGroupSeparator() {
        testFormatAndParse(
            twentyBytes,
            "000102030405060708090a0b0c0d0e0f10111213",
            HexFormat { bytes.bytesPerGroup = 8; bytes.groupSeparator = "" }
        )
        testFormatAndParse(
            twentyBytes,
            "00 01 02 03 04 05 06 0708 09 0a 0b 0c 0d 0e 0f10 11 12 13",
            HexFormat { bytes.bytesPerGroup = 8; bytes.groupSeparator = ""; bytes.byteSeparator = " " }
        )
    }

    @Test
    fun bytesPerGroupBiggerThanBytesPerLine() {
        val format = HexFormat { bytes.bytesPerLine = 8; bytes.bytesPerGroup = 10 }
        val expected = "0001020304050607\n08090a0b0c0d0e0f\n10111213"
        testFormatAndParse(twentyBytes, expected, format)
    }

    @Test
    fun groupSeparatorWithNewLine() {
        val format = HexFormat { bytes.bytesPerLine = 8; bytes.bytesPerGroup = 3; bytes.groupSeparator = "\n" }
        val expected = "000102\n030405\n0607\n08090a\n0b0c0d\n0e0f\n101112\n13"
        testFormatAndParse(twentyBytes, expected, format)
    }

    // Invalid HexFormat configuration

    @Test
    fun nonPositiveBytesPerLine() {
        assertFailsWith<IllegalArgumentException> { HexFormat { bytes.bytesPerLine = 0 } }
        assertFailsWith<IllegalArgumentException> { HexFormat { bytes.bytesPerLine = -1 } }
        assertFailsWith<IllegalArgumentException> { HexFormat { bytes.bytesPerLine = Int.MIN_VALUE } }
    }

    @Test
    fun nonPositiveBytesPerGroup() {
        assertFailsWith<IllegalArgumentException> { HexFormat { bytes.bytesPerGroup = 0 } }
        assertFailsWith<IllegalArgumentException> { HexFormat { bytes.bytesPerGroup = -1 } }
        assertFailsWith<IllegalArgumentException> { HexFormat { bytes.bytesPerGroup = Int.MIN_VALUE } }
    }

    @Test
    fun byteSeparatorWithNewLine() {
        assertFailsWith<IllegalArgumentException> { HexFormat { bytes.byteSeparator = "ab\n" } }
        assertFailsWith<IllegalArgumentException> { HexFormat { bytes.byteSeparator = "\rcd" } }
    }

    @Test
    fun bytePrefixWithNewLine() {
        assertFailsWith<IllegalArgumentException> { HexFormat { bytes.bytePrefix = "\n" } }
        assertFailsWith<IllegalArgumentException> { HexFormat { bytes.bytePrefix = "\r" } }
    }

    @Test
    fun byteSuffixWithNewLine() {
        assertFailsWith<IllegalArgumentException> { HexFormat { bytes.byteSuffix = "ab\n\rcd" } }
        assertFailsWith<IllegalArgumentException> { HexFormat { bytes.byteSuffix = "ab\r\ncd" } }
    }

    // format.toString()

    @Test
    fun formatToString() {
        val format = HexFormat {
            upperCase = true
            bytes {
                bytesPerLine = 10
                bytesPerGroup = 4
                groupSeparator = "---"
                byteSeparator = " "
                bytePrefix = "#"
                byteSuffix = ";"
            }
            number {
                prefix = "0x"
                suffix = "h"
                removeLeadingZeros = true
            }
        }

        val expectedHexFormat = """
            HexFormat(
                upperCase = true,
                bytes = BytesHexFormat(
                    bytesPerLine = 10,
                    bytesPerGroup = 4,
                    groupSeparator = "---",
                    byteSeparator = " ",
                    bytePrefix = "#",
                    byteSuffix = ";"
                ),
                number = NumberHexFormat(
                    prefix = "0x",
                    suffix = "h",
                    removeLeadingZeros = true
                )
            )
            """.trimIndent()
        assertEquals(expectedHexFormat, format.toString())

        val expectedBytesHexFormat = """
            BytesHexFormat(
                bytesPerLine = 10,
                bytesPerGroup = 4,
                groupSeparator = "---",
                byteSeparator = " ",
                bytePrefix = "#",
                byteSuffix = ";"
            )
            """.trimIndent()
        assertEquals(expectedBytesHexFormat, format.bytes.toString())

        val expectedNumberHexFormat = """
            NumberHexFormat(
                prefix = "0x",
                suffix = "h",
                removeLeadingZeros = true
            )
            """.trimIndent()
        assertEquals(expectedNumberHexFormat, format.number.toString())
    }

    // Create format objects on demand

    @Test
    fun createOnDemand() {
        assertSame(HexFormat.Default.bytes, HexFormat.UpperCase.bytes)
        assertSame(HexFormat.Default.number, HexFormat.UpperCase.number)

        val emptyFormat = HexFormat {}
        assertNotSame(HexFormat.Default, emptyFormat)
        assertEquals(HexFormat.Default.upperCase, emptyFormat.upperCase)
        assertSame(HexFormat.Default.bytes, emptyFormat.bytes)
        assertSame(HexFormat.Default.number, emptyFormat.number)

        val bytesFormat = HexFormat { bytes }
        assertNotSame(HexFormat.Default.bytes, bytesFormat.bytes)
        assertSame(HexFormat.Default.number, bytesFormat.number)

        val numberFormat = HexFormat { number {} }
        assertSame(HexFormat.Default.bytes, numberFormat.bytes)
        assertNotSame(HexFormat.Default.number, numberFormat.number)
    }

    @Test
    fun formattedStringLength() {
        run {
//            00010203\n04050607\n08090a0b\n0c0d0e0f\n10111213
            val length = formattedStringLength(
                totalBytes = 20,
                bytesPerLine = 4,
                bytesPerGroup = Int.MAX_VALUE,
                groupSeparatorLength = 2,
                byteSeparatorLength = 0,
                bytePrefixLength = 0,
                byteSuffixLength = 0
            )
            assertEquals(44, length)
        }
        run {
//            0001020304050607---08090a0b0c0d0e0f---10111213
            val length = formattedStringLength(
                totalBytes = 20,
                bytesPerLine = Int.MAX_VALUE,
                bytesPerGroup = 8,
                groupSeparatorLength = 3,
                byteSeparatorLength = 0,
                bytePrefixLength = 0,
                byteSuffixLength = 0
            )
            assertEquals(46, length)
        }
        run {
//            #00; #01; #02; #03;---#04; #05; #06; #07;---#08; #09;
//            #0A; #0B; #0C; #0D;---#0E; #0F; #10; #11;---#12; #13;
//            #14; #15; #16; #17;---#18; #19; #1A; #1B;---#1C; #1D;
//            #1E;
            val length = formattedStringLength(
                totalBytes = 31,
                bytesPerLine = 10,
                bytesPerGroup = 4,
                groupSeparatorLength = 3,
                byteSeparatorLength = 1,
                bytePrefixLength = 1,
                byteSuffixLength = 1
            )
            assertEquals(166, length)
        }
        run {
            val length = formattedStringLength(
                totalBytes = Int.MAX_VALUE / 2,
                bytesPerLine = Int.MAX_VALUE,
                bytesPerGroup = Int.MAX_VALUE,
                groupSeparatorLength = 0,
                byteSeparatorLength = 0,
                bytePrefixLength = 0,
                byteSuffixLength = 0
            )
            assertEquals(Int.MAX_VALUE / 2 * 2, length)
        }
        assertFailsWith<IllegalArgumentException> {
            formattedStringLength(
                totalBytes = Int.MAX_VALUE,
                bytesPerLine = Int.MAX_VALUE,
                bytesPerGroup = Int.MAX_VALUE,
                groupSeparatorLength = Int.MAX_VALUE,
                byteSeparatorLength = Int.MAX_VALUE,
                bytePrefixLength = Int.MAX_VALUE,
                byteSuffixLength = Int.MAX_VALUE
            )
        }
        assertFailsWith<IllegalArgumentException> {
            formattedStringLength(
                totalBytes = Int.MAX_VALUE / 2 + 1,
                bytesPerLine = Int.MAX_VALUE,
                bytesPerGroup = Int.MAX_VALUE,
                groupSeparatorLength = 0,
                byteSeparatorLength = 0,
                bytePrefixLength = 0,
                byteSuffixLength = 0
            )
        }
    }

    @Test
    fun parsedByteArrayMaxSize() {
        run {
//            00010203\n04050607\n08090a0b\n0c0d0e0f\n10111213
            val maxSize = parsedByteArrayMaxSize(
                stringLength = 44,
                bytesPerLine = 4,
                bytesPerGroup = Int.MAX_VALUE,
                groupSeparatorLength = 2,
                byteSeparatorLength = 0,
                bytePrefixLength = 0,
                byteSuffixLength = 0
            )
            assertEquals(20, maxSize)
        }
        run {
//            0001020304050607---08090a0b0c0d0e0f---10111213
            val maxSize = parsedByteArrayMaxSize(
                stringLength = 46,
                bytesPerLine = Int.MAX_VALUE,
                bytesPerGroup = 8,
                groupSeparatorLength = 3,
                byteSeparatorLength = 0,
                bytePrefixLength = 0,
                byteSuffixLength = 0
            )
            assertEquals(20, maxSize)
        }
        run {
//            #00; #01; #02; #03;---#04; #05; #06; #07;---#08; #09;
//            #0A; #0B; #0C; #0D;---#0E; #0F; #10; #11;---#12; #13;
//            #14; #15; #16; #17;---#18; #19; #1A; #1B;---#1C; #1D;
//            #1E;
            val maxSize = parsedByteArrayMaxSize(
                stringLength = 166,
                bytesPerLine = 10,
                bytesPerGroup = 4,
                groupSeparatorLength = 3,
                byteSeparatorLength = 1,
                bytePrefixLength = 1,
                byteSuffixLength = 1
            )
            assertEquals(31, maxSize)
        }
        run {
//            0001020304050607\n08090a0b0c0d0e0f\n10111213
            // bytesPerGroup > bytesPerLine
            val maxSize = parsedByteArrayMaxSize(
                stringLength = 42,
                bytesPerLine = 8,
                bytesPerGroup = 10,
                groupSeparatorLength = 3,
                byteSeparatorLength = 0,
                bytePrefixLength = 0,
                byteSuffixLength = 0
            )
            assertEquals(20, maxSize)
        }
        run {
            val maxSize = parsedByteArrayMaxSize(
                stringLength = Int.MAX_VALUE,
                bytesPerLine = Int.MAX_VALUE,
                bytesPerGroup = Int.MAX_VALUE,
                groupSeparatorLength = 0,
                byteSeparatorLength = 0,
                bytePrefixLength = 0,
                byteSuffixLength = 0
            )
            assertEquals(Int.MAX_VALUE / 2 + 1, maxSize)
        }
        run {
            val maxSize = parsedByteArrayMaxSize(
                stringLength = Int.MAX_VALUE,
                bytesPerLine = Int.MAX_VALUE,
                bytesPerGroup = Int.MAX_VALUE,
                groupSeparatorLength = Int.MAX_VALUE,
                byteSeparatorLength = Int.MAX_VALUE,
                bytePrefixLength = Int.MAX_VALUE,
                byteSuffixLength = Int.MAX_VALUE
            )
            assertEquals(1, maxSize)
        }
        run {
            val maxSize = parsedByteArrayMaxSize(
                stringLength = Int.MAX_VALUE,
                bytesPerLine = Int.MAX_VALUE,
                bytesPerGroup = Int.MAX_VALUE / 2,
                groupSeparatorLength = Int.MAX_VALUE,
                byteSeparatorLength = Int.MAX_VALUE,
                bytePrefixLength = Int.MAX_VALUE,
                byteSuffixLength = Int.MAX_VALUE
            )
            assertEquals(1, maxSize)
        }
    }
}