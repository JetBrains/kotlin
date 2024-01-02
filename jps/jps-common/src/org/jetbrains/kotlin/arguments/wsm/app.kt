/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.wsm

import org.jetbrains.kotlin.cli.common.arguments.Freezable
import java.io.File

fun main() {
    val rootPath = System.getProperty("user.dir")
    println("Root dir is: $rootPath")
    if(rootPath.isNullOrEmpty()) error("Fail to determine project root")
    for (fileName in COMPILER_ARGUMENTS_FILES) {
        println("Processing $fileName")
        val originalCompilerArgumentsFile = "$rootPath/compiler/cli/cli-common/src/org/jetbrains/kotlin/cli/common/arguments/$fileName.kt"
        val processedCompilerArgumentsFile = "$rootPath/jps/jps-common/src/org/jetbrains/kotlin/arguments/wsm/$fileName$wsmSuffix.kt"
        processAndWriteCompilerArgumentsFile(File(originalCompilerArgumentsFile), File(processedCompilerArgumentsFile))
    }
}

val COMPILER_ARGUMENTS_FILES = listOf(
    "K2JVMCompilerArguments",
    "CommonCompilerArguments",
    "CommonToolArguments",
//    "Freezable",
    "K2JSCompilerArguments",
    "K2JSDceArguments",
    "K2JVMCompilerArguments",
    "K2MetadataCompilerArguments",
    "K2NativeCompilerArguments",
)

const val wsmSuffix = "WsmGenerated"



fun processAndWriteCompilerArgumentsFile(originalFile: File, generatedFile: File) {
    println("Generating compiler arguments from $originalFile to $generatedFile")
    if (!originalFile.exists()) error("File does not exist: ${originalFile.path}")

    val updatedContent = originalFile.readLines()
        .removeAnnotations()
        .fixNaming(originalFile.name, generatedFile.name)
        .fixPackage("package org.jetbrains.kotlin.cli.common.arguments", "package org.jetbrains.kotlin.arguments.wsm")
        .removeFreezable()
        .removeTransient()
        .addImports(originalFile.name)
        .joinToString("\n")

    generatedFile.writeText(updatedContent)
}

private fun List<String>.removeFreezable(): List<String> {
    val updatedLines: MutableList<String> = mutableListOf()
    for (line: String in this) {
        updatedLines.add(
            line.replace("Freezable(),", "")
                .replace("checkFrozen()", "")
                .takeIf { !it.contains("copyOf") } ?: "")
    }
    return updatedLines
}

private fun List<String>.removeTransient(): List<String> {
    val updatedLines: MutableList<String> = mutableListOf()
    for (line: String in this) {
        updatedLines.add(line.replace("@Transient", "").replace("var errors: ArgumentParseErrors? = null", ""))
    }
    return updatedLines
}

private fun List<String>.addImports(fileName: String): List<String> {
    val updatedLines: MutableList<String> = mutableListOf()
    for (line: String in this) {
        updatedLines.add(line)
        if(line.contains("package ")) {
            updatedLines.add("import org.jetbrains.kotlin.cli.common.arguments.Argument")
            if (fileName.contains("K2JVMCompilerArguments")) updatedLines.add("import org.jetbrains.kotlin.cli.common.arguments.JavaTypeEnhancementStateParser")
            if (fileName.contains("CommonToolArguments")) {
                updatedLines.add("import org.jetbrains.kotlin.cli.common.arguments.InternalArgument")
            }
            if (fileName.contains("CommonCompilerArguments")) {
                updatedLines.add("import org.jetbrains.kotlin.cli.common.arguments.ManualLanguageFeatureSetting")
            }
        }
    }
    return updatedLines
}

private fun List<String>.fixNaming(originalFileName: String, generatedFileName: String): List<String> {
    val updatedLines: MutableList<String> = mutableListOf()
    for (line: String in this) {
        var updatedLine = line
        COMPILER_ARGUMENTS_FILES.forEach { fileName ->
            updatedLine = updatedLine.replace(fileName, "${fileName}$wsmSuffix")
        }
        updatedLines.add(updatedLine)
    }
    return updatedLines
}

private fun List<String>.fixPackage(originalPackageString: String, generatedPackageString: String): List<String> {
    val updatedLines: MutableList<String> = mutableListOf()
    for (line: String in this) {
        updatedLines.add(line.replace(originalPackageString, generatedPackageString))
    }
    return updatedLines
}

fun List<String>.removeAnnotations(): List<String> {
    var insideAnnotation = false
    var openBrackets = 0
    val updatedLines = mutableListOf<String>()

    for (line: String in this) {
        if (line.trimStart().startsWith("@GradleOption") || line.trimStart().startsWith("@GradleDeprecatedOption")) {
            insideAnnotation = true
        }

        if (insideAnnotation) {
            openBrackets += line.count { it == '(' }
            openBrackets -= line.count { it == ')' }

            if (openBrackets <= 0) {
                insideAnnotation = false
                openBrackets = 0
                continue
            }
        } else {
            updatedLines.add(line)
        }
    }

    return updatedLines
}