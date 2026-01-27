package org.jetbrains.kotlin

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.jetbrains.report.BenchmarksReport
import org.jetbrains.report.json.JsonTreeParser
import javax.inject.Inject

/**
 * Merge together existing benchmark reports from [inputReports] and store the merged report in [outputReport]
 */
open class MergeNativeReportsTask @Inject constructor(
        objectFactory: ObjectFactory,
) : DefaultTask() {
    /**
     * Benchmark reports to merge together
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE) // only the content of the reports matters
    val inputReports: ConfigurableFileCollection = objectFactory.fileCollection()

    /**
     * Where to place the generated report
     */
    @get:OutputFile
    val outputReport: RegularFileProperty = objectFactory.fileProperty()

    @TaskAction
    fun run() {
        val output = inputReports.filter {
            it.exists()
        }.map {
            BenchmarksReport.create(JsonTreeParser.parse(it.readText()))
        }.groupBy {
            it.compiler.backend.flags.joinToString()
        }.values.joinToString(prefix = "[", postfix = "]") {
            it.reduce(BenchmarksReport::plus).toJson()
        }
        outputReport.get().asFile.writeText(output)
    }
}