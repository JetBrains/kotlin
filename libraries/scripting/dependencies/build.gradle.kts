plugins {
    kotlin("jvm")
    id("jps-compatible")
}

project.updateJvmTarget("1.6")

dependencies {
    api(kotlinStdlib())
    api(project(":kotlin-scripting-common"))
    testApi(commonDependency("junit"))
    testImplementation(commonDependency("org.jetbrains.kotlinx", "kotlinx-coroutines-core"))
    kotlinCompilerClasspath(project(":libraries:tools:stdlib-compiler-classpath"))
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
