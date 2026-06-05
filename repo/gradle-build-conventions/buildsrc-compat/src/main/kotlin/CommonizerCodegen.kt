/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

abstract class GenerateSupportSources : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceTemplateDir: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val rawSourceDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun run() {
        val output = outputDir.get().asFile
        output.deleteRecursively()

        val similarToSearchIndex = substituteAllInclusions(buildClassToContentsMap(sourceTemplateDir.get().asFile))
        val classesThatNeedRange = mutableSetOf<String>()
        val classesThatNeedIterator = mutableSetOf<String>()
        val classesThatNeedVar = mutableSetOf<String>()

        traverseRawSources(rawSourceDir.get().asFile) { file, destination ->
            var contents = file.readText().replace("package support.raw", "package support")

            for (nextMatch in expectNumberClassPattern.findAll(contents)) {
                val (entireMatch, name) = nextMatch.groupValues
                val (_, similarToFqName) = similarityPattern.find(entireMatch)?.groupValues ?: continue
                val similarToName = similarToFqName.split(".").last()

                if (varCounterpartPattern.containsMatchIn(entireMatch)) {
                    classesThatNeedVar.add(name)
                }

                val similarToContent = similarToSearchIndex[similarToName] ?: error("$similarToName not found")

                val ranges = listOf("AnyNumberRange", "SignedNumberRange", "UnsignedNumberRange")
                val iterators = listOf("AnyNumberIterator")

                val adjustedContent = similarToContent.replace(similarToName, name)
                    .withAppendixIfMentioned(ranges, similarToSearchIndex) { rangeName ->
                        classesThatNeedRange.add(name)
                        replace(rangeName, "${name}Range")
                    }
                    .withAppendixIfMentioned(iterators, similarToSearchIndex) { iteratorName ->
                        classesThatNeedIterator.add(name)
                        replace(iteratorName, "${name}Iterator")
                    }
                    .let {
                        val varOfVariant = "expect class ${name}VarOf<T : $name> : kotlinx.cinterop.CVariable"
                        val valueAccessor = """
                        expect object ${name}VarOfSupport {
                            @Suppress("WRONG_MODIFIER_TARGET")
                            expect var <T : $name> ${name}VarOf<T>.value: T
                        }
                    """.trimIndent()
                        if (name in classesThatNeedVar) it.plus("\n\n$varOfVariant\n\n$valueAccessor") else it
                    }
                    .replace("AnyNumber", name)

                contents = contents.replaceFirst(entireMatch, adjustedContent)
            }

            destination.parentFile.mkdirs()
            destination.writeText(contents)
        }

        traverseRawSources(rawSourceDir.get().asFile) { _, destination ->
            var contents = destination.readText()

            for (nextMatch in actualTypealiasPattern.findAll(contents)) {
                val (entireMatch, name, expansion) = nextMatch.groupValues
                val rangeAppendix = if (name in classesThatNeedRange) "\nactual typealias ${name}Range = ${expansion}Range" else ""
                val iteratorAppendix = if (name in classesThatNeedIterator) "\nactual typealias ${name}Iterator = ${expansion}Iterator" else ""

                val varAppendix = if (name in classesThatNeedVar) {
                    val varOfVariant = "actual typealias ${name}VarOf<T> = ${expansion}VarOf<T>"

                    val expandsToBuiltin = expansion in listOf("Byte", "Short", "Int", "Long", "UByte", "UShort", "UInt", "ULong", "Float", "Double")
                    val getterBody = when {
                        expandsToBuiltin -> "valueFromCinterop"
                        else -> "with(${expansion}VarOfSupport) { value }"
                    }
                    val setterBody = when {
                        expandsToBuiltin -> "valueFromCinterop = newValue"
                        else -> "with(${expansion}VarOfSupport) { value = newValue }"
                    }
                    val valueAccessor = """
                    actual object ${name}VarOfSupport {
                        @Suppress("FINAL_UPPER_BOUND")
                        actual var <T : $name> ${name}VarOf<T>.value: T
                            get() = $getterBody
                            set(newValue) { $setterBody }
                    }
                """.trimIndent()

                    "\n$varOfVariant\n\n$valueAccessor\n"
                } else {
                    ""
                }

                contents = contents.replaceFirst(
                    entireMatch,
                    entireMatch.plus(rangeAppendix).plus(iteratorAppendix).plus(varAppendix),
                )
            }

            destination.writeText(contents)
        }
    }
}

