plugins {
    kotlin("jvm")
    id("jps-compatible")
}

project.updateJvmTarget("1.8")

dependencies {
    api(kotlinStdlib())
    api(project(":kotlin-scripting-common"))
    testImplementation(libs.junit4)
    testImplementation(commonDependency("org.jetbrains.kotlinx", "kotlinx-coroutines-core"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions.freeCompilerArgs += listOf(
        "-Xallow-kotlin-package"
    )
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
testsJar()
