description = "Kotlin Power-Assert Compiler Plugin (CLI)"

plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:ir.backend.common"))

    implementation(project(":kotlin-power-assert-compiler-plugin.backend"))
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
sourcesJar()
javadocJar()
