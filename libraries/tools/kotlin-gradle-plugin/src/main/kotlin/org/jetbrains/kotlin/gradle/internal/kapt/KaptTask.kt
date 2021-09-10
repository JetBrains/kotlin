package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporterImpl
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.ClasspathSnapshot
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.KaptClasspathChanges
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.KaptIncrementalChanges
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.UnknownSnapshot
import org.jetbrains.kotlin.gradle.internal.tasks.TaskConfigurator
import org.jetbrains.kotlin.gradle.internal.tasks.TaskWithLocalState
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.tasks.cacheOnlyIfEnabledForKotlin
import org.jetbrains.kotlin.gradle.tasks.clearLocalState
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.utils.isConfigurationCacheAvailable
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.gradle.utils.propertyWithConvention
import org.jetbrains.kotlin.gradle.utils.propertyWithNewInstance
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.io.File
import java.util.concurrent.Callable
import java.util.jar.JarFile
import javax.inject.Inject

@CacheableTask
abstract class KaptTask @Inject constructor(
    objectFactory: ObjectFactory
) : ConventionTask(),
    TaskWithLocalState,
    UsesKotlinJavaToolchain {

    open class Configurator<T: KaptTask>(protected val kotlinCompileTask: KotlinCompile) : TaskConfigurator<T> {
        override fun configure(task: T) {
            val objectFactory = task.project.objects
            val providerFactory = task.project.providers

            task.compilerClasspath.from({ kotlinCompileTask.defaultCompilerClasspath })
            task.classpath.from(kotlinCompileTask.classpath)
            task.kotlinSourceRoots.value(
                providerFactory.provider { kotlinCompileTask.sourceRootsContainer.sourceRoots }
            ).disallowChanges()
            task.compiledSources.from(
                kotlinCompileTask.destinationDirectory,
                Callable { kotlinCompileTask.javaOutputDir.takeIf { it.isPresent } }
            ).disallowChanges()
            task.sourceSetName.value(kotlinCompileTask.sourceSetName).disallowChanges()
            task.localStateDirectories.from(Callable { task.incAptCache.orNull }).disallowChanges()
            task.source.from(
                objectFactory.fileCollection().from(task.kotlinSourceRoots, task.stubsDir).asFileTree
                    .matching { it.include("**/*.java") }
                    .filter { f -> task.isRootAllowed(f) }
            ).disallowChanges()
            task.verbose.set(queryKaptVerboseProperty(task.project))
        }
    }

    init {
        cacheOnlyIfEnabledForKotlin()

        val reason = "Caching is disabled for kapt with 'kapt.useBuildCache'"
        outputs.cacheIf(reason) { useBuildCache }
    }

    @get:Internal
    internal abstract val stubsDir: DirectoryProperty

    @get:Classpath
    abstract val kaptClasspath: ConfigurableFileCollection

    //part of kaptClasspath consisting from external artifacts only
    //basically kaptClasspath = kaptExternalClasspath + artifacts built locally
    @get:Classpath
    abstract val kaptExternalClasspath: ConfigurableFileCollection

    @get:Classpath
    abstract val compilerClasspath: ConfigurableFileCollection

    @get:Internal
    internal abstract val kaptClasspathConfigurationNames: ListProperty<String>

    @get:PathSensitive(PathSensitivity.NONE)
    @get:Incremental
    @get:IgnoreEmptyDirectories
    @get:Optional
    @get:InputFiles
    abstract val classpathStructure: ConfigurableFileCollection

    /**
     * Output directory that contains caches necessary to support incremental annotation processing.
     */
    @get:LocalState
    abstract val incAptCache: DirectoryProperty

    @get:OutputDirectory
    internal lateinit var classesDir: File

    @get:OutputDirectory
    lateinit var destinationDir: File

    @get:OutputDirectory
    lateinit var kotlinSourcesDestinationDir: File

    @get:Nested
    internal val annotationProcessorOptionProviders: MutableList<Any> = mutableListOf()

    @get:Input
    internal val includeCompileClasspath: Property<Boolean> = objectFactory
        .propertyWithConvention(true)
        .chainedFinalizeValueOnRead()

    @get:Input
    internal var isIncremental = true

    // @Internal because _abiClasspath and _nonAbiClasspath are used for actual checks
    @get:Internal
    abstract val classpath: ConfigurableFileCollection

    @get:Internal
    internal val defaultKotlinJavaToolchain: Provider<DefaultKotlinJavaToolchain> = objectFactory
        .propertyWithNewInstance(
            project.gradle,
            { null }
        )

    final override val kotlinJavaToolchainProvider: Provider<KotlinJavaToolchain> = defaultKotlinJavaToolchain.cast()

    @Suppress("unused", "DeprecatedCallableAddReplaceWith")
    @Deprecated(
        message = "Don't use directly. Used only for up-to-date checks",
        level = DeprecationLevel.ERROR
    )
    @get:Incremental
    @get:CompileClasspath
    internal val internalAbiClasspath: FileCollection = project.objects.fileCollection().from(
        { if (includeCompileClasspath.get()) null else classpath }
    )


    @Suppress("unused", "DeprecatedCallableAddReplaceWith")
    @Deprecated(
        message = "Don't use directly. Used only for up-to-date checks",
        level = DeprecationLevel.ERROR
    )
    @get:Incremental
    @get:Classpath
    internal val internalNonAbiClasspath: FileCollection = project.objects.fileCollection().from(
        { if (includeCompileClasspath.get()) classpath else null }
    )

    @get:Internal
    var useBuildCache: Boolean = false

    @get:Internal
    abstract val kotlinSourceRoots: ListProperty<File>

    /** Needed for the model builder. */
    @get:Internal
    abstract val sourceSetName: Property<String>

    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:Incremental
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val source: ConfigurableFileCollection

    @get:Internal
    override val metrics: Property<BuildMetricsReporter> = objectFactory
        .property(BuildMetricsReporterImpl())

    @get:Input
    abstract val verbose: Property<Boolean>

    private fun isRootAllowed(file: File): Boolean =
        file.exists() &&
            !isAncestor(destinationDir, file) &&
            !isAncestor(classesDir, file)

    //Have to avoid using FileUtil because it is required system property reading that is not allowed for configuration cache
    private fun isAncestor(dir: File, file: File): Boolean {
        val path = file.canonicalPath
        val prefix = dir.canonicalPath
        val pathLength = path.length
        val prefixLength = prefix.length
        //TODO
        val caseSensitive = true
        return if (prefixLength == 0) {
            true
        } else if (prefixLength > pathLength) {
            false
        } else if (!path.regionMatches(0, prefix, 0, prefixLength, ignoreCase = !caseSensitive)) {
            return false
        } else if (pathLength == prefixLength) {
            return true
        } else {
            val lastPrefixChar: Char = prefix.get(prefixLength - 1)
            var slashOrSeparatorIdx = prefixLength
            if (lastPrefixChar == '/' || lastPrefixChar == File.separatorChar) {
                slashOrSeparatorIdx = prefixLength - 1
            }
            val next1 = path[slashOrSeparatorIdx]
            return !(next1 != '/' && next1 != File.separatorChar)
        }
    }

    protected fun checkAnnotationProcessorClasspath() {
        if (!includeCompileClasspath.get()) return

        val kaptClasspath = kaptClasspath.toSet()
        val processorsFromCompileClasspath = classpath.files.filterTo(LinkedHashSet()) {
            hasAnnotationProcessors(it)
        }
        val processorsAbsentInKaptClasspath = processorsFromCompileClasspath.filter { it !in kaptClasspath }
        if (processorsAbsentInKaptClasspath.isNotEmpty()) {
            if (logger.isInfoEnabled) {
                logger.warn(
                    "Annotation processors discovery from compile classpath is deprecated."
                            + "\nSet 'kapt.includeCompileClasspath = false' to disable discovery."
                            + "\nThe following files, containing annotation processors, are not present in KAPT classpath:\n"
                            + processorsAbsentInKaptClasspath.joinToString("\n") { "  '$it'" }
                            + "\nAdd corresponding dependencies to any of the following configurations:\n"
                            + kaptClasspathConfigurationNames.get().joinToString("\n") { " '$it'" }
                )
            } else {
                logger.warn(
                    "Annotation processors discovery from compile classpath is deprecated."
                            + "\nSet 'kapt.includeCompileClasspath = false' to disable discovery."
                            + "\nRun the build with '--info' for more details."
                )
            }

        }
    }

    // TODO(gavra): Here we assume that kotlinc and javac output is available for incremental runs. We should insert some checks.
    @get:Internal
    internal abstract val compiledSources: ConfigurableFileCollection

    protected fun getIncrementalChanges(inputChanges: InputChanges): KaptIncrementalChanges {
        return if (isIncremental) {
            findClasspathChanges(inputChanges)
        } else {
            clearLocalState()
            KaptIncrementalChanges.Unknown
        }
    }

    private fun findClasspathChanges(inputChanges: InputChanges): KaptIncrementalChanges {
        val incAptCacheDir = incAptCache.asFile.get()
        incAptCacheDir.mkdirs()
        val allDataFiles = classpathStructure.files

        return if (inputChanges.isIncremental) {
            val startTime = System.currentTimeMillis()

            @Suppress("DEPRECATION_ERROR")
            val changedFiles = listOf(
                source,
                internalNonAbiClasspath,
                internalAbiClasspath,
                classpathStructure
            ).fold(mutableSetOf<File>()) { acc, prop ->
                inputChanges.getFileChanges(prop).forEach { acc.add(it.file) }
                acc
            }

            val previousSnapshot = loadPreviousSnapshot(incAptCacheDir, allDataFiles, changedFiles)
            val currentSnapshot = ClasspathSnapshot.ClasspathSnapshotFactory
                .createCurrent(
                    incAptCacheDir,
                    classpath.files.toList(),
                    kaptClasspath.files.toList(),
                    allDataFiles
                )
            val classpathChanges = currentSnapshot.diff(previousSnapshot, changedFiles)
            if (classpathChanges == KaptClasspathChanges.Unknown) {
                // We are unable to determine classpath changes, so clean the local state as we will run non-incrementally
                clearLocalState()
            }
            currentSnapshot.writeToCache()

            if (logger.isInfoEnabled) {
                val time = "Took ${System.currentTimeMillis() - startTime}ms."
                when {
                    previousSnapshot == UnknownSnapshot ->
                        logger.info("Initializing classpath information for KAPT. $time")
                    classpathChanges == KaptClasspathChanges.Unknown ->
                        logger.info("Unable to use existing data, re-initializing classpath information for KAPT. $time")
                    else -> {
                        classpathChanges as KaptClasspathChanges.Known
                        logger.info("Full list of impacted classpath names: ${classpathChanges.names}. $time")
                    }
                }
            }

            return when (classpathChanges) {
                is KaptClasspathChanges.Unknown -> KaptIncrementalChanges.Unknown
                is KaptClasspathChanges.Known -> KaptIncrementalChanges.Known(
                    changedFiles.filter { it.extension == "java" }.toSet(), classpathChanges.names
                )
            }
        } else {
            clearLocalState("Kapt is running non-incrementally")

            ClasspathSnapshot.ClasspathSnapshotFactory
                .createCurrent(
                    incAptCacheDir,
                    classpath.files.toList(),
                    kaptClasspath.files.toList(),
                    allDataFiles
                )
                .writeToCache()

            KaptIncrementalChanges.Unknown
        }
    }

    private fun loadPreviousSnapshot(
        cacheDir: File,
        allDataFiles: MutableSet<File>,
        changedFiles: MutableSet<File>
    ): ClasspathSnapshot {
        val loadedPrevious = ClasspathSnapshot.ClasspathSnapshotFactory.loadFrom(cacheDir)

        val previousAndCurrentDataFiles = lazy { loadedPrevious.getAllDataFiles() + allDataFiles }
        val allChangesRecognized = changedFiles.all {
            val extension = it.extension
            if (extension.isEmpty() || extension == "java" || extension == "jar" || extension == "class") {
                return@all true
            }
            // if not a directory, Java source file, jar, or class, it has to be a structure file, in order to understand changes
            it in previousAndCurrentDataFiles.value
        }
        return if (allChangesRecognized) {
            loadedPrevious
        } else {
            ClasspathSnapshot.ClasspathSnapshotFactory.getEmptySnapshot()
        }
    }

    private fun hasAnnotationProcessors(file: File): Boolean {
        val processorEntryPath = "META-INF/services/javax.annotation.processing.Processor"

        try {
            when {
                file.isDirectory -> {
                    return file.resolve(processorEntryPath).exists()
                }
                file.isFile && file.extension.equals("jar", ignoreCase = true) -> {
                    return JarFile(file).use { jar ->
                        jar.getJarEntry(processorEntryPath) != null
                    }
                }
            }
        } catch (e: Exception) {
            logger.debug("Could not check annotation processors existence in $file: $e")
        }
        return false
    }

    companion object {
        const val KAPT_VERBOSE_OPTION_NAME = "kapt.verbose"

        internal fun queryKaptVerboseProperty(
            project: Project
        ): Provider<Boolean> {
            return if (isConfigurationCacheAvailable(project.gradle)) {
                project
                    .providers
                    .gradleProperty(KAPT_VERBOSE_OPTION_NAME)
                    .forUseAtConfigurationTime()
                    .map { it.toString().toBoolean() }
                    .orElse(false)
            } else {
                project.objects.property(
                    project.hasProperty(KAPT_VERBOSE_OPTION_NAME) &&
                            project.property(KAPT_VERBOSE_OPTION_NAME).toString().toBoolean()
                )
            }
        }
    }
}
