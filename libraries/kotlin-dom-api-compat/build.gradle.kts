import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import plugins.configureDefaultPublishing
import plugins.configureKotlinPomAttributes

plugins {
    `maven-publish`
    kotlin("js")
}

val jsStdlibSources = "${projectDir}/../stdlib/js/src"

kotlin {
    js(IR) {
        @Suppress("UNUSED_VARIABLE")
        sourceSets {
            val main by getting {
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
        val main by compilations.getting
        val test by compilations.getting
        // TODO: Remove together with kotlin.js.compiler.publish.attribute=false property
        listOf(main, test).forEach { compilation ->
            listOf(compilation.compileDependencyConfigurationName, compilation.runtimeDependencyConfigurationName).forEach { configurationName ->
                configurations[configurationName].attributes {
                    attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
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
        val mavenPublication = register<MavenPublication>("maven") {
            from(components["kotlin"])
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