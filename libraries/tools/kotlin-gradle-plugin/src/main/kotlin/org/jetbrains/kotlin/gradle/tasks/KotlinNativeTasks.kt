package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.IvyPatternRepositoryLayout
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.compilerRunner.*
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultCInteropSettings
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.defaultSourceSetName
import org.jetbrains.kotlin.gradle.plugin.mpp.isMainCompilation
import org.jetbrains.kotlin.konan.KonanVersion
import org.jetbrains.kotlin.konan.KonanVersionImpl
import org.jetbrains.kotlin.konan.MetaVersion
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.DependencyDirectories
import java.io.File

// TODO: It's just temporary tasks used while KN isn't integrated with Big Kotlin compilation infrastructure.
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

private fun File.providedByCompiler(project: Project): Boolean =
    toPath().startsWith(project.file(project.konanHome).toPath())
// endregion


open class KotlinNativeCompile : AbstractCompile() {

    init {
        dependsOn(KonanCompilerDownloadTask.KONAN_DOWNLOAD_TASK_NAME)

        @Suppress("LeakingThis")
        setDestinationDir(project.provider {
            val output = outputFile.get()
            if (output.isDirectory) output else output.parentFile
        })

        sourceCompatibility = "1.6"
        targetCompatibility = "1.6"
    }

    @Internal
    lateinit var compilation: KotlinNativeCompilation

    // region inputs/outputs
    @Input
    lateinit var outputKind: CompilerOutputKind

    // Inputs and outputs

    override fun getSource(): FileTree = sources.asFileTree

    val sources: FileCollection
        @InputFiles
        @SkipWhenEmpty
        get() = compilation.allSources

    val libraries: FileCollection
        @InputFiles get() = compilation.compileDependencyFiles

    private val friendModule: FileCollection?
        // It's already taken into account in libraries
        @Internal get() = compilation.friendCompilation?.output

    override fun getClasspath(): FileCollection = libraries
    override fun setClasspath(configuration: FileCollection?) {
        throw UnsupportedOperationException("Setting classpath directly is unsupported.")
    }

    @Input
    var optimized = false
    @Input
    var debuggable = true

    val processTests
        @Input get() = compilation.isTestCompilation

    val target: String
        @Input get() = compilation.target.konanTarget.name

    val entryPoint: String?
        @Optional @Input get() = compilation.entryPoint

    val linkerOpts: List<String>
        @Input get() = compilation.linkerOpts

    val additionalCompilerOptions: Collection<String>
        @Input get() = compilation.extraOpts

    // region Language settings imported from a SourceSet.
    val languageSettings: LanguageSettingsBuilder?
        @Internal get() = project.kotlinExtension.sourceSets.findByName(compilation.defaultSourceSetName)?.languageSettings

    val languageVersion: String?
        @Optional @Input get() = languageSettings?.languageVersion

    val apiVersion: String?
        @Optional @Input get() = languageSettings?.apiVersion

    val progressiveMode: Boolean
        @Input get() = languageSettings?.progressiveMode ?: false

    val enabledLanguageFeatures: Set<String>
        @Input get() = languageSettings?.enabledLanguageFeatures ?: emptySet()
    // endregion.

    val kotlinNativeVersion: String
        @Input get() = KonanCompilerDownloadTask.compilerVersion.toString()

    // We manually register this property as output file or directory depending on output kind.
    @Internal
    val outputFile: Property<File> = project.objects.property(File::class.java)

    // endregion
    @Internal
    val compilerPluginOptions = CompilerPluginOptions()

    val compilerPluginCommandLine
        @Input get()= compilerPluginOptions.arguments

    @Optional @InputFiles
    var compilerPluginClasspath: FileCollection? = null

    @TaskAction
    override fun compile() {
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
            addArgIfNotNull("-entry", entryPoint)

            add("-Xmulti-platform")

            // Language features.
            addArgIfNotNull("-language-version", languageVersion)
            addArgIfNotNull("-api-version", apiVersion)
            addKey("-Xprogressive", progressiveMode)
            enabledLanguageFeatures.forEach { featureName ->
                add("-XXLanguage:+$featureName")
            }

            // Compiler plugins.
            compilerPluginClasspath?.let { pluginClasspath ->
                pluginClasspath.map { it.canonicalPath }.sorted().forEach { path ->
                    add("-Xplugin=$path")
                }
                compilerPluginOptions.arguments.forEach {
                    add("-P$it")
                }
            }

            addAll(additionalCompilerOptions)

            // Libraries.
            libraries.files.filter {
                // Support only klib files for now.
                it.extension == "klib" && !it.providedByCompiler(project)
            }.forEach { library ->
                library.parent?.let { addArg("-r", it) }
                addArg("-l", library.nameWithoutExtension)
            }

            val friends = friendModule?.files
            if (friends != null && friends.isNotEmpty()) {
                addArg("-friend-modules", friends.map { it.absolutePath }.joinToString(File.pathSeparator))
            }

            addListArg("-linkerOpts", linkerOpts)

            // Sources.
            // TODO: Filter only kt files?
            addAll(sources.files.map { it.absolutePath })
        }

