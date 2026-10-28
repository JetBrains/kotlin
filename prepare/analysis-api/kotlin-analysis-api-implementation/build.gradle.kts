plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    `java-library`
    id("analysis-api-artifact")
}

val analysisApiSurfaceDependencies: List<String> = CompilerModules.analysisApiSurfaceDependencies
val compilerModules: Array<String> = CompilerModules.compilerModules
val analysisApiSurfaceModules: Array<String> = CompilerModules.analysisApiSurfaceModules
val analysisApiSyntaxSurfaceModules: Array<String> = CompilerModules.analysisApiSyntaxSurfaceModules
val analysisApiSyntaxImplementationModules: Array<String> = CompilerModules.analysisApiSyntaxImplementationModules
val analysisApiPlatformInterfaceModules: Array<String> = CompilerModules.analysisApiPlatformInterfaceModules
val analysisApiStandaloneModules: Array<String> = CompilerModules.analysisApiStandaloneModules
val analysisApiModules: Array<String> = CompilerModules.analysisApiModules

val additionalCompilerProjects = listOf(
    ":kotlin-annotations-jvm",
)

val excludedCompilerProjects = listOf(
    // JS and Wasm backends
    ":compiler:backend.js",
    ":compiler:backend.wasm",
    ":js:js.parser",
    ":js:js.sourcemap",
    ":js:js.translator",
    ":js:typescript-export-model",
    ":js:typescript-printer",
    ":wasm:wasm.ir",
    ":compiler:ir.serialization.js",

    // K1
    ":core:descriptors.runtime",
    ":native:frontend.native",

    // Light tree and multiplatform parsing
    ":compiler:fir:raw-fir:light-tree2fir",
    ":compiler:fir:raw-fir:mp-parsing2fir",
    ":compiler:multiplatform-parsing",
    ":compiler:java-direct",

    // CLI
    ":compiler:cli-js",
    ":compiler:cli-jklib",
    ":compiler:cli-metadata",

    // Other
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
    implementation(project(":prepare:analysis-api:kotlin-analysis-api-syntax-implementation"))

    // Diagnostics are shipped separately, but the FIR implementation instantiates them, so they are required at runtime.
    implementation(project(":prepare:analysis-api:kotlin-analysis-api-fir-diagnostics"))

    implementation(libs.kotlinx.serialization.core)
    implementation(libs.antlr.runtime)
    implementation(project(":kotlin-tooling-core"))

    implementation(project(":kotlin-scripting-compiler")) {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
    }

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

            // Avoid copying content of 'kotlin-analysis-api-syntax-surface'
            removeAll(analysisApiSyntaxSurfaceModules)

            // Avoid copying content of 'kotlin-analysis-api-syntax-implementation'
            removeAll(analysisApiSyntaxImplementationModules)

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
