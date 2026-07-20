/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.konan.test.blackbox.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.PackageName
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCaseId
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.TestFile
import org.jetbrains.kotlin.konan.test.blackbox.support.TestKind
import org.jetbrains.kotlin.konan.test.blackbox.support.TestModule
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.ExecutableCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.LLDB
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.util.LLDBSessionSpec
import org.junit.jupiter.api.Test
import java.io.File

@EnforcedProperty(ClassLevelProperty.COMPILER_OUTPUT_INTERCEPTOR, "NONE")
@EnforcedProperty(ClassLevelProperty.OPTIMIZATION_MODE, propertyValue = "DEBUG")
class LldbStdlibCollectionsInspectionExperimentTest : AbstractNativeSimpleTest() {
    @Test
    fun experimentInspectStdlibCollections() {
        val testDirectory = buildDir.resolve("experimentInspectStdlibCollections").apply {
            deleteRecursively()
            mkdirs()
        }
        val sourceDirectory = testDirectory.resolve("lldbStdlibCollectionsInspectionExperiment").apply {
            mkdirs()
        }

        val sourceFile = sourceDirectory.resolve("main.kt").apply {
            writeText(KOTLIN_SOURCE)
        }
        val inspectionScript = sourceDirectory.resolve("inspect.py").apply {
            writeText(INSPECTION_SCRIPT)
        }

        val compileModule = TestModule.Exclusive(
            "lldbStdlibCollectionsInspectionExperiment",
            emptySet(),
            emptySet(),
            emptySet()
        ).apply {
            files += TestFile.createCommitted(sourceFile, this)
        }
        val compileTestCase = TestCase(
            id = TestCaseId.Named("lldbStdlibCollectionsInspectionExperimentCompilation"),
            kind = TestKind.STANDALONE_NO_TR,
            modules = setOf(compileModule),
            freeCompilerArgs = TestCompilerArgs(listOf("-g")),
            nominalPackageName = PackageName.EMPTY,
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout),
            extras = TestCase.NoTestRunnerExtras(entryPoint = "main")
        ).apply {
            initialize(null, null)
        }
        val compilationResult = ExecutableCompilation(
            settings = testRunSettings,
            freeCompilerArgs = compileTestCase.freeCompilerArgs,
            sourceModules = compileTestCase.modules,
            extras = compileTestCase.extras,
            dependencies = emptyList(),
            expectedArtifact = TestCompilationArtifact.Executable(testDirectory.resolve("app.${targets.testTarget.family.exeSuffix}")),
            tryPassSystemCacheDirectory = true,
        ).result.assertSuccess()

        val rawOutputFile = testDirectory.resolve("lldb-stdlib-collections-inspection.raw.txt")
        val normalizedOutputFile = testDirectory.resolve("lldb-stdlib-collections-inspection.txt")

