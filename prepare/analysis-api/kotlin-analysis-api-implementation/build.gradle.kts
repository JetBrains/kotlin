import org.gradle.kotlin.dsl.project

plugins {
    `java-library`
}

val analysisApiSurfaceDependencies: List<String> by rootProject.extra
val compilerModules: Array<String> by rootProject.extra

val analysisApiImplementationProjects = listOf(
    ":analysis:analysis-api-impl-base",
    ":analysis:analysis-api-fir",
    ":analysis:analysis-api-standalone:analysis-api-standalone-base",
    ":analysis:analysis-api-standalone:analysis-api-fir-standalone-base",
    ":analysis:analysis-internal-utils",
    ":analysis:low-level-api-fir",
    ":analysis:symbol-light-classes",
)

val additionalCompilerProjects = listOf(
    ":kotlin-annotations-jvm",
    ":kotlin-script-runtime",
    ":kotlin-scripting-common",
    ":kotlin-scripting-dependencies",
    ":kotlin-scripting-jvm",
    ":kotlin-scripting-compiler",
    ":kotlin-scripting-compiler-impl",
)

val excludedCompilerProjects = listOf(
    ":compiler:javac-wrapper",
    ":compiler:incremental-compilation-impl",
    ":compiler:build-tools:kotlin-build-statistics",
    ":kotlin-compiler-runner-unshaded",
    ":daemon-common",
    ":kotlin-daemon-client",
    ":kotlin-build-common",
)

dependencies {
    api(project(":prepare:analysis-api:kotlin-analysis-api-surface"))
    api(project(":prepare:analysis-api:kotlin-analysis-api-platform-interface"))

    implementation(project(":prepare:analysis-api:kotlin-analysis-api-intellij-implementation-components"))
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.antlr.runtime)

    val embeddedProjects = buildList {
        addAll(compilerModules)
        addAll(additionalCompilerProjects)
        removeAll(excludedCompilerProjects)
        removeAll(analysisApiImplementationProjects)
        addAll(analysisApiImplementationProjects)
    }

    for (projectPath in embeddedProjects) {
        embedded(project(projectPath)) { isTransitive = false }
    }

    embedded(protobufFull())
}

publishAnalysisApiArtifact()