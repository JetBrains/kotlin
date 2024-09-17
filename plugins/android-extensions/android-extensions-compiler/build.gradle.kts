description = "Kotlin Android Extensions Compiler"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testApi(intellijCore())

    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":compiler:backend.jvm"))
    compileOnly(project(":compiler:ir.tree"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":kotlin-android-extensions-runtime"))
    compileOnly(intellijCore())
    compileOnly(libs.intellij.asm)

    embedded(project(":kotlin-android-extensions-runtime")) { isTransitive = false }
}

optInToExperimentalCompilerApi()
optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
}

runtimeJar()

sourcesJar()

javadocJar()
