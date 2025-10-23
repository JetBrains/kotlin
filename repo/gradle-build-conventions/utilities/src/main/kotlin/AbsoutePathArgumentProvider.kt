import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.process.CommandLineArgumentProvider
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
    abstract val buildDirectory: DirectoryProperty

    @Input
    fun getRelativePath(): Provider<String> = buildDirectory.map { it.asFile.relativeTo(layout.projectDirectory.asFile).path }

    @Internal
    fun getAbsolutePath(): String = buildDirectory.get().asFile.absolutePath

    override fun asArguments(): Iterable<String> {
        return listOf(
            "-D${property.get()}=${this.getAbsolutePath()}"
        )
    }
}
