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


val JK_ROOT = File("./j2k/newSrc/org/jetbrains/kotlin/j2k/tree")

val JK_OUT_ROOT = File(JK_ROOT, "visitors")

val JK_KT_FILE = File(JK_ROOT, "k.kt")
val JK_JAVA_FILE = File(JK_ROOT, "j.kt")
val JK_COMMON_FILE = File(JK_ROOT, "jk.kt")


val interfaceRegex = "interface (JK[a-zA-Z]+)\\s?:?\\s?(JK[a-zA-Z]+)?".toRegex()


data class InterfaceData(val name: String, val extends: String?)

fun File.interfaceNames() =
        sequenceOf(this)
                .map { it.readText() }
                .flatMap { interfaceRegex.findAll(it) }
                .map { match -> InterfaceData(match.groupValues[1], match.groupValues.getOrNull(2)) }
                .toList()


fun String.safeVarName() = when (this) {
    "class" -> "klass"
    else -> this
}

fun genVisitors(commonData: List<InterfaceData>, uncommonData: List<InterfaceData>, visitorName: String, transformerName: String, visitorExtends: String? = null, transformerExtends: String? = null) {

    val interfaceData = commonData + uncommonData

    val pkg = "package org.jetbrains.kotlin.j2k.tree.visitors"

    fun String.firstCommonInterfaceName(): String {
        val data = interfaceData.find { it.name == this }!!
        if (commonData.contains(data))
            return this
        return data.extends!!.firstCommonInterfaceName()
    }

    File(JK_OUT_ROOT, "$visitorName.kt").writeText(buildString {
        appendln(pkg)
        appendln()
        appendln("import org.jetbrains.kotlin.j2k.tree.*")
        appendln()
        val extends = visitorExtends?.let { " : $it<out R, in D>" } ?: ""
        appendln("interface $visitorName<out R, in D>$extends {")
        interfaceData.joinTo(this, separator = "\n") { (name, ext) ->
            val nameWithoutPrefix = name.removePrefix("JK")
            val argName = nameWithoutPrefix.decapitalize().safeVarName()
            val generifyCall = if (name != "JKElement") "= visit${ext!!.removePrefix("JK")}($argName, data)" else ""
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
        appendln("import org.jetbrains.kotlin.j2k.tree.*")
        appendln()
        val extends = visitorExtends?.let { ", ${it}Void" } ?: ""

        appendln("interface ${visitorName}Void : $visitorName<Unit, Nothing?>$extends {")
        interfaceData.joinTo(this, separator = "\n") { (name, ext) ->
            val nameWithoutPrefix = name.removePrefix("JK")
            val argName = nameWithoutPrefix.decapitalize().safeVarName()
            val arg = "$argName: $name"
            val generifyCall = if (name != "JKElement") "= visit${ext!!.removePrefix("JK")}($argName, null)" else ""
            """
            |    fun visit$nameWithoutPrefix($arg) $generifyCall
            |    override fun visit$nameWithoutPrefix($arg, data: Nothing?) = visit$nameWithoutPrefix($argName)
            """.trimMargin()
        }
        appendln()
        appendln("}")
    })

    File(JK_OUT_ROOT, "$transformerName.kt").writeText(buildString {
        appendln(pkg)
        appendln()
        appendln("import org.jetbrains.kotlin.j2k.tree.*")
        appendln()
        val extends = transformerExtends?.let { " : $it<in D>" } ?: ""

        appendln("interface $transformerName<in D>$extends {")

        interfaceData.joinTo(this, separator = "\n") { (name, ext) ->
            val nameWithoutPrefix = name.removePrefix("JK")
            val argName = nameWithoutPrefix.decapitalize().safeVarName()
            val generifyCall = if (name != "JKElement") "= transform${ext!!.removePrefix("JK")}($argName, data)" else ""

            val leastCommonName = name.firstCommonInterfaceName()
            """
            |    fun <E: $leastCommonName> transform$nameWithoutPrefix($argName: $name, data: D): E $generifyCall
            """.trimMargin()
        }
        appendln()
        appendln("}")
    })


    File(JK_OUT_ROOT, "${transformerName}Void.kt").writeText(buildString {
        appendln(pkg)
        appendln()
        appendln("import org.jetbrains.kotlin.j2k.tree.*")
        appendln()
        val extends = transformerExtends?.let { ", ${it}Void" } ?: ""

        appendln("interface ${transformerName}Void : $transformerName<Nothing?>$extends {")
        interfaceData.joinTo(this, separator = "\n") { (name, ext) ->
            val nameWithoutPrefix = name.removePrefix("JK")
            val argName = nameWithoutPrefix.decapitalize().safeVarName()
            val arg = "$argName: $name"
            val generifyCall = if (name != "JKElement") "= transform${ext!!.removePrefix("JK")}($argName, null)" else ""

            val leastCommonName = name.firstCommonInterfaceName()
            """
            |    fun <E: $leastCommonName> transform$nameWithoutPrefix($arg): E $generifyCall
            |    override fun <E: $leastCommonName> transform$nameWithoutPrefix($arg, data: Nothing?): E = transform$nameWithoutPrefix($argName)
            """.trimMargin()
        }
        appendln()
        appendln("}")
    })
}

genVisitors(JK_COMMON_FILE.interfaceNames(), JK_JAVA_FILE.interfaceNames() + JK_KT_FILE.interfaceNames(), "JKVisitor", "JKTransformer")