        val testName =
            "${LldbStdlibCollectionsInspectionExperimentTest::class.qualifiedName}.${::experimentInspectStdlibCollections.name}"
        val moduleForRun = TestModule.Exclusive(
            "lldbStdlibCollectionsInspectionExperiment",
            emptySet(),
            emptySet(),
            emptySet()
        )
        val testCase = TestCase(
            id = TestCaseId.Named(testName),
            kind = TestKind.STANDALONE_LLDB,
            modules = setOf(moduleForRun),
            freeCompilerArgs = compileTestCase.freeCompilerArgs,
            nominalPackageName = PackageName.EMPTY,
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout).copy(
                outputMatcher = TestRunCheck.OutputMatcher { output ->
                    rawOutputFile.writeText(output)
                    normalizedOutputFile.writeText(LLDBSessionSpec.replaceUnstableIds(output))
                    true
                }
            ),
            extras = TestCase.NoTestRunnerExtras(
                entryPoint = "main",
                arguments = generateLLDBArguments(inspectionScript)
            )
        ).apply {
            initialize(null, null)
        }

        val executable = TestExecutable.fromCompilationResult(testCase, compilationResult)
        runExecutableAndVerify(testCase, executable)
    }

    private fun generateLLDBArguments(inspectionScript: File): List<String> = buildList {
        val prettyPrinters = testRunSettings.get<LLDB>().prettyPrinters
        add("--no-lldbinit")
        add("-b")
        add("-o")
        add("settings set stop-disassembly-display never")
        add("-o")
        add("script import os; _ = os.environ.pop('GLOG_log_dir', None)")
        add("-o")
        add("command script import ${prettyPrinters.absolutePath}")
        add("-o")
        add("b main.kt:32")
        add("-o")
        add("r")
        add("-o")
        add("script exec(open('${inspectionScript.absolutePath}').read(), globals())")
        add("-o")
        add("q")
    }

    companion object {
        private val KOTLIN_SOURCE = """
            data class Point(val x: Int, val y: Int)

            fun main() {
                val emptyString = ""
                val helloString = "hello"
                val multilineString = "hello\nworld"

                val emptyList = emptyList<Int>()
                val intList = listOf(1, 2, 3)
                val stringList = listOf("", "alpha", "beta")
                val pointList = listOf(Point(1, 2), Point(3, 4))
                val mutableIntList = mutableListOf(1, 2, 3)
                val arrayList = arrayListOf(1, 2, 3)
                val builtList = buildList { add(10); add(20); add(30) }

                val emptySet = emptySet<Int>()
                val intSet = setOf(1, 2, 3)
                val mutableIntSet = mutableSetOf(1, 2, 3)
                val linkedIntSet = linkedSetOf(1, 2, 3)
                val hashIntSet = hashSetOf(1, 2, 3)
                val builtSet = buildSet { add(10); add(20); add(30) }

                val emptyMap = emptyMap<String, Int>()
                val intMap = mapOf("one" to 1, "two" to 2, "" to 3)
                val mutableIntMap = mutableMapOf("one" to 1, "two" to 2)
                val linkedIntMap = linkedMapOf("one" to 1, "two" to 2)
                val hashIntMap = hashMapOf("one" to 1, "two" to 2)
                val builtMap = buildMap { put("ten", 10); put("twenty", 20) }

                val arrayDeque = ArrayDeque(listOf(1, 2, 3, 4))

                println(
                    emptyString.length +
                        helloString.length +
                        multilineString.length +
                        emptyList.size +
                        intList.size +
                        stringList.size +
                        pointList.size +
                        mutableIntList.size +
                        arrayList.size +
                        builtList.size +
                        emptySet.size +
                        intSet.size +
                        mutableIntSet.size +
                        linkedIntSet.size +
                        hashIntSet.size +
                        builtSet.size +
                        emptyMap.size +
                        intMap.size +
                        mutableIntMap.size +
                        linkedIntMap.size +
                        hashIntMap.size +
                        builtMap.size +
                        arrayDeque.size
                )
            }
        """.trimIndent()

        private val INSPECTION_SCRIPT = """
            import lldb


            VARS = [
                "emptyString",
                "helloString",
                "multilineString",
                "emptyList",
                "intList",
                "stringList",
                "pointList",
                "mutableIntList",
                "arrayList",
                "builtList",
                "emptySet",
                "intSet",
                "mutableIntSet",
                "linkedIntSet",
                "hashIntSet",
                "builtSet",
                "emptyMap",
                "intMap",
                "mutableIntMap",
                "linkedIntMap",
                "hashIntMap",
                "builtMap",
                "arrayDeque",
            ]


            def selected_frame():
                target = lldb.debugger.GetSelectedTarget()
                process = target.GetProcess()
                thread = process.GetSelectedThread()
                return thread.GetSelectedFrame()


            def resolve_var(name):
                raw = selected_frame().FindVariable(name)
                synthetic = raw.GetSyntheticValue()
                return synthetic if synthetic.IsValid() else raw


            def format_scalar(value):
                if value is None:
                    return None
                return value.replace("\n", "\\n")


            def describe(value):
                return {
                    "name": value.GetName(),
                    "type": value.GetTypeName(),
                    "value": format_scalar(value.GetValue()),
                    "summary": format_scalar(value.GetSummary()),
                    "num_children": value.GetNumChildren(),
                }


            def print_children(value, indent, limit):
                total = value.GetNumChildren()
                for index in range(min(total, limit)):
                    child = value.GetChildAtIndex(index)
                    print(f"{indent}child[{index}] {describe(child)}")

                    grandchild_total = child.GetNumChildren()
                    for nested_index in range(min(grandchild_total, 3)):
                        grandchild = child.GetChildAtIndex(nested_index)
                        print(
                            f"{indent}  grandchild[{nested_index}] "
                            f"{describe(grandchild)}"
                        )

                if total > limit:
                    print(f"{indent}... {total - limit} more children")


            for name in VARS:
                value = resolve_var(name)
                print(f"== {name} ==")
                print(f"root {describe(value)}")
                print_children(value, "  ", 8)
        """.trimIndent()
    }
}