private val expectClassPattern = """expect (?:\w+\s+)*class (\w+)""".toRegex()
private val includeContentsPattern = """^\s*/\*\* Include contents of \[([\w.]+)] \*/""".toRegex(RegexOption.MULTILINE)
private val expectNumberClassPattern = """/\*\*(?:.|\n)*?\*/\nexpect (?:\w+\s+)*class (\w+)""".toRegex()
private val similarityPattern = """Similar to \[([\w.]+)]""".toRegex()
private val varCounterpartPattern = """With Var""".toRegex()
private val actualTypealiasPattern = """actual typealias (\w+) = ([\w.]+)""".toRegex()

private fun String.walkUntilNewlineAfterBalancedBraces(startIndex: Int = 0): Int {
    var current = startIndex
    var balance = 0

    while (current < length && (get(current) != '\n' || balance > 0)) {
        if (get(current) == '{') balance++
        if (get(current) == '}') balance--
        current++
    }

    return current
}

private fun buildClassToContentsMap(location: File): Map<String, String> {
    val searchIndex = mutableMapOf<String, String>()

    location.traverseTopDown { file ->
        val text = file.readText()

        for (match in expectClassPattern.findAll(text)) {
            val name = match.groupValues[1]
            val start = match.range.first
            val contents = text.substring(start, text.walkUntilNewlineAfterBalancedBraces(start))
            searchIndex[name] = contents
        }
    }

    return searchIndex
}

private fun substituteAllInclusions(classToContents: Map<String, String>): Map<String, String> {
    val newMap = mutableMapOf<String, String>()
    val stack = classToContents.mapTo(mutableListOf()) { it.key }

    while (stack.isNotEmpty()) {
        val name = stack.removeLast()
        val contents = newMap[name] ?: classToContents[name] ?: error("No contents for $name")
        val nextMatch = includeContentsPattern.find(contents)

        if (nextMatch == null) {
            newMap[name] = contents
            continue
        }

        stack.add(name)

        val (entireMatch, inclusionFqName) = nextMatch.groupValues
        val inclusionName = inclusionFqName.split(".").last()

        if (inclusionName !in newMap) {
            stack.add(inclusionName)
            continue
        }

        val inclusionContents = newMap[inclusionName]
            ?.replace(inclusionName, name)
            ?: error("Inclusion not found: $inclusionName when substituting $name")
        val members = inclusionContents.lines()
            .filter { it.isEmpty() || it.startsWith("    ") }
            .joinToString("\n")

        newMap[name] = contents.replaceFirst(entireMatch, members)
    }

    return newMap
}

private fun String.withAppendixIfMentioned(variants: List<String>, classToContents: Map<String, String>, transformFor: String.(String) -> String): String {
    val variant = variants.firstOrNull { it in this } ?: return this
    val appendix = classToContents[variant] ?: error("$variant not found")
    return this.plus("\n\n").plus(appendix).transformFor(variant)
}

private inline fun traverseRawSources(location: File, block: (File, File) -> Unit) {
    location.traverseTopDown { file ->
        val relativeToSrc = file.relativeTo(location)
        val sourceSetName = relativeToSrc.toPath().firstOrNull()?.toString() ?: return@traverseTopDown

        val prefixDir = location.resolve(sourceSetName).resolve("kotlin")
            .resolve("support").resolve("raw")
        val relativeToPrefix = when {
            // `relativeToOrNull` can also give a sequence of `../../..`.
            file.absolutePath.startsWith(prefixDir.absolutePath) -> file.relativeToOrNull(prefixDir) ?: return@traverseTopDown
            else -> return@traverseTopDown
        }

        val destination = location.resolve("../build/src-gen/").resolve(sourceSetName).resolve("kotlin")
            .resolve("support")
            .resolve(relativeToPrefix)

        block(file, destination)
    }
}

private inline fun File.traverseTopDown(block: (File) -> Unit) = walkTopDown()
    .filter { it.isFile }
    .forEach(block)
