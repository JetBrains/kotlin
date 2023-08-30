/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.unicode

import generators.unicode.mappings.oneToOne.MappingsGenerator
import generators.unicode.mappings.oneToMany.OneToManyMappingsGenerator
import generators.unicode.ranges.CharCategoryTestGenerator
import generators.unicode.ranges.RangesGenerator
import generators.unicode.mappings.string.StringCasingTestGenerator
import generators.unicode.mappings.string.StringLowercaseGenerator
import generators.unicode.mappings.string.StringUppercaseGenerator
import generators.unicode.ranges.OtherLowercaseRangesGenerator
import generators.unicode.ranges.OtherUppercaseRangesGenerator
import templates.KotlinTarget
import java.io.File
import java.net.URL
import kotlin.system.exitProcess


// Go to https://www.unicode.org/versions/latest/ to find out the latest public version of the Unicode Character Database files.
private const val unicodeVersion = "13.0.0"
private const val unicodeDataUrl = "https://www.unicode.org/Public/$unicodeVersion/ucd/UnicodeData.txt"
private const val specialCasingUrl = "https://www.unicode.org/Public/$unicodeVersion/ucd/SpecialCasing.txt"
private const val propListUrl = "https://www.unicode.org/Public/$unicodeVersion/ucd/PropList.txt"
private const val wordBreakPropertyUrl = "https://www.unicode.org/Public/$unicodeVersion/ucd/auxiliary/WordBreakProperty.txt"
private const val derivedCorePropertiesUrl = "https://www.unicode.org/Public/$unicodeVersion/ucd/DerivedCoreProperties.txt"

/**
 * This program generates sources related to UnicodeData.txt and SpecialCasing.txt.
 * Pass the root directory of the project to generate sources for js, js-ir and native.
 *  _CharCategoryTest.kt and supporting files are also generated to test the generated sources.
 *  The generated test is meant to be run after updating Unicode version and should not be merged to master.
 */
