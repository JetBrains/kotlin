package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionTask
import org.gradle.api.provider.ListProperty
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
import org.jetbrains.kotlin.gradle.utils.getValue
import org.jetbrains.kotlin.gradle.utils.isJavaFile
import java.io.File
import java.util.jar.JarFile

@CacheableTask
abstract class KaptTask : ConventionTask(), TaskWithLocalState {
    init {
        cacheOnlyIfEnabledForKotlin()

        val reason = "Caching is disabled for kapt with 'kapt.useBuildCache'"
        outputs.cacheIf(reason) { useBuildCache }
    }

    override fun localStateDirectories(): FileCollection = objects.fileCollection()

    @get:Internal
    @field:Transient
    internal lateinit var kotlinCompileTask: KotlinCompile

    @get:Internal
    internal lateinit var stubsDir: File

    @get:Internal
    internal val objects = project.objects

    @get:Classpath
    @get:InputFiles
    abstract val kaptClasspath: ConfigurableFileCollection

    @get:Classpath
    @get:InputFiles
    val compilerClasspath: List<File> by project.provider {
        kotlinCompileTask.computedCompilerClasspath
    }

    @get:Internal
    internal abstract val kaptClasspathConfigurationNames: ListProperty<String>


    @get:PathSensitive(PathSensitivity.NONE)
    @get:Optional
    @get:InputFiles
    internal var classpathStructure: FileCollection? = null

    /**
     * Output directory that contains caches necessary to support incremental annotation processing.
     * [LocalState] should be used here, but in order to be compatible with Gradle 4.2, correct input
     * annotations are specified during task configuration.
     */
    @get:Internal
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
    val classpath: FileCollection by project.provider {
        kotlinCompileTask.classpath
    }

    @Suppress("unused", "DeprecatedCallableAddReplaceWith")
    @Deprecated(
        message = "Don't use directly. Used only for up-to-date checks",
        level = DeprecationLevel.ERROR
    )
    @get:CompileClasspath
    @get:InputFiles
    internal val internalAbiClasspath: FileCollection by project.provider {
        if (includeCompileClasspath) project.files() else kotlinCompileTask.classpath
    }


    @Suppress("unused", "DeprecatedCallableAddReplaceWith")
    @Deprecated(
        message = "Don't use directly. Used only for up-to-date checks",
        level = DeprecationLevel.ERROR
    )
    @get:Classpath
    @get:InputFiles
    internal val internalNonAbiClasspath: FileCollection by project.provider {
        if (includeCompileClasspath) kotlinCompileTask.classpath else project.files()
    }

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

    // store the files before filtering, so that Gradle doesn't serialize the filtered collection for instant execution state and reuse it
    private val unfilteredJavaSourceRoots by project.provider {
        (kotlinCompileTask.sourceRootsContainer.sourceRoots + stubsDir)
    }

    @get:Internal
    protected val javaSourceRoots: Set<File>
        get() = unfilteredJavaSourceRoots.filterTo(HashSet(), ::isRootAllowed)

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
    internal val compiledSources by project.provider {
        listOfNotNull(kotlinCompileTask.destinationDir, kotlinCompileTask.javaOutputDir)
    }

    protected fun getIncrementalChanges(inputs: IncrementalTaskInputs): KaptIncrementalChanges {
        return if (isIncremental) {
            findClasspathChanges(inputs)
        } else {
            clearLocalState()
            KaptIncrementalChanges.Unknown
        }
    }

    private fun findClasspathChanges(inputs: IncrementalTaskInputs): KaptIncrementalChanges {
        val incAptCacheDir = incAptCache!!
        incAptCacheDir.mkdirs()

        val allDataFiles = classpathStructure!!.files
        val changedFiles = if (inputs.isIncremental) {
            with(mutableSetOf<File>()) {
                inputs.outOfDate { this.add(it.file) }
                inputs.removed { this.add(it.file) }
                return@with this
            }
        } else {
            allDataFiles
        }

        val startTime = System.currentTimeMillis()

        val previousSnapshot = if (inputs.isIncremental) {
            val loadedPrevious = ClasspathSnapshot.ClasspathSnapshotFactory.loadFrom(incAptCacheDir)

            val previousAndCurrentDataFiles = lazy { loadedPrevious.getAllDataFiles() + allDataFiles }
            val allChangesRecognized = changedFiles.all {
                val extension = it.extension
                if (extension.isEmpty() || extension == "java" || extension == "jar" || extension == "class") {
                    return@all true
                }
                // if not a directory, Java source file, jar, or class, it has to be a structure file, in order to understand changes
                it in previousAndCurrentDataFiles.value
            }
            if (allChangesRecognized) {
                loadedPrevious
            } else {
                ClasspathSnapshot.ClasspathSnapshotFactory.getEmptySnapshot()
            }
        } else {
            ClasspathSnapshot.ClasspathSnapshotFactory.getEmptySnapshot()
        }
        val currentSnapshot =
            ClasspathSnapshot.ClasspathSnapshotFactory.createCurrent(
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