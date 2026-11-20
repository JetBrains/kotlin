import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
}

/*
 * Precompiled `*.settings.gradle.kts` plugins are compiled by the Kotlin version pinned with `compilerVersion`.
 * Kotlin versions since 2.2.0, where LightTree  became the default K2 frontend mode, and until 2.3.20, where scripts
 * became supported in LightTree (KT-77584), do not compile scripts at all. Instead, they emit a warning and produce jars without
 * the script plugin classes.
 * This affects builds in `kotlin-native/performance`, which share settings conventions, but use Gradle 9.0.0 with embedded Kotlin 2.2.0.
 */
private val embeddedKotlinSupportsScriptsInLightTree: Boolean =
    embeddedKotlinVersion.split('.').map { it.toInt() }
        .let { it[0] > 2 || (it[0] == 2 && (it[1] > 3 || (it[1] == 3 && it[2] >= 20))) }

private val effectiveKotlinVersion: String =
    if (embeddedKotlinSupportsScriptsInLightTree) embeddedKotlinVersion else "2.3.20"

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalBuildToolsApi::class)
    compilerVersion = effectiveKotlinVersion
    @Suppress("DEPRECATION")
    coreLibrariesVersion = embeddedKotlinVersion
    jvmToolchain(17)
    compilerOptions.allWarningsAsErrors = true
}

dependencies {
    implementation(kotlinBuildHelpers())
    implementation(libs.develocity.gradlePlugin)
    implementation(libs.gradle.customUserData.gradlePlugin)
}

listOf(
    org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME + "Main",
    "compilePluginsBlocksPluginClasspathElements",
).forEach { confName ->
    project.configurations.named(confName) {
        resolutionStrategy {
            eachDependency {
                if (this.requested.group == "org.jetbrains.kotlin") useVersion(effectiveKotlinVersion)
            }
        }
    }
}
