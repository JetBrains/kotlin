@file:JvmName("JvmToolchain")

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.*
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

enum class JdkMajorVersion(
    val majorVersion: Int,
    val targetName: String = majorVersion.toString()
) {
    JDK_1_8(8, targetName = "1.8"),
    JDK_9_0(9),
    JDK_11_0(11),
    JDK_17_0(17),
    JDK_21_0(21);

    val envName = name
}

val DEFAULT_JVM_TOOLCHAIN = JdkMajorVersion.JDK_11_0

fun Project.configureJvmDefaultToolchain() {
    configureJvmToolchain(DEFAULT_JVM_TOOLCHAIN)
}

fun Project.configureJvmToolchain(jdkVersion: JdkMajorVersion) {
    @Suppress("NAME_SHADOWING")
    val jdkVersion = chooseJdk_1_8ForJpsBuild(jdkVersion)
    // Ensure java only modules also set default toolchain
    configureJavaOnlyToolchain(jdkVersion)

    plugins.withId("org.jetbrains.kotlin.jvm") {
        val kotlinExtension = extensions.getByType<KotlinTopLevelExtension>()
        kotlinExtension.jvmToolchain {
            setupToolchain(jdkVersion)
        }
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

fun Project.configureJavaOnlyToolchain(
    jdkVersion: JdkMajorVersion
) {
    @Suppress("NAME_SHADOWING")
    val jdkVersion = chooseJdk_1_8ForJpsBuild(jdkVersion)
    plugins.withId("java-base") {
        val javaExtension = extensions.getByType<JavaPluginExtension>()
        javaExtension.toolchain {
            setupToolchain(jdkVersion)
        }
    }
}

fun Project.chooseJdk_1_8ForJpsBuild(jdkVersion: JdkMajorVersion): JdkMajorVersion {
    return if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
        maxOf(jdkVersion, JdkMajorVersion.JDK_1_8)
    } else {
        jdkVersion
    }
}

fun KotlinJvmCompile.configureTaskToolchain(
    jdkVersion: JdkMajorVersion
) {
    kotlinJavaToolchain.toolchain.use(
        project.getToolchainLauncherFor(jdkVersion)
    )
}

fun JavaCompile.configureTaskToolchain(
    jdkVersion: JdkMajorVersion
) {
    javaCompiler.set(project.getToolchainCompilerFor(jdkVersion))
}

fun Project.updateJvmTarget(
    jvmTarget: String
) {
    @Suppress("NAME_SHADOWING")
    val jvmTarget = if (kotlinBuildProperties.isInJpsBuildIdeaSync && jvmTarget == "1.6") {
        "1.8"
    } else {
        jvmTarget
    }
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
    jdkVersion: JdkMajorVersion
): Provider<JavaCompiler> {
    val service = project.extensions.getByType<JavaToolchainService>()
    return service.compilerFor {
        this.languageVersion.set(JavaLanguageVersion.of(jdkVersion.majorVersion))
    }
}

fun Project.getToolchainLauncherFor(
    jdkVersion: JdkMajorVersion
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
