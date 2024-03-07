import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
}

description = "SIR wrappers over KtSymbol"

kotlin {
    explicitApi()
}

dependencies {
    compileOnly(kotlinStdlib())

    api(project(":compiler:psi"))
    api(project(":native:swift:sir"))
    api(project(":analysis:analysis-api"))
}

sourceSets {
    "main" { projectDefault() }
}

tasks.withType<KotlinJvmCompile> {
    compilerOptions.freeCompilerArgs.add("-Xcontext-receivers")
}
