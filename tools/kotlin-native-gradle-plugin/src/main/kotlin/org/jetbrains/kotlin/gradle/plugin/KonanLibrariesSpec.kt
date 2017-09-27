package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import java.io.File


/**
 *  libraries {
        file 'sdfsdf'
        file project.files(sdfsd) // == file: Any

        klib 'posix'    // no changes
        klib config



        artifact 'myLib' // in the project
        interop 'microhttpd'

        artifact project, 'myLib'
        interop project, 'name'

        artifact config
        interop config



        allArtifactsFrom  project
        allInteropsFrom   project

        allArtifactsFrom  'project'
        allInteropsFrom   'project'

        useRepo Any (-> Project.files)
    }


val task = libConfig.task

if (config == libConfig) {
throw IllegalArgumentException("Attempt to use a library as its own dependency: " +
"${libConfig.name} (in project: ${project.path})")
}

project.evaluationDependsOn(libConfig.project.path)
config.dependsOn(task)
 *
 *
 *
 */

// TODO: Get rid of two things: Interop and artifact. Replace with one KonanArtifact.
class KonanLibrariesSpec(val project: Project) {

    @InputFiles val files = mutableSetOf<FileCollection>() // TODO: Immutable interface?

    @Input val namedKlibs = mutableSetOf<String>()

    @Nested val artifacts = mutableSetOf<KonanBuildingConfig>()

   // TODO: input?
    val repos = mutableSetOf<File>().apply {
       add(project.file(project.konanCompilerOutputDir))
       add(project.file(project.konanInteropOutputDir))
   }

    // DSL Methods

    /** Absolute path */
    fun file(file: Any)                   = files.add(project.files(file))
    fun files(vararg files: Any)          = this.files.addAll(files.map { project.files(it) })
    fun files(collection: FileCollection) = this.files.add(collection)

    // TODO: doc.
    /** The compiler with search the library in repos */
    fun klib(lib: String)             = namedKlibs.add(lib)
    fun klibs(vararg libs: String)    = namedKlibs.addAll(libs)
    fun klibs(libs: Iterable<String>) = namedKlibs.addAll(libs)

    /** Direct link to a config */
    fun klib(libConfig: KonanBuildingConfig) {
        useReposOf(libConfig)
        artifacts.add(libConfig)
    }

    /** Artifact in the specified project by name */
    fun artifact(libraryProject: Project, name: String) {
        project.evaluationDependsOn(libraryProject)
        klib(libraryProject.konanArtifactsContainer.getByName(name))
    }
    /** Interop in the specified project by name */
    fun interop(libraryProject: Project, name: String) {
        project.evaluationDependsOn(libraryProject)
        klib(libraryProject.konanInteropContainer.getByName(name))
    }

    /** Artifact in the current project by name */
    fun artifact(name: String) = artifact(project, name)
    /** Interop library in the current project by name */
    fun interop(name: String) = interop(project, name)

    /** Artifact by direct link */
    fun artifact(artifact: KonanCompileConfig) = klib(artifact)
    /** Interop library by direct link */
    fun interop(artifact: KonanInteropConfig) = klib(artifact)

    /** All artifacts from the projects by direct link */
    fun allArtifactsFrom(vararg libraryProjects: Project) = libraryProjects.forEach {
        project.evaluationDependsOn(it)
        it.konanArtifactsContainer
                .filter { it.task.isLibrary }
                .forEach { klib(it) }
    }
    /** All interop libraries from the projects by direct link */
    fun allInteropsFrom(vararg libraryProjects: Project) = libraryProjects.forEach {
        project.evaluationDependsOn(it)
        it.konanInteropContainer
                .forEach { klib(it) }
    }

    /** All artifacts from the project by its relative path */
    fun allArtifactsFrom(vararg paths: String) = allArtifactsFrom(*paths.map { project.project(it) }.toTypedArray())
    /** All interop libraries from the project by its name */
    fun allInteropsFrom(vararg paths: String) = allInteropsFrom(*paths.map { project.project(it) }.toTypedArray())

    /** Add repo for library search */
    fun useRepo(directory: Any) = repos.add(project.file(directory))
    /** Add repos for library search */
    fun useRepos(vararg directories: Any) = directories.forEach { useRepo(it) }
    /** Add repos for library search */
    fun useRepos(directories: Iterable<Any>) = directories.forEach { useRepo(it) }

    /** Add the output directory of the config and default output directories of its project as repos  */
    private fun useReposOf(config: KonanBuildingConfig) {
        useRepo(config.task.outputDir)
        useRepo(config.project.konanCompilerOutputDir)
        useRepo(config.project.konanInteropOutputDir)
    }

    fun Project.evaluationDependsOn(another: Project) {
        if (this != another) { evaluationDependsOn(another.path) }
    }
}