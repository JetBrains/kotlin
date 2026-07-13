plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    `java-library`
    id("analysis-api-artifact")
}

val analysisApiSurfaceDependencies: List<String> = CompilerModules.analysisApiSurfaceDependencies
val compilerModules: Array<String> = CompilerModules.compilerModules
val analysisApiSurfaceModules: Array<String> = CompilerModules.analysisApiSurfaceModules
val analysisApiModules: Array<String> = CompilerModules.analysisApiModules

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

    embedded(protobufFull())
}

analysisApiArtifact {
    content {
        val implementationProjects = buildSet {
            addAll(compilerModules)
            addAll(additionalCompilerProjects)
            addAll(analysisApiModules)

            removeAll(excludedCompilerProjects)

            // Avoid copying content of 'kotlin-analysis-api-surface'
            removeAll(analysisApiSurfaceDependencies)
            removeAll(analysisApiSurfaceModules)
        }

        projects(implementationProjects)
    }
}
