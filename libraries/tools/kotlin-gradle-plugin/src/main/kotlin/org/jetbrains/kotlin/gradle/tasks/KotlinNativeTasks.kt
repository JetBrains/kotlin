package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.IvyPatternRepositoryLayout
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.compilerRunner.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.konan.KonanVersion
import org.jetbrains.kotlin.konan.KonanVersionImpl
import org.jetbrains.kotlin.konan.MetaVersion
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.util.DependencyDirectories
import java.io.File

// TODO: It's just temporary tasks used while KN isn't integrated with Big Kotlin compilation infrastructure.

open class KotlinNativeCompile : DefaultTask() {

    init {
        super.dependsOn(KonanCompilerDownloadTask.KONAN_DOWNLOAD_TASK_NAME)
    }

    @Internal
    lateinit var compilation: KotlinNativeCompilation

    // region inputs/outputs
    @Input
    lateinit var outputKind: CompilerOutputKind

    // Inputs and outputs

    val sources: FileCollection
        @InputFiles
        @SkipWhenEmpty
        get() = compilation.allSources

    val libraries: FileCollection
        @InputFiles get() = compilation.compileDependencyFiles.filter {
            it.extension == "klib"
        }

    @Input
    var optimized = false
    @Input
    var debuggable = true

    val processTests
        @Input get() = compilation.isTestCompilation

    val target: String
        @Input get() = compilation.target.konanTarget.name

    val additionalCompilerOptions: Collection<String>
        @Input get() = compilation.extraOpts


    @OutputFile
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

            libraries.files.forEach { library ->
                library.parent?.let { addArg("-r", it) }
                addArg("-l", library.nameWithoutExtension)
            }

            // TODO: Filter only kt files?
            addAll(sources.files.map { it.absolutePath })
        }

        KonanCompilerRunner(project).run(args)
    }
}

open class KonanCompilerDownloadTask : DefaultTask() {

    internal companion object {

        val simpleOsName: String = HostManager.simpleOsName()

        val compilerDirectory: File
            get() = DependencyDirectories.localKonanDir.resolve("kotlin-native-$simpleOsName-$compilerVersion")

        // TODO: Support project property for Kotlin/Native compiler version
        val compilerVersion: KonanVersion = KonanVersionImpl(MetaVersion.RELEASE, 0, 8, 2)

        internal const val BASE_DOWNLOAD_URL = "https://download.jetbrains.com/kotlin/native/builds"
        const val KONAN_DOWNLOAD_TASK_NAME = "checkNativeCompiler"
    }

    private val useZip = HostManager.hostIsMingw

    private val archiveExtension
        get() = if (useZip) {
            "zip"
        } else {
            "tar.gz"
        }

    private fun archiveFileTree(archive: File): FileTree =
        if (useZip) {
            project.zipTree(archive)
        } else {
            project.tarTree(archive)
        }

    private fun setupRepo(url: String): ArtifactRepository {
        return project.repositories.ivy { repo ->
            repo.setUrl(url)
            repo.layout("pattern") {
                val layout = it as IvyPatternRepositoryLayout
                layout.artifact("[artifact]-[revision].[ext]")
            }
            repo.metadataSources {
                it.artifact()
            }
        }
    }

    private fun removeRepo(repo: ArtifactRepository) {
        project.repositories.remove(repo)
    }

    private fun downloadAndExtract() {
        val versionString = compilerVersion.toString()

        val url = buildString {
            append("$BASE_DOWNLOAD_URL/")
            append(if (compilerVersion.meta == MetaVersion.DEV) "dev/" else "releases/")
            append("$versionString/")
            append(simpleOsName)
        }

        val repo = setupRepo(url)

        val compilerDependency = project.dependencies.create(
            mapOf(
                "name" to "kotlin-native-$simpleOsName",
                "version" to versionString,
                "ext" to archiveExtension
            )
        )

        val configuration = project.configurations.detachedConfiguration(compilerDependency)
        val archive = configuration.files.single()

        logger.info("Use Kotlin/Native compiler archive: ${archive.absolutePath}")
        logger.lifecycle("Unpack Kotlin/Native compiler (version $versionString)...")
        project.copy {
            it.from(archiveFileTree(archive))
            it.into(DependencyDirectories.localKonanDir)
        }

        removeRepo(repo)

    }


    @TaskAction
    fun checkCompiler() {
        if (!project.hasProperty(KotlinNativeProjectProperty.DOWNLOAD_COMPILER)) {
            val konanHome = project.getProperty(KotlinNativeProjectProperty.KONAN_HOME)
            logger.info("Use a user-defined compiler path: $konanHome")
        } else {
            if (!compilerDirectory.exists()) {
                downloadAndExtract()
            }
            logger.info("Use Kotlin/Native distribution: $compilerDirectory")
        }
    }

}