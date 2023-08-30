package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.ClasspathSnapshot
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.KaptClasspathChanges
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.KaptIncrementalChanges
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.UnknownSnapshot
import org.jetbrains.kotlin.gradle.internal.tasks.TaskWithLocalState
import org.jetbrains.kotlin.gradle.plugin.CompilerPluginConfig
import org.jetbrains.kotlin.gradle.plugin.internal.configurationTimePropertiesAccessor
import org.jetbrains.kotlin.gradle.plugin.internal.usedAtConfigurationTime
import org.jetbrains.kotlin.gradle.report.GradleBuildMetricsReporter
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.io.File
import java.util.jar.JarFile
import javax.inject.Inject

@CacheableTask
abstract class KaptTask @Inject constructor(
    objectFactory: ObjectFactory
) : DefaultTask(),
    TaskWithLocalState,
    BaseKapt {

    init {
        cacheOnlyIfEnabledForKotlin()

        val reason = "Caching is disabled for kapt with 'kapt.useBuildCache'"
        outputs.cacheIf(reason) { useBuildCache }
    }

    @get:Classpath
    abstract val compilerClasspath: ConfigurableFileCollection

    @get:PathSensitive(PathSensitivity.NONE)
    @get:Incremental
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:Optional
    @get:InputFiles
    abstract val classpathStructure: ConfigurableFileCollection

    @get:Nested
    abstract val kaptPluginOptions: ListProperty<CompilerPluginConfig>

    @get:Nested
    override val annotationProcessorOptionProviders: MutableList<Any> = mutableListOf()

    @get:Input
    override val includeCompileClasspath: Property<Boolean> = objectFactory
        .propertyWithConvention(true)
        .chainedFinalizeValueOnRead()

    @get:Input
    internal var isIncremental = true

    @get:Internal
    internal val defaultKotlinJavaToolchain: Provider<DefaultKotlinJavaToolchain> = objectFactory
        .propertyWithNewInstance({ null })

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
    override val metrics: Property<BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>> = project.objects
        .property(GradleBuildMetricsReporter())

    @get:Input
    abstract val verbose: Property<Boolean>

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
                            + "\nSet 'kapt.include.compile.classpath=false' to disable discovery."
                            + "\nThe following files, containing annotation processors, are not present in KAPT classpath:\n"
                            + processorsAbsentInKaptClasspath.joinToString("\n") { "  '$it'" }
                            + "\nAdd corresponding dependencies to any of the following configurations:\n"
                            + kaptClasspathConfigurationNames.get().joinToString("\n") { " '$it'" }
                )
            } else {
                logger.warn(
                    "Annotation processors discovery from compile classpath is deprecated."
                            + "\nSet 'kapt.include.compile.classpath=false' to disable discovery."
                            + "\nRun the build with '--info' for more details."
                )
            }

        }
    }

    protected fun getIncrementalChanges(inputChanges: InputChanges): KaptIncrementalChanges {
        return if (isIncremental) {
            findClasspathChanges(inputChanges)
        } else {
            cleanOutputsAndLocalState()
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
                cleanOutputsAndLocalState()
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
            cleanOutputsAndLocalState("Kapt is running non-incrementally")

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
        private const val KAPT_VERBOSE_OPTION_NAME = "kapt.verbose"

        internal fun queryKaptVerboseProperty(
            project: Project
        ): Provider<Boolean> {
            return project
                .providers
                .gradleProperty(KAPT_VERBOSE_OPTION_NAME)
                .usedAtConfigurationTime(project.configurationTimePropertiesAccessor)
                .map { it.toString().toBoolean() }
                .orElse(false)
        }
    }
}
