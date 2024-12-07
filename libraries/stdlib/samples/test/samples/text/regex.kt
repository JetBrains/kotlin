package samples.text

import samples.*

class Regexps {

    @Sample
    fun matchDestructuringToGroupValues() {
        val inputString = "John 9731879"
        val match = Regex("(\\w+) (\\d+)").find(inputString)!!
        val (name, phone) = match.destructured

        assertPrints(name, "John")     // value of the first group matched by \w+
        assertPrints(phone, "9731879") // value of the second group matched by \d+

        // group with the zero index is the whole substring matched by the regular expression
        assertPrints(match.groupValues, "[John 9731879, John, 9731879]")

        val numberedGroupValues = match.destructured.toList()
        // destructured group values only contain values of the groups, excluding the zeroth group.
        assertPrints(numberedGroupValues, "[John, 9731879]")
    }

    @Sample
    fun find() {
        val inputString = "to be or not to be"
        val regex = "to \\w{2}".toRegex()
        // If there is matching string, then find method returns non-null MatchResult
        val match = regex.find(inputString)!!
        assertPrints(match.value, "to be")
        assertPrints(match.range, "0..4")

        val nextMatch = match.next()!!
        assertPrints(nextMatch.range, "13..17")

        val regex2 = "this".toRegex()
        // If there is no matching string, then find method returns null
        assertPrints(regex2.find(inputString), "null")

        val regex3 = regex
        // to be or not to be
        //              ^^^^^
        // Because the search starts from the index 2, it finds the last "to be".
        assertPrints(regex3.find(inputString, 2)!!.range, "13..17")
    }

    @Sample
    fun findAll() {
        val text = "Hello Alice. Hello Bob. Hello Eve."
        val regex = Regex("Hello (.*?)[.]")
        val matches = regex.findAll(text)
        val names = matches.map { it.groupValues[1] }.joinToString()
        assertPrints(names, "Alice, Bob, Eve")
    }

    @Sample
    fun splitToSequence() {
        val colors = "green, red , brown&blue, orange, pink&green"
        val regex = "[,\\s]+".toRegex()

        val mixedColor = regex.splitToSequence(colors)
            .onEach { println(it) }
            .firstOrNull { it.contains('&') }

        assertPrints(mixedColor, "brown&blue")
    }

    @Sample
    fun matchesAt() {
        val releaseText = "Kotlin 1.5.30 is released!"
        val versionRegex = "\\d[.]\\d[.]\\d+".toRegex()
        assertPrints(versionRegex.matchesAt(releaseText, 0), "false")
        assertPrints(versionRegex.matchesAt(releaseText, 7), "true")
    }

    @Sample
    fun matchAt() {
        val releaseText = "Kotlin 1.5.30 is released!"
        val versionRegex = "\\d[.]\\d[.]\\d+".toRegex()
        assertPrints(versionRegex.matchAt(releaseText, 0), "null")
        assertPrints(versionRegex.matchAt(releaseText, 7)?.value, "1.5.30")
    }

    @Sample
    fun replaceWithExpression() {
        val text = "The events are on 15-09-2024 and 16-10-2024."
        // Regex to match dates in dd-mm-yyyy format
        val dateRegex = "(?<day>\\d{2})-(?<month>\\d{2})-(?<year>\\d{4})".toRegex()
        // Replacement expression that puts day, month and year values in the ISO 8601 recommended yyyy-mm-dd format
        val replacement = "\${year}-\${month}-\${day}"

        // Replacing all occurrences of dates from dd-mm-yyyy to yyyy-mm-dd format
        assertPrints(dateRegex.replace(text, replacement), "The events are on 2024-09-15 and 2024-10-16.")

        // One can also reference the matched groups by index
        assertPrints(dateRegex.replace(text, "$3-$2-$1"), "The events are on 2024-09-15 and 2024-10-16.")

        // Using a backslash to include the special character '$' as a literal in the result
        assertPrints(dateRegex.replace(text, "$3-\\$2-$1"), "The events are on 2024-\$2-15 and 2024-\$2-16.")
    }

    @Sample
    fun replaceFirstWithExpression() {
        val text = "The events are on 15-09-2024 and 16-10-2024."
        // Regex to match dates in dd-mm-yyyy format
        val dateRegex = "(?<day>\\d{2})-(?<month>\\d{2})-(?<year>\\d{4})".toRegex()
        // Replacement expression that puts day, month and year values in the ISO 8601 recommended yyyy-mm-dd format
        val replacement = "\${year}-\${month}-\${day}"

        // Replacing the first occurrence of a date from dd-mm-yyyy to yyyy-mm-dd format
        assertPrints(dateRegex.replaceFirst(text, replacement), "The events are on 2024-09-15 and 16-10-2024.")

        // One can also reference the matched groups by index
        assertPrints(dateRegex.replaceFirst(text, "$3-$2-$1"), "The events are on 2024-09-15 and 16-10-2024.")

        // Using a backslash to include the special character '$' as a literal in the result
        assertPrints(dateRegex.replaceFirst(text, "$3-\\$2-$1"), "The events are on 2024-\$2-15 and 16-10-2024.")
    }
}