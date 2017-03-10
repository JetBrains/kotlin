package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import java.io.File

// TODO: form groups for tasks
// TODO: Make the task class nested for config with properties accessible for outer users.
open class KonanCompileTask: DefaultTask() {

    companion object {
        const val COMPILER_MAIN = "org.jetbrains.kotlin.cli.bc.K2NativeKt"
    }

    val COMPILER_JVM_ARGS: List<String>
        get() = listOf("-Dkonan.home=${project.konanHome}", "-Djava.library.path=${project.konanHome}/konan/nativelib")
    val COMPILER_CLASSPATH: String
        get() = "${project.konanHome}/konan/lib/"

    // Output artifact --------------------------------------------------------

    protected lateinit var artifactName: String

    @OutputDirectory
    lateinit var outputDir: File
        internal set

    internal fun initialize(artifactName: String) {
        dependsOn(project.konanCompilerDownloadTask)
        this.artifactName = artifactName
        outputDir = project.file("${project.konanCompilerOutputDir}/$artifactName")
    }

    val artifactPath: String
        get() = "${outputDir.absolutePath}/$artifactName.${ if (noLink) "bc" else "kexe" }"

    // Other compilation parameters -------------------------------------------

    @InputFiles var inputFiles      = mutableSetOf<FileCollection>()
        internal set

    @InputFiles var libraries       = mutableSetOf<FileCollection>()
        internal set
    @InputFiles var nativeLibraries = mutableSetOf<FileCollection>()
        internal set

    @Input var linkerOpts = mutableListOf<String>()
        internal set

    @Input var noStdLib           = false
        internal set
    @Input var noLink             = false
        internal set
    @Input var noMain             = false
        internal set
    @Input var enableOptimization = false
        internal set
    @Input var enableAssertions   = false
        internal set

    @Optional @Input var target          : String? = null
        internal set
    @Optional @Input var languageVersion : String? = null
        internal set
    @Optional @Input var apiVersion      : String? = null
        internal set

    // Useful extensions and functions ---------------------------------------

    protected fun MutableList<String>.addArg(parameter: String, value: String) {
        add(parameter)
        add(value)
    }

    protected fun MutableList<String>.addArgIfNotNull(parameter: String, value: String?) {
        if (value != null) {
            addArg(parameter, value)
        }
    }

    protected fun MutableList<String>.addKey(key: String, enabled: Boolean) {
        if (enabled) {
            add(key)
        }
    }

    protected fun MutableList<String>.addFileArgs(parameter: String, values: FileCollection) {
        values.files.forEach {
            addArg(parameter, it.canonicalPath)
        }
    }

    protected fun MutableList<String>.addFileArgs(parameter: String, values: Collection<FileCollection>) {
        values.forEach {
            addFileArgs(parameter, it)
        }
    }

    // Task action ------------------------------------------------------------

    protected fun buildArgs() = mutableListOf<String>().apply {
        addArg("-output", artifactPath)

        addFileArgs("-library", libraries)
        addFileArgs("-nativelibrary", nativeLibraries)

        linkerOpts.forEach {
            addArg("-linkerArgs", it)
        }

        addArgIfNotNull("-target", target)
        addArgIfNotNull("-language-version", languageVersion)
        addArgIfNotNull("-api-version", apiVersion)

        addKey("-nostdlib", noStdLib)
        addKey("-nolink", noLink)
        addKey("-nomain", noMain)
        addKey("-opt", enableOptimization)
        addKey("-ea", enableAssertions)

        inputFiles.forEach {
            it.files.filter { it.name.endsWith(".kt") }.mapTo(this) { it.canonicalPath }
        }
    }

    @TaskAction
    fun compile() {
        project.file(outputDir).mkdirs()

        // TODO: Use compiler service.
        project.javaexec {
            with(it) {
                main = COMPILER_MAIN
                classpath = project.fileTree(COMPILER_CLASSPATH).apply { include("*.jar") }
                jvmArgs(COMPILER_JVM_ARGS)
                args(buildArgs())
            }
        }
    }

}
