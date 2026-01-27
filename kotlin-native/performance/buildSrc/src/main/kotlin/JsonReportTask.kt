package org.jetbrains.kotlin

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.jetbrains.report.*
import org.jetbrains.report.json.JsonTreeParser
import java.io.File
import javax.inject.Inject

private fun getFileSize(filePath: String): Long? {
    val file = File(filePath)
    return if (file.exists()) file.length() else null
}

private fun getCodeSizeBenchmark(programName: String, filePath: String): BenchmarkResult {
    val codeSize = getFileSize(filePath)
    return BenchmarkResult(programName, codeSize?.let { BenchmarkResult.Status.PASSED }
            ?: run { BenchmarkResult.Status.FAILED }, codeSize?.toDouble()
            ?: 0.0, BenchmarkResult.Metric.CODE_SIZE, codeSize?.toDouble() ?: 0.0, 1, 0)
}

// Create benchmarks json report based on information get from gradle project
private fun createJsonReport(projectProperties: Map<String, Any>): String {
    fun getValue(key: String): String = projectProperties[key] as? String ?: "unknown"
    val machine = Environment.Machine(getValue("cpu"), getValue("os"))
    val jdk = Environment.JDKInstance(getValue("jdkVersion"), getValue("jdkVendor"))
    val env = Environment(machine, jdk)
    val flags: List<String> = (projectProperties["flags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
    val backend = Compiler.Backend(Compiler.backendTypeFromString(getValue("type"))!!, getValue("compilerVersion"), flags)
    val kotlin = Compiler(backend, getValue("kotlinVersion"))
    val benchDesc = getValue("benchmarks")
    val benchmarksArray = JsonTreeParser.parse(benchDesc)
    val benchmarks = parseBenchmarksArray(benchmarksArray).union(listOf(projectProperties["codeSize"] as? BenchmarkResult).filterNotNull()).toList()
    val report = BenchmarksReport(env, benchmarks, kotlin)
    return report.toJson()
}

/**
 * Convert the json from the benchmark run into a full benchmark report.
 *
 * Adds cpu, os, java versions, compiler version and flags and adds a code size metric.
 */
open class JsonReportTask @Inject constructor(
        objectFactory: ObjectFactory,
) : DefaultTask() {
    /**
     * Name to use for the code size metric
     */
    @get:Input
    val codeSizeName: Property<String> = objectFactory.property(String::class.java)

    /**
     * Size of which binary to measure for the code size metric
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE) // only the contents of the binary matters
    val codeSizeBinary: RegularFileProperty = objectFactory.fileProperty()

    /**
     * json with the report from the benchmark executable
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE) // only the contents of the benchmarks report matter
    val benchmarksReportFile: RegularFileProperty = objectFactory.fileProperty()

    /**
     * Compiler version to record in the report
     */
    @get:Input
    val compilerVersion: Property<String> = objectFactory.property(String::class.java)

    /**
     * Compiler flags to record in the report
     */
    @get:Input
    val compilerFlags: ListProperty<String> = objectFactory.listProperty(String::class.java)

    /**
     * Where to place the benchmarks report
     */
    @get:OutputFile
    val reportFile: RegularFileProperty = objectFactory.fileProperty()

    @get:Input
    protected val systemProperties: Map<String, Any> = mapOf(
            "cpu" to System.getProperty("os.arch"),
            "os" to System.getProperty("os.name"),
            "jdkVersion" to System.getProperty("java.version"),
            "jdkVendor" to System.getProperty("java.vendor"),
    )

    @TaskAction
    fun run() {
        val properties = buildMap<String, Any> {
            putAll(systemProperties)
            put("kotlinVersion", compilerVersion.get())
            put("compilerVersion", compilerVersion.get())
            put("type", "native")
            put("flags", compilerFlags.get())
            put("benchmarks", benchmarksReportFile.asFile.orNull?.readText() ?: "[]")
            put("codeSize", getCodeSizeBenchmark(codeSizeName.get(), codeSizeBinary.get().asFile.absolutePath))
        }
        val output = createJsonReport(properties)
        reportFile.get().asFile.writeText(output)
    }
}