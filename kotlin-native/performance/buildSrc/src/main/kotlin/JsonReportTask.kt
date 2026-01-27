package org.jetbrains.kotlin

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.jetbrains.report.*
import org.jetbrains.report.json.JsonTreeParser
import java.io.File
import javax.inject.Inject

// Create benchmarks json report based on information get from gradle project
private fun createJsonReport(
        systemProperties: Map<String, String>,
        compilerVersion: String,
        compilerFlags: List<String>,
        benchmarkFiles: List<File>
): String {
    fun getValue(key: String): String = systemProperties[key] ?: "unknown"

    return BenchmarksReport(
            Environment(
                    Environment.Machine(getValue("cpu"), getValue("os")),
                    Environment.JDKInstance(getValue("jdkVersion"), getValue("jdkVendor")),
            ),
            benchmarkFiles.flatMap {
                parseBenchmarksArray(JsonTreeParser.parse(it.readText())).toList()
            },
            Compiler(Compiler.Backend(Compiler.BackendType.NATIVE, compilerVersion, compilerFlags), compilerVersion)
    ).toJson()
}

/**
 * Convert the json from the benchmark run into a full benchmark report.
 *
 * Adds cpu, os, java versions, compiler version and flags.
 */
open class JsonReportTask @Inject constructor(
        objectFactory: ObjectFactory,
) : DefaultTask() {
    /**
     * jsons with the reports from the benchmark executable
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE) // only the contents of the benchmarks report matter
    val benchmarksReports: ConfigurableFileCollection = objectFactory.fileCollection()

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
    protected val systemProperties: Map<String, String> = mapOf(
            "cpu" to System.getProperty("os.arch"),
            "os" to System.getProperty("os.name"),
            "jdkVersion" to System.getProperty("java.version"),
            "jdkVendor" to System.getProperty("java.vendor"),
    )

    @TaskAction
    fun run() {
        val output = createJsonReport(
                systemProperties,
                compilerVersion.get(),
                compilerFlags.get(),
                benchmarksReports.files.toList()
        )
        reportFile.get().asFile.writeText(output)
    }
}