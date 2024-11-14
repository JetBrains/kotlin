import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import javax.inject.Inject

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

abstract class PluginsApiDocumentationExtension @Inject constructor(
    objectFactory: ObjectFactory,
    private val childProjectConfiguration: (Project) -> Unit
) {
    abstract val documentationOutput: DirectoryProperty
    abstract val documentationOldVersions: DirectoryProperty
    abstract val templatesArchiveUrl: Property<String>
    val templatesArchiveSubDirectoryPattern: Property<String> = objectFactory.property(String::class.java).convention("")
    val templatesArchivePrefixToRemove: Property<String> = objectFactory.property(String::class.java).convention("")
    internal abstract val gradlePluginsProjects: SetProperty<Project>

    fun addGradlePluginProject(project: Project) {
        gradlePluginsProjects.add(project)
        childProjectConfiguration(project)
    }
}