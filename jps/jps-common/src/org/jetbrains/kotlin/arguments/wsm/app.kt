/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.wsm

import org.jetbrains.kotlin.cli.common.arguments.Freezable
import java.io.File

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

fun main() {
    val rootPath = System.getProperty("user.dir")
    println("Root dir is: $rootPath")
    if(rootPath.isNullOrEmpty()) error("Fail to determine project root")
    for (fileName in COMPILER_ARGUMENTS_FILES) {
        println("Processing $fileName")
        val originalCompilerArgumentsFile = rootPath + "/compiler/cli/cli-common/src/org/jetbrains/kotlin/cli/common/arguments/" + fileName + ".kt"
        val processedCompilerArgumentsFile = rootPath + "/jps/jps-common/src/org/jetbrains/kotlin/arguments/wsm/" + fileName + "WsmGenerated.kt"
        processAndWriteCompilerArgumentsFile(File(originalCompilerArgumentsFile), File(processedCompilerArgumentsFile))
    }
}

fun processAndWriteCompilerArgumentsFile(originalFile: File, generatedFile: File) {
    println("Generating compiler arguments from $originalFile to $generatedFile")
    if (!originalFile.exists()) error("File does not exist: ${originalFile.path}")

    val updatedContent = originalFile.readLines()
        .removeAnnotations()
        .fixNaming(originalFile.name, generatedFile.name)
        .fixPackage("package org.jetbrains.kotlin.cli.common.arguments", "package org.jetbrains.kotlin.arguments.wsm")
        .removeFreezable()
        .joinToString("\n")

    generatedFile.writeText(updatedContent)
}

private fun List<String>.removeFreezable(): List<String> {
    val updatedLines: MutableList<String> = mutableListOf()
    for (line: String in this) {
        updatedLines.add(line.replace("Freezable(),", ""))
    }
    return updatedLines
}

private fun List<String>.fixNaming(originalFileName: String, generatedFileName: String): List<String> {
    val updatedLines: MutableList<String> = mutableListOf()
    for (line: String in this) {
        updatedLines.add(line.replace(originalFileName.substringBefore(".kt"), generatedFileName.substringBefore(".kt")))
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
        if (line.trimStart().startsWith("@GradleOption") || line.trimStart().startsWith("@Argument")) {
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