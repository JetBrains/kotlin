import java.io.File

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * Merges two verification-metadata.xml files into their union of <component> entries.
 *
 * Both inputs share an identical header (<configuration>/<components>); only the set of
 * <component> blocks differs. The output contains every component present in either file.
 * When the same coordinate (group + name + version) appears in both files, the entry from
 * the first (normal) file wins. This keeps org.jetbrains.kotlin entries pinned to the
 * published artifacts produced by the normal run instead of the local bootstrap hashes.
 */

if (args.size != 3) {
    println("Usage: kotlinc -script merge-verification-metadata.kts normal.xml bootstrap.xml output.xml")
    System.exit(1)
}

val (normalPath, bootstrapPath, outputPath) = args

val normalText = File(normalPath).readText()
val bootstrapText = File(bootstrapPath).readText()

val componentRegex = Regex("""(?ms)^      <component .*?^      </component>""")
val coordinateRegex = Regex("""<component group="([^"]*)" name="([^"]*)" version="([^"]*)">""")

data class Coordinate(val group: String, val name: String, val version: String)

fun blockCoordinate(block: String): Coordinate {
    val match = coordinateRegex.find(block)
        ?: error("Cannot parse component coordinate from block:\n$block")
    val (group, name, version) = match.destructured
    return Coordinate(group, name, version)
}

fun componentsOf(text: String): List<String> = componentRegex.findAll(text).map { it.value }.toList()

val merged = LinkedHashMap<Coordinate, String>()
// Bootstrap first, then normal overwrites shared coordinates so the normal entry wins.
for (block in componentsOf(bootstrapText)) merged[blockCoordinate(block)] = block
for (block in componentsOf(normalText)) merged[blockCoordinate(block)] = block

val sortedBlocks = merged.entries
    .sortedWith(compareBy({ it.key.group }, { it.key.name }, { it.key.version }))
    .map { it.value }

val firstComponentIndex = normalText.indexOf("      <component ")
if (firstComponentIndex == -1) error("No <component> entries found in $normalPath")
val header = normalText.substring(0, firstComponentIndex)

val lastComponentEnd = normalText.lastIndexOf("      </component>")
if (lastComponentEnd == -1) error("No </component> found in $normalPath")
val footer = normalText.substring(lastComponentEnd + "      </component>".length)

val output = buildString {
    append(header)
    append(sortedBlocks.joinToString("\n"))
    append(footer)
}

File(outputPath).writeText(output)

println("Merged ${componentsOf(normalText).size} (normal) + ${componentsOf(bootstrapText).size} (bootstrap) -> ${sortedBlocks.size} components into $outputPath")
