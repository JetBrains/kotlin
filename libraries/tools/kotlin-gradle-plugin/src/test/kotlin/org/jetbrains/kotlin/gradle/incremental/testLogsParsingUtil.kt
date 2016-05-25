package org.jetbrains.kotlin.gradle.incremental

import org.jetbrains.kotlin.com.intellij.openapi.util.io.FileUtil
import java.io.File

private const val BEGIN_COMPILED_FILES = "Compiling files:"
private const val END_COMPILED_FILES = "End of files"
private const val BEGIN_ERRORS = "COMPILATION FAILED"

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
            if (line.startsWith(BEGIN_COMPILED_FILES)) {
                readFiles = true
                continue
            }

            if (readFiles && line.startsWith(END_COMPILED_FILES)) {
                readFiles = false
                continue
            }

            if (readFiles) {
                val path = FileUtil.normalize(line.trim())

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
        val startIndex = stepLines.indexOfLast { it.startsWith(BEGIN_ERRORS) }

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

fun dumpBuildLog(buildSteps: Iterable<BuildStep>): String {
    val sb = StringBuilder()

    for ((i, step) in buildSteps.withIndex()) {
        if (i > 0) {
            sb.appendln()
        }

        sb.appendln("================ Step #${i+1} =================")
        sb.appendln()
        sb.appendln(BEGIN_COMPILED_FILES)
        step.compiledKotlinFiles.sorted().forEach { sb.appendln(it) }
        step.compiledJavaFiles.sorted().forEach { sb.appendln(it) }
        sb.appendln(END_COMPILED_FILES)
        sb.appendln("------------------------------------------")

        if (!step.compileSucceeded) {
            sb.appendln(BEGIN_ERRORS)
            step.compileErrors.forEach { sb.appendln(it) }
        }
    }

    return sb.toString()
}