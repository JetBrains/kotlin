plugins {
    kotlin("jvm")
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

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions {
        freeCompilerArgs += "-Xcontext-receivers"
    }
}
