import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Infrastructure of transformations over SIR"

kotlin {
    explicitApi()
}

dependencies {
    compileOnly(kotlinStdlib())

    api(project(":native:swift:sir"))

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