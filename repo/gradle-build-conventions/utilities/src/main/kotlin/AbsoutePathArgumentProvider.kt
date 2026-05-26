import org.gradle.api.file.Directory
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.testing.Test
import org.gradle.process.CommandLineArgumentProvider
import java.io.File
import javax.inject.Inject

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

abstract class AbsolutePathArgumentProvider : CommandLineArgumentProvider {
    @get:Inject
    protected abstract val layout: ProjectLayout

    @get:Input
    abstract val property: Property<String>

    @get:Internal
    abstract val path: Property<String>

    @Input
    fun getRelativePath(): Provider<String> = path.map { File(it).relativeTo(layout.projectDirectory.asFile).path }

    @Internal
    fun getAbsolutePath(): String = File(path.get()).absolutePath

    override fun asArguments(): Iterable<String> {
        return listOf(
            "-D${property.get()}=${this.getAbsolutePath()}"
        )
    }
}

fun Test.addAbsoluteFileProperty(file: Provider<RegularFile>, property: String) {
    val provider = project.objects.newInstance(AbsolutePathArgumentProvider::class.java)
    provider.path.set(file.map { it.asFile.absolutePath })
    provider.property.set(property)
    jvmArgumentProviders.add(provider)
}

fun Test.addAbsoluteDirectoryProperty(directory: Provider<Directory>, property: String) {
    val provider = project.objects.newInstance(AbsolutePathArgumentProvider::class.java)
    provider.path.set(directory.map { it.asFile.absolutePath })
    provider.property.set(property)
    jvmArgumentProviders.add(provider)
}

fun Test.addAbsoluteDirectoryProperty(directory: Directory, property: String) {
    val provider = project.objects.newInstance(AbsolutePathArgumentProvider::class.java)
    provider.path.set(directory.asFile.absolutePath)
    provider.property.set(property)
    jvmArgumentProviders.add(provider)
}
