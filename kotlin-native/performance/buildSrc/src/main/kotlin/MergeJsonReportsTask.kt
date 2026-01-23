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
import org.jetbrains.report.*
import org.jetbrains.report.json.*
import javax.inject.Inject

open class MergeJsonReportsTask @Inject constructor(
        objectFactory: ObjectFactory,
) : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE) // only the contents of the files matters
    val reports: ConfigurableFileCollection = objectFactory.fileCollection()

    @get:OutputFile
    val outputFile: RegularFileProperty = objectFactory.fileProperty()

    @TaskAction
    fun run() {
        val jsons = reports.files.toList().map {
            BenchmarksReport.create(JsonTreeParser.parse(it.readText()))
        }.groupBy {
            it.compiler.backend.flags.joinToString()
        }.values.map {
            it.reduce { result, it -> result + it }.toJson()
        }
        outputFile.get().asFile.writeText(when (jsons.size) {
            0 -> ""
            1 -> jsons[0]
            else -> jsons.joinToString(prefix = "[", postfix = "]")
        })
    }
}