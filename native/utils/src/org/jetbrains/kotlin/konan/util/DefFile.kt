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
import org.jetbrains.kotlin.konan.util.DefFileProperty.NullableStringProperty.*
import org.jetbrains.kotlin.konan.util.DefFileProperty.BooleanProperty.*
import org.jetbrains.kotlin.konan.util.DefFileProperty.StringListProperty.*
import java.io.File
import java.io.StringReader
import java.util.*
import kotlin.reflect.KProperty

class DefFile(val file: File?, val config: DefFileConfig, val manifestAddendProperties: Properties, val defHeaderLines: List<String>) {
    private constructor(file: File?, triple: Triple<Properties, Properties, List<String>>) : this(
        file,
        DefFileConfig(triple.first),
        triple.second,
        triple.third
    )

    constructor(file: File?, substitutions: Map<String, String>) : this(file, parseDefFile(file, substitutions))

    val name by lazy {
        file?.nameWithoutExtension ?: ""
    }

    class DefFileConfig(private val properties: Properties) {
        private operator fun <T> DefFileProperty<T>.getValue(thisRef: DefFileConfig, property: KProperty<*>): T =
            parse(thisRef.properties.getProperty(propertyName))

        val headers: List<String> by Headers
        val modules: List<String> by Modules
        val language: String? by Language
        val compilerOpts: List<String> by CompilerOpts
        val excludeSystemLibs: Boolean by ExcludeSystemLibs
        val excludeDependentModules: Boolean by ExcludeDependentModules
        val entryPoints: List<String> by EntryPoints
        val linkerOpts: List<String> by LinkerOpts
        val linker: String? by Linker
        val excludedFunctions: List<String> by ExcludedFunctions
        val excludedMacros: List<String> by ExcludedMacros
        val staticLibraries: List<String> by StaticLibraries
        val libraryPaths: List<String> by LibraryPaths
        val packageName: String? by PackageName

        /**
         * Header inclusion globs.
         */
        val headerFilter: List<String> by HeaderFilter

        /**
         * Header exclusion globs. Have higher priority than [headerFilter].
         */
        val excludeFilter: List<String> by ExcludeFilter

        val strictEnums: List<String> by StrictEnums
        val nonStrictEnums: List<String> by NonStrictEnums
        val noStringConversion: List<String> by NoStringConversion
        val depends: List<String> by Depends
        val exportForwardDeclarations: List<String> by ExportForwardDeclarations
        val allowedOverloadsForCFunctions: List<String> by AllowedOverloadsForCFunctions
        val disableDesignatedInitializerChecks: Boolean by DisableDesignatedInitializerChecks
        val foreignExceptionMode: String? by ForeignExceptionMode
        val objcClassesIncludingCategories: List<String> by ObjcClassesIncludingCategories
        val allowIncludingObjCCategoriesFromDefFile: Boolean by AllowIncludingObjCCategoriesFromDefFile
        val userSetupHint: String? by UserSetupHint
    }
}

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
