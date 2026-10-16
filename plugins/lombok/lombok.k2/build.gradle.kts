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
    // Required to access 'FirErrors.MISSING_DEPENDENCY_CLASS'.
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
