import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

internal fun Project.configureJvmToolchainsWiring(extension: JvmToolchainsExtension) {
    plugins.withId("java-base") {
        val javaExtension = extensions.getByType<JavaPluginExtension>()
        javaExtension.toolchain {
            languageVersion.set(extension.jdkVersion.map { JavaLanguageVersion.of(it.majorVersion) })
        }

        tasks.withType<JavaCompile>().configureEach {
            if (isClaimedBySourceSetOverride(name, extension, "java")) return@configureEach
            wireJavaTargetAndRelease(extension.jdkVersion, extension.targetBytecodeVersion, extension.jdkApiVersion)
        }
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        val kotlinExtension = extensions.getByType<KotlinBaseExtension>()
        kotlinExtension.jvmToolchain {
            languageVersion.set(extension.jdkVersion.map { JavaLanguageVersion.of(it.majorVersion) })
        }

        tasks.withType<KotlinJvmCompile>().configureEach {
            if (isClaimedBySourceSetOverride(name, extension, "kotlin")) return@configureEach
            wireKotlinTargetAndRelease(extension.jdkVersion, extension.targetBytecodeVersion, extension.jdkApiVersion)
        }
    }

    extension.sourceSetConfigurations.configureEach {
        configureSourceSetOverride(this)
    }
}

private fun Project.isClaimedBySourceSetOverride(taskName: String, extension: JvmToolchainsExtension, language: String): Boolean {
    return extension.sourceSetConfigurations.names.any { sourceSetName ->
        sourceSets.findByName(sourceSetName)?.getCompileTaskName(language) == taskName
    }
}

private fun Project.configureSourceSetOverride(config: SourceSetToolchainConfiguration) {
    plugins.withId("java-base") {
        sourceSets.matching { it.name == config.name }.configureEach {
            val javaTaskName = getCompileTaskName("java")
            tasks.withType<JavaCompile>().matching { it.name == javaTaskName }.configureEach {
                javaCompiler.set(config.jdkVersion.flatMap { getToolchainCompilerFor(it) })
                wireJavaTargetAndRelease(config.jdkVersion, config.targetBytecodeVersion, config.jdkApiVersion)
            }

            val kotlinTaskName = getCompileTaskName("kotlin")
            tasks.withType<KotlinJvmCompile>().matching { it.name == kotlinTaskName }.configureEach {
                kotlinJavaToolchain.toolchain.use(config.jdkVersion.flatMap { getToolchainLauncherFor(it) })
                wireKotlinTargetAndRelease(config.jdkVersion, config.targetBytecodeVersion, config.jdkApiVersion)
            }
        }
    }
}

private fun JavaCompile.wireJavaTargetAndRelease(
    jdkVersion: Provider<JdkMajorVersion>,
    target: Provider<JdkMajorVersion>,
    apiVersion: Provider<JdkMajorVersion>,
) {
    val resolvedJdk = jdkVersion.get()
    val resolvedTarget = target.get()
    val resolvedApi = apiVersion.get()
    validateToolchainVersions(path, resolvedJdk, resolvedTarget, resolvedApi)

    sourceCompatibility = resolvedTarget.targetName
    targetCompatibility = resolvedTarget.targetName
    if (resolvedJdk != resolvedTarget && resolvedApi == resolvedTarget) {
        options.release.set(resolvedTarget.majorVersion)
    }
}

private fun KotlinJvmCompile.wireKotlinTargetAndRelease(
    jdkVersion: Provider<JdkMajorVersion>,
    target: Provider<JdkMajorVersion>,
    apiVersion: Provider<JdkMajorVersion>,
) {
    val resolvedJdk = jdkVersion.get()
    val resolvedTarget = target.get()
    val resolvedApi = apiVersion.get()
    validateToolchainVersions(path, resolvedJdk, resolvedTarget, resolvedApi)

    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(resolvedTarget.targetName))
    if (resolvedJdk != resolvedTarget && resolvedApi == resolvedTarget) {
        compilerOptions.freeCompilerArgs.add("-Xjdk-release=${resolvedTarget.targetName}")
    }
}

private fun validateToolchainVersions(
    label: String,
    jdkVersion: JdkMajorVersion,
    targetBytecodeVersion: JdkMajorVersion,
    jdkApiVersion: JdkMajorVersion,
) {
    check(jdkVersion.majorVersion >= targetBytecodeVersion.majorVersion) {
        "$label: jdkVersion ($jdkVersion) cannot be lower than targetBytecodeVersion ($targetBytecodeVersion)"
    }
    check(jdkApiVersion == targetBytecodeVersion || jdkApiVersion == jdkVersion) {
        "$label: jdkApiVersion ($jdkApiVersion) must be equal to either targetBytecodeVersion " +
            "($targetBytecodeVersion) or jdkVersion ($jdkVersion)"
    }
}
