package org.jetbrains.kotlin.tools.projectWizard.plugins.printer

import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildSystemIR

class GradlePrinter(val dsl: GradleDsl, override val indent: Int = 4) : BuildFilePrinter() {
    fun inBrackets(inner: () -> Unit) {
        indented {
            +"{"
            nl()
            inner()
        }
        nlIndented()
        +"}"
    }

    inline fun par(inner: () -> Unit) {
        +"("
        inner()
        +")"
    }

    @Suppress("SpellCheckingInspection")
    val String.quotified
        get() = when (dsl) {
            GradleDsl.KOTLIN -> "\"$this\""
            GradleDsl.GROOVY -> "'$this'"
        }

    fun call(@NonNls name: String, forceBrackets: Boolean = false, body: () -> Unit) {
        +name
        when {
            dsl == GradleDsl.KOTLIN || forceBrackets -> {
                par(body)
            }
            dsl == GradleDsl.GROOVY -> {
                +" "
                body()
            }
        }
    }

    fun sectionCall(@NonNls name: String, needIndent: Boolean = false, body: () -> Unit) {
        +name
        +" "
        inBrackets {
            if (needIndent) indent()
            body()
        }
    }

    fun <I : BuildSystemIR> sectionCall(
        @NonNls name: String,
        irs: List<I>,
        renderEmpty: Boolean = false
    ) {
        if (irs.isEmpty() && !renderEmpty) return
        +name
        +" "
        inBrackets {
            irs.listNl()
        }
    }

    fun assignmentOrCall(@NonNls target: String, assignee: () -> Unit) = when (dsl) {
        GradleDsl.KOTLIN -> {
            +"$target = "
            assignee()
        }
        GradleDsl.GROOVY -> call(target, forceBrackets = false, body = assignee)
    }

    fun assignment(@NonNls target: String, assignee: () -> Unit) {
        +"$target = "
        assignee()
    }


    inline fun getting(@NonNls name: String, @NonNls prefix: String?, body: () -> Unit = {}) {
        when (dsl) {
            GradleDsl.GROOVY -> {
                prefix?.let { +it; +"." }
                +name
            }
            GradleDsl.KOTLIN -> {
                +"val $name by "
                prefix?.let { +it; +"." }
                +"getting"
            }
        }
        body()
    }

    val String.identifier
        get() = when (dsl) {
            GradleDsl.KOTLIN -> if (this in KOTLIN_KEYWORDS) "`$this`" else this
            GradleDsl.GROOVY -> if (this in GROOVY_KEYWORDS) "'$this'" else this
        }

    enum class GradleDsl {
        KOTLIN, GROOVY
    }

    companion object {
        private val KOTLIN_KEYWORDS = setOf(
            "as", "break", "class", "continue", "do", "else",
            "false", "for", "fun", "if", "in", "interface",
            "is", "null", "object", "package", "return", "super",
            "this", "throw", "true", "try", "typealias", "typeof",
            "val", "var", "when", "while"
        )

        private val GROOVY_KEYWORDS = setOf(
            "abstract", "as", "assert",
            "boolean", "break", "byte",
            "case", "catch", "char",
            "class", "const", "continue",
            "def", "default", "do",
            "double", "else", "enum",
            "extends", "false", "final",
            "finally", "float", "for",
            "goto", "if", "implements",
            "import", "in", "instanceof",
            "int", "interface", "long",
            "native", "new", "null",
            "package", "private", "protected",
            "public", "return", "short",
            "static", "strictfp", "super",
            "switch", "synchronized", "this",
            "threadsafe", "throw", "throws",
            "transient", "true", "try",
            "void", "volatile", "while"
        )
    }
}
