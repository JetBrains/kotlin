package org.jetbrains.kotlin.doc.highlighter

import java.util.HashMap
import kotlin.template.HtmlFormatter
import com.intellij.psi.*
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.jet.lexer.*

fun main(args: Array<String>) {
    val tool = SyntaxHighligher()
    val answer = tool.highlight("""    val x = arrayList(1, 2, 3)
    println("hello")""")
    println(answer)
}

/**
 * Syntax highlights Kotlin code
 */
class SyntaxHighligher() {
    var formatter: HtmlFormatter = HtmlFormatter()
    val styleMap = createStyleMap()

    /** Highlights the given kotlin code as HTML */
    fun highlight(code: String): String {
        try {
            val builder = StringBuilder()
            builder.append(
            "<div class=\"code panel\" style=\"border-width: 1px\">" +
            "<div class=\"codeContent panelContent\">" +
            "<div class=\"container\">"
            )

            // lets add the leading whitespace first
            var idx = 0
            while (Character.isWhitespace(code[idx])) {
                idx++
            }
            if (idx > 0) {
                val space = code.substring(0, idx)
                builder.append("""<code class="jet whitespace">$space</code>""")
            }
            val lexer = JetLexer()
            lexer.start(code)
            val end = lexer.getTokenEnd()
            while (true) {
                lexer.advance()
                val token = lexer.getTokenType()
                if (token == null) break
                val tokenText = lexer.getTokenSequence().toString().replaceAll("\n", "\r\n")
                var style: String? = null
                if (token is JetKeywordToken) {
                    style = "keyword"
                } else if (token == JetTokens.IDENTIFIER) {
                    val types = JetTokens.SOFT_KEYWORDS.getTypes()
                    if (types != null) {
                        for (softKeyword in types) {
                            if (softKeyword is JetKeywordToken) {
                                if (softKeyword.getValue().equals(tokenText)) {
                                    style = "softkeyword"
                                    break
                                }
                            }
                        }
                    }
                    style = if (style == null) "plain" else style
                } else if (styleMap.containsKey(token)) {
                    style = styleMap.get(token)
                    if (style == null) {
                        println("Warning: No style for token $token")
                    }
                } else {
                    style = "plain"
                }
                builder.append("""<code class="jet $style">""")
                formatter.format(builder, tokenText)
                builder.append("</code>")
            }

            builder.append("</div>")
            builder.append("</div>")
            builder.append("</div>")
            return builder.toString() ?: ""
        } catch (e: Exception) {
            println("Warning: failed to parse code $e")
            val builder = StringBuilder()
            builder.append("""<div class="jet herror">Jet highlighter error ["${e.javaClass.getSimpleName()}"]: """)
            formatter.format(builder, e.getMessage())
            builder.append("<br/>")
            builder.append("Original text:")
            builder.append("<pre>")
            formatter.format(builder, code)
            builder.append("</pre>")
            builder.append("</div>")
            return builder.toString() ?: ""
        }
    }


    protected fun createStyleMap(): Map<IElementType?, String> {
        val styleMap = HashMap<IElementType?, String>()

        fun putAll(tokenSet: TokenSet?, style: String): Unit {
            if (tokenSet != null) {
                for (token in tokenSet.getTypes().orEmpty()) {
                    styleMap.put(token, style)
                }
            }
        }

        styleMap.put(JetTokens.BLOCK_COMMENT, "jet-comment")
        styleMap.put(JetTokens.DOC_COMMENT, "jet-comment")
        styleMap.put(JetTokens.EOL_COMMENT, "jet-comment")
        styleMap.put(JetTokens.WHITE_SPACE, "whitespace")
        styleMap.put(JetTokens.INTEGER_LITERAL, "number")
        styleMap.put(JetTokens.FLOAT_LITERAL, "number")
        styleMap.put(JetTokens.OPEN_QUOTE, "string")
        styleMap.put(JetTokens.REGULAR_STRING_PART, "string")
        styleMap.put(JetTokens.ESCAPE_SEQUENCE, "escape")
        styleMap.put(JetTokens.LONG_TEMPLATE_ENTRY_START, "escape")
        styleMap.put(JetTokens.LONG_TEMPLATE_ENTRY_END, "escape")
        styleMap.put(JetTokens.SHORT_TEMPLATE_ENTRY_START, "escape")
        styleMap.put(JetTokens.ESCAPE_SEQUENCE, "escape")
        styleMap.put(JetTokens.CLOSING_QUOTE, "string")
        styleMap.put(JetTokens.CHARACTER_LITERAL, "string")
        styleMap.put(JetTokens.LABEL_IDENTIFIER, "label")
        styleMap.put(JetTokens.ATAT, "label")
        styleMap.put(JetTokens.FIELD_IDENTIFIER, "field")
        styleMap.put(TokenType.BAD_CHARACTER, "bad")
        putAll(JetTokens.STRINGS, "string")
        putAll(JetTokens.MODIFIER_KEYWORDS, "softkeyword")
        putAll(JetTokens.SOFT_KEYWORDS, "softkeyword")
        putAll(JetTokens.COMMENTS, "jet-comment")
        putAll(JetTokens.OPERATIONS, "operation")
        return styleMap
    }
}
