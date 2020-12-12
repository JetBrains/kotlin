package samples.text

import samples.*
import kotlin.test.*

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
    fun chunked() {
        val dnaFragment = "ATTCGCGGCCGCCAA"

        val codons = dnaFragment.chunked(3)

        assertPrints(codons, "[ATT, CGC, GGC, CGC, CAA]")
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
    fun filter() {
        val text = "a1b2c3d4e5"

        val textWithOnlyDigits = text.filter { it.isDigit() }

        assertPrints(textWithOnlyDigits, "12345")
    }

    @Sample
    fun filterNot() {
        val text = "a1b2c3d4e5"

        val textWithoutDigits = text.filterNot { it.isDigit() }

        assertPrints(textWithoutDigits, "abcde")
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
    fun associate() {
        val string = "bonne journée"
        // associate each character with its code
        val result = string.associate { char -> char to char.toInt() }
        // notice each letter occurs only once
        assertPrints(result, "{b=98, o=111, n=110, e=101,  =32, j=106, u=117, r=114, é=233}")
    }

    @Sample
    fun associateBy() {
        val string = "bonne journée"
        // associate each character by its code
        val result = string.associateBy { char -> char.toInt() }
        // notice each char code occurs only once
        assertPrints(result, "{98=b, 111=o, 110=n, 101=e, 32= , 106=j, 117=u, 114=r, 233=é}")
    }

    @Sample
    fun associateByWithValueTransform() {
        val string = "bonne journée"
        // associate each character by the code of its upper case equivalent and transform the character to upper case
        val result = string.associateBy({ char -> char.toUpperCase().toInt() }, { char -> char.toUpperCase() })
        // notice each char code occurs only once
        assertPrints(result, "{66=B, 79=O, 78=N, 69=E, 32= , 74=J, 85=U, 82=R, 201=É}")
    }

    @Sample
    fun associateByTo() {
        val string = "bonne journée"
        // associate each character by its code
        val result = mutableMapOf<Int, Char>()
        string.associateByTo(result) { char -> char.toInt() }
        // notice each char code occurs only once
        assertPrints(result, "{98=b, 111=o, 110=n, 101=e, 32= , 106=j, 117=u, 114=r, 233=é}")
    }

    @Sample
    fun associateByToWithValueTransform() {
        val string = "bonne journée"
        // associate each character by the code of its upper case equivalent and transform the character to upper case
        val result = mutableMapOf<Int, Char>()
        string.associateByTo(result, { char -> char.toUpperCase().toInt() }, { char -> char.toUpperCase() })
        // notice each char code occurs only once
        assertPrints(result, "{66=B, 79=O, 78=N, 69=E, 32= , 74=J, 85=U, 82=R, 201=É}")
    }

    @Sample
    fun associateTo() {
        val string = "bonne journée"
        // associate each character with its code
        val result = mutableMapOf<Char, Int>()
        string.associateTo(result) { char -> char to char.toInt() }
        // notice each letter occurs only once
        assertPrints(result, "{b=98, o=111, n=110, e=101,  =32, j=106, u=117, r=114, é=233}")
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
    fun associateWithTo() {
        val string = "bonne journée"
        // associate each character with its code
        val result = mutableMapOf<Char, Int>()
        string.associateWithTo(result) { char -> char.toInt() }
        // notice each letter occurs only once
        assertPrints(result, "{b=98, o=111, n=110, e=101,  =32, j=106, u=117, r=114, é=233}")
    }

    @Sample
    fun partition() {
        fun isVowel(c: Char) = "aeuio".contains(c, ignoreCase = true)
        val string = "Discussion"
        val result = string.partition(::isVowel)
        assertPrints(result, "(iuio, Dscssn)")
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

    @Sample
    fun stringIsBlank() {
        fun validateName(name: String): String {
            if (name.isBlank()) throw IllegalArgumentException("Name cannot be blank")
            return name
        }

        assertPrints(validateName("Adam"), "Adam")
        assertFails { validateName("") }
        assertFails { validateName("  \t\n") }
    }

    @Sample
    fun stringIsNotBlank() {
        fun validateName(name: String): String {
            require(name.isNotBlank()) { "Name cannot be blank" }
            return name
        }

        assertPrints(validateName("Adam"), "Adam")
        assertFails { validateName("") }
        assertFails { validateName("  \t\n") }
    }

    @Sample
    fun stringIsNullOrBlank() {
        fun validateName(name: String?): String {
            if (name.isNullOrBlank()) throw IllegalArgumentException("Name cannot be blank")
            // name is not nullable here anymore due to a smart cast after calling isNullOrBlank
            return name
        }

        assertPrints(validateName("Adam"), "Adam")
        assertFails { validateName(null) }
        assertFails { validateName("") }
        assertFails { validateName("  \t\n") }
    }

    @Sample
    fun stringIsEmpty() {
        fun markdownLink(title: String, url: String) =
            if (title.isEmpty()) url else "[$title]($url)"

        // plain link
        assertPrints(markdownLink(title = "", url = "https://kotlinlang.org"), "https://kotlinlang.org")

        // link with custom title
        assertPrints(markdownLink(title = "Kotlin Language", url = "https://kotlinlang.org"), "[Kotlin Language](https://kotlinlang.org)")
    }

    @Sample
    fun stringIsNotEmpty() {
        fun markdownLink(title: String, url: String) =
            if (title.isNotEmpty()) "[$title]($url)" else url

        // plain link
        assertPrints(markdownLink(title = "", url = "https://kotlinlang.org"), "https://kotlinlang.org")

        // link with custom title
        assertPrints(markdownLink(title = "Kotlin Language", url = "https://kotlinlang.org"), "[Kotlin Language](https://kotlinlang.org)")
    }


    @Sample
    fun stringIsNullOrEmpty() {
        fun markdownLink(title: String?, url: String) =
            if (title.isNullOrEmpty()) url else "[$title]($url)"

        // plain link
        assertPrints(markdownLink(title = null, url = "https://kotlinlang.org"), "https://kotlinlang.org")

        // link with custom title
        assertPrints(markdownLink(title = "Kotlin Language", url = "https://kotlinlang.org"), "[Kotlin Language](https://kotlinlang.org)")
    }

    @Sample
    fun commonPrefixWith() {
        assertPrints("Hot_Coffee".commonPrefixWith("Hot_cocoa"), "Hot_")
        assertPrints("Hot_Coffee".commonPrefixWith("Hot_cocoa", true), "Hot_Co")
        assertPrints("Hot_Coffee".commonPrefixWith("Iced_Coffee"), "")
    }

    @Sample
    fun commonSuffixWith() {
        assertPrints("Hot_Tea".commonSuffixWith("iced_tea"), "ea")
        assertPrints("Hot_Tea".commonSuffixWith("iced_tea", true), "_Tea")
        assertPrints("Hot_Tea".commonSuffixWith("Hot_Coffee"), "")
    }

    @Sample
    fun take() {
        val string = "<<<First Grade>>>"
        assertPrints(string.take(8), "<<<First")
        assertPrints(string.takeLast(8), "Grade>>>")
        assertPrints(string.takeWhile { !it.isLetter() }, "<<<")
        assertPrints(string.takeLastWhile { !it.isLetter() }, ">>>")
    }

    @Sample
    fun drop() {
        val string = "<<<First Grade>>>"
        assertPrints(string.drop(6), "st Grade>>>")
        assertPrints(string.dropLast(6), "<<<First Gr")
        assertPrints(string.dropWhile { !it.isLetter() }, "First Grade>>>")
        assertPrints(string.dropLastWhile { !it.isLetter() }, "<<<First Grade")
    }

    @Sample
    fun map() {
        val string = "kotlin"
        assertPrints(string.map { it.toUpperCase() }, "[K, O, T, L, I, N]")
    }

    @Sample
    fun indexOf() {
        fun matchDetails(inputString: String, whatToFind: String, startIndex: Int = 0): String {
            val matchIndex = inputString.indexOf(whatToFind, startIndex)
            return "Searching for '$whatToFind' in '$inputString' starting at position $startIndex: " +
                    if (matchIndex >= 0) "Found at $matchIndex" else "Not found"
        }

        val inputString = "Never ever give up"
        val toFind = "ever"

        assertPrints(matchDetails(inputString, toFind), "Searching for 'ever' in 'Never ever give up' starting at position 0: Found at 1")
        assertPrints(matchDetails(inputString, toFind, 2), "Searching for 'ever' in 'Never ever give up' starting at position 2: Found at 6")
        assertPrints(matchDetails(inputString, toFind, 10), "Searching for 'ever' in 'Never ever give up' starting at position 10: Not found")
    }

    @Sample
    fun last() {
        val string = "Kotlin 1.4.0"
        assertPrints(string.last(), "0")
        assertPrints(string.last { it.isLetter() }, "n")
        assertPrints(string.lastOrNull { it > 'z' }, "null")
        assertFails { string.last { it > 'z' } }

        val emptyString = ""
        assertPrints(emptyString.lastOrNull(), "null")
        assertFails { emptyString.last() }
    }

    @Sample
    fun toPattern() {
        val string = "this is a regex"
        val pattern = string.toPattern(1)
        assertPrints(pattern.pattern(), string)
        assertTrue(pattern.flags() == 1)
    }

    @Sample
    fun equals() {
        val nullString: String? = null
        val emptyString = ""
        val lowerCaseString = "something"
        val upperCaseString = "SOMETHING"
        val lowerAndUpperCaseString = "SoMeThInG"

        assertTrue(nullString.equals(null))
        assertFalse(nullString.equals("something"))
        assertFalse(nullString.equals("SOMETHING"))
        assertFalse(nullString.equals(""))
        assertFalse(nullString.equals("something", ignoreCase = true))
        assertFalse(nullString.equals("SOMETHING", ignoreCase = true))
        assertFalse(nullString.equals("", ignoreCase = true))

        assertFalse(emptyString.equals(null))
        assertFalse(emptyString.equals("something"))
        assertFalse(emptyString.equals("SOMETHING"))
        assertTrue(emptyString.equals(""))
        assertFalse(emptyString.equals("something", ignoreCase = true))
        assertFalse(emptyString.equals("SOMETHING", ignoreCase = true))
        assertTrue(emptyString.equals("", ignoreCase = true))

        assertFalse(lowerCaseString.equals(null))
        assertTrue(lowerCaseString.equals("something"))
        assertFalse(lowerCaseString.equals("SOMETHING"))
        assertFalse(lowerCaseString.equals(""))
        assertTrue(lowerCaseString.equals("something", ignoreCase = true))
        assertTrue(lowerCaseString.equals("SOMETHING", ignoreCase = true))
        assertFalse(lowerCaseString.equals("", ignoreCase = true))

        assertFalse(upperCaseString.equals(null))
        assertFalse(upperCaseString.equals("something"))
        assertTrue(upperCaseString.equals("SOMETHING"))
        assertFalse(upperCaseString.equals(""))
        assertTrue(upperCaseString.equals("something", ignoreCase = true))
        assertTrue(upperCaseString.equals("SOMETHING", ignoreCase = true))
        assertFalse(upperCaseString.equals("", ignoreCase = true))

        assertFalse(lowerAndUpperCaseString.equals(null))
        assertFalse(lowerAndUpperCaseString.equals("something"))
        assertFalse(lowerAndUpperCaseString.equals("SOMETHING"))
        assertFalse(lowerAndUpperCaseString.equals(""))
        assertTrue(lowerAndUpperCaseString.equals("something", ignoreCase = true))
        assertTrue(lowerAndUpperCaseString.equals("SOMETHING", ignoreCase = true))
        assertFalse(lowerAndUpperCaseString.equals("", ignoreCase = true))
    }

    @Sample
    fun encodeToByteArray() {
        val str = "Hello"
        val byteArray = str.encodeToByteArray()
        assertPrints(byteArray.contentToString(), "[72, 101, 108, 108, 111]")
        assertPrints(byteArray.toString(Charsets.UTF_8), str)

        val byteArrayWithoutFirstLetter = str.encodeToByteArray(startIndex = 1, endIndex = str.length)
        assertPrints(byteArrayWithoutFirstLetter.contentToString(), "[101, 108, 108, 111]")
        assertPrints(byteArrayWithoutFirstLetter.toString(Charsets.UTF_8), "ello")

        val byteArrayWithoutLastLetter = str.encodeToByteArray(startIndex = 0, endIndex = str.length - 1)
        assertPrints(byteArrayWithoutLastLetter.contentToString(), "[72, 101, 108, 108]")
        assertPrints(byteArrayWithoutLastLetter.toString(Charsets.UTF_8), "Hell")
    }

    @Sample
    fun subString() {
        val str = "abcde"
        assertPrints(str.substring(0), "abcde")
        assertPrints(str.substring(1), "bcde")
        assertFails { str.substring(6) }
        assertPrints(str.substring(0, 0), "")
        assertPrints(str.substring(0, 1), "a")
        assertFails { str.substring(0, 6) }
        assertFails { str.substring(1, 0) }
        assertPrints(str.substring(1, 2), "b")
        assertFails { str.substring(1, 6) }
    }

    @Sample
    fun startsWith() {
        val str = "abcde"
        assertTrue(str.startsWith("abc", false))
        assertTrue(str.startsWith("abc", true))
        assertFalse(str.startsWith("ABC", false))
        assertTrue(str.startsWith("ABC", true))
        assertFalse(str.startsWith("abc", 1, false))
        assertFalse(str.startsWith("abc", 1, true))
        assertFalse(str.startsWith("ABC", 1, false))
        assertFalse(str.startsWith("ABC", 1, true))
        assertFalse(str.startsWith("bcd", false))
        assertFalse(str.startsWith("bcd", true))
        assertFalse(str.startsWith("BCD", false))
        assertFalse(str.startsWith("BCD", true))
        assertTrue(str.startsWith("bcd", 1, false))
        assertTrue(str.startsWith("bcd", 1, true))
        assertFalse(str.startsWith("BCD", 1, false))
        assertTrue(str.startsWith("BCD", 1, true))
    }

    @Sample
    fun endsWith() {
        val str = "abcde"
        assertTrue(str.endsWith("cde", false))
        assertTrue(str.endsWith("cde", true))
        assertFalse(str.endsWith("CDE", false))
        assertTrue(str.endsWith("CDE", true))
        assertFalse(str.endsWith("bcd", false))
        assertFalse(str.endsWith("bcd", true))
        assertFalse(str.endsWith("BCD", false))
        assertFalse(str.endsWith("BCD", true))
    }

    @Sample
    fun codePointAt() {
        val str = "abc"
        assertPrints(str.codePointAt(0).toString(), "97")
        assertPrints(str.codePointAt(1).toString(), "98")
        assertPrints(str.codePointAt(2).toString(), "99")
        assertFails { str.codePointAt(3) }
    }

    @Sample
    fun codePointBefore() {
        val str = "abc"
        assertFails { str.codePointBefore(0) }
        assertPrints(str.codePointBefore(1).toString(), "97")
        assertPrints(str.codePointBefore(2).toString(), "98")
        assertPrints(str.codePointBefore(3).toString(), "99")
    }

    @Sample
    fun codePointCount() {
        val str = "abc"
        assertPrints(str.codePointCount(0, 2).toString(), "2")
        assertPrints(str.codePointCount(1, 3).toString(), "2")
        assertPrints(str.codePointCount(2, 2).toString(), "0")
        assertFails { str.codePointCount(3, 2) }
    }
}
