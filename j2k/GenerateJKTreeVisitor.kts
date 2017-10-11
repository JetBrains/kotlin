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

fun genVisitors(interfaceData: List<InterfaceData>, visitorName: String, extends: String = "") {

    val outFile = File(JK_ROOT, "$visitorName.kt")

    outFile.writeText(buildString {
        appendln("package org.jetbrains.kotlin.j2k.tree")
        appendln()
        val extends = if (extends.isNotBlank()) ": $extends<R, D>" else ""
        appendln("interface $visitorName<R, D> $extends {")
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

    val outVoidFile = File(JK_ROOT, "${visitorName}Void.kt")

    outVoidFile.writeText(buildString {
        appendln("package org.jetbrains.kotlin.j2k.tree")
        appendln()
        val extends = if (extends.isNotBlank()) ", ${extends}Void" else ""

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
}

// JKVisitor<R,D> + Void
genVisitors(JK_COMMON_FILE.interfaceNames(), "JKVisitor")
genVisitors(JK_JAVA_FILE.interfaceNames(), "JKJavaVisitor", "JKVisitor")
genVisitors(JK_KT_FILE.interfaceNames(), "JKKtVisitor", "JKVisitor")

/*   JKKtVisitor<R,D> : JKVisitor<R,D> + Void
    JKJavaVisitor<R,D> : JKVisitor<R,D> + Void
 */

//val visitNames =


