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
}
