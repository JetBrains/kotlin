/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package dokkabuild.utils

import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskInputFilePropertyBuilder
import org.gradle.api.tasks.TaskInputPropertyBuilder
import org.gradle.api.tasks.TaskOutputFilePropertyBuilder
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.gradle.process.CommandLineArgumentProvider
import java.io.File
import javax.inject.Inject


/**
 * Utility for adding a System Property command line arguments to this [Test] task,
 * and correctly registering the values as task inputs (for Gradle up-to-date checks).
 */
// https://github.com/gradle/gradle/issues/11534
// https://github.com/gradle/gradle/issues/12247
val Test.systemProperty: SystemPropertyAdder
    get() {
        val spa = extensions.findByType<SystemPropertyAdder>()
            ?: extensions.create<SystemPropertyAdder>("SystemPropertyAdder", this)
        return spa
    }


abstract class SystemPropertyAdder @Inject internal constructor(
    private val task: Test,
) {
    private val objects: ObjectFactory = task.project.objects

    @JvmName("inputDirectoryProvider")
    fun inputDirectory(
        key: String,
        value: Provider<out Directory>,
    ): TaskInputFilePropertyBuilder {
        task.jvmArgumentProviders.add(
            SystemPropertyArgumentProvider(key, value) {
                it.get().asFile.invariantSeparatorsPath
            }
        )
        return task.inputs.dir(value)
            .withPropertyName("SystemProperty input directory $key")
    }

    @JvmName("inputDirectoryFile")
    fun inputDirectory(
        key: String,
        value: Provider<File>,
    ): TaskInputFilePropertyBuilder =
        inputDirectory(key, objects.directoryProperty().fileProvider(value))

    fun inputDirectory(
        key: String,
        value: File,
    ): TaskInputFilePropertyBuilder =
        inputDirectory(key, objects.directoryProperty().fileValue(value))

    fun inputDirectory(
        key: String,
        value: Directory,
    ): TaskInputFilePropertyBuilder =
        inputDirectory(key, objects.directoryProperty().apply { set(value) })


    @JvmName("outputDirectoryProvider")
    fun outputDirectory(
        key: String,
        value: Provider<out Directory>,
    ): TaskOutputFilePropertyBuilder {
        task.jvmArgumentProviders.add(
            SystemPropertyArgumentProvider(key, value) {
                it.get().asFile.invariantSeparatorsPath
            }
        )
        return task.outputs.dir(value)
            .withPropertyName("SystemProperty input directory $key")
    }

    @JvmName("outputDirectoryFile")
    fun outputDirectory(
        key: String,
        value: Provider<File>,
    ): TaskOutputFilePropertyBuilder =
        outputDirectory(key, objects.directoryProperty().fileProvider(value))

    fun outputDirectory(
        key: String,
        value: File,
    ): TaskOutputFilePropertyBuilder =
        outputDirectory(key, objects.directoryProperty().fileValue(value))

    fun outputDirectory(
        key: String,
        value: Directory,
    ): TaskOutputFilePropertyBuilder =
        outputDirectory(key, objects.directoryProperty().apply { set(value) })

    fun inputFile(
        key: String,
        file: RegularFile,
    ): TaskInputFilePropertyBuilder {
        task.jvmArgumentProviders.add(
            SystemPropertyArgumentProvider(key, file) {
                it.asFile.invariantSeparatorsPath
            }
        )
        return task.inputs.file(file)
            .withPropertyName("SystemProperty input file $key")
    }

    fun inputFile(
        key: String,
        file: Provider<out RegularFile>,
    ): TaskInputFilePropertyBuilder {
        task.jvmArgumentProviders.add(
            SystemPropertyArgumentProvider(key, file) {
                it.orNull?.asFile?.invariantSeparatorsPath
            }
        )
        return task.inputs.file(file)
            .withPropertyName("SystemProperty input file $key")
    }

    fun inputFiles(
        key: String,
        files: Provider<out FileCollection>,
    ): TaskInputFilePropertyBuilder {
        task.jvmArgumentProviders.add(
            SystemPropertyArgumentProvider(key, files) { it.orNull?.asPath }
        )
        return task.inputs.files(files)
            .withPropertyName("SystemProperty input files $key")
    }

    fun inputProperty(
        key: String,
        value: Provider<out String>,
    ): TaskInputPropertyBuilder {
        task.jvmArgumentProviders.add(
            SystemPropertyArgumentProvider(key, value) { it.orNull }
        )
        return task.inputs.property("SystemProperty input property $key", value)
    }

    @JvmName("inputBooleanProperty")
    fun inputProperty(
        key: String,
        value: Provider<out Boolean>,
    ): TaskInputPropertyBuilder = inputProperty(key, value.map { it.toString() })

    /**
     * Add a System Property (in the format `-D$key=$value`).
     *
     * [value] will be treated as if it were annotated with [org.gradle.api.tasks.Internal]
     * and will _not_ be registered as a Gradle [org.gradle.api.Task] input.
     * (Which is beneficial in cases where [value] is optional, or is not reproducible,
     * because such a property might disrupt task caching.)
     *
     * If you want to register the property as a Task input that can be normalized, use one of
     * the typed inputs above.
     *
     * @see org.gradle.api.tasks.Internal
     */
    fun internalProperty(
        key: String,
        value: Provider<out String>,
    ) {
        task.jvmArgumentProviders.add(
            SystemPropertyArgumentProvider(
                key = key,
                value = value,
                transformer = { it.orNull },
            )
        )
    }
}

/**
 * Provide a Java system property.
 *
 * [value] is not registered as a Gradle Task input.
 * The value must be registered as a task input, using the [SystemPropertyAdder] utils.
 */
private class SystemPropertyArgumentProvider<T : Any>(
    @get:Input
    val key: String,
    private val value: T,
    private val transformer: (value: T) -> String?,
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> {
        val value = transformer(value) ?: return emptyList()
        return listOf("-D$key=$value")
    }
}
