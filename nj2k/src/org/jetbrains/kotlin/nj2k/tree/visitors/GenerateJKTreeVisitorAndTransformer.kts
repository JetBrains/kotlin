/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File


val JK_ROOT = File("./nj2k/src/org/jetbrains/kotlin/nj2k/tree")

val JK_OUT_ROOT = File(JK_ROOT, "visitors")

val JK_KT_FILE = File(JK_ROOT, "k.kt")
val JK_JAVA_FILE = File(JK_ROOT, "j.kt")
val JK_COMMON_FILE = File(JK_ROOT, "jk.kt")


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

    fun String.firstCommonInterfaceName(): String {
        val data = interfaceData.find { it.name == this }!!
        if (commonData.contains(data))
            return this
        return data.extends!!.firstCommonInterfaceName()
    }

    File(JK_OUT_ROOT, "$visitorName.kt").writeText(buildString {
        appendln(pkg)
        appendln()
        appendln("import org.jetbrains.kotlin.nj2k.tree.*")
        appendln()
        appendln("interface $visitorName<out R, in D> {")
        interfaceData.joinTo(this, separator = "\n") { (name, ext) ->
            val nameWithoutPrefix = name.removePrefix("JK")
            val argName = nameWithoutPrefix.decapitalize().safeVarName()
            val generifyCall = if (name != "JKTreeElement") "= visit${ext!!.removePrefix("JK")}($argName, data)" else ""
            """
            |    fun visit$nameWithoutPrefix($argName: $name, data: D): R $generifyCall
            """.trimMargin()
        }
        appendln()
        appendln("}")
    })


    File(JK_OUT_ROOT, "${visitorName}Void.kt").writeText(buildString {
        appendln(pkg)
        appendln()
        appendln("import org.jetbrains.kotlin.nj2k.tree.*")
        appendln()

        appendln("interface ${visitorName}Void : $visitorName<Unit, Nothing?> {")
        interfaceData.joinTo(this, separator = "\n") { (name, ext) ->
            val nameWithoutPrefix = name.removePrefix("JK")
            val argName = nameWithoutPrefix.decapitalize().safeVarName()
            val arg = "$argName: $name"
            val generifyCall = if (name != "JKTreeElement") "= visit${ext!!.removePrefix("JK")}($argName, null)" else ""
            """
            |    fun visit$nameWithoutPrefix($arg) $generifyCall
            |    override fun visit$nameWithoutPrefix($arg, data: Nothing?) = visit$nameWithoutPrefix($argName)
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

        appendln("interface ${visitorName}WithCommentsPrinting : ${visitorName}Void {")
        appendln(
            """
        |    fun printLeftNonCodeElements(element: JKNonCodeElementsListOwner)
        |    fun printRightNonCodeElements(element: JKNonCodeElementsListOwner)
        |
        """.trimMargin()
        )

        interfaceData.joinTo(this, separator = "\n\n") { (name, ext) ->
            val nameWithoutPrefix = name.removePrefix("JK")
            val argName = nameWithoutPrefix.decapitalize().safeVarName()
            val arg = "$argName: $name"
            val rawVisitSuffix = "Raw"
            val generifyCall = if (name != "JKTreeElement") "= visit${ext!!.removePrefix("JK")}$rawVisitSuffix($argName)" else ""
            """
            |    override fun visit$nameWithoutPrefix($arg) {
            |        printLeftNonCodeElements($argName)
            |        visit$nameWithoutPrefix$rawVisitSuffix($argName)
            |        printRightNonCodeElements($argName)
            |    }
            |
            |    fun visit$nameWithoutPrefix$rawVisitSuffix($arg) $generifyCall
            """.trimMargin()
        }
        appendln()
        appendln("}")
    })

    /*File(JK_OUT_ROOT, "$transformerName.kt").writeText(buildString {
        appendln(pkg)
        appendln()
        appendln("import org.jetbrains.kotlin.nj2k.tree.*")
        appendln()

        appendln("interface $transformerName<in D> : JKVisitor<JKTreeElement, D> {")

        interfaceData.joinTo(this, separator = "\n") { (name, ext) ->
            val nameWithoutPrefix = name.removePrefix("JK")
            val argName = nameWithoutPrefix.decapitalize().safeVarName()

            val leastCommonName = name.firstCommonInterfaceName()
            val cast = "as $leastCommonName".takeIf { leastCommonName == name } ?: ""

            val generifyCall = if (name != "JKTreeElement") "= visit${ext!!.removePrefix("JK")}($argName, data) $cast" else ""

            """
            |    override fun visit$nameWithoutPrefix($argName: $name, data: D): $leastCommonName $generifyCall
            """.trimMargin()
        }
        appendln()
        appendln("}")
    })


    File(JK_OUT_ROOT, "${transformerName}Void.kt").writeText(buildString {
        appendln(pkg)
        appendln()
        appendln("import org.jetbrains.kotlin.nj2k.tree.*")
        appendln()


        appendln("interface ${transformerName}Void : $transformerName<Nothing?> {")
        interfaceData.joinTo(this, separator = "\n") { (name, ext) ->
            val nameWithoutPrefix = name.removePrefix("JK")
            val argName = nameWithoutPrefix.decapitalize().safeVarName()
            val arg = "$argName: $name"

            val leastCommonName = name.firstCommonInterfaceName()
            val cast = "as $leastCommonName".takeIf { leastCommonName == name } ?: ""
            val generifyCall = if (name != "JKTreeElement") "= visit${ext!!.removePrefix("JK")}($argName) $cast" else ""

            """
            |    fun visit$nameWithoutPrefix($arg): $leastCommonName $generifyCall
            |    override fun visit$nameWithoutPrefix($arg, data: Nothing?): $leastCommonName = visit$nameWithoutPrefix($argName)
            """.trimMargin()
        }
        appendln()
        appendln("}")
    })*/
}

genVisitors(JK_COMMON_FILE.interfaceNames(), JK_JAVA_FILE.interfaceNames() + JK_KT_FILE.interfaceNames(), "JKVisitor", "JKTransformer")
