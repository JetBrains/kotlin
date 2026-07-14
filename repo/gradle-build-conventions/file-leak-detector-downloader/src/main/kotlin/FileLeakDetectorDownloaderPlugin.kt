/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvableConfiguration
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.attributes.Usage
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.newInstance
import org.gradle.process.CommandLineArgumentProvider
import javax.inject.Inject

abstract class FileLeakDetectorDownloaderPlugin
@Inject
internal constructor(
    private val objects: ObjectFactory,
) : Plugin<Project> {

    override fun apply(project: Project) {
        val fileLeakDetectorVersion = getFldVersionFromVersionCatalog(project)
        val resolver = createConfigurations(project, fileLeakDetectorVersion)
        createExtension(project, resolver)
    }

    private fun getFldVersionFromVersionCatalog(project: Project): String {
        val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
        return libs.findVersion("fileLeakDetector").get().requiredVersion
    }

    private fun createConfigurations(
        project: Project,
        fileLeakDetectorVersion: String,
    ): NamedDomainObjectProvider<ResolvableConfiguration> {
        val dependencyScope = project.configurations.dependencyScope("fileLeakDetector") { c ->
            c.description = "Declare a dependency on file-leak-detector fat-jar."
            c.defaultDependencies { dependencies ->
                dependencies.add(project.dependencies.create("io.jenkins.tools:file-leak-detector:$fileLeakDetectorVersion:jar-with-dependencies"))
            }
        }

        return project.configurations.resolvable(dependencyScope.name + "Resolver") { c ->
            c.description = "Resolve ${dependencyScope.name}."
            c.attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
            c.extendsFrom(dependencyScope)
            c.isTransitive = false
        }
    }

    private fun createExtension(project: Project, resolver: NamedDomainObjectProvider<ResolvableConfiguration>) {
        val flddExt = project.extensions.create("fileLeakDetectorDownloader", FileLeakDetectorDownloaderExtension::class)
        flddExt.fileLeakDetectorJar.from(resolver)
    }
}

abstract class FileLeakDetectorDownloaderExtension
@Inject
internal constructor(
    objects: ObjectFactory,
) {
    /**
     * Contains the file leak detector JAR from the 'resolver' [ResolvableConfiguration].
     */
    internal abstract val fileLeakDetectorJar: ConfigurableFileCollection

    /**
     * Adds the file-leak-detector (jar-with-dependencies) as a System Property.
     */
    val argProvider: CommandLineArgumentProvider =
        objects.newInstance<FileLeakDetectorJarArgProvider>().also { args ->
            args.fileLeakDetectorJar.from(this@FileLeakDetectorDownloaderExtension.fileLeakDetectorJar)
        }

}


internal abstract class FileLeakDetectorJarArgProvider : CommandLineArgumentProvider {

    /**
     * Name of the system property.
     */
    @get:Input
    val propertyName: String = "fileLeakDetectorJar"

    @get:Classpath
    abstract val fileLeakDetectorJar: ConfigurableFileCollection

    override fun asArguments(): Iterable<String> =
        listOf(
            "-D${propertyName}=${fileLeakDetectorJar.singleFile}",
        )
}
