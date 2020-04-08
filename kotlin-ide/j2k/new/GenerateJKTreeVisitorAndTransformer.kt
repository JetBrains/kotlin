/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import java.io.File


private val JK_ROOT = File("./nj2k/src/org/jetbrains/kotlin/nj2k/tree")

private val JK_OUT_ROOT = File(JK_ROOT, "visitors")

private val JK_TREE_FILES = listOf(
    "declarations.kt",
    "elements.kt",
    "expressions.kt",
    "modifiers.kt",
    "statements.kt"
).map { File(JK_ROOT, it) }


val elementRegex = """(class|object)\s+(JK[\w]+)(\([\w\s:,<>=\(\)\\]+\))?\s*:\s*(JK[a-zA-Z]+)""".toRegex()


data class InterfaceData(val name: String, val extends: String?)

fun File.interfaceNames() =
    sequenceOf(this)
        .map { it.readText() }
        .flatMap { elementRegex.findAll(it) }
        .map { match ->
            InterfaceData(
                match.groupValues[2],
                match.groupValues[4].let { if (it == "JKAnnotationMemberValue") "JKTreeElement" else it }
            )
        }.toList()


fun String.safeVarName() = when (this) {
    "class" -> "klass"
    else -> this
}

fun genVisitors(
    interfaceData: List<InterfaceData>,
    visitorName: String
) {
    val pkg = "package org.jetbrains.kotlin.nj2k.tree.visitors"

    File(JK_OUT_ROOT, "$visitorName.kt").writeText(buildString {
        appendln(pkg)
        appendln()
        appendln("import org.jetbrains.kotlin.nj2k.tree.*")
        appendln()
        appendln("abstract class $visitorName {")
        interfaceData.joinTo(this, separator = "\n") { (name, ext) ->
            val nameWithoutPrefix = name.removePrefix("JK")
            val argName = nameWithoutPrefix.decapitalize().safeVarName()
            val generifyCall = if (name != "JKTreeElement") "= visit${ext?.removePrefix("JK") ?: error(name)}($argName)" else ""
            val modifier = if (name == "JKTreeElement") "abstract" else "open"
            """
            |    $modifier fun visit$nameWithoutPrefix($argName: $name) $generifyCall
            """.trimMargin()
        }
        appendln()
        appendln("}")
    })


    File(JK_OUT_ROOT, "${visitorName}WithCommentsPrinting.kt").writeText(buildString {
        appendln(pkg)
        appendln()
        appendln("import org.jetbrains.kotlin.nj2k.tree.*")
        appendln()

        appendln("abstract class ${visitorName}WithCommentsPrinting : $visitorName() {")
        appendln(
            """
        |    abstract fun printLeftNonCodeElements(element: JKNonCodeElementsListOwner)
        |    abstract fun printRightNonCodeElements(element: JKNonCodeElementsListOwner)
        |
        """.trimMargin()
        )

        interfaceData.joinTo(this, separator = "\n\n") { (name, ext) ->
            val nameWithoutPrefix = name.removePrefix("JK")
            val argName = nameWithoutPrefix.decapitalize().safeVarName()
            val arg = "$argName: $name"
            val rawVisitSuffix = "Raw"
            val generifyCall = if (name != "JKTreeElement") "= visit${ext!!.removePrefix("JK")}$rawVisitSuffix($argName)" else ""
            val modifier = if (name == "JKTreeElement") "abstract" else "open"
            """
            |    final override fun visit$nameWithoutPrefix($arg) {
            |        printLeftNonCodeElements($argName)
            |        visit$nameWithoutPrefix$rawVisitSuffix($argName)
            |        printRightNonCodeElements($argName)
            |    }
            |
            |    $modifier fun visit$nameWithoutPrefix$rawVisitSuffix($arg) $generifyCall
            """.trimMargin()
        }
        appendln()
        appendln("}")
    })
}

fun main() {
    genVisitors(JK_TREE_FILES.flatMap { it.interfaceNames() }, "JKVisitor")
}

