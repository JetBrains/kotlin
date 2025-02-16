package samples.text

import samples.*
import java.util.Locale
import java.util.regex.Pattern
import kotlin.test.*

class Strings {

    @Suppress("DEPRECATION")
    @Sample
    fun capitalize() {
        assertPrints("abcd".capitalize(), "Abcd")
        assertPrints("Abcd".capitalize(), "Abcd")
    }

    @Suppress("DEPRECATION")
    @Sample
    fun decapitalize() {
        assertPrints("abcd".decapitalize(), "abcd")
        assertPrints("Abcd".decapitalize(), "abcd")
    }

    @Sample
    fun replaceFirstChar() {
        assertPrints("kotlin".replaceFirstChar { it.uppercase() }, "Kotlin")

        val sentence = "Welcome to Kotlin!"
        val words = sentence.split(' ');
        assertPrints(words.joinToString(separator = "_") { word -> word.replaceFirstChar { it.lowercase() } }, "welcome_to_kotlin!")
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
    fun findLast() {
        val text = "a1b2c3d4e5"

        assertPrints(text.findLast { it.isLetter() }, "e")
        assertPrints(text.findLast { it.isUpperCase() }, "null")
        assertPrints("".findLast { it.isLowerCase() }, "null")
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
        val result = string.associate { char -> char to char.code }
        // notice each letter occurs only once
        assertPrints(result, "{b=98, o=111, n=110, e=101,  =32, j=106, u=117, r=114, é=233}")
    }

    @Sample
    fun associateBy() {
        val string = "bonne journée"
        // associate each character by its code
        val result = string.associateBy { char -> char.code }
        // notice each char code occurs only once
        assertPrints(result, "{98=b, 111=o, 110=n, 101=e, 32= , 106=j, 117=u, 114=r, 233=é}")
    }

    @Sample
    fun associateByWithValueTransform() {
        val string = "bonne journée"
        // associate each character by the code of its upper case equivalent and transform the character to upper case
        val result = string.associateBy({ char -> char.uppercaseChar().code }, { char -> char.uppercaseChar() })
        // notice each char code occurs only once
        assertPrints(result, "{66=B, 79=O, 78=N, 69=E, 32= , 74=J, 85=U, 82=R, 201=É}")
    }

    @Sample
    fun associateByTo() {
        val string = "bonne journée"
        // associate each character by its code
        val result = mutableMapOf<Int, Char>()
        string.associateByTo(result) { char -> char.code }
        // notice each char code occurs only once
        assertPrints(result, "{98=b, 111=o, 110=n, 101=e, 32= , 106=j, 117=u, 114=r, 233=é}")
    }

    @Sample
    fun associateByToWithValueTransform() {
        val string = "bonne journée"
        // associate each character by the code of its upper case equivalent and transform the character to upper case
        val result = mutableMapOf<Int, Char>()
        string.associateByTo(result, { char -> char.uppercaseChar().code }, { char -> char.uppercaseChar() })
        // notice each char code occurs only once
        assertPrints(result, "{66=B, 79=O, 78=N, 69=E, 32= , 74=J, 85=U, 82=R, 201=É}")
    }

    @Sample
    fun associateTo() {
        val string = "bonne journée"
        // associate each character with its code
        val result = mutableMapOf<Char, Int>()
        string.associateTo(result) { char -> char to char.code }
        // notice each letter occurs only once
        assertPrints(result, "{b=98, o=111, n=110, e=101,  =32, j=106, u=117, r=114, é=233}")
    }

    @Sample
    fun associateWith() {
        val string = "bonne journée"
        // associate each character with its code
        val result = string.associateWith { char -> char.code }
        // notice each letter occurs only once
        assertPrints(result, "{b=98, o=111, n=110, e=101,  =32, j=106, u=117, r=114, é=233}")
    }

    @Sample
    fun associateWithTo() {
        val string = "bonne journée"
        // associate each character with its code
        val result = mutableMapOf<Char, Int>()
        string.associateWithTo(result) { char -> char.code }
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
    fun lowercase() {
        assertPrints("Iced frappé!".lowercase(), "iced frappé!")
    }

    @Sample
    fun lowercaseLocale() {
        assertPrints("KOTLIN".lowercase(), "kotlin")
        val turkishLocale = Locale.forLanguageTag("tr")
        assertPrints("KOTLIN".lowercase(turkishLocale), "kotlın")
    }

    @Sample
    fun uppercase() {
        assertPrints("Iced frappé!".uppercase(), "ICED FRAPPÉ!")
    }

    @Sample
    fun uppercaseLocale() {
        assertPrints("Kotlin".uppercase(), "KOTLIN")
        val turkishLocale = Locale.forLanguageTag("tr")
        assertPrints("Kotlin".uppercase(turkishLocale), "KOTLİN")
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
        assertPrints(string.map { it.uppercaseChar() }, "[K, O, T, L, I, N]")
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
    fun replace() {
        val inputString0 = "Mississippi"
        val inputString1 = "Insufficient data for meaningful answer."

        assertPrints(inputString0.replace('s', 'z'), "Mizzizzippi")
        assertPrints(inputString1.replace("data", "information"), "Insufficient information for meaningful answer.")
    }

    @Sample
    fun contentEquals() {
        val stringBuilder = StringBuilder()
        stringBuilder.append("Kot").append("lin")
        assertPrints(stringBuilder, "Kotlin")
        assertTrue(stringBuilder contentEquals "Kotlin")

        stringBuilder.setCharAt(0, 'k')
        assertPrints(stringBuilder, "kotlin")
        assertFalse("Kotlin".contentEquals(stringBuilder))
        assertTrue("Kotlin".contentEquals(stringBuilder, ignoreCase = true))
    }

    @Sample
    fun toBooleanStrict() {
        assertPrints("true".toBooleanStrict(), "true")
        assertFails { "True".toBooleanStrict() }
        assertFails { "TRUE".toBooleanStrict() }

        assertPrints("false".toBooleanStrict(), "false")
        assertFails { "False".toBooleanStrict() }
        assertFails { "FALSE".toBooleanStrict() }

        assertFails { "abc".toBooleanStrict() }
    }

    @Sample
    fun toBooleanStrictOrNull() {
        assertPrints("true".toBooleanStrictOrNull(), "true")
        assertPrints("True".toBooleanStrictOrNull(), "null")
        assertPrints("TRUE".toBooleanStrictOrNull(), "null")

        assertPrints("false".toBooleanStrictOrNull(), "false")
        assertPrints("False".toBooleanStrictOrNull(), "null")
        assertPrints("FALSE".toBooleanStrictOrNull(), "null")

        assertPrints("abc".toBooleanStrictOrNull(), "null")
    }

    @Sample
    fun splitToSequence() {
        val colors = "green, red , brown&blue, orange, pink&green"
        val regex = "[,\\s]+".toRegex()

        val mixedColor = colors.splitToSequence(regex)
            .onEach { println(it) }
            .firstOrNull { it.contains('&') }

        assertPrints(mixedColor, "brown&blue")
    }

    @Sample
    fun toPattern() {
        val string = "Kotlin [1-9]+\\.[0-9]\\.[0-9]+"
        val pattern = string.toPattern(Pattern.CASE_INSENSITIVE)
        assertPrints(pattern.pattern(), string)
        assertTrue(pattern.flags() == Pattern.CASE_INSENSITIVE)
        assertTrue(pattern.matcher("Kotlin 2.1.255").matches())
        assertTrue(pattern.matcher("kOtLiN 21.0.1").matches())
        assertFalse(pattern.matcher("Java 21.0.1").matches())
        assertFalse(pattern.matcher("Kotlin 2.0").matches())

        assertFails { "[0-9".toPattern(Pattern.CASE_INSENSITIVE) } // the given regex is malformed
    }

    @Sample
    fun encodeToByteArray() {
        val str = "Kòtlin\u00a02.1.255"
        val byteArray = str.encodeToByteArray()
        assertPrints(byteArray.contentToString(), "[75, -61, -78, 116, 108, 105, 110, -62, -96, 50, 46, 49, 46, 50, 53, 53]")

        val byteArrayWithoutFirstLetter = str.encodeToByteArray(startIndex = 1, endIndex = str.length)
        assertPrints(byteArrayWithoutFirstLetter.contentToString(), "[-61, -78, 116, 108, 105, 110, -62, -96, 50, 46, 49, 46, 50, 53, 53]")

        val byteArrayWithoutLastLetter = str.encodeToByteArray(startIndex = 0, endIndex = str.length - 1)
        assertPrints(byteArrayWithoutLastLetter.contentToString(), "[75, -61, -78, 116, 108, 105, 110, -62, -96, 50, 46, 49, 46, 50, 53]")

        val byteArrayWithoutNbsp = str.replace("\u00a0", " ").replace("ò", "o").encodeToByteArray()
        assertPrints(byteArrayWithoutNbsp.contentToString(), "[75, 111, 116, 108, 105, 110, 32, 50, 46, 49, 46, 50, 53, 53]")
    }

    @Sample
    fun subStringFromStartIndex() {
        val str = "abcde"
        assertPrints(str.substring(0), "abcde")
        assertPrints(str.substring(1), "bcde")
        assertFails { str.substring(6) } // index exceeds string length
    }

    @Sample
    fun substringByStandAndEndIndices() {
        val str = "abcde"
        assertPrints(str.substring(0, 0), "")
        assertPrints(str.substring(0, 1), "a")
        assertFails { str.substring(0, 6) } // index end exceeds string length
        assertFails { str.substring(1, 0) } // index end is smaller thant index start
    }

    @Sample
    fun startsWithFromPrefix() {
        val str = "abcde"
        assertTrue(str.startsWith("abc"))
        assertFalse(str.startsWith("ABC"))
        assertFalse(str.startsWith("bcd"))
        assertFalse(str.startsWith("BCD"))
    }

    @Sample
    fun startsWithFromPrefixAndIgnoreCase() {
        val str = "abcde"
        assertTrue(str.startsWith("abc", true))
        assertTrue(str.startsWith("ABC", true))
        assertFalse(str.startsWith("bcd", true))
        assertFalse(str.startsWith("BCD", true))
    }

    @Sample
    fun startsWithFromPrefixAndStartIndex() {
        val str = "abcde"
        assertFalse(str.startsWith("abc", 1))
        assertFalse(str.startsWith("ABC", 1))
        assertFalse(str.startsWith("BCD", 1))
        assertTrue(str.startsWith("bcd", 1))
    }

    @Sample
    fun startsWithFromPrefixAndStartIndexAndIgnoreCase() {
        val str = "abcde"
        assertFalse(str.startsWith("abc", 1, true))
        assertFalse(str.startsWith("ABC", 1, true))
        assertTrue(str.startsWith("bcd", 1, true))
        assertTrue(str.startsWith("BCD", 1, true))
    }

    @Sample
    fun endsWithFromSuffix() {
        val str = "abcde"
        assertTrue(str.endsWith("cde"))
        assertFalse(str.endsWith("CDE"))
        assertFalse(str.endsWith("bcd"))
        assertFalse(str.endsWith("BCD"))
    }

    @Sample
    fun endsWith() {
        val str = "abcde"
        assertTrue(str.endsWith("cde", true))
        assertTrue(str.endsWith("CDE", true))
        assertFalse(str.endsWith("bcd", true))
        assertFalse(str.endsWith("BCD", true))
    }

    @Sample
    fun codePointAt() {
        val str = "abc"
        assertPrints(str.codePointAt(0).toString(), "97")
        assertPrints(str.codePointAt(1).toString(), "98")
        assertPrints(str.codePointAt(2).toString(), "99")
        assertFails { str.codePointAt(3) }
        assertFails { str.codePointAt(-1) }
        assertFails { str.codePointAt(str.length) }

        val broccoli = "🥦"
        assertPrints(broccoli.codePointAt(0), "129382")
        assertPrints(broccoli.codePointAt(1), "56678")
    }

    @Sample
    fun codePointBefore() {
        val str = "abc"
        assertPrints(str.codePointBefore(1).toString(), "97")
        assertPrints(str.codePointBefore(2).toString(), "98")
        assertPrints(str.codePointBefore(3).toString(), "99")
        assertPrints(str.codePointBefore(str.length).toString(), "99")
        assertFails { str.codePointBefore(0) }
        assertFails { str.codePointBefore(-1) }
        assertFails { str.codePointBefore(str.length + 1) }

        val broccoli = "🥦"
        assertPrints(broccoli.codePointBefore(1), "55358")
        assertPrints(broccoli.codePointBefore(2), "129382")
        assertFails { broccoli.codePointBefore(3) }
    }

    @Sample
    fun codePointCount() {
        val str = "abc"
        assertPrints(str.codePointCount(0, 2).toString(), "2")
        assertPrints(str.codePointCount(1, 3).toString(), "2")
        assertPrints(str.codePointCount(2, 2).toString(), "0")
        assertFails { str.codePointCount(3, 2) }
        assertFails { str.codePointCount(-1, 2) }
        assertFails { str.codePointCount(0, str.length+1) }

        val broccoli = "🥦"
        assertPrints(broccoli.codePointCount(0, broccoli.length /* = 2 */), "1")
        assertPrints(broccoli.codePointCount(0, broccoli.length - 1 /* = 1 */), "1")
    }

    @Sample
    fun formatStatic() {
        // format negative number in parentheses
        val negativeNumberInParentheses = String.format("%(d means %1\$d", -31416)
        assertPrints(negativeNumberInParentheses, "(31416) means -31416")
    }

    @Sample
    fun formatExtension() {
        // format negative number in parentheses
        val negativeNumberInParentheses = "%(d means %1\$d".format(-31416)
        assertPrints(negativeNumberInParentheses, "(31416) means -31416")
    }

    @Sample
    fun formatWithLocaleStatic() {
        // format with German conventions
        val withGermanThousandsSeparator = String.format(Locale.GERMANY, "%,d", 12345)
        assertPrints(withGermanThousandsSeparator, "12.345")

        // format with US conventions
        val withUSThousandsSeparator = String.format(Locale.US, "%,d", 12345)
        assertPrints(withUSThousandsSeparator, "12,345")
    }

    @Sample
    fun formatWithLocaleExtension() {
        // format with German conventions
        val withGermanThousandsSeparator = "%,d".format(Locale.GERMANY, 12345)
        assertPrints(withGermanThousandsSeparator, "12.345")
        // 12.345

        // format with US conventions
        val withUSThousandsSeparator = "%,d".format(Locale.US, 12345)
        assertPrints(withUSThousandsSeparator, "12,345")
    }

    @Sample
    fun replaceWithExpression() {
        val text = "The events are on 15-09-2024 and 16-10-2024."
        // Regex to match dates in dd-mm-yyyy format
        val dateRegex = "(?<day>\\d{2})-(?<month>\\d{2})-(?<year>\\d{4})".toRegex()
        // Replacement expression that puts day, month and year values in the ISO 8601 recommended yyyy-mm-dd format
        val replacement = "\${year}-\${month}-\${day}"

        // Replacing all occurrences of dates from dd-mm-yyyy to yyyy-mm-dd format
        assertPrints(text.replace(dateRegex, replacement), "The events are on 2024-09-15 and 2024-10-16.")

        // One can also reference the matched groups by index
        assertPrints(text.replace(dateRegex, "$3-$2-$1"), "The events are on 2024-09-15 and 2024-10-16.")

        // Using a backslash to include the special character '$' as a literal in the result
        assertPrints(text.replace(dateRegex, "$3-\\$2-$1"), "The events are on 2024-\$2-15 and 2024-\$2-16.")
    }

    @Sample
    fun replaceFirstWithExpression() {
        val text = "The events are on 15-09-2024 and 16-10-2024."
        // Regex to match dates in dd-mm-yyyy format
        val dateRegex = "(?<day>\\d{2})-(?<month>\\d{2})-(?<year>\\d{4})".toRegex()
        // Replacement expression that puts day, month and year values in the ISO 8601 recommended yyyy-mm-dd format
        val replacement = "\${year}-\${month}-\${day}"

        // Replacing the first occurrence of a date from dd-mm-yyyy to yyyy-mm-dd format
        assertPrints(text.replaceFirst(dateRegex, replacement), "The events are on 2024-09-15 and 16-10-2024.")

        // One can also reference the matched groups by index
        assertPrints(text.replaceFirst(dateRegex, "$3-$2-$1"), "The events are on 2024-09-15 and 16-10-2024.")

        // Using a backslash to include the special character '$' as a literal in the result
        assertPrints(text.replaceFirst(dateRegex, "$3-\\$2-$1"), "The events are on 2024-\$2-15 and 16-10-2024.")
    }

    @Sample
    fun removeRangeString() {
        val text = "Hello, world!"

        // Removing the range that correspond to the "world" substring
        assertPrints(text.removeRange(startIndex = 7, endIndex = 12), "Hello, !")

        // Also possible to remove using a Range
        assertPrints(text.removeRange(7..11), "Hello, !")

        // The original string is not changed
        assertPrints(text, "Hello, world!")

        // Throws if startIndex is greater than endIndex
        assertFails { text.removeRange(startIndex = 7, endIndex = 4) }
        // Throws if startIndex or endIndex is out of range of the string indices
        assertFails { text.removeRange(startIndex = 7, endIndex = 20) }
    }

    @Sample
    fun removeRangeCharSequence() {
        val text = StringBuilder("Hello, world!")

        // Removing the range that correspond to the "world" subsequence
        assertPrints(text.removeRange(startIndex = 7, endIndex = 12), "Hello, !")

        // Also possible to remove using a Range
        assertPrints(text.removeRange(7..11), "Hello, !")

        // The original char sequence is not changed
        assertPrints(text, "Hello, world!")

        // Throws if startIndex is greater than endIndex
        assertFails { text.removeRange(startIndex = 7, endIndex = 4) }
        // Throws if startIndex or endIndex is out of range of the char sequence indices
        assertFails { text.removeRange(startIndex = 7, endIndex = 20) }
    }
}
