/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import groovy.lang.Closure
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.tasks.AbstractExecTask
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.plugin.AbstractKotlinTargetConfigurator
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import java.io.File

// TODO: Extract API.

// TODO: Should the baseName be a var?

/**
 * A base class representing a final binary produced by the Kotlin/Native compiler
 * @param name - a name of the DSL entity.
 * @param baseName - a base name for the output binary file. E.g. for baseName foo we produce binaries foo.kexe, libfoo.so, foo.framework.
 * @param compilation - a compilation used to produce this binary.
 *
 */
sealed class NativeBinary(
    private val name: String,
    val baseName: String,
    val buildType: NativeBuildType,
    var compilation: KotlinNativeCompilation
) : Named {

    val target: KotlinNativeTarget
        get() = compilation.target

    val project: Project
        get() = target.project

    abstract val outputKind: NativeOutputKind

    // Configuration DSL.
    var debuggable: Boolean = false
    var optimized: Boolean = false

    var linkerOpts: MutableList<String> = mutableListOf()

    fun linkerOpts(vararg options: String) {
        linkerOpts.addAll(options.toList())
    }

    fun linkerOpts(options: Iterable<String>) {
        linkerOpts.addAll(options)
    }

    // Link task access.
    val linkTaskName: String
        get() = lowerCamelCaseName("link", name, target.targetName)

    val linkTask: KotlinNativeLink
        get() = project.tasks.getByName(linkTaskName) as KotlinNativeLink

    // Output access.
    // TODO: Provide output configurations and integrate them with Gradle Native.
    val outputDirectory: File = with(project) {
        val targetSubDirectory = target.disambiguationClassifier?.let { "$it/" }.orEmpty()
        buildDir.resolve("bin/$targetSubDirectory${this@NativeBinary.name}")
    }

    val outputFile: File
        get() = linkTask.outputFile.get()

    // Named implementation.
    override fun getName(): String = name
}

class Executable constructor(
    name: String,
    baseName: String,
    buildType: NativeBuildType,
    compilation: KotlinNativeCompilation,
    internal val isDefaultTestExecutable: Boolean
) : NativeBinary(name, baseName, buildType, compilation) {

    constructor(
        name: String,
        baseName: String,
        buildType: NativeBuildType,
        compilation: KotlinNativeCompilation) : this(name, baseName, buildType, compilation, false)

    override val outputKind: NativeOutputKind
        get() = NativeOutputKind.EXECUTABLE

    var entryPoint: String? = null

    fun entryPoint(point: String?) {
        entryPoint = point
    }

    val runTaskName: String
        get() = if (isDefaultTestExecutable) {
            lowerCamelCaseName(compilation.target.targetName, AbstractKotlinTargetConfigurator.testTaskNameSuffix)
        } else {
            lowerCamelCaseName("run", name, compilation.target.targetName)
        }

    // TODO: may make it lateinit (along with linkTasks)?
    val runTask: AbstractExecTask<*>
        get() = project.tasks.getByName(runTaskName) as AbstractExecTask<*>

    fun runTask(configure: AbstractExecTask<*>.() -> Unit) {
        runTask.configure()
    }

    fun runTask(configure: Closure<*>) {
        ConfigureUtil.configure(configure, runTask)
    }
}

class StaticLibrary(
    name: String,
    baseName: String,
    buildType: NativeBuildType,
    compilation: KotlinNativeCompilation
) : NativeBinary(name, baseName, buildType, compilation) {
    override val outputKind: NativeOutputKind
        get() = NativeOutputKind.STATIC
}

class SharedLibrary(
    name: String,
    baseName: String,
    buildType: NativeBuildType,
    compilation: KotlinNativeCompilation
) : NativeBinary(name, baseName, buildType, compilation) {
    override val outputKind: NativeOutputKind
        get() = NativeOutputKind.DYNAMIC
}

class Framework(
    name: String,
    baseName: String,
    buildType: NativeBuildType,
    compilation: KotlinNativeCompilation
) : NativeBinary(name, baseName, buildType, compilation) {

    override val outputKind: NativeOutputKind
        get() = NativeOutputKind.FRAMEWORK

    // TODO: Pack task configuration.
}


