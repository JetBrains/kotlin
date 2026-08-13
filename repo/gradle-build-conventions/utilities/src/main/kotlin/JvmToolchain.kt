@file:JvmName("JvmToolchain")

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.*
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

enum class JdkMajorVersion(
    val majorVersion: Int,
    val targetName: String = majorVersion.toString(),
) {
    JDK_1_8(8, targetName = "1.8"),
    JDK_9_0(9),
    JDK_11_0(11),
    JDK_17_0(17),
    JDK_21_0(21),
    JDK_25_0(25);

    val envName = name
}

/**
 * Default JDK running the compilers.
 *
 * You can override it like this:
 * ```
 * project.configureJvmToolchain(JdkMajorVersion.JDK_17_0)
 * ```
 */
val DEFAULT_JVM_TOOLCHAIN = JdkMajorVersion.JDK_1_8

/**
 * Default Java version the produced bytecode is compatible with.
 *
 * It is independent of [DEFAULT_JVM_TOOLCHAIN]: the compilers run on a modern JDK, but cross-compile down to
 * this version, so the artifacts stay usable on it.
 *
 * You can override it like this:
 * ```
 * project.configureJvmToolchain(DEFAULT_JVM_TOOLCHAIN, target = JdkMajorVersion.JDK_17_0)
 * ```
 *
 * A module that cannot be cross-compiled at all — because it uses JDK internals hidden by `--release`, for
 * example — should instead run the compilers on the very JDK it targets:
 * ```
 * project.configureJvmToolchain(JdkMajorVersion.JDK_1_8)
 * ```
 */
val DEFAULT_JVM_TARGET = JdkMajorVersion.JDK_1_8

/**
 * Default Java version used to run tests (for test tasks registered via `project-tests-convention`)
 *
 * You can override like this:
 * ```
 * projectTests {
 *     testTask(javaLauncher = JdkMajorVersion.JDK_17_0)
 * }
 * ```
 *
 * If your test task is registered via plain Gradle, it will use [DEFAULT_JVM_TOOLCHAIN] for running tests,
 * unless you override it via:
 * ```
 * javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_17_0))
 * ```
 */
val DEFAULT_JAVA_LAUNCHER_FOR_TESTS = JdkMajorVersion.JDK_11_0

fun Project.configureJvmDefaultToolchain() {
    configureJvmToolchain(DEFAULT_JVM_TOOLCHAIN, DEFAULT_JVM_TARGET)
}

/**
 * Runs the compilers of this project on [jdkVersion] and makes them produce bytecode for [target].
 *
 * @param restrictApiToTarget whether the compilers only see the API of [target]. Pass `false` for code that
 * calls newer JDK APIs behind a runtime version check and therefore needs the full API of [jdkVersion] while
 * still producing [target] bytecode.
 */
@JvmOverloads
fun Project.configureJvmToolchain(
    jdkVersion: JdkMajorVersion,
    target: JdkMajorVersion = jdkVersion,
    restrictApiToTarget: Boolean = true,
) {
    restrictApiToTargetFlag.set(restrictApiToTarget)
    for (pluginId in listOf("org.jetbrains.kotlin.jvm", "org.jetbrains.kotlin.multiplatform")) {
        plugins.withId(pluginId) {
            configureKotlinToolchain(jdkVersion, target)
        }
    }
    configureJavaOnlyToolchain(jdkVersion, target, restrictApiToTarget)
}

