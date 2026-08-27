plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    `java-library`
    id("analysis-api-artifact")
}

val analysisApiSurfaceDependencies: List<String> = CompilerModules.analysisApiSurfaceDependencies
val compilerModules: Array<String> = CompilerModules.compilerModules
val analysisApiSurfaceModules: Array<String> = CompilerModules.analysisApiSurfaceModules
val analysisApiPlatformInterfaceModules: Array<String> = CompilerModules.analysisApiPlatformInterfaceModules
val analysisApiStandaloneModules: Array<String> = CompilerModules.analysisApiStandaloneModules
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

    // Diagnostics are shipped separately, but the FIR implementation instantiates them, so they are required at runtime.
    implementation(project(":prepare:analysis-api:kotlin-analysis-api-fir-diagnostics"))

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

            // Avoid copying content of 'kotlin-analysis-api-platform-interface'
            removeAll(analysisApiPlatformInterfaceModules)

            // Standalone modules are shipped by the dedicated standalone artifacts
            removeAll(analysisApiStandaloneModules)

            // Avoid copying content of 'kotlin-analysis-api-fir-diagnostics'
            remove(":analysis:analysis-api-fir-diagnostics")
        }

        projects(implementationProjects)
    }
}
