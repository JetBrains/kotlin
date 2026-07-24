import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    implementation(kotlinStdlib())
    compileOnly(libs.intellij.asm)
    compileOnly(intellijPlatformUtil())
}

sourceSets {
    "main" { projectDefault() }
}
