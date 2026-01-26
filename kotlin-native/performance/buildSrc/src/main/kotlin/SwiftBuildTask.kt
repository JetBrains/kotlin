package org.jetbrains.kotlin

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.process.ExecSpec
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

/**
 * Runs `swift build --product [product]` in the project directory and places [product]
 * in [outputFile].
 */
open class SwiftBuildTask @Inject constructor(
        objectFactory: ObjectFactory,
        private val execOperations: ExecOperations,
        private val fileSystemOperations: FileSystemOperations,
) : DefaultTask() {
    init {
        outputs.upToDateWhen { false } // `swift build` is a build system, that performs up-to-date itself
    }

    /**
     * Build state for `swift build`
     */
    @get:LocalState
    val scratchPath: DirectoryProperty = objectFactory.directoryProperty()

    /**
     * Additional arguments to append to the `swift build` invocation
     */
    @get:Input
    val options: ListProperty<String> = objectFactory.listProperty(String::class.java)

    /**
     * Which product to build.
     *
     * NOTE: the product is expected to be executable, no other type was tested.
     */
    @get:Input
    val product: Property<String> = objectFactory.property(String::class.java)

    /**
     * Directory where to store the built product.
     */
    @get:Internal
    val outputDirectory: DirectoryProperty = objectFactory.directoryProperty()

    /**
     * Built product location.
     *
     * NOTE: this is not editable, use [outputDirectory] to select the alternative output directory; file name
     * always matches [product]
     */
    @get:OutputFile
    val outputFile = outputDirectory.file(product)

    private fun ExecOperations.swiftBuild(configure: Action<ExecSpec> = {}) = exec {
        executable("swift")
        args(
                "build",
                "--scratch-path", scratchPath.get().asFile.absolutePath,
                "--product", product.get(),
                *options.get().toTypedArray(),
        )
        configure.execute(this)
    }

    @TaskAction
    fun run() {
        execOperations.swiftBuild().assertNormalExitValue()
        val buildLocation = ByteArrayOutputStream().apply {
            execOperations.swiftBuild {
                standardOutput = this@apply
                args("--show-bin-path")
            }.assertNormalExitValue()
        }.toString().trim()
        fileSystemOperations.copy {
            from(File(buildLocation).resolve(product.get()))
            into(outputDirectory.get())
        }
    }
}