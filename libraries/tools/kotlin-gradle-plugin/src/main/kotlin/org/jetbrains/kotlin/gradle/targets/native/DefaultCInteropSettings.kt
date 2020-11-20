/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.plugin.CInteropSettings
import org.jetbrains.kotlin.gradle.plugin.CInteropSettings.IncludeDirectories
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import java.io.File
import javax.inject.Inject

open class DefaultCInteropSettings @Inject constructor(
    private val project: Project,
    private val name: String,
    override val compilation: KotlinNativeCompilation
) : CInteropSettings {

    inner class DefaultIncludeDirectories : CInteropSettings.IncludeDirectories {
        var allHeadersDirs: FileCollection = project.files()
        var headerFilterDirs: FileCollection = project.files()

        override fun allHeaders(vararg includeDirs: Any) = allHeaders(includeDirs.toList())
        override fun allHeaders(includeDirs: Collection<Any>) {
            allHeadersDirs += project.files(*includeDirs.toTypedArray())
        }

        override fun headerFilterOnly(vararg includeDirs: Any) = headerFilterOnly(includeDirs.toList())
        override fun headerFilterOnly(includeDirs: Collection<Any>) {
            headerFilterDirs += project.files(*includeDirs.toTypedArray())
        }
    }

    override fun getName(): String = name

    val target: KotlinNativeTarget
        get() = compilation.target

    override val dependencyConfigurationName: String
        get() = compilation.disambiguateName("${name.capitalize()}CInterop")

    override var dependencyFiles: FileCollection = project.files()

    val interopProcessingTaskName: String
        get() = lowerCamelCaseName(
            "cinterop",
            compilation.compilationName.takeIf { it != "main" }.orEmpty(),
            name,
            target.disambiguationClassifier
        )

    val defFileProperty: Property<File> = project.objects.property(File::class.java)
        .apply { set(project.projectDir.resolve("src/nativeInterop/cinterop/$name.def")) }

    var defFile: File
        get() = defFileProperty.get()
        set(value) {
            defFileProperty.set(value)
        }

    var packageName: String?
        get() = _packageNameProp.orNull
        set(value) {
            _packageNameProp.set(value)
        }

    internal val _packageNameProp: Property<String> = project.objects.property(String::class.java)

    val compilerOpts = mutableListOf<String>()
    val linkerOpts = mutableListOf<String>()
    var extraOpts: List<String>
        get() = _extraOptsProp.get()
        set(value) {
            _extraOptsProp = project.objects.listProperty(String::class.java)
            extraOpts(value)
        }

    internal var _extraOptsProp: ListProperty<String> = project.objects.listProperty(String::class.java)

    val includeDirs = DefaultIncludeDirectories()
    var headers: FileCollection = project.files()

    // DSL methods.

    override fun defFile(file: Any) {
        defFileProperty.set(project.file(file))
    }

    override fun packageName(value: String) {
        _packageNameProp.set(value)
    }

    override fun header(file: Any) = headers(file)
    override fun headers(vararg files: Any) = headers(project.files(files))
    override fun headers(files: FileCollection) {
        headers += files
    }

    override fun includeDirs(vararg values: Any) = includeDirs.allHeaders(values.toList())
    override fun includeDirs(closure: Closure<Unit>) = includeDirs(ConfigureUtil.configureUsing(closure))
    override fun includeDirs(action: Action<IncludeDirectories>) = includeDirs { action.execute(this) }
    override fun includeDirs(configure: IncludeDirectories.() -> Unit) = includeDirs.configure()

    override fun compilerOpts(vararg values: String) = compilerOpts(values.toList())
    override fun compilerOpts(values: List<String>) {
        compilerOpts.addAll(values)
    }

    override fun linkerOpts(vararg values: String) = linkerOpts(values.toList())
    override fun linkerOpts(values: List<String>) {
        linkerOpts.addAll(values)
    }

    override fun extraOpts(vararg values: Any) = extraOpts(values.toList())
    override fun extraOpts(values: List<Any>) {
        _extraOptsProp.addAll(project.provider { values.map { it.toString() } })
    }
}
