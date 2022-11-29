/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.plugin.CInteropSettings
import org.jetbrains.kotlin.gradle.plugin.CInteropSettings.IncludeDirectories
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmNativeVariantCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.disambiguateName
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropIdentifier
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.gradle.utils.property
import java.io.File
import javax.inject.Inject

abstract class DefaultCInteropSettings @Inject internal constructor(
    private val params: Params
) : CInteropSettings {

    internal data class Params(
        val name: String,
        val identifier: CInteropIdentifier,
        val dependencyConfigurationName: String,
        val interopProcessingTaskName: String,
        val services: Services
    ) {
        open class Services @Inject constructor(
            val providerFactory: ProviderFactory,
            val objectFactory: ObjectFactory,
            val projectLayout: ProjectLayout,
            val fileOperations: FileOperations,
        )
    }

    private fun files() = params.services.objectFactory.fileCollection()
    private fun files(vararg paths: Any) = params.services.objectFactory.fileCollection().from(*paths)

    inner class DefaultIncludeDirectories : IncludeDirectories {
        var allHeadersDirs: FileCollection = files()
        var headerFilterDirs: FileCollection = files()

        override fun allHeaders(vararg includeDirs: Any) = allHeaders(includeDirs.toList())
        override fun allHeaders(includeDirs: Collection<Any>) {
            allHeadersDirs += files(*includeDirs.toTypedArray())
        }

        override fun headerFilterOnly(vararg includeDirs: Any) = headerFilterOnly(includeDirs.toList())
        override fun headerFilterOnly(includeDirs: Collection<Any>) {
            headerFilterDirs += files(*includeDirs.toTypedArray())
        }
    }

    override fun getName(): String = params.name

    internal val identifier = params.identifier

    override val dependencyConfigurationName: String
        get() = params.dependencyConfigurationName

    override var dependencyFiles: FileCollection = files()

    val interopProcessingTaskName get() = params.interopProcessingTaskName

    val defFileProperty: Property<File> = params.services.objectFactory.property<File>().value(
        params.services.projectLayout.projectDirectory.file("src/nativeInterop/cinterop/$name.def").asFile
    )

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

    internal val _packageNameProp: Property<String> = params.services.objectFactory.property(String::class.java)

    val compilerOpts = mutableListOf<String>()
    val linkerOpts = mutableListOf<String>()
    var extraOpts: List<String>
        get() = _extraOptsProp.get()
        set(value) {
            _extraOptsProp = params.services.objectFactory.listProperty(String::class.java)
            extraOpts(value)
        }

    internal var _extraOptsProp: ListProperty<String> = params.services.objectFactory.listProperty(String::class.java)

    val includeDirs = DefaultIncludeDirectories()
    var headers: FileCollection = files()

    // DSL methods.

    override fun defFile(file: Any) {
        defFileProperty.set(params.services.fileOperations.file(file))
    }

    override fun packageName(value: String) {
        _packageNameProp.set(value)
    }

    override fun header(file: Any) = headers(file)
    override fun headers(vararg files: Any) = headers(files(files))
    override fun headers(files: FileCollection) {
        headers += files
    }

    override fun includeDirs(vararg values: Any) = includeDirs.allHeaders(values.toList())
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
        _extraOptsProp.addAll(params.services.providerFactory.provider { values.map { it.toString() } })
    }
}

internal class DefaultCInteropSettingsFactory(private val compilation: KotlinCompilation<*>) :
    NamedDomainObjectFactory<DefaultCInteropSettings> {
    override fun create(name: String): DefaultCInteropSettings {
        val params = DefaultCInteropSettings.Params(
            name = name,
            identifier = CInteropIdentifier(CInteropIdentifier.Scope.create(compilation), name),
            dependencyConfigurationName = compilation.disambiguateName("${name.capitalize()}CInterop"),
            interopProcessingTaskName = lowerCamelCaseName(
                "cinterop",
                compilation.name.takeIf { it != "main" }.orEmpty(),
                name,
                compilation.target.disambiguationClassifier
            ),
            services = compilation.project.objects.newInstance()
        )

        return compilation.project.objects.newInstance(params)
    }
}

internal class GradleKpmDefaultCInteropSettingsFactory(private val compilation: GradleKpmNativeVariantCompilationData) :
    NamedDomainObjectFactory<DefaultCInteropSettings> {
    override fun create(name: String): DefaultCInteropSettings {
        val params = DefaultCInteropSettings.Params(
            name = name,
            identifier = CInteropIdentifier(CInteropIdentifier.Scope.create(compilation), name),
            dependencyConfigurationName = compilation.owner.disambiguateName("${name.capitalize()}CInterop"),
            interopProcessingTaskName = lowerCamelCaseName(
                "cinterop",
                compilation.compilationPurpose.takeIf { it != "main" }.orEmpty(),
                name,
                compilation.compilationClassifier
            ),
            services = compilation.project.objects.newInstance()
        )

        return compilation.project.objects.newInstance(params)
    }
}
