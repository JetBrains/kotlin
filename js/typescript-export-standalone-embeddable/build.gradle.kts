plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    embedded(libs.caffeine) { isTransitive = false }
    embedded(libs.kotlinx.serialization.core.jvm) { isTransitive = false }
    embedded(project(":js:typescript-export-standalone")) { isTransitive = false }
    embedded(project(":analysis:analysis-api-standalone")) { isTransitive = false }
    embedded(project(":analysis:analysis-api-standalone:analysis-api-standalone-fir")) { isTransitive = false }
    embedded(project(":libraries:tools:analysis-api-based-klib-reader")) { isTransitive = false }
    embedded(project(":analysis:decompiled:decompiler-to-file-stubs")) { isTransitive = false }
    embedded(project(":analysis:decompiled:decompiler-to-psi")) { isTransitive = false }
    embedded(project(":analysis:analysis-api")) { isTransitive = false }
    embedded(project(":analysis:analysis-api-fir")) { isTransitive = false }
    embedded(project(":analysis:analysis-api-impl-base")) { isTransitive = false }
    embedded(project(":analysis:analysis-api-platform-interface")) { isTransitive = false }
    embedded(project(":analysis:low-level-api-fir")) { isTransitive = false }
    embedded(project(":analysis:light-classes-base")) { isTransitive = false }
    embedded(project(":analysis:symbol-light-classes")) { isTransitive = false }
    embedded(project(":analysis:decompiled:decompiler-js")) { isTransitive = false }
    embedded(project(":analysis:decompiled:decompiler-native")) { isTransitive = false }
    embedded(project(":analysis:decompiled:light-classes-for-decompiled")) { isTransitive = false }
}

publish {
    artifactId = "typescript-export-standalone-embeddable"
}

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
sourcesJar()
javadocJar()
