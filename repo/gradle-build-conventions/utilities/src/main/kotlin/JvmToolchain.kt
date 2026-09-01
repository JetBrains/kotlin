@file:JvmName("JvmToolchain")

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.jvm.toolchain.*
import org.gradle.kotlin.dsl.getByType

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
 * Default Java version used to compile code.
 *
 * You can override it via the `jvmToolchains { }` convention plugin DSL:
 * ```
 * jvmToolchains {
 *     jdkVersion = JdkMajorVersion.JDK_17_0
 * }
 * ```
 */
val DEFAULT_JVM_TOOLCHAIN = JdkMajorVersion.JDK_1_8

/**
 * Default Java version the produced bytecode is compatible with.
 *
 * You can override it via the `jvmToolchains { }` convention plugin DSL:
 * ```
 * jvmToolchains {
 *     targetBytecodeVersion = JdkMajorVersion.JDK_17_0
 * }
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

fun Project.getToolchainCompilerFor(
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
