import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
}

repositories {
    mavenLocal()
    mavenCentral()
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.languageVersion.set(KotlinVersion.KOTLIN_2_0)
    compilerOptions.freeCompilerArgs.add("-XXLanguage:+ErrorAboutDataClassCopyVisibilityChange")
    compilerOptions.freeCompilerArgs.add("-XXLanguage:+DataClassCopyRespectsConstructorVisibility")
}
