import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Implementation of SwiftIR backed by Analysis API"

kotlin {
    explicitApi()
}

dependencies {
    compileOnly(kotlinStdlib())

    api(project(":native:swift:sir"))
    api(project(":native:swift:sir-providers"))

    api(project(":compiler:psi"))
    api(project(":analysis:analysis-api"))
}

sourceSets {
    "main" { projectDefault() }
}

tasks.withType<KotlinJvmCompile> {
    compilerOptions.freeCompilerArgs.add("-Xcontext-receivers")
}

if (kotlinBuildProperties.isSwiftExportPluginPublishingEnabled) {
    publish()
}

runtimeJar()
sourcesJar()
javadocJar()