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
import kotlin.test.assertContains

@EnforcedProperty(ClassLevelProperty.COMPILER_OUTPUT_INTERCEPTOR, "NONE")
@EnforcedProperty(ClassLevelProperty.OPTIMIZATION_MODE, propertyValue = "DEBUG")
class LldbArrayInspectionBenchmarkTest : AbstractNativeSimpleTest() {
    @Test
    fun benchmarkLargeArrayInspection() {
        val testDirectory = buildDir.resolve("benchmarkLargeArrayInspection").apply {
            deleteRecursively()
            mkdirs()
        }
        val sourceDirectory = testDirectory.resolve("lldbArrayInspectionBenchmark").apply {
            mkdirs()
        }

        val sourceFile = sourceDirectory.resolve("main.kt").apply {
            writeText(KOTLIN_SOURCE)
        }
        val benchmarkScript = sourceDirectory.resolve("bench.py").apply {
            writeText(BENCHMARK_SCRIPT)
        }

        val compileModule = TestModule.Exclusive("lldbArrayInspectionBenchmark", emptySet(), emptySet(), emptySet()).apply {
            files += TestFile.createCommitted(sourceFile, this)
        }
        val compileTestCase = TestCase(
            id = TestCaseId.Named("lldbArrayInspectionBenchmarkCompilation"),
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

        val rawOutputFile = testDirectory.resolve("lldb-array-inspection-benchmark.raw.txt")
        val normalizedOutputFile = testDirectory.resolve("lldb-array-inspection-benchmark.txt")

        val testName = "${LldbArrayInspectionBenchmarkTest::class.qualifiedName}.${::benchmarkLargeArrayInspection.name}"
        val moduleForRun = TestModule.Exclusive("lldbArrayInspectionBenchmark", emptySet(), emptySet(), emptySet())
        val testCase = TestCase(
            id = TestCaseId.Named(testName),
            kind = TestKind.STANDALONE_LLDB,
            modules = setOf(moduleForRun),
            freeCompilerArgs = compileTestCase.freeCompilerArgs,
            nominalPackageName = PackageName.EMPTY,
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout).copy(
                outputMatcher = TestRunCheck.OutputMatcher { output ->
                    rawOutputFile.writeText(output)

                    val normalizedOutput = LLDBSessionSpec.replaceUnstableIds(output)
                    normalizedOutputFile.writeText(normalizedOutput)

                    assertContains(normalizedOutput, "== ints ==")
                    assertContains(normalizedOutput, "== points ==")
                    assertContains(normalizedOutput, "batch 000-049")
                    assertContains(normalizedOutput, "batch 250-299")
                    true
                }
            ),
            extras = TestCase.NoTestRunnerExtras(
                entryPoint = "main",
                arguments = generateLLDBArguments(benchmarkScript)
            )
        ).apply {
            initialize(null, null)
        }

        val executable = TestExecutable.fromCompilationResult(testCase, compilationResult)
        runExecutableAndVerify(testCase, executable)
    }

    private fun generateLLDBArguments(benchmarkScript: File): List<String> = buildList {
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
        add("b main.kt:5")
        add("-o")
        add("r")
        add("-o")
        add("script exec(open('${benchmarkScript.absolutePath}').read(), globals())")
        add("-o")
        add("q")
    }

