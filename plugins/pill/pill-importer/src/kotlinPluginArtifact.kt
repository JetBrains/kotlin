/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.pill

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.kotlin.dsl.extra
import org.jetbrains.kotlin.pill.ArtifactElement.*
import java.io.File

class PArtifact(val artifactName: String, private val outputDir: File, private val contents: Root) {
    fun render(context: PathContext) = xml("component", "name" to "ArtifactManager") {
        xml("artifact", "name" to artifactName) {
            xml("output-path") {
                raw(context(outputDir))
            }

            add(contents.renderRecursively(context))
        }
    }
}

interface ArtifactDependencyMapper {
    fun map(dependency: PDependency): List<PDependency>
}

sealed class ArtifactElement {
    private val myChildren = mutableListOf<ArtifactElement>()
    private val children get() = myChildren

    fun add(child: ArtifactElement) {
        myChildren += child
    }

    fun add(children: List<ArtifactElement>) {
        myChildren += children
    }

    abstract fun render(context: PathContext): XmlNode

    fun renderRecursively(context: PathContext): XmlNode {
        return render(context).apply {
            children.forEach { add(it.renderRecursively(context)) }
        }
    }

    class Root : ArtifactElement() {
        override fun render(context: PathContext) = xml("root", "id" to "root")
    }

    data class Directory(val name: String) : ArtifactElement() {
        override fun render(context: PathContext) = xml("element", "id" to "directory", "name" to name)
    }

    data class Archive(val name: String) : ArtifactElement() {
        override fun render(context: PathContext) = xml("element", "id" to "archive", "name" to name)
    }

    data class ModuleOutput(val moduleName: String) : ArtifactElement() {
        override fun render(context: PathContext) = xml("element", "id" to "module-output", "name" to moduleName)
    }

    data class FileCopy(val source: File, val outputFileName: String? = null) : ArtifactElement() {
        override fun render(context: PathContext): XmlNode {
            val args = mutableListOf("id" to "file-copy", "path" to context(source))
            if (outputFileName != null) {
                args += "output-file-name" to outputFileName
            }

            return xml("element", *args.toTypedArray())
        }
    }

    data class DirectoryCopy(val source: File) : ArtifactElement() {
        override fun render(context: PathContext) = xml("element", "id" to "dir-copy", "path" to context(source))
    }

    data class ProjectLibrary(val name: String) : ArtifactElement() {
        override fun render(context: PathContext) = xml("element", "id" to "library", "level" to "project", "name" to name)
    }

    data class ExtractedDirectory(val archive: File, val pathInJar: String = "/") : ArtifactElement() {
        override fun render(context: PathContext) =
            xml("element", "id" to "extracted-dir", "path" to context(archive), "path-in-jar" to pathInJar)
    }
}

fun generateKotlinPluginArtifactFile(rootProject: Project, dependencyMapper: ArtifactDependencyMapper): PFile {
    val root = Root()

    fun Project.getProject(name: String) = findProject(name) ?: error("Cannot find project $name")

    val prepareIdeaPluginProject = rootProject.getProject(":prepare:idea-plugin")

    root.add(Directory("kotlinc").apply {
        val kotlincDirectory = rootProject.extra["distKotlinHomeDir"].toString()
        add(DirectoryCopy(File(kotlincDirectory)))
    })

    root.add(Directory("lib").apply {
        val librariesConfiguration = prepareIdeaPluginProject.configurations.getByName("libraries")
        add(getArtifactElements(librariesConfiguration, dependencyMapper, false))

        add(Directory("jps").apply {
            val prepareJpsPluginProject = rootProject.getProject(":kotlin-jps-plugin")
            add(Archive(prepareJpsPluginProject.name + ".jar").apply {
                val jpsPluginConfiguration = prepareJpsPluginProject.configurations.getByName(EMBEDDED_CONFIGURATION_NAME)
                add(getArtifactElements(jpsPluginConfiguration, dependencyMapper, true))
            })
        })

        add(Archive("kotlin-plugin.jar").apply {
            add(FileCopy(File(rootProject.projectDir, "resources/kotlinManifest.properties")))

            val embeddedConfiguration = prepareIdeaPluginProject.configurations.getByName(EMBEDDED_CONFIGURATION_NAME)
            add(getArtifactElements(embeddedConfiguration, dependencyMapper, true))
        })
    })

    val artifact = PArtifact("KotlinPlugin", File(rootProject.projectDir, "out/artifacts/Kotlin"), root)
    return PFile(
        File(rootProject.projectDir, ".idea/artifacts/${artifact.artifactName}.xml"),
        artifact.render(ProjectContext(rootProject))
    )
}

private fun getArtifactElements(
    configuration: Configuration,
    dependencyMapper: ArtifactDependencyMapper,
    extractDependencies: Boolean
): List<ArtifactElement> {
    val artifacts = mutableListOf<ArtifactElement>()

    fun process(dependency: PDependency) {
        when (dependency) {
            is PDependency.Module -> {
                val moduleOutput = ModuleOutput(dependency.name)

                if (extractDependencies) {
                    artifacts += moduleOutput
                } else {
                    artifacts += Archive(dependency.name + ".jar").apply {
                        add(moduleOutput)
                    }
                }
            }
            is PDependency.Library -> artifacts += ProjectLibrary(dependency.name)
            is PDependency.ModuleLibrary -> {
                val files = dependency.library.classes
                if (extractDependencies) {
                    files.mapTo(artifacts) { ExtractedDirectory(it) }
                } else {
                    files.mapTo(artifacts) { FileCopy(it) }
                }
            }
        }
    }

    parseDependencies(configuration, dependencyMapper).forEach(::process)
    return artifacts
}

private fun parseDependencies(configuration: Configuration, dependencyMapper: ArtifactDependencyMapper): List<PDependency> {
    val dependencies = mutableListOf<PDependency>()
    for (file in configuration.resolve()) {
        val library = PLibrary(file.name, listOf(file))
        dependencies += dependencyMapper.map(PDependency.ModuleLibrary(library))
    }
    return dependencies
}