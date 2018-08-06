package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleScriptException
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.compilerRunner.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.konan.KonanVersion
import org.jetbrains.kotlin.konan.MetaVersion
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.util.DependencyProcessor
import org.jetbrains.kotlin.konan.util.DependencySource
import java.io.File
import java.io.IOException

// TODO: It's just a temporary task used while KN isn't integrated with Big Kotlin compilation infrastructure.

open class KotlinNativeCompile: DefaultTask() {

    init {
        super.dependsOn(KonanCompilerDownloadTask.KONAN_DOWNLOAD_TASK_NAME)
    }

    @Internal
    lateinit var compilation: KotlinNativeCompilation

    // region inputs/outputs
    @Input
    lateinit var outputKind: CompilerOutputKind

    // Inputs and outputs

    val sources: Collection<FileCollection>
        @InputFiles
        @SkipWhenEmpty
        get() = compilation.kotlinSourceSets.map { it.kotlin }

    val libraries: FileCollection
        @InputFiles get() = compilation.compileDependencyFiles.filter {
            it.extension == "klib"
        }

    @Input var optimized = false
    @Input var debuggable = true

    val processTests
        @Input get() = compilation.isTestCompilation

    val target: String
        @Input get() = compilation.target.konanTarget.name

    val additionalCompilerOptions: Collection<String>
        @Input get() = compilation.extraOpts


    val outputFile: Property<File> = project.objects.property(File::class.java)

    // endregion

    // region Useful extensions
    internal fun MutableList<String>.addArg(parameter: String, value: String) {
        add(parameter)
        add(value)
    }

    internal fun MutableList<String>.addArgs(parameter: String, values: Iterable<String>) {
        values.forEach {
            addArg(parameter, it)
        }
    }

    internal fun MutableList<String>.addArgIfNotNull(parameter: String, value: String?) {
        if (value != null) {
            addArg(parameter, value)
        }
    }

    internal fun MutableList<String>.addKey(key: String, enabled: Boolean) {
        if (enabled) {
            add(key)
        }
    }

    internal fun MutableList<String>.addFileArgs(parameter: String, values: FileCollection) {
        values.files.forEach {
            addArg(parameter, it.canonicalPath)
        }
    }

    internal fun MutableList<String>.addFileArgs(parameter: String, values: Collection<FileCollection>) {
        values.forEach {
            addFileArgs(parameter, it)
        }
    }

    internal fun MutableList<String>.addListArg(parameter: String, values: List<String>) {
        if (values.isNotEmpty()) {
            addArg(parameter, values.joinToString(separator = " "))
        }
    }
    // endregion

    @TaskAction
    fun compile() {
        val output = outputFile.get()
        output.parentFile.mkdirs()

        val args = mutableListOf<String>().apply {
            addArg("-o", outputFile.get().absolutePath)
            addKey("-opt", optimized)
            addKey("-g", debuggable)
            addKey("-ea", debuggable)
            addKey("-tr", processTests)

            addArg("-target", target)
            addArg("-p", outputKind.name.toLowerCase())

            add("-Xmulti-platform")

            addAll(additionalCompilerOptions)

            libraries.files.forEach {library ->
                library.parent?.let { addArg("-r", it) }
                addArg("-l", library.nameWithoutExtension)
            }

            // TODO: Filter only kt files?
            addAll(sources.flatMap { it.files }.map { it.absolutePath })
        }

        KonanCompilerRunner(project).run(args)
    }
}

/** Copied from Kotlin/Native repo. */

open class KonanCompilerDownloadTask : DefaultTask() {

    internal companion object {

        val simpleOsName: String = HostManager.simpleOsName()

        val konanCompilerName: String =
            "kotlin-native-$simpleOsName-${KonanVersion.CURRENT}"

        val konanCompilerDownloadDir: String =
            DependencyProcessor.localKonanDir.resolve(KonanCompilerDownloadTask.konanCompilerName).absolutePath

        internal const val BASE_DOWNLOAD_URL = "https://download.jetbrains.com/kotlin/native/builds"
        const val KONAN_DOWNLOAD_TASK_NAME = "checkKonanCompiler"
    }

    @TaskAction
    fun downloadAndExtract() {
        if (!project.hasProperty(KotlinNativeProjectProperty.DOWNLOAD_COMPILER)) {
            val konanHome = project.getProperty(KotlinNativeProjectProperty.KONAN_HOME)
            logger.info("Use a user-defined compiler path: $konanHome")
        } else {
            try {
                val downloadUrlDirectory = buildString {
                    append("$BASE_DOWNLOAD_URL/")
                    val version = KonanVersion.CURRENT
                    when (version.meta) {
                        MetaVersion.DEV -> append("dev/")
                        else -> append("releases/")
                    }
                    append("$version/")
                    append(simpleOsName)
                }
                val konanCompiler = konanCompilerName
                val parentDir = DependencyProcessor.localKonanDir
                logger.info("Downloading Kotlin/Native compiler from $downloadUrlDirectory/$konanCompiler into $parentDir")
                DependencyProcessor(
                    parentDir,
                    downloadUrlDirectory,
                    mapOf(konanCompiler to listOf(DependencySource.Remote.Public))
                ).run()
            } catch (e: IOException) {
                throw GradleScriptException("Cannot download Kotlin/Native compiler", e)
            }
        }
    }
}