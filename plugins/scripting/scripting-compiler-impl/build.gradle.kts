import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

description = "Kotlin Compiler Infrastructure for Scripting"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:psi:psi-api"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:ir.serialization.js"))
    api(project(":kotlin-scripting-common"))
    api(project(":kotlin-scripting-jvm"))
    api(kotlinStdlib())
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    compileOnly(intellijCore())
    compileOnly(libs.intellij.asm)

    // FIXME: drop after removing references to LocalFileSystem they don't exist in intellij-core
    compileOnly(intellijAnalysis())

    runtimeOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xskip-metadata-version-check")
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
