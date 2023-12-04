import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    dependencies {
        classpath("com.google.code.gson:gson:2.8.9")
    }
}

plugins {
    kotlin("jvm")
}

val isNativeBuildToolsProject = rootProject.name == "native-build-tools"

if (!isNativeBuildToolsProject) {
    // The module is shared between the main project and 'native-build-tools',
    // in which there is no 'jps-compatible' plugin configured.
    apply(plugin = "jps-compatible")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.8.9")
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "com.google.code.gson" && requested.name == "gson") {
                useVersion("2.8.9")
                because("Force using same gson version because of https://github.com/google/gson/pull/1991")
            }
        }
    }

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")

    // KT-61897: Workaround for https://github.com/gradle/gradle/issues/26358
    // (wrong conflict resolution, causing selection of not the latest version of `:kotlin-util-klib` module)
    if (isNativeBuildToolsProject) {
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

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs += listOf(
                "-Xskip-prerelease-check",
                "-Xsuppress-version-warnings",
                "-opt-in=kotlin.ExperimentalStdlibApi",
                "-opt-in=kotlin.RequiresOptIn"
        )
    }
}
