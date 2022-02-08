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

package org.jetbrains.kotlin.konan.util

import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.util.parseSpaceSeparatedArgs
import java.io.File
import java.io.StringReader
import java.util.*

class DefFile(val file:File?, val config:DefFileConfig, val manifestAddendProperties:Properties, val defHeaderLines:List<String>) {
    private constructor(file0:File?, triple: Triple<Properties, Properties, List<String>>): this(file0, DefFileConfig(triple.first), triple.second, triple.third)
    constructor(file:File?, substitutions: Map<String, String>) : this(file, parseDefFile(file, substitutions))

    val name by lazy {
        file?.nameWithoutExtension ?: ""
    }
    class DefFileConfig(private val properties: Properties) {
        val headers by lazy {
            properties.getSpaceSeparated("headers")
        }

        val modules by lazy {
            properties.getSpaceSeparated("modules")
        }

        val language by lazy {
            properties.getProperty("language")
        }

        val compilerOpts by lazy {
            properties.getSpaceSeparated("compilerOpts")
        }

        val excludeSystemLibs by lazy {
            properties.getProperty("excludeSystemLibs")?.toBoolean() ?: false
        }

        val excludeDependentModules by lazy {
            properties.getProperty("excludeDependentModules")?.toBoolean() ?: false
        }

        val entryPoints by lazy {
            properties.getSpaceSeparated("entryPoint")
        }

        val linkerOpts by lazy {
            properties.getSpaceSeparated("linkerOpts")
        }

        val linker by lazy {
            properties.getProperty("linker", "clang")
        }

        val excludedFunctions by lazy {
            properties.getSpaceSeparated("excludedFunctions")
        }

        val excludedMacros by lazy {
            properties.getSpaceSeparated("excludedMacros")
        }

        val staticLibraries by lazy {
            properties.getSpaceSeparated("staticLibraries")
        }

        val libraryPaths by lazy {
            properties.getSpaceSeparated("libraryPaths")
        }

        val packageName by lazy {
            properties.getProperty("package")
        }

        val headerFilter by lazy {
            properties.getSpaceSeparated("headerFilter")
        }

        val strictEnums by lazy {
            properties.getSpaceSeparated("strictEnums")
        }

        val nonStrictEnums by lazy {
            properties.getSpaceSeparated("nonStrictEnums")
        }

        val noStringConversion by lazy {
            properties.getSpaceSeparated("noStringConversion")
        }

        val depends by lazy {
            properties.getSpaceSeparated("depends")
        }

        val exportForwardDeclarations by lazy {
            properties.getSpaceSeparated("exportForwardDeclarations")
        }

        val disableDesignatedInitializerChecks by lazy {
            properties.getProperty("disableDesignatedInitializerChecks")?.toBoolean() ?: false
        }

        val foreignExceptionMode by lazy {
            properties.getProperty("foreignExceptionMode")
        }

        val pluginName by lazy {
            properties.getProperty("plugin")
        }

    }
}

private fun Properties.getSpaceSeparated(name: String): List<String> =
        this.getProperty(name)?.let { parseSpaceSeparatedArgs(it) } ?: emptyList()

private fun parseDefFile(file: File?, substitutions: Map<String, String>): Triple<Properties, Properties, List<String>> {
     val properties = Properties()

     if (file == null) {
         return Triple(properties, Properties(), emptyList())
     }

     val lines = file.readLines()

     val separator = "---"
     val separatorIndex = lines.indexOf(separator)

     val propertyLines: List<String>
     val headerLines: List<String>

     if (separatorIndex != -1) {
         propertyLines = lines.subList(0, separatorIndex)
         headerLines = lines.subList(separatorIndex + 1, lines.size)
     } else {
         propertyLines = lines
         headerLines = emptyList()
     }

     // \ isn't escaping character in quotes, so replace them with \\.
     val joinedLines = propertyLines.joinToString(System.lineSeparator())
     val escapedTokens = joinedLines.split('"')
     val postprocessProperties = escapedTokens.mapIndexed { index, token ->
         if (index % 2 != 0) {
             token.replace("""\\(?=.)""".toRegex(), Regex.escapeReplacement("""\\"""))
         } else {
             token
         }
     }.joinToString("\"")
     val propertiesReader = StringReader(postprocessProperties)
     properties.load(propertiesReader)

     // Pass unsubstituted copy of properties we have obtained from `.def`
     // to compiler `-manifest`.
     val manifestAddendProperties = properties.duplicate()

     substitute(properties, substitutions)

     return Triple(properties, manifestAddendProperties, headerLines)
}

private fun Properties.duplicate() = Properties().apply { putAll(this@duplicate) }

fun DefFile(file: File?, target: KonanTarget) = DefFile(file, defaultTargetSubstitutions(target))
