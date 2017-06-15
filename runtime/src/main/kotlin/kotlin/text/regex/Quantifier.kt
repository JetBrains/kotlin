// TODO: License.

package kotlin.text.regex

import kotlin.IllegalArgumentException

/**
 * Represents RE quantifier; contains two fields responsible for min and max number of repetitions.
 * Negative value for maximum number of repetition represents infinity(i.e. +,*)

 * @author Nikolay A. Kuznetsov
 */
// TODO: May be replace with some other class (Range?).
internal class Quantifier(val min: Int, val max: Int = min) : SpecialToken() {

    init {
        if (min < 0 || max < -1) {
            throw IllegalArgumentException()
        }
    }

    override fun toString() = "{$min, ${if (max == -1) "" else max}}"

    override val type: Type = SpecialToken.Type.QUANTIFIER

    companion object {
        val starQuantifier = Quantifier(0, -1)
        val plusQuantifier = Quantifier(1, -1)
        val altQuantifier  = Quantifier(0,  1)

        val INF = -1

        fun fromLexerToken(token: Int) = when(token) {
            Lexer.QUANT_STAR, Lexer.QUANT_STAR_P, Lexer.QUANT_STAR_R -> starQuantifier
            Lexer.QUANT_ALT, Lexer.QUANT_ALT_P, Lexer.QUANT_ALT_R -> altQuantifier
            Lexer.QUANT_PLUS, Lexer.QUANT_PLUS_P, Lexer.QUANT_PLUS_R -> plusQuantifier
            else -> throw IllegalArgumentException()
        }
    }
}