        KonanCompilerRunner(project).run(args)
    }
}

open class CInteropProcess: DefaultTask() {

    @Internal
    lateinit var settings: DefaultCInteropSettings

    @Internal // Taken into account in the outputFileProvider property
    lateinit var destinationDir: Provider<File>

    val konanTarget: KonanTarget
        @Internal get() = settings.compilation.target.konanTarget

    val interopName: String
        @Internal get() = settings.name

    val outputFileName: String
        @Internal get() = with(CompilerOutputKind.LIBRARY) {
            val baseName = settings.compilation.let {
                if (it.isMainCompilation) project.name else it.name
            }
            val suffix = suffix(konanTarget)
            return "$baseName-cinterop-$interopName$suffix"
        }

    val outputFile: File
        @Internal get() = outputFileProvider.get().asFile

    // Inputs and outputs.

    @OutputFile
    val outputFileProvider = newOutputFile().apply {
        set { destinationDir.get().resolve(outputFileName) }
    }

    val defFile: File
        @InputFile get() = settings.defFile

    val packageName: String?
        @Optional @Input get() = settings.packageName

    val compilerOpts: List<String>
        @Input get() = settings.compilerOpts

    val linkerOpts: List<String>
        @Input get() = settings.linkerOpts

    val headers: FileCollection
        @InputFiles get() = settings.headers

    val allHeadersDirs: Set<File>
        @Input get() = settings.includeDirs.allHeadersDirs.files

    val headerFilterDirs: Set<File>
        @Input get() = settings.includeDirs.headerFilterDirs.files

    val libraries: FileCollection
        @InputFiles get() = settings.dependencyFiles

    val extraOpts: List<String>
        @Input get() = settings.extraOpts


    // Task action.
    @TaskAction
    fun processInterop() {
        val args = mutableListOf<String>().apply {
            addArg("-o", outputFile.absolutePath)

            addArgIfNotNull("-target", konanTarget.visibleName)
            addArgIfNotNull("-def", defFile.canonicalPath)
            addArgIfNotNull("-pkg", packageName)

            addFileArgs("-h", headers)

            compilerOpts.forEach {
                addArg("-copt", it)
            }

            linkerOpts.forEach {
                addArg("-lopt", it)
            }

            libraries.files.filter {
                // Support only klib files for now.
                it.extension == "klib" && !it.providedByCompiler(project)
            }.forEach { library ->
                library.parent?.let { addArg("-r", it) }
                addArg("-l", library.nameWithoutExtension)
            }

            addArgs("-copt", allHeadersDirs.map { "-I${it.absolutePath}" })
            addArgs("-headerFilterAdditionalSearchPrefix", headerFilterDirs.map { it.absolutePath })

            addAll(extraOpts)
        }

        outputFile.parentFile.mkdirs()
        KonanInteropRunner(project).run(args)
    }
}

open class KonanCompilerDownloadTask : DefaultTask() {

    internal companion object {

        val simpleOsName: String = HostManager.simpleOsName()

        val compilerDirectory: File
            get() = DependencyDirectories.localKonanDir.resolve("kotlin-native-$simpleOsName-$compilerVersion")

        // TODO: Support project property for Kotlin/Native compiler version
        val compilerVersion: KonanVersion = KonanVersionImpl(MetaVersion.RELEASE, 0, 9, 0)

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
        logger.lifecycle("Unpacking Kotlin/Native compiler (version $versionString)...")
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
            // TODO: Move Kotlin/Native Distribution class in the Big Kotlin repo and use it here.
            if (KonanCompilerRunner(project).classpath.isEmpty) {
                downloadAndExtract()
            }
            logger.info("Use Kotlin/Native distribution: $compilerDirectory")
        }
    }
}