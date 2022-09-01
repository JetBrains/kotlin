description = "Kotlin Value Container Assignment Compiler Plugin (CLI)"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":kotlin-value-container-assignment-compiler-plugin.common"))
    api(project(":kotlin-value-container-assignment-compiler-plugin.k1"))
    api(project(":kotlin-value-container-assignment-compiler-plugin.k2"))
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:fir:entrypoint"))
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
