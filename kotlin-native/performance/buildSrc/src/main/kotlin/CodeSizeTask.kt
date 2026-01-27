package org.jetbrains.kotlin

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.jetbrains.report.BenchmarkResult
import java.io.File
import javax.inject.Inject

private fun getCodeSizeBenchmark(name: String, file: File): String {
    val codeSize = if (file.exists()) file.length().toDouble() else null
    return BenchmarkResult(
            name,
            if (codeSize != null) BenchmarkResult.Status.PASSED else BenchmarkResult.Status.FAILED,
            codeSize ?: 0.0,
            BenchmarkResult.Metric.CODE_SIZE,
            codeSize ?: 0.0,
            1,
            0
    ).toJson()
}

open class CodeSizeTask @Inject constructor(
        objectFactory: ObjectFactory,
) : DefaultTask() {
    /**
     * Name to use for the code size metric
     */
    @get:Input
    val name: Property<String> = objectFactory.property(String::class.java)

    /**
     * Size of which binary to measure for the code size metric
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE) // only the contents of the binary matters
    @get:Optional
    val codeSizeBinary: RegularFileProperty = objectFactory.fileProperty()

    /**
     * Where to put the code size report
     */
    @get:OutputFile
    val reportFile: RegularFileProperty = objectFactory.fileProperty()

    @TaskAction
    fun run() {
        val output = getCodeSizeBenchmark(name.get(), codeSizeBinary.get().asFile)
        reportFile.get().asFile.writeText("[$output]")
    }
}