    companion object {
        private val KOTLIN_SOURCE = """
            fun main() {
                val ints = IntArray(300) { it }
                val points = Array(300) { Point(it, it + 1) }

                println(ints[0])
            }

            data class Point(val x: Int, val y: Int)
        """.trimIndent()

        private val BENCHMARK_SCRIPT = """
            import collections
            import time
            
            import lldb
            import konan_lldb


            def _make_stat():
                return {"count": 0, "total": 0.0, "sample": None}


            EVAL_STATS = collections.defaultdict(_make_stat)
            CALL_STATS = collections.defaultdict(_make_stat)


            def classify_expr(expr):
                if "Konan_DebugBatchGetFieldAddress" in expr:
                    return "Konan_DebugBatchGetFieldAddress"
                if "Konan_DebugBatchGetFieldCount" in expr:
                    return "Konan_DebugBatchGetFieldCount"
                if "Konan_DebugBatchObjectToUtf8Array" in expr:
                    return "Konan_DebugBatchObjectToUtf8Array"
                if "Konan_DebugGetFieldAddress" in expr:
                    return "Konan_DebugGetFieldAddress"
                if "Konan_DebugGetFieldType" in expr:
                    return "Konan_DebugGetFieldType"
                if "Konan_DebugGetFieldCount" in expr:
                    return "Konan_DebugGetFieldCount"
                if "Konan_DebugGetFieldName" in expr:
                    return "Konan_DebugGetFieldName"
                if "Konan_DebugObjectToUtf8Array" in expr:
                    return "Konan_DebugObjectToUtf8Array"
                if "Konan_DebugBufferSize" in expr:
                    return "Konan_DebugBufferSize"
                if "Konan_DebugBuffer()" in expr:
                    return "Konan_DebugBuffer"
                if "Konan_DebugIsInstance" in expr or "Konan_DebugIsArray" in expr:
                    return "Konan_DebugIsInstanceOrArray"
                if "*(void **)((uintptr_t)(*(void**)" in expr:
                    return "TypeInfoLookup"
                return "other"


            def patch_function(module, name, stats, classifier=None):
                original = getattr(module, name)
                original_argcount = original.__code__.co_argcount

                def wrapped(*args, **kwargs):
                    start = time.perf_counter()
                    try:
                        return original(*args[:original_argcount], **kwargs)
                    finally:
                        key = classifier(*args, **kwargs) if classifier is not None else name
                        entry = stats[key]
                        entry["count"] += 1
                        entry["total"] += time.perf_counter() - start
                        if entry["sample"] is None:
                            entry["sample"] = args[0] if args else name

                setattr(module, name, wrapped)


            def patch_method(cls, name, stats, label=None):
                original = getattr(cls, name)

                def wrapped(self, *args, **kwargs):
                    start = time.perf_counter()
                    try:
                        return original(self, *args, **kwargs)
                    finally:
                        key = label or f"{cls.__name__}.{name}"
                        entry = stats[key]
                        entry["count"] += 1
                        entry["total"] += time.perf_counter() - start
                        if entry["sample"] is None:
                            entry["sample"] = key

                setattr(cls, name, wrapped)


            patch_function(konan_lldb, "_evaluate", EVAL_STATS, lambda expr: classify_expr(expr))
            patch_function(konan_lldb, "_type_info", CALL_STATS)
            patch_function(konan_lldb, "_is_string_or_array", CALL_STATS)
            patch_function(konan_lldb, "_render_object", CALL_STATS)
            patch_function(konan_lldb, "kotlin_object_type_summary", CALL_STATS)
            patch_function(konan_lldb, "_select_provider", CALL_STATS)
            patch_method(konan_lldb.KonanProxyTypeProvider, "__init__", CALL_STATS)
            patch_method(konan_lldb.KonanHelperProvider, "_read_value", CALL_STATS)
            patch_method(konan_lldb.KonanHelperProvider, "_field_type", CALL_STATS)
            patch_method(konan_lldb.KonanHelperProvider, "_field_address", CALL_STATS)
            patch_method(konan_lldb.KonanArraySyntheticProvider, "get_child_at_index", CALL_STATS)
            if hasattr(konan_lldb, "FastKonanArraySyntheticProvider"):
                patch_method(konan_lldb.FastKonanArraySyntheticProvider, "get_child_at_index", CALL_STATS)
            patch_method(konan_lldb.KonanStringSyntheticProvider, "__init__", CALL_STATS)
            
            
            def selected_frame():
                target = lldb.debugger.GetSelectedTarget()
                process = target.GetProcess()
                thread = process.GetSelectedThread()
                return thread.GetSelectedFrame()
            
            
            def resolve_var(name):
                raw = selected_frame().FindVariable(name)
                synthetic = raw.GetSyntheticValue()
                return synthetic if synthetic.IsValid() else raw
            
            
            def describe(child):
                return {
                    "name": child.GetName(),
                    "value": child.GetValue(),
                    "summary": child.GetSummary(),
                    "num_children": child.GetNumChildren(),
                }


            def snapshot(stats):
                return {
                    key: {"count": value["count"], "total": value["total"]}
                    for key, value in stats.items()
                }


            def print_stats_delta(title, stats, before):
                rows = []
                for key, value in stats.items():
                    previous = before.get(key, {"count": 0, "total": 0.0})
                    count = value["count"] - previous["count"]
                    total = value["total"] - previous["total"]
                    if count > 0:
                        rows.append((total, count, key, value["sample"]))

                print(title)
                if not rows:
                    print("  <none>")
                    return

                for total, count, key, sample in sorted(rows, reverse=True)[:10]:
                    print(
                        f"  {key}: count={count} total={total:.6f}s"
                        f" sample={sample}"
                    )
            
            
            def bench_var(name, page_size=50, limit=300):
                eval_before = snapshot(EVAL_STATS)
                call_before = snapshot(CALL_STATS)
                value = resolve_var(name)
                print(f"== {name} ==")
                print(f"type={value.GetTypeName()} valid={value.IsValid()}")
            
                start = time.perf_counter()
                root_summary = value.GetSummary() or value.GetValue()
                print(f"root summary: {time.perf_counter() - start:.6f}s -> {root_summary}")
            
                start = time.perf_counter()
                num_children = value.GetNumChildren()
                print(f"num children: {time.perf_counter() - start:.6f}s -> {num_children}")
            
                limit = min(limit, num_children)
                start_total = time.perf_counter()
                for batch_start in range(0, limit, page_size):
                    batch_end = min(batch_start + page_size, limit)
                    batch_start_time = time.perf_counter()
                    first_child = None
                    for index in range(batch_start, batch_end):
                        child = value.GetChildAtIndex(index)
                        if index == batch_start:
                            first_child = describe(child)
                        _ = child.MightHaveChildren()
                        _ = child.GetValue()
                        _ = child.GetSummary()
                    batch_duration = time.perf_counter() - batch_start_time
                    print(
                        f"batch {batch_start:03d}-{batch_end - 1:03d}: "
                        f"{batch_duration:.6f}s first={first_child}"
                    )
                print(f"total child traversal: {time.perf_counter() - start_total:.6f}s")
                print_stats_delta("eval hotspots:", EVAL_STATS, eval_before)
                print_stats_delta("provider hotspots:", CALL_STATS, call_before)
            
            
            bench_var("ints")
            bench_var("points")
        """.trimIndent()
    }
}
