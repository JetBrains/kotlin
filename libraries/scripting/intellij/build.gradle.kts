import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
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
    compilerOptions.jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
    compilerOptions.freeCompilerArgs.addAll(
        listOf(
            "-Xallow-kotlin-package",
        )
    )
}

publish()

standardPublicJars()
