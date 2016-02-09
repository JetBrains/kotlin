package org.jetbrains.kotlin.gradle.incremental

import java.io.File

class BuildStep(
        val compiledKotlinFiles: MutableSet<String> = hashSetOf(),
        val compiledJavaFiles: MutableSet<String> = hashSetOf(),
        val compileErrors: MutableList<String> = arrayListOf()
) {
    val compileSucceeded: Boolean
        get() = compileErrors.isEmpty()
}

fun parseTestBuildLog(file: File): List<BuildStep> {
    fun splitSteps(lines: List<String>): List<List<String>> {
        val stepsLines = mutableListOf<MutableList<String>>()

        for (line in lines) {
            when {
                line.matches("=+ Step #\\d+ =+".toRegex()) -> {
                    stepsLines.add(mutableListOf())
                }
                else -> {
                    stepsLines.lastOrNull()?.add(line)
                }
            }
        }

        return stepsLines
    }

    fun BuildStep.parseStepCompiledFiles(stepLines: List<String>) {
        var readFiles = false

        for (line in stepLines) {
            if (line.startsWith("Compiling files:")) {
                readFiles = true
                continue
            }

            if (readFiles && line.startsWith("End of files")) {
                readFiles = false
                continue
            }

            if (readFiles) {
                val path = line.trim()

                if (path.endsWith(".kt")) {
                    compiledKotlinFiles.add(path)
                }
                else if (path.endsWith(".java")) {
                    compiledJavaFiles.add(path)
                }
                else {
                    throw IllegalStateException("Expected .kt or .java file, got: $path")
                }
            }
        }
    }

    fun BuildStep.parseErrors(stepLines: List<String>) {
        val startIndex = stepLines.indexOfLast { it.startsWith("COMPILATION FAILED") }

        if (startIndex > 0) {
            compileErrors.addAll(stepLines.subList(startIndex + 1, stepLines.size))
        }
    }

    val stepsLines = splitSteps(file.readLines())


    return stepsLines.map { stepLines ->
        val buildStep = BuildStep()
        buildStep.parseStepCompiledFiles(stepLines)
        buildStep.parseErrors(stepLines)
        buildStep
    }
}
