/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.plugin.experimental.internal.cinterop

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.gradle.language.ComponentDependencies
import org.gradle.language.cpp.CppBinary
import org.gradle.language.internal.DefaultComponentDependencies
import org.gradle.nativeplatform.OperatingSystemFamily
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.experimental.CInteropSettings
import org.jetbrains.kotlin.gradle.plugin.experimental.CInteropSettings.IncludeDirectories
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeBinary
import org.jetbrains.kotlin.gradle.plugin.experimental.internal.KotlinNativeBuildType
import org.jetbrains.kotlin.gradle.plugin.experimental.internal.getGradleOSFamily
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import javax.inject.Inject

open class CInteropSettingsImpl @Inject constructor(
    private val project: Project,
    val baseName: String,
    val konanTarget: KonanTarget
) : CInteropSettings,
    Named
{
    inner class IncludeDirectoriesSpecImpl: IncludeDirectories {
        var allHeadersDirs: FileCollection   = project.files()
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

    override fun getName(): String = "$baseName${konanTarget.name.capitalize()}"

    var defFile: File = project.projectDir.resolve("src/main/c_interop/$baseName.def")
    var packageName: String? = null

    val compilerOpts = mutableListOf<String>()
    val linkerOpts   = mutableListOf<String>()
    val extraOpts    = mutableListOf<String>()

    val includeDirs = IncludeDirectoriesSpecImpl()
    var headers: FileCollection = project.files()

    // DSL methods.

    override val dependencies = DefaultComponentDependencies(
        project.configurations,
        name + "InteropImplementation"
    ).apply {
        with(implementationDependencies) {
            val objects = project.objects
            isCanBeConsumed = false
            isCanBeResolved = true
            attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, KotlinUsages.KOTLIN_API))
            attributes.attribute(CppBinary.DEBUGGABLE_ATTRIBUTE, KotlinNativeBuildType.DEBUG.debuggable)
            attributes.attribute(CppBinary.OPTIMIZED_ATTRIBUTE, KotlinNativeBuildType.DEBUG.optimized)
            attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
            attributes.attribute(KotlinNativeBinary.KONAN_TARGET_ATTRIBUTE, konanTarget.name)
            attributes.attribute(KotlinNativeBinary.OLD_KONAN_TARGET_ATTRIBUTE, konanTarget.name)
            attributes.attribute(
                OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE,
                konanTarget.getGradleOSFamily(objects)
            )
        }
    }

    override fun defFile(file: Any) {
        defFile = project.file(file)
    }

    override fun packageName(value: String) {
        packageName = value
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
        extraOpts.addAll(values.map { it.toString() })
    }

    override fun dependencies(action: ComponentDependencies.() -> Unit) {
        dependencies.action()
    }
    override fun dependencies(action: Closure<Unit>) =
        dependencies(ConfigureUtil.configureUsing(action))
    override fun dependencies(action: Action<ComponentDependencies>) {
        action.execute(dependencies)
    }
}