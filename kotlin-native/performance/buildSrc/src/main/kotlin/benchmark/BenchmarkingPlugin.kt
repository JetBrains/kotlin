package org.jetbrains.kotlin.benchmark

import groovy.lang.Closure
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeTargetPreset
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.konan.target.HostManager
import javax.inject.Inject
import kotlin.reflect.KClass

internal val NamedDomainObjectContainer<KotlinSourceSet>.commonMain
    get() = maybeCreate("commonMain")

internal val NamedDomainObjectContainer<KotlinSourceSet>.nativeMain
    get() = maybeCreate("nativeMain")

internal val Project.nativeWarmup: Int
    get() = (property("nativeWarmup") as String).toInt()

internal val Project.attempts: Int
    get() = (property("attempts") as String).toInt()

internal val Project.nativeBenchResults: String
    get() = property("nativeBenchResults") as String

// Gradle property to add flags to benchmarks run from command line.
internal val Project.compilerArgs: List<String>
    get() = (findProperty("compilerArgs") as String?)?.split("\\s".toRegex()).orEmpty()

internal val Project.kotlinVersion: String
    get() = property("kotlinVersion") as String

internal val Project.konanVersion: String
    get() = property("konanVersion") as String

internal val Project.nativeJson: String
    get() = project.property("nativeJson") as String

internal val Project.jvmJson: String
    get() = project.property("jvmJson") as String

internal val Project.buildType: NativeBuildType
    get() = (findProperty("nativeBuildType") as String?)?.let { NativeBuildType.valueOf(it) } ?: NativeBuildType.RELEASE

internal val Project.crossTarget: String?
    get() = findProperty("crossTarget") as String?

internal val Project.commonBenchmarkProperties: Map<String, Any>
    get() = mapOf(
            "cpu" to System.getProperty("os.arch"),
            "os" to System.getProperty("os.name"),
            "jdkVersion" to System.getProperty("java.version"),
            "jdkVendor" to System.getProperty("java.vendor"),
            "kotlinVersion" to kotlinVersion
    )

open class BenchmarkExtension @Inject constructor(val project: Project) {
    var applicationName: String = project.name
    var commonSrcDirs: Collection<Any> = emptyList()
    var nativeSrcDirs: Collection<Any> = emptyList()
    var compileTasks: List<String> = emptyList()
    var linkerOpts: Collection<String> = emptyList()
    var compilerOpts: List<String> = emptyList()
    var buildType: NativeBuildType = project.buildType
    var repeatingType: BenchmarkRepeatingType = BenchmarkRepeatingType.INTERNAL
    var cleanBeforeRunTask: String? = "konanRun"

    val dependencies: BenchmarkDependencies = BenchmarkDependencies()

    fun dependencies(action: BenchmarkDependencies.() -> Unit) =
            dependencies.action()

    fun dependencies(action: Closure<*>) {
        project.configure(dependencies, action)
    }

    inner class BenchmarkDependencies  {
        public val sourceSets: NamedDomainObjectContainer<KotlinSourceSet>
            get() = project.kotlin.sourceSets

        fun project(path: String): Dependency = project.dependencies.project(mapOf("path" to path))

        fun project(path: String, configuration: String): Dependency =
                project.dependencies.project(mapOf("path" to path, "configuration" to configuration))

        fun common(notation: Any) = sourceSets.commonMain.dependencies {
            implementation(notation)
        }

        fun native(notation: Any) = sourceSets.nativeMain.dependencies {
            implementation(notation)
        }
    }
}

/**
 * A plugin configuring a benchmark Kotlin/Native project.
 */
abstract class BenchmarkingPlugin: Plugin<Project> {
    protected abstract val Project.nativeExecutable: String
    protected abstract val Project.nativeLinkTask: Task
    protected abstract val Project.benchmark: BenchmarkExtension
    protected abstract val benchmarkExtensionName: String
    protected abstract val benchmarkExtensionClass: KClass<*>

    protected val mingwPath: String = System.getenv("MINGW64_DIR") ?: "c:/msys64/mingw64"

    protected open fun Project.determinePreset(): AbstractKotlinNativeTargetPreset<*> =
            (crossTarget?.let { targetHostPreset(this, it) } ?:
            defaultHostPreset(this).also { preset ->
                logger.quiet("$project has been configured for ${preset.name} platform.")
            }) as AbstractKotlinNativeTargetPreset<*>

    protected abstract fun NamedDomainObjectContainer<KotlinSourceSet>.configureSources(project: Project)

    protected open fun NamedDomainObjectContainer<KotlinSourceSet>.additionalConfigurations(project: Project) {}

    protected open fun Project.configureSourceSets(kotlinVersion: String) {
        with(kotlin.sourceSets) {
            commonMain.dependencies {
                implementation(files("${project.findProperty("kotlin_dist")}/kotlinc/lib/kotlin-stdlib.jar"))
            }

            repositories.flatDir {
                dir("${project.findProperty("kotlin_dist")}/kotlinc/lib")
            }

            additionalConfigurations(this@configureSourceSets)

            // Add sources specified by a user in the benchmark DSL.
            afterEvaluate {
                configureSources(project)
            }
        }
    }

