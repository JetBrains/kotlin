description = "Kotlin Power-Assert Compiler Plugin (CLI)"

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:fir:entrypoint"))
    compileOnly(project(":compiler:ir.backend.common"))

    implementation(project(":kotlin-power-assert-compiler-plugin.backend"))
    implementation(project(":kotlin-power-assert-compiler-plugin.frontend"))
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
sourcesJar()
javadocJar()
