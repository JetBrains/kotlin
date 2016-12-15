package kotlin.text

header interface Appendable {
    fun append(c: Char): Appendable
}

header class StringBuilder : Appendable, CharSequence {
    constructor()
    constructor(capacity: Int)
    constructor(seq: CharSequence)

    override val length: Int
    override operator fun get(index: Int): Char
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence

    fun reverse(): StringBuilder
    override fun append(c: Char): Appendable
}

header class Regex {
    constructor(pattern: String)
    constructor(pattern: String, option: RegexOption)
    constructor(pattern: String, options: Set<RegexOption>)

    fun matches(input: CharSequence): Boolean
    fun containsMatchIn(input: CharSequence): Boolean
    fun replace(input: CharSequence, replacement: String): String
    fun replace(input: CharSequence, transform: (MatchResult) -> CharSequence): String
    fun replaceFirst(input: CharSequence, replacement: String): String
    fun split(input: CharSequence, limit: Int): List<String>
}

header class MatchGroup

header enum class RegexOption

public header interface MatchGroupCollection : Collection<MatchGroup?> {
    public operator fun get(index: Int): MatchGroup?
}

public header interface MatchNamedGroupCollection : MatchGroupCollection {
    public operator fun get(name: String): MatchGroup?
}


public header interface MatchResult {
    public val range: IntRange
    public val value: String
    public val groups: MatchGroupCollection
    public val groupValues: List<String>
    //public val destructured: Destructured

    public fun next(): MatchResult?


}

// From char.kt

header fun Char.isWhitespace(): Boolean
header fun Char.toLowerCase(): Char
header fun Char.toUpperCase(): Char
header fun Char.isHighSurrogate(): Boolean
header fun Char.isLowSurrogate(): Boolean

// From string.kt

internal header fun String.nativeIndexOf(str: String, fromIndex: Int): Int
internal header fun String.nativeLastIndexOf(str: String, fromIndex: Int): Int
internal header fun String.nativeStartsWith(s: String, position: Int): Boolean
internal header fun String.nativeEndsWith(s: String): Boolean

// From stringsCode.kt

internal inline header fun String.nativeIndexOf(ch: Char, fromIndex: Int): Int
internal inline header fun String.nativeLastIndexOf(ch: Char, fromIndex: Int): Int

header fun CharSequence.isBlank(): Boolean
header fun CharSequence.regionMatches(thisOffset: Int, other: CharSequence, otherOffset: Int, length: Int, ignoreCase: Boolean): Boolean
