/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import java.io.File
import java.util.regex.Pattern

class ProjectInfo(val name: String, val modules: List<String>, val steps: List<ProjectBuildStep>, val muted: Boolean) {

    class ProjectBuildStep(val id: Int, val order: List<String>)
}

class ModuleInfo(val moduleName: String) {

    sealed class Modification {
        class Delete(private val fileName: String) : Modification() {
            override fun execute(testDirectory: File, sourceDirectory: File) {
                File(sourceDirectory, fileName).delete()
            }
        }

        class Update(private val fromFile: String, private val toFile: String) : Modification() {
            override fun execute(testDirectory: File, sourceDirectory: File) {
                val toFile = File(sourceDirectory, toFile)
                if (toFile.exists()) {
                    toFile.delete()
                }

                val fromFile = File(testDirectory, fromFile)

                fromFile.copyTo(toFile, overwrite = true)
            }
        }

        abstract fun execute(testDirectory: File, sourceDirectory: File)
    }

    class ModuleStep(val id: Int, val dependencies: Collection<String>, val dirtyFiles: Collection<String>, val modifications: List<Modification>)

    val steps = mutableListOf<ModuleStep>()
}

const val MODULES_LIST = "MODULES"

private val STEP_PATTERN = Pattern.compile("^\\s*STEP\\s+(\\d+)\\s*:?$")

private val MODIFICATION_PATTERN = Pattern.compile("^([UD])\\s*:(.+)$")

abstract class InfoParser<Info>(protected val infoFile: File) {
    protected var lineCounter = 0
    protected val lines = infoFile.readLines()

    abstract fun parse(entryName: String): Info

    protected fun loop(lambda: (String) -> Boolean) {
        while (lineCounter < lines.size) {
            val line = lines[lineCounter]
            if (line.isBlank()) {
                ++lineCounter
                continue
            }

            if (lambda(line.trim())) {
                break
            }
        }
    }


    protected fun diagnosticMessage(message: String, line: String): String {
        return "$message in '$line' at ${infoFile.path}:${lineCounter - 1}"
    }

    protected fun throwSyntaxError(line: String): Nothing {
        throw AssertionError(diagnosticMessage("Syntax error", line))
    }

}

class ProjectInfoParser(infoFile: File) : InfoParser<ProjectInfo>(infoFile) {


    private fun parseStep(stepId: Int): ProjectInfo.ProjectBuildStep {
        val order = mutableListOf<String>()

        loop { line ->
            val splitIndex = line.indexOf(':')
            if (splitIndex < 0) throwSyntaxError(line)

            val splitted = line.split(":")
            val op = splitted[0]

            if (op.matches(STEP_PATTERN.toRegex())) {
                return@loop true // break the loop
            }

            ++lineCounter

            when (op) {
                "libs" -> {
                    val args = splitted[1]
                    args.split(",").filter { it.isNotBlank() }.forEach { order.add(it.trim()) }
                }
                else -> println(diagnosticMessage("Unknown op $op", line))
            }

            false
        }

        return ProjectInfo.ProjectBuildStep(stepId, order)
    }

    override fun parse(entryName: String): ProjectInfo {
        val libraries = mutableListOf<String>()
        val steps = mutableListOf<ProjectInfo.ProjectBuildStep>()
        var muted = false

        loop { line ->
            lineCounter++

            if (line == "MUTED") {
                muted = true
                return@loop false
            }

            val splitIndex = line.indexOf(':')
            if (splitIndex < 0) throwSyntaxError(line)

            val splitted = line.split(":")
            val op = splitted[0]

            when {
                op == MODULES_LIST -> {
                    val arguments = splitted[1]
                    arguments.split(",").filter { it.isNotBlank() }.forEach { libraries.add(it.trim()) }
                }
                op.matches(STEP_PATTERN.toRegex()) -> {
                    val m = STEP_PATTERN.matcher(op)
                    if (!m.matches()) throwSyntaxError(line)
                    val stepId = Integer.parseInt(m.group(1))
                    steps.add(parseStep(stepId))
                }
                else -> println(diagnosticMessage("Unknown op $op", line))
            }

            false
        }

        return ProjectInfo(entryName, libraries, steps, muted)
    }
}

class ModuleInfoParser(infoFile: File) : InfoParser<ModuleInfo>(infoFile) {

    private fun parseModifications(): List<ModuleInfo.Modification> {
        val modifications = mutableListOf<ModuleInfo.Modification>()

        loop { line ->
            val matcher3 = MODIFICATION_PATTERN.matcher(line)
            if (matcher3.matches()) {
                lineCounter++
                val mop = matcher3.group(1)
                val cmd = matcher3.group(2)
                when (mop) {
                    "U" -> {
                        val (from, to) = cmd.split("->")
                        modifications.add(ModuleInfo.Modification.Update(from.trim(), to.trim()))
                    }
                    "D" -> modifications.add(ModuleInfo.Modification.Delete(cmd.trim()))
                    else -> error("Unknown modification $line")
                }
                false
            } else {
                true
            }
        }

        return modifications
    }

    private fun parseStep(stepId: Int): ModuleInfo.ModuleStep {
        val dependencies = mutableSetOf<String>()
        val dirtyFiles = mutableSetOf<String>()
        val modifications = mutableListOf<ModuleInfo.Modification>()

        loop { line ->
            if (line.matches(STEP_PATTERN.toRegex()))
                return@loop true
            lineCounter++

            val opIndex = line.indexOf(':')
            if (opIndex < 0) throwSyntaxError(line)
            val op = line.substring(0, opIndex)

            when (op) {
                "dependencies" -> line.substring(opIndex + 1).split(",").filter { it.isNotBlank() }.forEach { dependencies.add(it.trim()) }
                "dirty" -> line.substring(opIndex + 1).split(",").filter { it.isNotBlank() }.forEach { dirtyFiles.add(it.trim()) }
                "modifications" -> modifications.addAll(parseModifications())
                else -> println(diagnosticMessage("Unknown op $op", line))
            }
            false
        }

        return ModuleInfo.ModuleStep(stepId, dependencies, dirtyFiles, modifications)
    }

    override fun parse(entryName: String): ModuleInfo {
        val result = ModuleInfo(entryName)

        loop { line ->
            lineCounter++
            val stepMatcher = STEP_PATTERN.matcher(line)
            if (stepMatcher.matches()) {
                val id = Integer.parseInt(stepMatcher.group(1))
                result.steps.add(parseStep(id))
            }
            false
        }

        return result
    }
}
