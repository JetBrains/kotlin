description = "Kotlin Serialization Compiler Plugin (CLI)"

plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:fir:entrypoint"))
    compileOnly(project(":kotlin-util-klib-metadata"))

    implementation(project(":kotlinx-serialization-compiler-plugin.common"))
    implementation(project(":kotlinx-serialization-compiler-plugin.k1"))
    implementation(project(":kotlinx-serialization-compiler-plugin.k2"))
    implementation(project(":kotlinx-serialization-compiler-plugin.backend"))
    implementation(project(":core:descriptors"))
    implementation(project(":compiler:container"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:resolution"))
    implementation(project(":compiler:serialization"))

    compileOnly(intellijCore())
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

optInToK1Deprecation()

runtimeJar()
sourcesJar()
javadocJar()
