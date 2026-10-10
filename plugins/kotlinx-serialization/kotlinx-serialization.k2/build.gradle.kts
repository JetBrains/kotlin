description = "Kotlin Serialization Compiler Plugin (K2)"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":compiler:fir:plugin.api"))
    // Required to access 'FirMetadataSerializerPlugin' internal FIR extension.
    compileOnly(project(":compiler:fir:fir-serialization"))
    // TODO(KT-90146): Required to access various utility functions.
    compileOnly(project(":compiler:fir:fir-deserialization"))
    // TODO(KT-90146): Required to access various utility functions.
    compileOnly(project(":compiler:fir:checkers:checkers.common"))

    compileOnly(project(":compiler:cli-base"))
    compileOnly(project(":native:native.config"))

    implementation(project(":kotlinx-serialization-compiler-plugin.common"))
    implementation(project(":compiler:frontend.common-psi"))
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":core:compiler.common.jvm"))

    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
sourcesJar()
javadocJar()
