plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    embedded(project(":js:typescript-export-standalone")) { isTransitive = false }
    embedded(project(":libraries:tools:analysis-api-based-klib-reader")) { isTransitive = false }

    embedded(libs.caffeine) { isTransitive = false }
    embedded(libs.kotlinx.serialization.core.jvm) { isTransitive = false }

    // FIXME: Stop embedding Analysis API after KT-61404
    val lowLevelApiFir = ":analysis:low-level-api-fir"
    val analysisApiFir = ":analysis:analysis-api-fir"
    embedded(project(":analysis:analysis-api")) { isTransitive = false }
    embedded(project(analysisApiFir)) { isTransitive = false }
    embedded(project(":analysis:analysis-api-impl-base")) { isTransitive = false }
    embedded(project(":analysis:analysis-api-platform-interface")) { isTransitive = false }
    embedded(project(":analysis:analysis-api-standalone")) { isTransitive = false }
    embedded(project(":analysis:analysis-api-standalone:analysis-api-standalone-fir")) { isTransitive = false }
    embedded(project(lowLevelApiFir)) { isTransitive = false }
    embedded(project(":analysis:light-classes-base")) { isTransitive = false }
    embedded(project(":analysis:symbol-light-classes")) { isTransitive = false }
    embedded(project(":analysis:analysis-internal-utils")) { isTransitive = false }
    embedded(project(":analysis:decompiled:decompiler")) { isTransitive = false }
    embedded(project(":analysis:decompiled:light-classes-for-decompiled")) { isTransitive = false }
}

publish {
    artifactId = "typescript-export-standalone-embeddable"
}

description = "Standalone embeddable runner for TypeScript Export"

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
sourcesJar()
javadocJar()
