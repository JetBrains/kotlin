description = "Kotlin Scripting Compiler JS Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:psi"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:backend.js"))
    compileOnly(project(":core:descriptors.runtime"))
    compileOnly(project(":compiler:ir.tree.impl"))
    compileOnly(project(":kotlin-reflect-api"))
    api(project(":kotlin-scripting-common"))
    api(project(":kotlin-scripting-js"))
    api(project(":kotlin-util-klib"))
    api(project(":kotlin-scripting-compiler"))
    api(kotlinStdlib())
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs - "-progressive" + "-Xskip-metadata-version-check"
    }
}