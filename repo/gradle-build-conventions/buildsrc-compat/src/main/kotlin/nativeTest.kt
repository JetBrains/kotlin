import TestProperty.*
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.project
import java.io.File

private enum class TestProperty(shortName: String) {
    // Use a separate Gradle property to pass Kotlin/Native home to tests: "kotlin.internal.native.test.nativeHome".
    // Don't use "kotlin.native.home" and similar properties for this purpose, as these properties may have undesired
    // effect on other Gradle tasks (ex: :kotlin-native:dist) that might be executed along with test task.
    KOTLIN_NATIVE_HOME("nativeHome"),
    COMPILER_CLASSPATH("compilerClasspath"),
    COMPILER_PLUGINS("compilerPlugins"),
    CUSTOM_KLIBS("customKlibs"),
    TEST_TARGET("target"),
    TEST_MODE("mode"),
    TEST_KIND("testKind"),
    FORCE_STANDALONE("forceStandalone"), // This is not passed directly into the test infra but transformed into TEST_KIND.
    COMPILE_ONLY("compileOnly"),
    OPTIMIZATION_MODE("optimizationMode"),
    USE_THREAD_STATE_CHECKER("useThreadStateChecker"),
    GC_TYPE("gcType"),
    GC_SCHEDULER("gcScheduler"),
    ALLOCATOR("alloc"),
    CACHE_MODE("cacheMode"),
    EXECUTION_TIMEOUT("executionTimeout"),
    SANITIZER("sanitizer"),
    SHARED_TEST_EXECUTION("sharedTestExecution"),
    TEAMCITY("teamcity");

    val fullName = "kotlin.internal.native.test.$shortName"
}

private sealed class ComputedTestProperty {
    abstract val name: String
    abstract val value: String?

    class Normal(override val name: String, override val value: String?) : ComputedTestProperty()
    class Lazy(override val name: String, private val lazyValue: kotlin.Lazy<String?>) : ComputedTestProperty() {
        override val value get() = lazyValue.value
    }
}

private class ComputedTestProperties(private val task: Test) {
    private val computedProperties = arrayListOf<ComputedTestProperty>()

    fun Project.compute(property: TestProperty, defaultValue: () -> String? = { null }) {
        val gradleValue = readFromGradle(property)
        computedProperties += ComputedTestProperty.Normal(property.fullName, gradleValue ?: defaultValue())
    }

    fun Project.computeLazy(property: TestProperty, defaultLazyValue: () -> Lazy<String?>) {
        val gradleValue = readFromGradle(property)
        computedProperties += if (gradleValue != null)
            ComputedTestProperty.Normal(property.fullName, gradleValue)
        else
            ComputedTestProperty.Lazy(property.fullName, defaultLazyValue())
    }

    // Do not attempt to read the property from Gradle. Instead, set it based on the lambda return value.
    fun computePrivate(property: TestProperty, value: () -> String) {
        computedProperties += ComputedTestProperty.Normal(property.fullName, value())
    }

    fun lazyClassPath(builder: MutableList<File>.() -> Unit): Lazy<String?> = lazy(LazyThreadSafetyMode.NONE) {
        buildList(builder).takeIf { it.isNotEmpty() }?.joinToString(File.pathSeparator) { it.absolutePath }
    }

    fun Project.readFromGradle(property: TestProperty): String? = findProperty(property.fullName)?.toString()

    fun resolveAndApplyToTask() {
        computedProperties.forEach { computedProperty ->
            task.systemProperty(computedProperty.name, computedProperty.value ?: return@forEach)
        }
    }
}

private fun Test.ComputedTestProperties(init: ComputedTestProperties.() -> Unit): ComputedTestProperties =
    ComputedTestProperties(this).apply { init() }

/**
 * @param taskName Name of Gradle task.
 * @param tag Optional JUnit test tag. See https://junit.org/junit5/docs/current/user-guide/#writing-tests-tagging-and-filtering
 * @param requirePlatformLibs Where platform KLIBs from the Kotlin/Native distribution are required for running this test.
 * @param customCompilerDependencies The [Configuration]s that provide additional JARs to be added to the compiler's classpath.
 * @param customTestDependencies The [Configuration]s that provide KLIBs to be added to Kotlin/Native compiler dependencies list
 *   along with Kotlin/Native stdlib KLIB and Kotlin/Native platform KLIBs (the latter only if [requirePlatformLibs] is `true`).
 * @param compilerPluginDependencies The [Configuration]s that provide compiler plugins to be enabled for the Kotlin/Native compiler
 *   for the duration of test execution.
 */
