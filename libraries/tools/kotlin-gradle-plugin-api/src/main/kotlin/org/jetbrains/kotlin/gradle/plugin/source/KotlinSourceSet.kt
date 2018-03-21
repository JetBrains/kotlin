/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.source

import groovy.lang.Closure
import org.gradle.api.Named
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.SourceSetOutput

interface KotlinSourceSet: Named {
    val kotlin: SourceDirectorySet

    fun kotlin(configureClosure: Closure<Any?>?): KotlinSourceSet

    var compileClasspath: FileCollection

    var runtimeClasspath: FileCollection

    val output: SourceSetOutput

    val resources: SourceDirectorySet

    val allSource: SourceDirectorySet

    val classesTaskName: String

    val processResourcesTaskName: String

    val compileKotlinTaskName: String

    val jarTaskName: String

    val compileConfigurationName: String

    val runtimeConfigurationName: String

    val compileOnlyConfigurationName: String

    val runtimeOnlyConfigurationName: String

    val implementationConfigurationName: String

    val compileClasspathConfigurationName: String

    val runtimeClasspathConfigurationName: String

    fun compiledBy(vararg taskPaths: Any)

    fun getCompileTaskName(suffix: String): String
}
