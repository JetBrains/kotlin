import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "SIR Providers - family of classes, that transforms KaSymbol into corresponding SIR nodes"

kotlin {
    explicitApi()
}

dependencies {
    compileOnly(kotlinStdlib())

    implementation(project(":native:swift:sir"))
    implementation(project(":analysis:analysis-api"))
    implementation(project(":compiler:psi"))
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