fun main(args: Array<String>) {
    fun readLines(url: String): List<String> {
        return URL(url).openStream().reader().readLines()
    }

    val unicodeDataLines = readLines(unicodeDataUrl).map { line -> UnicodeDataLine(line.split(";")) }
    val bmpUnicodeDataLines = unicodeDataLines.filter { line -> line.char.length <= 4 } // Basic Multilingual Plane (BMP)

    fun String.isEmptyOrComment(): Boolean = isEmpty() || startsWith("#")

    val specialCasingLines = readLines(specialCasingUrl).filterNot(String::isEmptyOrComment).map { line ->
        SpecialCasingLine(line.split("; "))
    }

    val propListLines = readLines(propListUrl).filterNot(String::isEmptyOrComment).map { line ->
        PropertyLine(line.split("; ").map { it.trim() })
    }

    val wordBreakPropertyLines = readLines(wordBreakPropertyUrl).filterNot(String::isEmptyOrComment).map { line ->
        PropertyLine(line.split("; ").map { it.trim() })
    }

    val derivedCorePropertiesLines = readLines(derivedCorePropertiesUrl).filterNot(String::isEmptyOrComment).map { line ->
        PropertyLine(line.split("; ").map { it.trim() })
    }

    val categoryRangesGenerators = mutableListOf<RangesGenerator>()
    val otherLowercaseGenerators = mutableListOf<OtherLowercaseRangesGenerator>()
    val otherUppercaseGenerators = mutableListOf<OtherUppercaseRangesGenerator>()

    fun addRangesGenerators(generatedDir: File, target: KotlinTarget) {
        val category = RangesGenerator.forCharCategory(generatedDir.resolve("_CharCategories.kt"), target)
        val digit = RangesGenerator.forDigit(generatedDir.resolve("_DigitChars.kt"), target)
        val letter = RangesGenerator.forLetter(generatedDir.resolve("_LetterChars.kt"), target)
        val whitespace = RangesGenerator.forWhitespace(generatedDir.resolve("_WhitespaceChars.kt"))
        categoryRangesGenerators.add(category)
        categoryRangesGenerators.add(digit)
        categoryRangesGenerators.add(letter)
        categoryRangesGenerators.add(whitespace)

        otherLowercaseGenerators.add(OtherLowercaseRangesGenerator(generatedDir.resolve("_OtherLowercaseChars.kt"), target))
        otherUppercaseGenerators.add(OtherUppercaseRangesGenerator(generatedDir.resolve("_OtherUppercaseChars.kt"), target))
    }

    val oneToOneMappingsGenerators = mutableListOf<MappingsGenerator>()

    fun addOneToOneMappingsGenerators(generatedDir: File, target: KotlinTarget) {
        val uppercase = MappingsGenerator.forUppercase(generatedDir.resolve("_UppercaseMappings.kt"), target)
        val lowercase = MappingsGenerator.forLowercase(generatedDir.resolve("_LowercaseMappings.kt"), target)
        val titlecase = MappingsGenerator.forTitlecase(generatedDir.resolve("_TitlecaseMappings.kt"))
        oneToOneMappingsGenerators.add(uppercase)
        oneToOneMappingsGenerators.add(lowercase)
        oneToOneMappingsGenerators.add(titlecase)
    }

    val oneToManyMappingsGenerators = mutableListOf<OneToManyMappingsGenerator>()

    fun addOneToManyMappingsGenerators(generatedDir: File, target: KotlinTarget) {
        val uppercase = OneToManyMappingsGenerator.forUppercase(generatedDir.resolve("_OneToManyUppercaseMappings.kt"), target, bmpUnicodeDataLines)
        val lowercase = OneToManyMappingsGenerator.forLowercase(generatedDir.resolve("_OneToManyLowercaseMappings.kt"), target, bmpUnicodeDataLines)
        oneToManyMappingsGenerators.add(uppercase)
        oneToManyMappingsGenerators.add(lowercase)
    }

    val stringUppercaseGenerators = mutableListOf<StringUppercaseGenerator>()
    val stringLowercaseGenerators = mutableListOf<StringLowercaseGenerator>()

    val categoryTestGenerator: CharCategoryTestGenerator

    val stringCasingTestGenerator: StringCasingTestGenerator

    when (args.size) {
        1 -> {
            val baseDir = File(args.first())

            val categoryTestFile = baseDir.resolve("libraries/stdlib/js/test/text/unicodeData/_CharCategoryTest.kt")
            categoryTestGenerator = CharCategoryTestGenerator(categoryTestFile)

            val commonGeneratedDir = baseDir.resolve("libraries/stdlib/common/src/generated")
            oneToManyMappingsGenerators.add(
                OneToManyMappingsGenerator.forTitlecase(commonGeneratedDir.resolve("_OneToManyTitlecaseMappings.kt"), bmpUnicodeDataLines)
            )

            val jsGeneratedDir = baseDir.resolve("libraries/stdlib/js/src/generated/")
            addRangesGenerators(jsGeneratedDir, KotlinTarget.JS)
            oneToOneMappingsGenerators.add(MappingsGenerator.forTitlecase(jsGeneratedDir.resolve("_TitlecaseMappings.kt")))

            val jsIrGeneratedDir = baseDir.resolve("libraries/stdlib/js-ir/src/generated/")
            addRangesGenerators(jsIrGeneratedDir, KotlinTarget.JS_IR)
            oneToOneMappingsGenerators.add(MappingsGenerator.forTitlecase(jsIrGeneratedDir.resolve("_TitlecaseMappings.kt")))

            val nativeGeneratedDir = baseDir.resolve("kotlin-native/runtime/src/main/kotlin/generated/")
            addRangesGenerators(nativeGeneratedDir, KotlinTarget.Native)
            addOneToOneMappingsGenerators(nativeGeneratedDir, KotlinTarget.Native)
            addOneToManyMappingsGenerators(nativeGeneratedDir, KotlinTarget.Native)
            stringUppercaseGenerators.add(
                StringUppercaseGenerator(nativeGeneratedDir.resolve("_StringUppercase.kt"), unicodeDataLines, KotlinTarget.Native)
            )
            stringLowercaseGenerators.add(
                StringLowercaseGenerator(nativeGeneratedDir.resolve("_StringLowercase.kt"), unicodeDataLines, KotlinTarget.Native)
            )

            val wasmGeneratedDir = baseDir.resolve("libraries/stdlib/wasm/src/generated/")
            addRangesGenerators(wasmGeneratedDir, KotlinTarget.WASM)
            addOneToOneMappingsGenerators(wasmGeneratedDir, KotlinTarget.WASM)
            addOneToManyMappingsGenerators(wasmGeneratedDir, KotlinTarget.WASM)
            stringUppercaseGenerators.add(
                StringUppercaseGenerator(wasmGeneratedDir.resolve("_StringUppercase.kt"), unicodeDataLines, KotlinTarget.WASM)
            )
            stringLowercaseGenerators.add(
                StringLowercaseGenerator(wasmGeneratedDir.resolve("_StringLowercase.kt"), unicodeDataLines, KotlinTarget.WASM)
            )

            val nativeTestDir = baseDir.resolve("kotlin-native/runtime/test/text")
            stringCasingTestGenerator = StringCasingTestGenerator(nativeTestDir)

            // For debugging. To see the file content
            fun downloadFile(fromUrl: String) {
                val fileName = File(fromUrl).name
                val dest = baseDir.resolve("libraries/tools/kotlin-stdlib-gen/src/generators/unicode/$fileName")
                dest.writeText(readLines(fromUrl).joinToString(separator = "\n"))
            }
            downloadFile(unicodeDataUrl)
            downloadFile(specialCasingUrl)
        }
        else -> {
            println(
                """Parameters:
    <kotlin-base-dir> - generates sources for js, js-ir and native targets using paths derived from specified base path
"""
            )
            exitProcess(1)
        }
    }

    categoryRangesGenerators.forEach {
        bmpUnicodeDataLines.forEach { line -> it.appendLine(line) }
        it.generate()
    }
    otherLowercaseGenerators.forEach {
        propListLines.forEach { line -> it.appendLine(line) }
        it.generate()
    }
    otherUppercaseGenerators.forEach {
        propListLines.forEach { line -> it.appendLine(line) }
        it.generate()
    }

    categoryTestGenerator.let {
        bmpUnicodeDataLines.forEach { line -> it.appendLine(line) }
        propListLines.forEach { line -> it.appendPropertyLine(line) }
        it.generate()
    }

    oneToOneMappingsGenerators.forEach {
        unicodeDataLines.forEach { line -> it.appendLine(line) }
        it.generate()
    }

    oneToManyMappingsGenerators.forEach {
        specialCasingLines.forEach { line -> it.appendLine(line) }
        it.generate()
    }

    stringUppercaseGenerators.forEach {
        specialCasingLines.forEach { line -> it.appendSpecialCasingLine(line) }
        it.generate()
    }

    stringLowercaseGenerators.forEach {
        specialCasingLines.forEach { line -> it.appendSpecialCasingLine(line) }
        wordBreakPropertyLines.forEach { line -> it.appendWordBreakPropertyLine(line) }
        it.generate()
    }

    stringCasingTestGenerator.let {
        derivedCorePropertiesLines.forEach { line -> it.appendDerivedCorePropertiesLine(line) }
        it.generate()
    }

}
