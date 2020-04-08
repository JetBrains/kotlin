import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
   kotlin("jvm") version "1.2.50"
}

val compileKotlin: KotlinCompile by tasks

compileKotlin.kotlinOptions {
   languageVersion = "1.0"
}
