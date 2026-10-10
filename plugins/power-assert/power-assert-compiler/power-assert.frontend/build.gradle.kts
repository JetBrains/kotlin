description = "Kotlin Power-Assert Compiler Plugin (Frontend)"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":compiler:fir:plugin.api"))
    // TODO(KT-90146): Required to access various utility functions.
    compileOnly(project(":compiler:fir:checkers:checkers.common"))

    implementation(project(":kotlin-power-assert-compiler-plugin.common"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
sourcesJar()
javadocJar()
