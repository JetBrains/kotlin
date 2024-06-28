import org.gradle.api.internal.file.archive.ZipFileTree
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import plugins.configureDefaultPublishing
import plugins.configureKotlinPomAttributes

plugins {
    `maven-publish`
    kotlin("js")
}

val jsStdlibSources = "${projectDir}/../stdlib/js/src"

val kotlinStdlibJs by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
    }
}

dependencies {
    kotlinStdlibJs(kotlinStdlib())
}

// Workaround for #KT-65266
val prepareFriendStdlibJs = tasks.register<Zip>("prepareFriendStdlibJs") {
    dependsOn(kotlinStdlibJs)
    from { zipTree(kotlinStdlibJs.singleFile).matching { exclude("META-INF/MANIFEST.MF") } }
    destinationDirectory = layout.buildDirectory.map { it.dir("libs") }
    archiveFileName = "friend-kotlin-stdlib-js.klib"
}

@Suppress("UNUSED_VARIABLE")
kotlin {
    explicitApi()
    js()

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
    dependsOn(prepareFriendStdlibJs)
    libraries.setFrom(prepareFriendStdlibJs)
    friendPaths.setFrom(libraries)
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