    protected open fun KotlinNativeTarget.configureNativeOutput(project: Project) {
        binaries.executable(NATIVE_EXECUTABLE_NAME, listOf(project.benchmark.buildType)) {
            if (HostManager.hostIsMingw) {
                linkerOpts.add("-L${mingwPath}/lib")
            }

            runTask?.apply {
                group = ""
                enabled = false
            }

            // Specify settings configured by a user in the benchmark extension.
            project.afterEvaluate {
                linkerOpts.addAll(project.benchmark.linkerOpts)
                freeCompilerArgs = project.benchmark.compilerOpts + project.compilerArgs
            }
        }
    }

    protected fun Project.configureNativeTarget(hostPreset: AbstractKotlinNativeTargetPreset<*>) {
        kotlin.targetFromPreset(hostPreset, NATIVE_TARGET_NAME) {
            compilations.named("main").configure {
                @Suppress("DEPRECATION")
                kotlinOptions.freeCompilerArgs = benchmark.compilerOpts + project.compilerArgs
                dependencies {
                    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
                }
            }
            configureNativeOutput(this@configureNativeTarget)
        }
    }

    protected open fun configureMPPExtension(project: Project) {
        project.configureSourceSets(project.kotlinVersion)
        project.configureNativeTarget(project.determinePreset())
    }

    protected open fun Project.configureNativeTask(nativeTarget: KotlinNativeTarget): Task {
        val konanRun = createRunTask(this, "konanRun", nativeLinkTask,
                nativeExecutable, layout.buildDirectory.file(nativeBenchResults).get().asFile.absolutePath).apply {
            group = BENCHMARKING_GROUP
            description = "Runs the benchmark for Kotlin/Native."
        }
        afterEvaluate {
            val task = konanRun as RunKotlinNativeTask
            task.args("-p", "${benchmark.applicationName}::")
            task.warmupCount = nativeWarmup
            task.repeatCount = attempts
            task.repeatingType = benchmark.repeatingType
        }
        return konanRun
    }

    protected abstract fun Project.configureJvmTask(): Task

    protected fun compilerFlagsFromBinary(project: Project): List<String> {
        val result = mutableListOf<String>()
        if (project.benchmark.buildType.optimized) {
            result.add("-opt")
        }
        if (project.benchmark.buildType.debuggable) {
            result.add("-g")
        }
        return result
    }

    @Suppress("DEPRECATION")
    protected open fun getCompilerFlags(project: Project, nativeTarget: KotlinNativeTarget) =
            compilerFlagsFromBinary(project) + nativeTarget.compilations.main.kotlinOptions.freeCompilerArgs.map { "\"$it\"" }

    protected open fun Project.collectCodeSize(applicationName: String) =
            getCodeSizeBenchmark(applicationName, nativeExecutable)

    @OptIn(ExperimentalStdlibApi::class)
    protected open fun Project.configureKonanJsonTask(nativeTarget: KotlinNativeTarget): Task {
        return tasks.create("konanJsonReport") {
            group = BENCHMARKING_GROUP
            description = "Builds the benchmarking report for Kotlin/Native."

            doLast {
                val applicationName = benchmark.applicationName
                val benchContents = layout.buildDirectory.file(nativeBenchResults).get().asFile.readText()
                val nativeCompileTasks = if (benchmark.compileTasks.isEmpty()) {
                   listOf("linkBenchmark${benchmark.buildType.name.lowercase().replaceFirstChar { it.uppercase() }}ExecutableNative")
                } else benchmark.compileTasks

                val nativeCompileTime = getNativeCompileTime(project, applicationName, nativeCompileTasks)

                val properties = commonBenchmarkProperties + mapOf(
                        "type" to "native",
                        "compilerVersion" to konanVersion,
                        "flags" to getCompilerFlags(project, nativeTarget).sorted(),
                        "benchmarks" to benchContents,
                        "compileTime" to listOf(nativeCompileTime),
                        "codeSize" to collectCodeSize(applicationName)
                )

                val output = createJsonReport(properties)
                layout.buildDirectory.file(nativeJson).get().asFile.writeText(output)
            }
        }
    }

    protected abstract fun Project.configureJvmJsonTask(jvmRun: Task): Task

    protected open fun Project.configureExtraTasks() {}

    private fun Project.configureTasks() {
        val nativeTarget = kotlin.targets.getByName(NATIVE_TARGET_NAME) as KotlinNativeTarget
        configureExtraTasks()
        // Native run task.
        configureNativeTask(nativeTarget)

        // JVM run task.
        val jvmRun = configureJvmTask()

        // Native report task.
        configureKonanJsonTask(nativeTarget)

        // JVM report task.
        configureJvmJsonTask(jvmRun)

        project.afterEvaluate {
            // Need to rebuild benchmark to collect compile time.
            project.benchmark.cleanBeforeRunTask?.let { tasks.getByName(it).dependsOn("clean") }
        }
    }

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("kotlin-multiplatform")

        // Use Kotlin compiler version specified by the project property.
        target.logger.info("BenchmarkingPlugin.kt:apply($kotlinVersion)")
        dependencies.add(
            "kotlinCompilerClasspath", files(
                "${project.findProperty("kotlin_dist")}/kotlinc/lib/kotlin-compiler.jar",
                "${project.findProperty("kotlin_dist")}/kotlinc/lib/kotlin-daemon.jar"
            )
        )
        addTimeListener(this)

        extensions.create(benchmarkExtensionName, benchmarkExtensionClass.java, this)
        configureMPPExtension(this)
        configureTasks()
    }

    companion object {
        const val NATIVE_TARGET_NAME = "native"
        const val NATIVE_EXECUTABLE_NAME = "benchmark"
        const val BENCHMARKING_GROUP = "benchmarking"
    }
}
