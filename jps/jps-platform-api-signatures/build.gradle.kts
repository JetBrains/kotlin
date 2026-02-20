import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("root-config")
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
