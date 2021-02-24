package org.jetbrains.kotlin.tools.projectWizard.plugins.printer

import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildSystemIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.render

abstract class BuildFilePrinter {
    abstract val indent: Int
    private val builder = StringBuilder()
    protected var currentIndent = 0

    fun nl(lineBreaks: Int = 1) {
        +"\n".repeat(lineBreaks)
    }

    fun nlIndented(lineBreaks: Int = 1) {
        nl(lineBreaks)
        indent()
    }

    fun indent() {
        +" ".repeat(currentIndent)
    }

    fun String.print() {
        builder.append(this)
    }

    operator fun @receiver:NonNls String.unaryPlus() {
        builder.append(this)
    }

    fun indented(inner: () -> Unit) {
        currentIndent += indent
        inner()
        currentIndent -= indent
    }

    inline fun <T> List<T>.list(
        prefix: BuildFilePrinter.() -> Unit = {},
        separator: BuildFilePrinter.() -> Unit = { +", " },
        suffix: BuildFilePrinter.() -> Unit = {},
        render: BuildFilePrinter.(T) -> Unit
    ) {
        prefix()
        if (isNotEmpty()) {
            render(first())
            for (i in 1..lastIndex) {
                separator()
                render(this[i])
            }
        }
        suffix()
    }

    open fun <T : BuildSystemIR> List<T>.listNl(needFirstIndent: Boolean = true, lineBreaks: Int = 1) {
        if (needFirstIndent) indent()
        list(separator = { nl(lineBreaks); indent() }) { it.render(this) }
    }

    fun result(): String = builder.toString()
}

inline fun <P : BuildFilePrinter> P.printBuildFile(builder: P.() -> Unit) =
    apply(builder).result()