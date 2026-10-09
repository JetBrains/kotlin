description = "Kotlin DataFrame Compiler Plugin (K2)"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":kotlin-dataframe-compiler-plugin.common"))
    implementation(project(":compiler:frontend.common-psi"))
    implementation(project(":compiler:psi:psi-api"))

    compileOnly(project(":compiler:fir:plugin.api"))
    // Required for 'FirFunctionCallRefinementExtension' internal FIR extension.
    compileOnly(project(":compiler:fir:resolve"))
    // Required for JVM specific FIR elements.
    compileOnly(project(":compiler:fir:fir-jvm"))
    // TODO(KT-90146): Required to access various utility functions.
    compileOnly(project(":compiler:fir:checkers:checkers.common"))
    compileOnly(project(":compiler:cli-base"))
    compileOnly(libs.kotlinx.serialization.core)
    compileOnly(libs.kotlinx.serialization.json)

    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

standardPublicJars()
