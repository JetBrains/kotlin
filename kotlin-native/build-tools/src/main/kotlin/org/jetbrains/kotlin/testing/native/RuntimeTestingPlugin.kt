/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.testing.native

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.property
import org.jetbrains.kotlin.bitcode.CompileToBitcodeExtension
import java.net.URI
import javax.inject.Inject

@Suppress("UnstableApiUsage")
open class RuntimeTestingPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        val extension = extensions.create(GOOGLE_TEST_EXTENSION_NAME, GoogleTestExtension::class.java)
        val downloadTask = registerDownloadTask(extension)

        val googleTestRoot = extension.sourceDirectory

        createBitcodeTasks(project.objects.directoryProperty().value(googleTestRoot), listOf(downloadTask))
    }

    private fun Project.registerDownloadTask(extension: GoogleTestExtension): TaskProvider<GitDownloadTask> =
            tasks.register("downloadGoogleTest", GitDownloadTask::class.java) {
                description = "Retrieves GoogleTest from the given repository"
                group = "Google Test"
                onlyIf {
                    !extension.hasLocalSourceRoot.get()
                }
                repository.set(extension.repository.map { URI.create(it) })
                revision.set(extension.revision)
                outputDirectory.set(extension.sourceDirectory)
                refresh.set(extension.refresh)
            }

    private fun Project.createBitcodeTasks(
            googleTestRoot: DirectoryProperty,
            dependencies: Iterable<TaskProvider<*>>
    ) {
        pluginManager.withPlugin("compile-to-bitcode") {
            val bitcodeExtension = project.extensions.getByType<CompileToBitcodeExtension>()

            bitcodeExtension.allTargets {
                module("googletest") {
                    sourceSets {
                        testFixtures {
                            inputFiles.from(googleTestRoot.dir("googletest/src"))
                            // That's how googletest/CMakeLists.txt builds gtest library.
                            inputFiles.include("gtest-all.cc")
                            headersDirs.setFrom(
                                    googleTestRoot.dir("googletest/include"),
                                    googleTestRoot.dir("googletest")
                            )
                        }
                    }
                    compilerArgs.set(listOf("-std=c++17", "-O2"))
                    this.dependencies.addAll(dependencies)
                }

                module("googlemock") {
                    sourceSets {
                        testFixtures {
                            inputFiles.from(googleTestRoot.dir("googlemock/src"))
                            // That's how googlemock/CMakeLists.txt builds gmock library.
                            inputFiles.include("gmock-all.cc")
                            headersDirs.setFrom(
                                    googleTestRoot.dir("googlemock"),
                                    googleTestRoot.dir("googlemock/include"),
                                    googleTestRoot.dir("googletest/include"),
                            )
                        }
                    }
                    compilerArgs.set(listOf("-std=c++17", "-O2"))
                    this.dependencies.addAll(dependencies)
                }
            }
        }
    }

    companion object {
        internal const val GOOGLE_TEST_EXTENSION_NAME = "googletest"
    }

}

/**
 * A project extension to configure from where we get the GoogleTest framework.
 */
abstract class GoogleTestExtension @Inject constructor(
        layout: ProjectLayout,
        providers: ProviderFactory,
        objects: ObjectFactory,
) {

    /**
     * A repository to fetch GoogleTest from.
     */
    val repository: Property<String> = objects.property<String>().convention("https://github.com/google/googletest.git")

    private var _revision: String? = null

    /**
     * A particular revision in the [repository] to be fetched. It can be a branch, a tag or a commit hash.
     */
    var revision: String
        get() = _revision
                ?: throw InvalidUserDataException(
                        "No value provided for property '${RuntimeTestingPlugin.GOOGLE_TEST_EXTENSION_NAME}.revision'. " +
                        "Please specify it in the buildscript."
                )
        set(value) { _revision = value }

    /**
     * Fetch the [revision] even if the [fetchDirectory] already contains it. Overwrite all changes manually made in the output directory.
     */
    val refresh: Property<Boolean> = objects.property<Boolean>().convention(false)

    /**
     * A directory to fetch the [revision] to.
     */
    private val fetchDirectory: Directory = layout.projectDirectory.dir("googletest")

    /**
     * Use a local directory with GoogleTest instead of the fetched one. If set, the download task will not be executed.
     */
    abstract val localSourceRoot: DirectoryProperty
    val hasLocalSourceRoot: Provider<Boolean> = providers.provider { localSourceRoot.isPresent }

    /**
     * A getter for directory that contains the GTest sources.
     * Returns a local source directory if it's specified (see [localSourceRoot]) or [fetchDirectory] otherwise.
     */
    val sourceDirectory: Provider<Directory> = localSourceRoot.orElse(fetchDirectory)

    /**
     * A file collection with header directories for GoogleTest and GoogleMock.
     * Useful to configure compilation against GTest.
     */
    val headersDirs: FileCollection = layout.files(
            sourceDirectory.map { it.dir("googletest/include")},
            sourceDirectory.map { it.dir("googlemock/include")}
    )
}

