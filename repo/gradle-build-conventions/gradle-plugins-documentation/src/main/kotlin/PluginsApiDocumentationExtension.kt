import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.SetProperty

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

abstract class PluginsApiDocumentationExtension() {
    abstract val documentationOutput: DirectoryProperty
    abstract val documentationOldVersions: DirectoryProperty
    abstract val gradlePluginsProjects: SetProperty<Project>
}