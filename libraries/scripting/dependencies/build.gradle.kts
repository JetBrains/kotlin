plugins {
    kotlin("jvm")
    id("jps-compatible")
}

project.updateJvmTarget("1.6")

dependencies {
    compile(kotlinStdlib())
    compile(project(":kotlin-scripting-common"))
    testCompile(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core"))
    testCompile(commonDep("junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions.freeCompilerArgs += listOf(
        "-Xallow-kotlin-package", "-Xsuppress-deprecated-jvm-target-warning"
    )
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
testsJar()