fun Project.nativeTest(
    taskName: String,
    tag: String?,
    requirePlatformLibs: Boolean = false,
    customCompilerDependencies: List<Configuration> = emptyList(),
    customTestDependencies: List<Configuration> = emptyList(),
    compilerPluginDependencies: List<Configuration> = emptyList(),
    body: Test.() -> Unit = {},
) = projectTest(
    taskName,
    jUnitMode = JUnitMode.JUnit5,
    maxHeapSizeMb = 3072 // Extra heap space for Kotlin/Native compiler.
) {
    group = "verification"

    if (kotlinBuildProperties.isKotlinNativeEnabled) {
        workingDir = rootDir
        outputs.upToDateWhen {
            // Don't treat any test task as up-to-date, no matter what.
            // Note: this project should contain only test tasks, including ones that build binaries, and ones that run binaries.
            false
        }

        // Effectively remove the limit for the amount of stack trace elements in Throwable.
        jvmArgs("-XX:MaxJavaStackTraceDepth=1000000")

        // Double the stack size. This is needed to compile some marginal tests with extra-deep IR tree, which requires a lot of stack frames
        // for visiting it. Example: codegen/box/strings/concatDynamicWithConstants.kt
        // Such tests are successfully compiled in old test infra with the default 1 MB stack just by accident. New test infra requires ~55
        // additional stack frames more compared to the old one because of another launcher, etc. and it turns out this is not enough.
        jvmArgs("-Xss2m")

        val availableCpuCores: Int = Runtime.getRuntime().availableProcessors()
        if (!kotlinBuildProperties.isTeamcityBuild
            && minOf(kotlinBuildProperties.junit5NumberOfThreadsForParallelExecution ?: 16, availableCpuCores) > 4
        ) {
            logger.info("$path JIT C2 compiler has been disabled")
            jvmArgs("-XX:TieredStopAtLevel=1") // Disable C2 if there are more than 4 CPUs at the host machine.
        }

        // Compute test properties in advance. Make sure that the necessary dependencies are settled.
        // But do not resolve any configurations until the execution phase.
        val computedTestProperties = ComputedTestProperties {
            compute(KOTLIN_NATIVE_HOME) {
                val testTarget = readFromGradle(TEST_TARGET)
                if (testTarget != null) {
                    dependsOn(":kotlin-native:${testTarget}CrossDist")
                    if (requirePlatformLibs) dependsOn(":kotlin-native:${testTarget}PlatformLibs")
                } else {
                    dependsOn(":kotlin-native:dist")
                    if (requirePlatformLibs) dependsOn(":kotlin-native:distPlatformLibs")
                }
                project(":kotlin-native").projectDir.resolve("dist").absolutePath
            }

            computeLazy(COMPILER_CLASSPATH) {
                val customNativeHome = readFromGradle(KOTLIN_NATIVE_HOME)

                val kotlinNativeCompilerEmbeddable = if (customNativeHome == null)
                    configurations.detachedConfiguration(
                        dependencies.project(":kotlin-native:prepare:kotlin-native-compiler-embeddable"),
                        dependencies.create(commonDependency("org.jetbrains.intellij.deps:trove4j"))
                    ).also { dependsOn(it) }
                else
                    null

                customCompilerDependencies.forEach(::dependsOn)

                lazyClassPath {
                    if (customNativeHome == null) {
                        addAll(kotlinNativeCompilerEmbeddable!!.files)
                    } else {
                        this += file(customNativeHome).resolve("konan/lib/kotlin-native-compiler-embeddable.jar")
                        this += file(customNativeHome).resolve("konan/lib/trove4j.jar")
                    }

                    customCompilerDependencies.flatMapTo(this) { it.files }
                }
            }

            computeLazy(COMPILER_PLUGINS) {
                compilerPluginDependencies.forEach(::dependsOn)
                lazyClassPath { compilerPluginDependencies.flatMapTo(this) { it.files } }
            }

            computeLazy(CUSTOM_KLIBS) {
                customTestDependencies.forEach(::dependsOn)
                lazyClassPath { customTestDependencies.flatMapTo(this) { it.files } }
            }

            compute(TEST_KIND) {
                readFromGradle(FORCE_STANDALONE)?.let { "STANDALONE" }
            }

            // Pass Gradle properties as JVM properties so test process can read them.
            compute(TEST_TARGET)
            compute(TEST_MODE)
            compute(COMPILE_ONLY)
            compute(OPTIMIZATION_MODE)
            compute(USE_THREAD_STATE_CHECKER)
            compute(GC_TYPE)
            compute(GC_SCHEDULER)
            compute(ALLOCATOR)
            compute(CACHE_MODE)
            compute(EXECUTION_TIMEOUT)
            compute(SANITIZER)
            compute(SHARED_TEST_EXECUTION)

            // Pass whether tests are running at TeamCity.
            computePrivate(TEAMCITY) { kotlinBuildProperties.isTeamcityBuild.toString() }
        }

        // Pass the current Gradle task name so test can use it in logging.
        environment("GRADLE_TASK_NAME", path)

        useJUnitPlatform {
            tag?.let { includeTags(it) }
        }

        doFirst {
            logger.info(
                buildString {
                    appendLine("$path parallel test execution parameters:")
                    append("  Available CPU cores = $availableCpuCores")
                    systemProperties.filterKeys { it.startsWith("junit.jupiter") }.toSortedMap().forEach { (key, value) ->
                        append("\n  $key = $value")
                    }
                }
            )

            // Compute lazy properties and apply them all as JVM process properties.
            // This forces to resolve the necessary configurations.
            computedTestProperties.resolveAndApplyToTask()
        }
    } else
        doFirst {
            throw GradleException(
                """
                    Can't run task $path. The Kotlin/Native part of the project is currently disabled.
                    Make sure that "kotlin.native.enabled" is set to "true" in local.properties file, or is passed
                    as a Gradle command-line parameter via "-Pkotlin.native.enabled=true".
                """.trimIndent()
            )
        }
    body()
}
