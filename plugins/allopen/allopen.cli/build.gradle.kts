description = "Kotlin AllOpen Compiler Plugin (CLI)"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    implementation(project(":kotlin-allopen-compiler-plugin.common"))
    implementation(project(":kotlin-allopen-compiler-plugin.k2"))
    compileOnly(project(":compiler:plugin-api"))

    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":compiler:fir:plugin.api"))

    compileOnly(intellijCore())

    runtimeOnly(kotlinStdlib())
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
sourcesJar()
javadocJar()
