/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.testing.Test
import org.gradle.process.CommandLineArgumentProvider
import java.io.File

abstract class SystemPropertyClasspathProvider : CommandLineArgumentProvider {
    @get:InputFiles
    @get:Classpath
    abstract val classpath: ConfigurableFileCollection

    @get:Input
    abstract val property: Property<String>

    override fun asArguments(): Iterable<String> {
        return listOf(
            "-D${property.get()}=${classpath.asPath}"
        )
    }
}

abstract class SystemPropertyFileProvider : CommandLineArgumentProvider {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val file: RegularFileProperty

    @get:Input
    abstract val property: Property<String>

    override fun asArguments(): Iterable<String> {
        return listOf(
            "-D${property.get()}=${file.get().asFile.absolutePath}"
        )
    }
}

fun Test.addClasspathProperty(classpath: Provider<out FileCollection>, property: String) {
    val classpathProvider = project.objects.newInstance(SystemPropertyClasspathProvider::class.java)
    classpathProvider.classpath.from(classpath)
    classpathProvider.property.set(property)
    jvmArgumentProviders.add(classpathProvider)
}

fun Test.addClasspathProperty(property: String, configureClasspath: ConfigurableFileCollection.() -> Unit) {
    val classpathProvider = project.objects.newInstance(SystemPropertyClasspathProvider::class.java)
    classpathProvider.classpath.configureClasspath()
    classpathProvider.property.set(property)
    jvmArgumentProviders.add(classpathProvider)
}

fun Test.addClasspathProperty(classpath: FileCollection, property: String) {
    val classpathProvider = project.objects.newInstance(SystemPropertyClasspathProvider::class.java)
    classpathProvider.classpath.from(classpath)
    classpathProvider.property.set(property)
    jvmArgumentProviders.add(classpathProvider)
}

fun Test.addFileProperty(file: RegularFile, property: String) {
    val fileProvider = project.objects.newInstance(SystemPropertyFileProvider::class.java)
    fileProvider.file.set(file)
    fileProvider.property.set(property)
    jvmArgumentProviders.add(fileProvider)
}

fun Test.addFileProperty(file: File, property: String) {
    val fileProvider = project.objects.newInstance(SystemPropertyFileProvider::class.java)
    fileProvider.file.set(file)
    fileProvider.property.set(property)
    jvmArgumentProviders.add(fileProvider)
}

abstract class SystemPropertyDirectoryProvider : CommandLineArgumentProvider {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val directory: DirectoryProperty

    @get:Input
    abstract val property: Property<String>

    override fun asArguments(): Iterable<String> =
        listOf("-D${property.get()}=${directory.get().asFile.absolutePath}")
}

fun Test.addDirectoryProperty(directory: File, property: String) {
    addDirectoryProperty(property = property) { set(directory) }
}

fun Test.addDirectoryProperty(directory: Provider<Directory>, property: String) {
    addDirectoryProperty(property = property) { set(directory) }
}

fun Test.addDirectoryProperty(property: String, directoryProperty: DirectoryProperty.() -> Unit) {
    val provider = project.objects.newInstance(SystemPropertyDirectoryProvider::class.java)
    provider.directory.directoryProperty()
    provider.property.set(property)
    jvmArgumentProviders.add(provider)
}
