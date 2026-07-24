description = "Kotlin Serialization Compiler Plugin (Backend)"

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":compiler:backend.jvm"))
    compileOnly(project(":compiler:backend.jvm.codegen"))
    compileOnly(project(":compiler:backend.jvm.lower"))
    compileOnly(project(":compiler:ir.tree"))
    compileOnly(project(":compiler:fir:fir2ir"))
    compileOnly(project(":compiler:fir:tree"))
    compileOnly(project(":compiler:fir:fir-deserialization"))
    compileOnly(project(":native:native.config"))
    compileOnly(project(":kotlin-util-klib-metadata"))
    compileOnly(project(":compiler:cli-base"))

    implementation(project(":compiler:backend.common.jvm"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:deserialization"))
    implementation(project(":core:language.targets.jvm"))
    implementation(project(":kotlinx-serialization-compiler-plugin.common"))

    compileOnly(intellijCore())
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
sourcesJar()
javadocJar()
