import plugins.configureDefaultPublishing
import plugins.configureKotlinPomAttributes

plugins {
    `maven-publish`
    kotlin("multiplatform")
}

val jsStdlibSources = "${projectDir}/../stdlib/js/src"

@Suppress("UNUSED_VARIABLE")
kotlin {
    explicitApi()
    js()

    sourceSets {
        jsMain {
            if (!kotlinBuildProperties.isInIdeaSync) {
                kotlin.srcDir("$jsStdlibSources/org.w3c")
                kotlin.srcDir("$jsStdlibSources/kotlinx")
                kotlin.srcDir("$jsStdlibSources/kotlin/browser")
                kotlin.srcDir("$jsStdlibSources/kotlin/dom")
            }
            dependencies {
                api(project(":kotlin-stdlib"))
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
    val renderDiagnosticNames by extra(project.kotlinBuildProperties.renderDiagnosticNames)
    if (renderDiagnosticNames) {
        compilerOptions.freeCompilerArgs.add("-Xrender-internal-diagnostic-names")
    }
    friendPaths.from(libraries)
    compilerOptions.allWarningsAsErrors.set(true)
}

val emptyJavadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        val mavenPublication = register<MavenPublication>("maven") {
            // FIXME: Remove customized publication in KT-83065
            from(kotlin.js().components.single())
            configureKotlinPomAttributes(project, "Kotlin DOM API compatibility library", packaging = "klib")
        }
        withType<MavenPublication> {
            artifact(emptyJavadocJar)
        }
        configureSbom(
            target = "Maven",
            gradleConfigurations = setOf(),
            publication = mavenPublication,
        )
    }
}

configureDefaultPublishing()