private fun Project.configureKotlinToolchain(jdkVersion: JdkMajorVersion, target: JdkMajorVersion) {
    // Update to KotlinBaseExtension once the bootstrap version will be higher than 2.1.20-dev-201
    @Suppress("Deprecation")
    val kotlinExtension = extensions.getByType<KotlinTopLevelExtension>()
    kotlinExtension.jvmToolchain {
        setupToolchain(jdkVersion)
    }
    // Java 9 tasks are exceptions that are configured in `configureJava9Compilation`.\
    // See details in `repo/gradle-build-conventions/buildsrc-compat/src/main/kotlin/LibrariesCommon.kt`
    val kotlinCompileTasks = tasks
        .withType<KotlinJvmCompile>()
        .matching { "Java9" !in it.name }
    kotlinCompileTasks.configureEach {
        compilerOptions.jvmTarget.set(JvmTarget.fromTarget(target.targetName))
    }
    onlyOnce("jdkReleaseArguments") {
        val restrictApi = restrictApiToTargetFlag
        kotlinCompileTasks.configureEach {
            compilerOptions.freeCompilerArgs.addAll(jdkReleaseArguments(restrictApi))
        }
    }
    onlyOnce("javaCompileDefaults") {
        tasks
            .matching { it.name != "compileJava9Java" && it is JavaCompile }
            .configureEach {
                with(this as JavaCompile) {
                    options.compilerArgs.add("-proc:none")
                    options.encoding = "UTF-8"
                }
            }
    }
}

fun JavaToolchainSpec.setupToolchain(jdkVersion: JdkMajorVersion) {
    languageVersion.set(JavaLanguageVersion.of(jdkVersion.majorVersion))
}

@JvmOverloads
fun Project.configureJavaOnlyToolchain(
    jdkVersion: JdkMajorVersion,
    target: JdkMajorVersion = jdkVersion,
    restrictApiToTarget: Boolean = true,
) {
    plugins.withId("java-base") {
        val javaExtension = extensions.getByType<JavaPluginExtension>()
        javaExtension.toolchain {
            setupToolchain(jdkVersion)
        }
        javaExtension.sourceCompatibility = JavaVersion.toVersion(target.targetName)
        javaExtension.targetCompatibility = JavaVersion.toVersion(target.targetName)
        restrictApiToTargetFlag.set(restrictApiToTarget)

        onlyOnce("javaReleaseArgument") {
            val restrictApi = restrictApiToTargetFlag
            tasks.withType<JavaCompile>().configureEach {
                options.compilerArgumentProviders.add(
                    JavaReleaseArgumentProvider(releaseArgument(restrictApi))
                )
            }
        }
    }
}

/**
 * Runs [action] on the first call for this project and does nothing on the following ones.
 *
 * Used for the parts of the toolchain setup that must not accumulate when a build script reconfigures the
 * toolchain of an already configured project.
 */
private fun Project.onlyOnce(id: String, action: () -> Unit) {
    val key = "jvmToolchain.$id"
    if (extensions.extraProperties.has(key)) return
    extensions.extraProperties.set(key, true)
    action()
}

/**
 * Whether the compilers of this project only see the API of the version they target.
 *
 * Held in a mutable property, so that the argument providers below observe the last
 * [configureJvmToolchain] call rather than the one that registered them.
 */
private val Project.restrictApiToTargetFlag: Property<Boolean>
    get() {
        val key = "jvmToolchain.restrictApiToTarget"
        val extras = extensions.extraProperties
        @Suppress("UNCHECKED_CAST")
        return extras.properties[key] as Property<Boolean>?
            ?: objects.property(Boolean::class.java).also { extras.set(key, it) }
    }

/**
 * `-Xjdk-release` for the JVM target of this task, or nothing when the compiler already runs on that JDK.
 */
private fun KotlinJvmCompile.jdkReleaseArguments(restrictApiToTarget: Provider<Boolean>): Provider<List<String>> =
    compilerOptions.jvmTarget
        .zip(kotlinJavaToolchain.javaVersion) { target, compilerVersion ->
            if (target.target == compilerVersion.toString()) {
                emptyList()
            } else {
                listOf("-Xjdk-release=${target.target}")
            }
        }
        .zip(restrictApiToTarget.orElse(true)) { arguments, restrict -> if (restrict) arguments else emptyList() }
        .orElse(emptyList())

