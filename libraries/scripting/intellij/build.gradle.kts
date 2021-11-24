
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":kotlin-script-runtime"))
    api(kotlinStdlib())
    api(project(":kotlin-scripting-common"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions.freeCompilerArgs += "-Xallow-kotlin-package"
}

publish()

standardPublicJars()

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xjvm-default=compatibility"
}