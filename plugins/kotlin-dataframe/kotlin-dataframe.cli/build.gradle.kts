description = "Kotlin DataFrame Compiler Plugin (CLI)"

plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":kotlin-dataframe-compiler-plugin.common"))
    api(project(":kotlin-dataframe-compiler-plugin.backend"))
    api(project(":kotlin-dataframe-compiler-plugin.k2"))
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:fir:entrypoint"))
    compileOnly(intellijCore())
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

standardPublicJars()
