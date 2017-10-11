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


val interfaceRegex = "interface (JK[a-zA-Z]+)".toRegex()


fun File.interfaceNames() =
        sequenceOf(this)
                .map { it.readText() }
                .flatMap { interfaceRegex.findAll(it) }
                .map { match -> match.groupValues[1] }
                .toList()


fun String.safeVarName() = when (this) {
    "class" -> "klass"
    else -> this
}

fun genVisitors(interfaceNames: List<String>, visitorName: String, extends: String = "") {

    val outFile = File(JK_ROOT, "$visitorName.kt")

    outFile.writeText(buildString {
        appendln("package org.jetbrains.kotlin.j2k.tree")
        appendln()
        val extends = if (extends.isNotBlank()) ": $extends<R, D>" else ""
        appendln("interface $visitorName<R, D> $extends {")
        interfaceNames.joinTo(this, separator = "\n") { name ->
            val nameWithoutPrefix = name.removePrefix("JK")
            """
            |    fun visit$nameWithoutPrefix(${nameWithoutPrefix.decapitalize().safeVarName()}: $name, data: D): R
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
        interfaceNames.joinTo(this, separator = "\n") { name ->
            val nameWithoutPrefix = name.removePrefix("JK")
            val argName = nameWithoutPrefix.decapitalize().safeVarName()
            val arg = "$argName: $name"
            """
            |    fun visit$nameWithoutPrefix($arg)
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
genVisitors(JK_JAVA_FILE.interfaceNames() + JK_KT_FILE.interfaceNames(), "JKJavaKtVisitor", "JKVisitor")

/*   JKKtVisitor<R,D> : JKVisitor<R,D> + Void
    JKJavaVisitor<R,D> : JKVisitor<R,D> + Void
    JKJavaKtVisitor<R, D> : JKVisitor<R,D> + Void
 */

//val visitNames =


