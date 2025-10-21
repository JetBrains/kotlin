description = "Kotlin Serialization Compiler Plugin (CLI)"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:ir.tree"))
    compileOnly(project(":compiler:fir:entrypoint"))
    compileOnly(project(":kotlin-util-klib-metadata"))

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
