// TODO: License.

package kotlin.text.regex

/**
 * Line terminator factory
 *
 * @author Nikolay A. Kuznetsov
 */
internal abstract class AbstractLineTerminator {

    /** Checks if the single character is a line terminator or not. */
    open fun isLineTerminator(char: Char): Boolean = isLineTerminator(char.toInt())
    /** Checks if the codepoint is a line terminator or not */
    abstract fun isLineTerminator(codepoint: Int): Boolean
    /** Checks if the pair of symbols is a line terminator (e.g. for \r\n case) */
    abstract fun isLineTerminatorPair(char1: Char, char2: Char): Boolean
    /** Checks if a [checked] character is after a line terminator using the [previous] character.*/
    abstract fun isAfterLineTerminator(previous: Char, checked: Char): Boolean

    companion object {
        val unixLT: AbstractLineTerminator by lazy {
            object : AbstractLineTerminator() {
                override fun isLineTerminator(codepoint: Int): Boolean = (codepoint == '\n'.toInt())
                override fun isLineTerminatorPair(char1: Char, char2: Char): Boolean = false
                override fun isAfterLineTerminator(previous: Char, checked: Char): Boolean = (previous == '\n')
            }
        }

        val unicodeLT: AbstractLineTerminator by lazy {
            object : AbstractLineTerminator() {
                override fun isLineTerminatorPair(char1: Char, char2: Char): Boolean {
                    return char1 == '\r' && char2 == '\n'
                }

                override fun isLineTerminator(codepoint: Int): Boolean {
                    return codepoint == '\n'.toInt()
                           || codepoint == '\r'.toInt()
                           || codepoint == '\u0085'.toInt()
                           || codepoint or 1 == '\u2029'.toInt()
                }

                override fun isAfterLineTerminator(previous: Char, checked: Char): Boolean {
                    return previous == '\n' || previous == '\u0085' || previous.toInt() or 1 == '\u2029'.toInt()
                           || previous == '\r' && checked != '\n'
                }
            }
        }

        fun getInstance(flag: Int): AbstractLineTerminator {
            if (flag and Pattern.UNIX_LINES != 0) {
                return unixLT
            } else {
                return unicodeLT
            }
        }
    }
}
