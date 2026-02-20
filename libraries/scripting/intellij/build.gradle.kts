import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("root-config")
    kotlin("jvm")
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

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.freeCompilerArgs.addAll(
        listOf(
            "-Xallow-kotlin-package",
            "-jvm-default=no-compatibility",
        )
    )
}

publish()

standardPublicJars()
