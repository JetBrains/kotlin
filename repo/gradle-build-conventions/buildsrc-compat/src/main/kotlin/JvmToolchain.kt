@file:JvmName("JvmToolchain")

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.*
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

enum class JdkMajorVersion(
    val majorVersion: Int,
    val targetName: String = majorVersion.toString(),
    private val overrideMajorVersion: Int? = null,
    private val mandatory: Boolean = true
) {
    JDK_1_6(6, targetName = "1.6", overrideMajorVersion = 8),
    JDK_1_7(7, targetName = "1.7", overrideMajorVersion = 8),
    JDK_1_8(8, targetName = "1.8"),
    JDK_9_0(9, overrideMajorVersion = 11),
    JDK_10_0(10, mandatory = false, overrideMajorVersion = 11),
    JDK_11_0(11, mandatory = false),
    JDK_17_0(17, mandatory = false),
    JDK_21_0(21, mandatory = false);

    fun isMandatory(): Boolean = mandatory

    val overrideVersion by lazy {
        if (overrideMajorVersion != null) {
            values().firstOrNull() { it.majorVersion == overrideMajorVersion }
                ?: error("Can't find the value with majorVersion=$overrideMajorVersion")
        } else {
            null
        }
    }

    val envName = name
}

val DEFAULT_JVM_TOOLCHAIN = JdkMajorVersion.JDK_1_8

fun Project.configureJvmDefaultToolchain() {
    configureJvmToolchain(DEFAULT_JVM_TOOLCHAIN)
}

fun Project.shouldOverrideObsoleteJdk(jdkVersion: JdkMajorVersion): Boolean =
    kotlinBuildProperties.isObsoleteJdkOverrideEnabled && jdkVersion.overrideVersion != null

fun Project.configureJvmToolchain(jdkVersion: JdkMajorVersion) {
    @Suppress("NAME_SHADOWING")
    val jdkVersion = chooseJdk_1_8ForJpsBuild(jdkVersion)
    // Ensure java only modules also set default toolchain
    configureJavaOnlyToolchain(jdkVersion)

    plugins.withId("org.jetbrains.kotlin.jvm") {
        val kotlinExtension = extensions.getByType<KotlinTopLevelExtension>()

        if (shouldOverrideObsoleteJdk(jdkVersion)) {
            kotlinExtension.jvmToolchain {
                setupToolchain(jdkVersion.overrideVersion ?: error("Substitution version should be defined for override mode"))
            }
            updateJvmTarget(jdkVersion.targetName)
        } else {
            kotlinExtension.jvmToolchain {
                setupToolchain(jdkVersion)
            }
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
        if (shouldOverrideObsoleteJdk(jdkVersion)) {
            javaExtension.toolchain {
                setupToolchain(jdkVersion.overrideVersion ?: error("Substitution version should be defined for override mode"))
            }
            tasks.withType<JavaCompile>().configureEach {
                targetCompatibility = jdkVersion.targetName
                sourceCompatibility = jdkVersion.targetName
            }
        } else {
            javaExtension.toolchain {
                setupToolchain(jdkVersion)
            }
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

fun KotlinCompile.configureTaskToolchain(
    jdkVersion: JdkMajorVersion
) {
    if (project.shouldOverrideObsoleteJdk(jdkVersion)) {
        kotlinJavaToolchain.toolchain.use(
            project.getToolchainLauncherFor(
                jdkVersion.overrideVersion ?: error("Substitution version should be defined for override mode")
            )
        )
        @Suppress("DEPRECATION")
        kotlinOptions {
            jvmTarget = jdkVersion.targetName
        }
    } else {
        kotlinJavaToolchain.toolchain.use(
            project.getToolchainLauncherFor(jdkVersion)
        )
    }
}

fun JavaCompile.configureTaskToolchain(
    jdkVersion: JdkMajorVersion
) {
    if (project.shouldOverrideObsoleteJdk(jdkVersion)) {
        javaCompiler.set(
            project.getToolchainCompilerFor(
                jdkVersion.overrideVersion ?: error("Substitution version should be defined for override mode")
            )
        )
        targetCompatibility = jdkVersion.targetName
        sourceCompatibility = jdkVersion.targetName
    } else {
        javaCompiler.set(project.getToolchainCompilerFor(jdkVersion))
    }
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
        .withType<KotlinCompile>()
        .matching { it.name != "compileJava9Kotlin" }
        .configureEach {
            @Suppress("DEPRECATION")
            kotlinOptions.jvmTarget = jvmTarget
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
    val jdkVersionWithOverride = project.getJdkVersionWithOverride(jdkVersion)
    return service.launcherFor {
        this.languageVersion.set(JavaLanguageVersion.of(jdkVersionWithOverride.majorVersion))
    }
}

fun Project.getToolchainJdkHomeFor(jdkVersion: JdkMajorVersion): Provider<String> {
    return getToolchainLauncherFor(jdkVersion).map {
        it.metadata.installationPath.asFile.absolutePath
    }
}


fun Project.getJdkVersionWithOverride(jdkVersion: JdkMajorVersion): JdkMajorVersion {
    return if (project.shouldOverrideObsoleteJdk(jdkVersion)) {
        jdkVersion.overrideVersion ?: error("Substitution version should be defined for override mode")
    } else {
        jdkVersion
    }
}
