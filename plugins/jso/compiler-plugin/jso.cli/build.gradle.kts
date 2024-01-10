description = "Kotlin JavaScript Object Compiler Plugin (CLI)"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:fir:entrypoint"))
    compileOnly(project(":compiler:ir.backend.common"))

    implementation(project(":plugins:jso:compiler-plugin:jso.common"))
    implementation(project(":plugins:jso:compiler-plugin:jso.backend"))
    implementation(project(":plugins:jso:compiler-plugin:jso.k2"))

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
