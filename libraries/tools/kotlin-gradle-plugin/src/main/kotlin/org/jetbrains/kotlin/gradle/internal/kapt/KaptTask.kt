package org.jetbrains.kotlin.gradle.internal

import com.intellij.openapi.util.io.FileUtil
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.ClasspathSnapshot
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.KaptClasspathChanges
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.KaptIncrementalChanges
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.UnknownSnapshot
import org.jetbrains.kotlin.gradle.internal.tasks.TaskWithLocalState
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.cacheOnlyIfEnabledForKotlin
import org.jetbrains.kotlin.gradle.tasks.clearLocalState
import org.jetbrains.kotlin.gradle.tasks.isBuildCacheSupported
import org.jetbrains.kotlin.gradle.utils.isJavaFile
import java.io.File
import java.util.jar.JarFile

@CacheableTask
abstract class KaptTask : ConventionTask(), TaskWithLocalState {
    init {
        cacheOnlyIfEnabledForKotlin()

        if (isBuildCacheSupported()) {
            val reason = "Caching is disabled for kapt with 'kapt.useBuildCache'"
            outputs.cacheIf(reason) { useBuildCache }
        }
    }

    override fun localStateDirectories(): FileCollection = project.files()

    @get:Internal
    internal lateinit var kotlinCompileTask: KotlinCompile

    @get:Internal
    internal lateinit var stubsDir: File

    @get:Classpath
    @get:InputFiles
    val kaptClasspath: FileCollection
        get() = project.files(*kaptClasspathConfigurations.toTypedArray())

    @get:Classpath
    @get:InputFiles
    val compilerClasspath: List<File>
        get() = kotlinCompileTask.computedCompilerClasspath

    @get:Internal
    internal lateinit var kaptClasspathConfigurations: List<Configuration>


    @get:PathSensitive(PathSensitivity.NONE)
    @get:Optional
    @get:InputFiles
    internal var classpathStructure: FileCollection? = null

    /** Output directory that contains caches necessary to support incremental annotation processing. */
    @get:OutputDirectory
    @get:Optional
    var incAptCache: File? = null

    @get:OutputDirectory
    internal lateinit var classesDir: File

    @get:OutputDirectory
    lateinit var destinationDir: File

    @get:OutputDirectory
    lateinit var kotlinSourcesDestinationDir: File

    @get:Nested
    internal val annotationProcessorOptionProviders: MutableList<Any> = mutableListOf()

    @get:Input
    internal var includeCompileClasspath: Boolean = true

    @get:Input
    internal var isIncremental = true

    // @Internal because _abiClasspath and _nonAbiClasspath are used for actual checks
    @get:Internal
    val classpath: FileCollection
        get() = kotlinCompileTask.classpath

    @Suppress("unused", "DeprecatedCallableAddReplaceWith")
    @Deprecated(
        message = "Don't use directly. Used only for up-to-date checks",
        level = DeprecationLevel.ERROR
    )
    @get:CompileClasspath
    @get:InputFiles
    internal val internalAbiClasspath: FileCollection
        get() = if (includeCompileClasspath) project.files() else kotlinCompileTask.classpath


    @Suppress("unused", "DeprecatedCallableAddReplaceWith")
    @Deprecated(
        message = "Don't use directly. Used only for up-to-date checks",
        level = DeprecationLevel.ERROR
    )
    @get:Classpath
    @get:InputFiles
    internal val internalNonAbiClasspath: FileCollection
        get() = if (includeCompileClasspath) kotlinCompileTask.classpath else project.files()

    @get:Internal
    var useBuildCache: Boolean = false

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val source: Collection<File>
        get() {
            val result = HashSet<File>()
            for (root in javaSourceRoots) {
                root.walk().filterTo(result) { it.isJavaFile() }
            }
            return result
        }

    @get:Internal
    protected val javaSourceRoots: Set<File>
        get() = (kotlinCompileTask.sourceRootsContainer.sourceRoots + stubsDir)
            .filterTo(HashSet(), ::isRootAllowed)

    private fun isRootAllowed(file: File): Boolean =
        file.exists() &&
                !FileUtil.isAncestor(destinationDir, file, /* strict = */ false) &&
                !FileUtil.isAncestor(classesDir, file, /* strict = */ false)

    private fun FileCollection?.orEmpty(): FileCollection =
        this ?: project.files()

    protected fun checkAnnotationProcessorClasspath() {
        if (!includeCompileClasspath) return

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
                            + kaptClasspathConfigurations.joinToString("\n") { " '${it.name}'" }
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
    @Internal
    protected fun getCompiledSources() = listOfNotNull(kotlinCompileTask.destinationDir, kotlinCompileTask.javaOutputDir)

    protected fun getIncrementalChanges(inputs: IncrementalTaskInputs): KaptIncrementalChanges {
        if (!isIncremental) {
            clearLocalState()
            return KaptIncrementalChanges.Unknown
        }
        val allDataFiles = classpathStructure!!.files
        if (!inputs.isIncremental) {
            clearLocalState()
            findClasspathChanges(allDataFiles, allDataFiles)
            return KaptIncrementalChanges.Unknown
        }

        val changedFiles = with(mutableSetOf<File>()) {
            inputs.outOfDate { this.add(it.file) }
            inputs.removed { this.add(it.file) }
            return@with this
        }

        val changedDataFiles = allDataFiles.filterTo(HashSet<File>()) { it in changedFiles }
        val classpathStatus = findClasspathChanges(allDataFiles, changedDataFiles)
        return when (classpathStatus) {
            is KaptClasspathChanges.Unknown -> KaptIncrementalChanges.Unknown
            is KaptClasspathChanges.Known -> KaptIncrementalChanges.Known(
                changedFiles.filter { it.extension == "java" }.toSet(), classpathStatus.names
            )
        }
    }

    private fun findClasspathChanges(allDataFile: Set<File>, changedDataFiles: Set<File>): KaptClasspathChanges {
        val incAptCacheDir = incAptCache!!
        incAptCacheDir.mkdirs()

        val startTime = System.currentTimeMillis()

        val previousSnapshot = ClasspathSnapshot.ClasspathSnapshotFactory.loadFrom(incAptCacheDir)
        val currentSnapshot =
            ClasspathSnapshot.ClasspathSnapshotFactory.createCurrent(incAptCacheDir, classpath.files.toList(), allDataFile)

        val classpathChanges = currentSnapshot.diff(previousSnapshot, changedDataFiles)
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
        return classpathChanges
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
}