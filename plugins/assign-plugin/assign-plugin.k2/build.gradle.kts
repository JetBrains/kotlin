description = "Kotlin Assignment Compiler Plugin (K2)"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    implementation(project(":kotlin-assignment-compiler-plugin.common"))
    implementation(project(":compiler:frontend.common-psi"))
    implementation(project(":compiler:psi:psi-api"))

    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":compiler:fir:plugin.api"))
    // Needed for internal 'FirAssignExpressionAltererExtension' FIR extension.
    compileOnly(project(":compiler:fir:resolve"))
    // TODO(KT-90146): Required to access various utility functions.
    compileOnly(project(":compiler:fir:checkers:checkers.common"))

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
