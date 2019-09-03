/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import java.io.File


private val JK_ROOT = File("./nj2k/src/org/jetbrains/kotlin/nj2k/tree")

private val JK_OUT_ROOT = File(JK_ROOT, "visitors")

private val JK_KT_FILE = File(JK_ROOT, "k.kt")
private val JK_JAVA_FILE = File(JK_ROOT, "j.kt")
private val JK_COMMON_FILE = File(JK_ROOT, "jk.kt")


val interfaceRegex = "(interface|abstract class)\\s+(JK[a-zA-Z]+)\\s+?:?\\s+(JK[a-zA-Z]+)?".toRegex()


data class InterfaceData(val name: String, val extends: String?)

fun File.interfaceNames() =
    sequenceOf(this)
        .map { it.readText() }
        .flatMap { interfaceRegex.findAll(it) }
        .map { match -> InterfaceData(match.groupValues[2], match.groupValues.getOrNull(3)) }
        .toList()


fun String.safeVarName() = when (this) {
    "class" -> "klass"
    else -> this
}

fun genVisitors(commonData: List<InterfaceData>, uncommonData: List<InterfaceData>, visitorName: String, transformerName: String) {

    val interfaceData = commonData + uncommonData

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
            val generifyCall = if (name != "JKTreeElement") "= visit${ext!!.removePrefix("JK")}($argName)" else ""
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
            |    override fun visit$nameWithoutPrefix($arg) {
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

genVisitors(JK_COMMON_FILE.interfaceNames(), JK_JAVA_FILE.interfaceNames() + JK_KT_FILE.interfaceNames(), "JKVisitor", "JKTransformer")
