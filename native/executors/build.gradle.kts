import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

buildscript {
    dependencies {
        classpath(libs.gson)
    }
}

plugins {
    kotlin("jvm")
}

val isNativeBuildToolsProject = rootProject.name == "native-build-tools"
val isPerformanceProject = rootProject.name == "performance"

if (!isNativeBuildToolsProject) {
    // The module is shared between the main project and 'native-build-tools',
    // in which there is no 'jps-compatible' plugin configured.
    apply(plugin = "jps-compatible")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.core)

    // KT-61897: Workaround for https://github.com/gradle/gradle/issues/26358
    // (wrong conflict resolution, causing selection of not the latest version of `:kotlin-util-klib` module)
    if (isNativeBuildToolsProject || isPerformanceProject) {
        implementation("org.jetbrains.kotlin:kotlin-native-utils:${project.bootstrapKotlinVersion}")
    } else {
        implementation(project(":native:kotlin-native-utils"))
    }
}

group = "org.jetbrains.kotlin"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        optIn.addAll(
            listOf(
                "kotlin.ExperimentalStdlibApi",
                "kotlin.RequiresOptIn",
            )
        )
        freeCompilerArgs.addAll(
            listOf(
                "-Xskip-prerelease-check",
                "-Xsuppress-version-warnings",
            )
        )
    }
}
