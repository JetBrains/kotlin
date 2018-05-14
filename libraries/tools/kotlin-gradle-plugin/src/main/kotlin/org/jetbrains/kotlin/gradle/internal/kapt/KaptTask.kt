package org.jetbrains.kotlin.gradle.internal

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil.compareVersionNumbers
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.compilerRunner.GradleCompilerEnvironment
import org.jetbrains.kotlin.compilerRunner.GradleCompilerRunner
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.incremental.isJavaFile
import java.io.File

@CacheableTask
open class KaptTask : ConventionTask() {
    init {
        cacheOnlyIfEnabledForKotlin()

        if (isBuildCacheSupported()) {
            val reason = "Caching is disabled by default for kapt because of arbitrary behavior of external " +
                         "annotation processors. You can enable it by adding 'kapt.useBuildCache = true' to the build script."
            outputs.cacheIf(reason) { useBuildCache }
        }
    }

    @get:Internal
    internal lateinit var kotlinCompileTask: KotlinCompile

    @get:Internal
    internal lateinit var stubsDir: File

    @get:Classpath @get:InputFiles
    val kaptClasspath: FileCollection
        get() = project.files(*kaptClasspathConfigurations.toTypedArray())

    @get:Classpath @get:InputFiles
    val compilerClasspath: List<File> get() = kotlinCompileTask.computedCompilerClasspath

    @get:Internal
    internal lateinit var kaptClasspathConfigurations: List<Configuration>

    @get:OutputDirectory
    internal lateinit var classesDir: File

    @get:OutputDirectory
    lateinit var destinationDir: File

    @get:OutputDirectory
    lateinit var kotlinSourcesDestinationDir: File

    @get:Nested
    internal val annotationProcessorOptionProviders: MutableList<Any> = mutableListOf()

    @get:Classpath @get:InputFiles
    val classpath: FileCollection
        get() = kotlinCompileTask.classpath

    @get:Internal
    var useBuildCache: Boolean = false

    @get:InputFiles @get:PathSensitive(PathSensitivity.RELATIVE)
    val source: Collection<File>
        get() {
            val result = HashSet<File>()
            for (root in javaSourceRoots) {
                root.walk().filterTo(result) { it.isJavaFile() }
            }
            return result
        }

    protected val javaSourceRoots: Set<File>
        get() = (kotlinCompileTask.sourceRootsContainer.sourceRoots + stubsDir)
            .filterTo(HashSet(), ::isRootAllowed)

    private fun isRootAllowed(file: File): Boolean =
        !FileUtil.isAncestor(destinationDir, file, /* strict = */ false) &&
                !FileUtil.isAncestor(classesDir, file, /* strict = */ false)
}