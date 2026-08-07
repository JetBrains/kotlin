import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalBuildToolsApi::class)
    compilerVersion = embeddedKotlinVersion
    coreLibrariesVersion = embeddedKotlinVersion
    jvmToolchain(17)

    compilerOptions {
        allWarningsAsErrors.set(true)
        optIn.add("kotlin.ExperimentalStdlibApi")
    }
}

dependencies {
    implementation(libs.intellij.asm)
    implementation("org.jetbrains.kotlin:kotlin-metadata-jvm:$embeddedKotlinVersion")
    implementation(libs.diff.utils)

    compileOnly(libs.shadow.gradlePlugin)

    testImplementation(kotlin("test"))
    testImplementation(gradleTestKit())
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

/*
In scope of: https://youtrack.jetbrains.com/issue/KT-81629
 */
listOf(
    org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME + "Main",
    "compilePluginsBlocksPluginClasspathElements",
).forEach { confName ->
    project.configurations.named(confName) {
        resolutionStrategy {
            eachDependency {
                if (this.requested.group == "org.jetbrains.kotlin") useVersion(embeddedKotlinVersion)
            }
        }
    }
}
