description = "Kotlin Serialization Compiler Plugin (CLI)"

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:fir:entrypoint"))
    compileOnly(project(":kotlin-util-klib-metadata"))

    implementation(project(":kotlinx-serialization-compiler-plugin.common"))
    implementation(project(":kotlinx-serialization-compiler-plugin.k2"))
    implementation(project(":kotlinx-serialization-compiler-plugin.backend"))

    compileOnly(intellijCore())
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
sourcesJar()
javadocJar()
