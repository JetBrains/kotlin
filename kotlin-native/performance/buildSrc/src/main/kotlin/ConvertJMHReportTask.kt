package org.jetbrains.kotlin

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

/**
 * Convert jmh-style json report into Native microbenchmarking report
 */
open class ConvertJMHReportTask @Inject constructor(
        private val execOperations: ExecOperations,
        objectFactory: ObjectFactory,
) : DefaultTask() {
    /**
     * Classpath of the `convertJMHReport` tool
     */
    @get:Classpath
    val convertJMHReportClasspath: ConfigurableFileCollection = objectFactory.fileCollection()

    /**
     * jmh report to convert
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE) // the location of the report does not matter
    val inputFile: RegularFileProperty = objectFactory.fileProperty()

    /**
     * Where to place the benchmark report file
     *
     * NOTE: this is not a complete report, [JsonReportTask] adds some additional information
     */
    @get:OutputFile
    val outputFile: RegularFileProperty = objectFactory.fileProperty()

    /**
     * Additional arguments for the `convertJMHReport` tool
     */
    @get:Input
    val arguments: ListProperty<String> = objectFactory.listProperty(String::class.java)

    @TaskAction
    fun run() {
        execOperations.javaexec {
            classpath = convertJMHReportClasspath
            mainClass.set("ConvertJMHReportCLI")
            args(inputFile.get().asFile.absolutePath)
            args("-o", outputFile.get().asFile.absolutePath)
            args(arguments.get())
        }.assertNormalExitValue()
    }
}