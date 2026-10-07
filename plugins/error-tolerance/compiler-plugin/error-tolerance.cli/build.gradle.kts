description = "Kotlin Error Tolerance Compiler Plugin (CLI)"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:fir:fir2ir"))
    compileOnly(project(":compiler:ir.backend.common"))

    implementation(project(":plugins:error-tolerance:compiler-plugin:error-tolerance.k2"))
    implementation(project(":plugins:error-tolerance:compiler-plugin:error-tolerance.backend"))
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
sourcesJar()
javadocJar()
