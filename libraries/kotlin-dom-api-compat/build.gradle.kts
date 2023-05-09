import plugins.configureDefaultPublishing
import plugins.configureKotlinPomAttributes

plugins {
    `maven-publish`
    kotlin("js")
}

val jsStdlibSources = "${projectDir}/../stdlib/js/src"

kotlin {
    js(IR) {
        sourceSets {
            val main by getting {
                if (!kotlinBuildProperties.isInIdeaSync) {
                    kotlin.srcDir("$jsStdlibSources/org.w3c")
                    kotlin.srcDir("$jsStdlibSources/kotlinx")
                    kotlin.srcDir("$jsStdlibSources/kotlin/browser")
                    kotlin.srcDir("$jsStdlibSources/kotlin/dom")
                }
                dependencies {
                    api(project(":kotlin-stdlib-js"))
                }
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile>().configureEach {
    compilerOptions.freeCompilerArgs
        .addAll(
            "-Xallow-kotlin-package",
            "-opt-in=kotlin.ExperimentalMultiplatform",
            "-opt-in=kotlin.contracts.ExperimentalContracts",
        )
    friendPaths.from(libraries)
    compilerOptions.allWarningsAsErrors.set(true)
}

val emptyJavadocJar by tasks.creating(Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
            configureKotlinPomAttributes(project, "Kotlin DOM API compatibility library", packaging = "klib")
        }
        withType<MavenPublication> {
            artifact(emptyJavadocJar)
        }
    }
}

configureDefaultPublishing()