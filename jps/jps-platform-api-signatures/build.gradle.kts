import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
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
