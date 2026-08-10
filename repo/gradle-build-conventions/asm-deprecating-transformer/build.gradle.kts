import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalBuildToolsApi::class)
    compilerVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation
    jvmToolchain(17)

    compilerOptions {
        allWarningsAsErrors.set(true)
        optIn.add("kotlin.ExperimentalStdlibApi")
    }
}

dependencies {
    compileOnly(kotlin("stdlib", embeddedKotlinVersion))
    implementation(libs.intellij.asm)
    implementation("org.jetbrains.kotlin:kotlin-metadata-jvm:${libs.versions.kotlin.`for`.gradle.plugins.compilation.get()}")
    implementation(libs.diff.utils)
    compileOnly(libs.shadow.gradlePlugin)

    testImplementation(kotlin("test", libs.versions.kotlin.`for`.gradle.plugins.compilation.get()))
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
project.configurations.configureEach {
    if (name.startsWith(org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME)) {
        resolutionStrategy {
            eachDependency {
                if (this.requested.group == "org.jetbrains.kotlin") useVersion(libs.versions.kotlin.`for`.gradle.plugins.compilation.get())
            }
        }
    }
}

kotlin.compilerOptions.moduleName.value(project.name)
