description = "Lombok compiler plugin"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    implementation(project(":compiler:frontend.common-psi"))
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":core:compiler.common.jvm"))

    compileOnly(project(":compiler:fir:plugin.api"))
    // Required for JVM specific FIR elements.
    compileOnly(project(":compiler:fir:fir-jvm"))
    // TODO(KT-90146): Required to access various utility functions.
    compileOnly(project(":compiler:fir:resolve"))
    // TODO(KT-90146): Required to access various utility functions.
    // Required to access FirError.
    compileOnly(project(":compiler:fir:checkers:checkers.common"))

    compileOnly(project(":compiler:ir.tree"))
    compileOnly(project(":compiler:ir.backend.common"))

    compileOnly(intellijCore())
    runtimeOnly(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
sourcesJar()
javadocJar()
