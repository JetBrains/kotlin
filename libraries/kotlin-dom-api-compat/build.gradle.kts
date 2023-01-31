import plugins.configureDefaultPublishing
import plugins.configureKotlinPomAttributes
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages

plugins {
    `maven-publish`
    kotlin("js")
}

val jsStdlibSources = "${projectDir}/../stdlib/js/src"

kotlin {
    js(IR) {
        // it is necessary because standard configuration api is resolved during configuration phase
        // making unusable unzip it before kotlin-stdlib-js assembled
        val jsApi = configurations.maybeCreate(
            "jsApi"
        ).apply {
            isVisible = false
            isCanBeResolved = true
            isCanBeConsumed = false
            attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, KotlinUsages.KOTLIN_API))
        }

        compilations["main"].compileKotlinTaskProvider.configure {
            libraries.setFrom(jsApi)
        }

        compilations["test"].compileKotlinTaskProvider.configure {
            libraries.setFrom(jsApi)
        }

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

dependencies {
    "jsApi"(project(":kotlin-stdlib-js"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile>().configureEach {
    compilerOptions.freeCompilerArgs
        .addAll(
            "-Xallow-kotlin-package",
            "-opt-in=kotlin.ExperimentalMultiplatform",
            "-opt-in=kotlin.contracts.ExperimentalContracts",
            // TODO: Better to use friendPaths property, but it does not work
            //  KT-56690
            "-Xfriend-modules=${libraries.joinToString(File.pathSeparator) { it.absolutePath }}"
        )
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