/**
 * The value for `--release`, or `null` when `javac` already runs on the JDK this task targets or when the
 * task passes `--release` on its own.
 */
private fun JavaCompile.releaseArgument(restrictApiToTarget: Provider<Boolean>): Provider<String> = project.provider {
    val target = JavaVersion.toVersion(targetCompatibility)
    val compilerVersion = javaCompiler.orNull
        ?.let { JavaVersion.toVersion(it.metadata.languageVersion.asInt()) }
        ?: JavaVersion.current()
    when {
        !restrictApiToTarget.getOrElse(true) || options.release.isPresent || target == compilerVersion -> null
        else -> target.majorVersion
    }
}

/**
 * Passes `--release` to `javac`, derived from the compile task's own `targetCompatibility`.
 *
 * [org.gradle.api.tasks.compile.CompileOptions.release] cannot be used for that: Gradle derives
 * `targetCompatibility` from it, so a convention pointing the other way is a cycle and fails the build.
 * An argument provider breaks the cycle, because it is only queried at execution time.
 *
 * A task setting `options.release` explicitly stays in control: this provider then contributes nothing, so
 * `--release` is never passed twice.
 */
private class JavaReleaseArgumentProvider(
    @get:Input @get:Optional val release: Provider<String>,
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> =
        release.orNull?.let { listOf("--release", it) } ?: emptyList()
}

/**
 * Runs the compiler of this task on [jdkVersion] and makes it produce bytecode for [target].
 *
 * Overrides both axes of [Project.configureJvmToolchain] for a single task. [target] defaults to
 * [jdkVersion], so pinning a task to a JDK also keeps the platform classes of that JDK fully visible.
 */
@JvmOverloads
fun KotlinJvmCompile.configureTaskToolchain(
    jdkVersion: JdkMajorVersion,
    target: JdkMajorVersion = jdkVersion,
) {
    kotlinJavaToolchain.toolchain.use(
        project.getToolchainLauncherFor(jdkVersion)
    )
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(target.targetName))
}

/**
 * @see [KotlinJvmCompile.configureTaskToolchain]
 */
@JvmOverloads
fun JavaCompile.configureTaskToolchain(
    jdkVersion: JdkMajorVersion,
    target: JdkMajorVersion = jdkVersion,
) {
    javaCompiler.set(project.getToolchainCompilerFor(jdkVersion))
    sourceCompatibility = target.targetName
    targetCompatibility = target.targetName
}

fun Project.updateJvmTarget(
    jvmTarget: String,
) {
    // Java 9 tasks are exceptions that are configured in configureJava9Compilation
    tasks
        .withType<KotlinJvmCompile>()
        .matching { it.name != "compileJava9Kotlin" }
        .configureEach {
            compilerOptions.jvmTarget.set(JvmTarget.fromTarget(jvmTarget))
        }

    tasks
        .withType<JavaCompile>()
        .matching { it.name != "compileJava9Java" }
        .configureEach {
            sourceCompatibility = jvmTarget
            targetCompatibility = jvmTarget
        }
}

private fun Project.getToolchainCompilerFor(
    jdkVersion: JdkMajorVersion,
): Provider<JavaCompiler> {
    val service = project.extensions.getByType<JavaToolchainService>()
    return service.compilerFor {
        this.languageVersion.set(JavaLanguageVersion.of(jdkVersion.majorVersion))
    }
}

fun Project.getToolchainLauncherFor(
    jdkVersion: JdkMajorVersion,
): Provider<JavaLauncher> {
    val service = project.extensions.getByType<JavaToolchainService>()
    return service.launcherFor {
        this.languageVersion.set(JavaLanguageVersion.of(jdkVersion.majorVersion))
    }
}

fun Project.getToolchainJdkHomeFor(jdkVersion: JdkMajorVersion): Provider<String> {
    return getToolchainLauncherFor(jdkVersion).map {
        it.metadata.installationPath.asFile.absolutePath
    }
}
