package org.jetbrains.kotlin.konan.util

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

        val depends by lazy {
            properties.getSpaceSeparated("depends")
        }
    }
}

private fun Properties.getSpaceSeparated(name: String): List<String> {
    return this.getProperty(name)?.split(' ')?.filter { it.isNotEmpty() } ?: emptyList()
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

     val propertiesReader = StringReader(propertyLines.joinToString(System.lineSeparator()))
     properties.load(propertiesReader)

     // Pass unsubstituted copy of properties we have obtained from `.def`
     // to compiler `-maniest`.
     val manifestAddendProperties = properties.duplicate()

     substitute(properties, substitutions)

     return Triple(properties, manifestAddendProperties, headerLines)
}

// Performs substitution similar to:
//  foo = ${foo} ${foo.${arch}} ${foo.${os}}
fun substitute(properties: Properties, substitutions: Map<String, String>) {
    for (key in properties.stringPropertyNames()) {
        for (substitution in substitutions.values) {
            val suffix = ".$substitution"
            if (key.endsWith(suffix)) {
                val baseKey = key.removeSuffix(suffix)
                val oldValue = properties.getProperty(baseKey, "")
                val appendedValue = properties.getProperty(key, "")
                val newValue = if (oldValue != "") "$oldValue $appendedValue" else appendedValue
                properties.setProperty(baseKey, newValue)
            }
        }
    }
}

private fun Properties.duplicate() = Properties().apply { putAll(this@duplicate) }
