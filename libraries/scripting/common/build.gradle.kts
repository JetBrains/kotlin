plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

dependencies {
    compile(kotlinStdlib())
    compile(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core"))
    compileOnly(project(":kotlin-reflect-api"))
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
