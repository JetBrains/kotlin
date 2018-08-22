package samples.text

import samples.*
import kotlin.test.assertTrue

class Strings {

    @Sample
    fun capitalize() {
        assertPrints("abcd".capitalize(), "Abcd")
        assertPrints("Abcd".capitalize(), "Abcd")
    }

    @Sample
    fun decapitalize() {
        assertPrints("abcd".decapitalize(), "abcd")
        assertPrints("Abcd".decapitalize(), "abcd")
    }

    @Sample
    fun repeat() {
        assertPrints("Word".repeat(4), "WordWordWordWord")
        assertPrints("Word".repeat(0), "")
    }

    @Sample
    fun trimIndent() {
        val withoutIndent =
                """
                    ABC
                    123
                    456
                """.trimIndent()
        assertPrints(withoutIndent, "ABC\n123\n456")
    }

    @Sample
    fun trimMargin() {
        val withoutMargin1 = """ABC
                        |123
                        |456""".trimMargin()
        assertPrints(withoutMargin1, "ABC\n123\n456")

        val withoutMargin2 = """
            #XYZ
            #foo
            #bar
        """.trimMargin("#")
        assertPrints(withoutMargin2, "XYZ\nfoo\nbar")
    }

    @Sample
    fun chunkedTransform() {
        val codonTable = mapOf("ATT" to "Isoleucine", "CAA" to "Glutamine", "CGC" to "Arginine", "GGC" to "Glycine")
        val dnaFragment = "ATTCGCGGCCGCCAA"

        val proteins = dnaFragment.chunked(3) { codon: CharSequence -> codonTable[codon.toString()] ?: error("Unknown codon") }

        assertPrints(proteins, "[Isoleucine, Arginine, Glycine, Arginine, Glutamine]")
    }

    @Sample
    fun chunkedTransformToSequence() {
        val codonTable = mapOf("ATT" to "Isoleucine", "CAA" to "Glutamine", "CGC" to "Arginine", "GGC" to "Glycine")
        val dnaFragment = "ATTCGCGGCCGCCAACGG"

        val proteins = dnaFragment.chunkedSequence(3) { codon: CharSequence -> codonTable[codon.toString()] ?: error("Unknown codon") }

        // sequence is evaluated lazily, so that unknown codon is not reached
        assertPrints(proteins.take(5).toList(), "[Isoleucine, Arginine, Glycine, Arginine, Glutamine]")
    }

    @Sample
    fun zip() {
        val stringA = "abcd"
        val stringB = "zyx"
        assertPrints(stringA zip stringB, "[(a, z), (b, y), (c, x)]")
    }

    @Sample
    fun zipWithTransform() {
        val stringA = "abcd"
        val stringB = "zyx"
        val result = stringA.zip(stringB) { a, b -> "$a$b" }
        assertPrints(result, "[az, by, cx]")
    }

    @Sample
    fun associateWith() {
        val string = "bonne journée"
        // associate each character with its code
        val result = string.associateWith { char -> char.toInt() }
        // notice each letter occurs only once
        assertPrints(result, "{b=98, o=111, n=110, e=101,  =32, j=106, u=117, r=114, é=233}")
    }

    @Sample
    fun stringToByteArray() {
        val charset = Charsets.UTF_8
        val byteArray = "Hello".toByteArray(charset)
        assertPrints(byteArray.contentToString(), "[72, 101, 108, 108, 111]")
        assertPrints(byteArray.toString(charset), "Hello")
    }

    @Sample
    fun toLowerCase() {
        assertPrints("Iced frappé!".toLowerCase(), "iced frappé!")
    }

    @Sample
    fun toUpperCase() {
        assertPrints("Iced frappé!".toUpperCase(), "ICED FRAPPÉ!")
    }

    @Sample
    fun padStart() {
        val padWithSpace = "125".padStart(5)
        assertPrints("'$padWithSpace'", "'  125'")

        val padWithChar = "a".padStart(5, '.')
        assertPrints("'$padWithChar'", "'....a'")

        // string is returned as is, when its length is greater than the specified
        val noPadding = "abcde".padStart(3)
        assertPrints("'$noPadding'", "'abcde'")
    }

    @Sample
    fun padEnd() {
        val padWithSpace = "125".padEnd(5)
        assertPrints("'$padWithSpace'", "'125  '")

        val padWithChar = "a".padEnd(5, '.')
        assertPrints("'$padWithChar'", "'a....'")

        // string is returned as is, when its length is greater than the specified
        val noPadding = "abcde".padEnd(3)
        assertPrints("'$noPadding'", "'abcde'")
    }
    @Sample
    fun clearStringBuilder() {
        val builder = StringBuilder()
        builder.append("content").append(1)
        assertPrints(builder, "content1")

        builder.clear()
        assertPrints(builder, "")
    }

    @Sample
    fun stringIfEmpty() {
        val empty = ""

        val emptyOrNull: String? = empty.ifEmpty { null }
        assertPrints(emptyOrNull, "null")

        val emptyOrDefault = empty.ifEmpty { "default" }
        assertPrints(emptyOrDefault, "default")

        val nonEmpty = "abc"
        val sameString = nonEmpty.ifEmpty { "def" }
        assertTrue(nonEmpty === sameString)
    }

    @Sample
    fun stringIfBlank() {
        val blank = "    "

        val blankOrNull: String? = blank.ifBlank { null }
        assertPrints(blankOrNull, "null")

        val blankOrDefault = blank.ifBlank { "default" }
        assertPrints(blankOrDefault, "default")

        val nonBlank = "abc"
        val sameString = nonBlank.ifBlank { "def" }
        assertTrue(nonBlank === sameString)
    }
}
