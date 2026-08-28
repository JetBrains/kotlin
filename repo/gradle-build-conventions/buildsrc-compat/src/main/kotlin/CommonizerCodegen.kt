/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import kotlin.collections.flatMapTo
import kotlin.collections.getOrPut

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

    // Drop after bootstrap
    @get:Input
    abstract val bootstrapEnabled: Property<Boolean>

    @get:Input
    abstract val supportHierarchy: MapProperty<String, String>

    private val leafSourceSets by lazy {
        buildSet {
            addAll(supportHierarchy.get().keys)
            removeAll(supportHierarchy.get().values)
        }
    }

    private val String.expandsToBuiltin: Boolean
        get() = this in listOf("Byte", "Short", "Int", "Long", "UByte", "UShort", "UInt", "ULong", "Float", "Double")

    @TaskAction
    fun run() {
        val rawSourceLocation = rawSourceDir.get().asFile
        val output = outputDir.get().asFile
        output.deleteRecursively()

        val immediateActualizations = mutableMapOf<String, MutableSet<String>>()
        val sourceSetToImmediateBuiltinExpansions = mutableMapOf<String, MutableSet<Pair<String, String>>>()

        fun collectAllBuiltinExpansionsInHierarchyOf(sourceSet: String): MutableSet<Pair<String, String>> {
            var current: String? = sourceSet
            val result = mutableSetOf<Pair<String, String>>()

            while (current != null) {
                result += sourceSetToImmediateBuiltinExpansions[current].orEmpty()
                current = supportHierarchy.get()[current]
            }

            return result
        }

        traverseRawSourcesInSourceSets(supportHierarchy, rawSourceLocation) { file, _, generatedSourceSet ->
            val contents = file.readText()
            val sourceSet = generatedSourceSet.name

            for (nextMatch in actualTypealiasPattern.findAll(contents)) {
                val (_, name, expansion) = nextMatch.groupValues
                immediateActualizations.getOrPut(name) { mutableSetOf() }.add(expansion)

                if (expansion.expandsToBuiltin) {
                    sourceSetToImmediateBuiltinExpansions.getOrPut(sourceSet, ::mutableSetOf).add(name to expansion)
                }
            }
        }

        val leafActualizations = immediateActualizations.toLeafExpansions()
        val similarToSearchIndex = substituteAllInclusions(buildClassToContentsMap(sourceTemplateDir.get().asFile))
        val classesThatNeedRange = mutableSetOf<String>()
        val classesThatNeedIterator = mutableSetOf<String>()
        val classesThatNeedVar = mutableSetOf<String>()

        val kotlinxCinteropBridgeGenerator = HelperFileGenerator("kotlinx" / "cinterop")
        val kotlinRangesBridgeGenerator = HelperFileGenerator("kotlin" / "ranges")

        fun List<String>.toSuppressCall() = when {
            isEmpty() -> null
            else -> joinToString(", ") { "\"$it\"" }.let { "@Suppress($it)" }
        }

        val deprecation = "@Deprecated(\"Ues the overload from the standard library instead.\", level = DeprecationLevel.HIDDEN)"
        val nonBootstrapExpectSuppressions = when {
            !bootstrapEnabled.getOrElse(false) -> listOf(
                "WRONG_ANNOTATION_TARGET", "ACTUAL_WITHOUT_EXPECT",
                "AMBIGUOUS_EXPECTS", "REDECLARATION", "CONFLICTING_OVERLOADS",
            )
            else -> emptyList()
        }
        val nonBootstrapAnnotations = when {
            !bootstrapEnabled.getOrElse(false) -> """
                            @OptIn(ExperimentalMultiplatform::class)
                            @kotlin.experimental.ExpectRefinement
                        """.trimIndent()
            else -> null
        }

        traverseRawSourcesInSourceSets(supportHierarchy, rawSourceLocation) { file, destination, generatedSourceSet ->
            var contents = file.readText().replace("""^(package .*)\.raw$""".toRegex(RegexOption.MULTILINE), "$1")

            val kotlinxXCinteropFileContents = kotlinxCinteropBridgeGenerator.getBuilderFor(generatedSourceSet)
            val kotlinRangesFileContents = kotlinRangesBridgeGenerator.getBuilderFor(generatedSourceSet)

            for (nextMatch in expectNumberClassPattern.findAll(contents)) {
                val (entireMatch, name) = nextMatch.groupValues
                val (_, similarToFqName) = similarityPattern.find(entireMatch)?.groupValues ?: continue
                val similarToName = similarToFqName.split(".").last()

                if (varCounterpartPattern.containsMatchIn(entireMatch)) {
                    classesThatNeedVar.add(name)
                }

                val prototypeReference = prototypePattern.find(entireMatch)?.groupValues?.getOrNull(1)
                val similarToContent = similarToSearchIndex[similarToName] ?: error("$similarToName not found")

                val ranges = listOf("AnyNumberRange", "SignedNumberRange", "UnsignedNumberRange")
                val iterators = listOf("AnyNumberIterator")

                val adjustedContent = similarToContent.replace(similarToName, name)
                    .withAppendixIfMentioned(ranges, similarToSearchIndex) { rangeName ->
                        classesThatNeedRange.add(name)

                        val untilFunction = listOfNotNull(
                            nonBootstrapExpectSuppressions.toSuppressCall(),
                            nonBootstrapAnnotations,
                            "expect inline infix fun support.$name.until(to: support.$name): support.${name}Range"
                        ).joinToString("\n")

                        kotlinRangesFileContents += untilFunction
                        replace(rangeName, "${name}Range")
                    }
                    .withAppendixIfMentioned(iterators, similarToSearchIndex) { iteratorName ->
                        classesThatNeedIterator.add(name)
                        replace(iteratorName, "${name}Iterator")
                    }
                    .let {
                        val varOfVariant = """
                            @kotlinx.cinterop.ExperimentalForeignApi
                            expect class ${name}VarOf<T : $name> : kotlinx.cinterop.CVariable
                        """.trimIndent()
                        val valueAccessor = listOfNotNull(
                            nonBootstrapExpectSuppressions.toSuppressCall(),
                            nonBootstrapAnnotations,
                            "@ExperimentalForeignApi",
                            "expect inline var <T : support.$name> support.${name}VarOf<T>.value: T"
                        ).joinToString("\n")
                        val allocFunction = listOfNotNull(
                            (nonBootstrapExpectSuppressions + "FINAL_UPPER_BOUND").toSuppressCall(),
                            nonBootstrapAnnotations,
                            "@ExperimentalForeignApi",
                            "expect inline fun <T : support.$name> NativePlacement.alloc(value: T): support.${name}VarOf<T>",
                        ).joinToString("\n")

                        if (name in classesThatNeedVar) {
                            kotlinxXCinteropFileContents += valueAccessor
                            kotlinxXCinteropFileContents += allocFunction
                            it.plus("\n\n$varOfVariant")
                        } else {
                            it
                        }
                    }
                    .replace("AnyNumber", name)
                    .let { content ->
                        val relevantActualizations = leafActualizations[name]
                            ?.joinToString(", ") { "$it::class" }
                            ?: error("No actualizations for $name")

                        "@NumericClass($relevantActualizations)\n$content"
                    }
                    .let { content ->
                        when {
                            prototypeReference != null -> "/**\n * Modeled after [$prototypeReference].\n */\n$content"
                            else -> content
                        }
                    }

                contents = contents.replaceFirst(entireMatch, adjustedContent)
            }

            destination.parentFile.mkdirs()
            destination.writeText(contents)
        }

        traverseRawSourcesInSourceSets(supportHierarchy, rawSourceLocation) { _, destination, _ ->
            var contents = destination.readText()

            for (nextMatch in actualTypealiasPattern.findAll(contents)) {
                val (entireMatch, name, expansion) = nextMatch.groupValues
                val rangeAppendix = if (name in classesThatNeedRange) "\nactual typealias ${name}Range = ${expansion}Range" else ""
                val iteratorAppendix = if (name in classesThatNeedIterator) "\nactual typealias ${name}Iterator = ${expansion}Iterator" else ""

                val varAppendix = if (name in classesThatNeedVar) {
                    val varOfVariant = """
                        @kotlinx.cinterop.ExperimentalForeignApi
                        actual typealias ${name}VarOf<T> = ${expansion}VarOf<T>
                    """.trimIndent()

                    "\n$varOfVariant\n"
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

        val nonBootstrapActualSuppressions = when {
            !bootstrapEnabled.getOrElse(false) -> listOf(
                "AMBIGUOUS_EXPECTS", "ACTUAL_WITHOUT_EXPECT",
            )
            else -> emptyList()
        }

        rawSourceLocation.traverseSourceSetsOf(supportHierarchy) { sourceSetDirectory, generatedSourceSet ->
            val sourceSet = sourceSetDirectory.name
            val isLeafSourceSet = sourceSet in leafSourceSets
            if (!isLeafSourceSet) return@traverseSourceSetsOf

            val kotlinxXCinteropFileContents = kotlinxCinteropBridgeGenerator.getBuilderFor(generatedSourceSet)
            val kotlinRangesFileContents = kotlinRangesBridgeGenerator.getBuilderFor(generatedSourceSet)

            for ((name, expansion) in collectAllBuiltinExpansionsInHierarchyOf(sourceSet)) {
                if (name in classesThatNeedVar) {
                    kotlinxXCinteropFileContents += listOfNotNull(
                        (nonBootstrapActualSuppressions + "FINAL_UPPER_BOUND").toSuppressCall(),
                        """
                        @ExperimentalForeignApi
                        $deprecation
                        actual inline var <T : $expansion> ${expansion}VarOf<T>.value: T
                            get() = error("Should not be called")
                            set(_) { error("Should not be called") }
                        """.trimIndent()
                    ).joinToString("\n")

                    kotlinxXCinteropFileContents += listOfNotNull(
                        (nonBootstrapActualSuppressions + "FINAL_UPPER_BOUND").toSuppressCall(),
                        """
                        @ExperimentalForeignApi
                        $deprecation
                        actual inline fun <T : $expansion> NativePlacement.alloc(value: T): ${expansion}VarOf<T> = error("Should not be called")
                        """.trimIndent(),
                    ).joinToString("\n")
                }

                if (name in classesThatNeedRange) {
                    kotlinRangesFileContents += listOfNotNull(
                        nonBootstrapActualSuppressions.toSuppressCall(),
                        """
                        $deprecation
                        actual inline infix fun $expansion.until(to: $expansion): ${expansion}Range = error("Should not be called")
                        """.trimIndent(),
                    ).joinToString("\n")
                }
            }
        }

        kotlinxCinteropBridgeGenerator.write()
        kotlinRangesBridgeGenerator.write()
    }
}

private val expectClassPattern = """expect (?:\w+\s+)*class (\w+)""".toRegex()
private val includeContentsPattern = """^\s*/\*\* Include contents of \[([\w.]+)] \*/""".toRegex(RegexOption.MULTILINE)
private val expectNumberClassPattern = """/\*\*(?:.|\n)*?\*/\nexpect (?:\w+\s+)*class (\w+)""".toRegex()
private val similarityPattern = """Similar to \[([\w.]+)]""".toRegex()
private val varCounterpartPattern = """With Var""".toRegex()
private val prototypePattern = """Modeled after \[([\w.]+)]""".toRegex()
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

private fun String.withAppendixIfMentioned(
    variants: List<String>,
    classToContents: Map<String, String>,
    transformFor: String.(String) -> String,
): String {
    val variant = variants.firstOrNull { it in this } ?: return this
    val appendix = classToContents[variant] ?: error("$variant not found")
    return this.plus("\n\n").plus(appendix).transformFor(variant)
}

private fun File.toGeneratedFile(
    rawSourceSet: File,
    generatedSourceSet: File,
): File? {
    val relativeToPrefix = when {
        parentFile.name != "raw" -> return null
        // `relativeToOrNull` can also give a sequence of `../../..`.
        absolutePath.startsWith(rawSourceSet.absolutePath) -> relativeToOrNull(rawSourceSet) ?: return null
        else -> return null
    }
    return generatedSourceSet.resolve(relativeToPrefix.parentFile.parentFile).resolve(name)
}

private inline fun File.traverseSourceSetsOf(
    supportHierarchy: MapProperty<String, String>,
    block: (File, File) -> Unit,
) {
    for (sourceSetName in supportHierarchy.get().keys) {
        val generatedSourceSet = resolve("../build/src-gen/").resolve(sourceSetName)
        block(resolve(sourceSetName), generatedSourceSet)
    }
}

private inline fun File.traverseRawSources(
    generatedSourceSet: File,
    block: (File, File) -> Unit,
): Unit = traverseTopDown { file ->
    file.toGeneratedFile(rawSourceSet = this, generatedSourceSet)?.let { block(file, it) }
}

private inline fun traverseRawSourcesInSourceSets(
    supportHierarchy: MapProperty<String, String>,
    location: File,
    block: (File, File, File) -> Unit,
): Unit = location.traverseSourceSetsOf(supportHierarchy) { rawSourceSet, generatedSourceSet ->
    rawSourceSet.traverseRawSources(generatedSourceSet) { file, destination ->
        block(file, destination, generatedSourceSet)
    }
}

private fun File.resolvePackage(packageSegments: List<String>): File =
    packageSegments.fold(this) { destination, segment -> destination.resolve(segment) }

private inline fun File.traverseTopDown(block: (File) -> Unit) = walkTopDown()
    .filter { it.isFile }
    .forEach(block)

private class HelperFileGenerator(
    private val packageSegments: List<String>,
    private val fileName: String = "Bridges.kt",
) {
    private val naivePackageFqName get() = packageSegments.joinToString(separator = ".")

    private val buildersByDestination = mutableMapOf<File, MutableSet<String>>()

    fun getBuilderFor(generatedSourceSet: File): MutableSet<String> =
        buildersByDestination.getOrPut(generatedSourceSet) { mutableSetOf() }

    fun write() {
        val filesMapByDestination = mutableMapOf<File, File>()

        for ((generatedSourceSet, contentBlocks) in buildersByDestination) {
            if (contentBlocks.isEmpty()) continue

            val file = filesMapByDestination.getOrPut(generatedSourceSet) {
                generatedSourceSet.resolve("kotlin")
                    .resolvePackage(packageSegments).also { it.mkdirs() }
                    .resolve(fileName).also { it.writeText("package $naivePackageFqName\n") }
            }

            file.appendText("\n" + contentBlocks.joinToString("\n\n") + "\n")
        }
    }
}

private operator fun String.div(other: String) = listOf(this) / other
private operator fun List<String>.div(other: String) = this + other

private fun <T> T.leafExpansion(immediateExpansions: Map<T, Set<T>>, cache: MutableMap<T, Set<T>> = mutableMapOf()): Set<T> =
    cache.getOrPut(this) {
        immediateExpansions[this]?.flatMapTo(mutableSetOf()) { it.leafExpansion(immediateExpansions, cache) }
            ?: setOf(this)
    }

private fun <T> Map<T, Set<T>>.toLeafExpansions(): Map<T, Set<T>> = buildMap {
    this@toLeafExpansions.keys.forEach { it.leafExpansion(this@toLeafExpansions, this) }
}
