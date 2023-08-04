import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    kotlin("multiplatform")
}

repositories {
	mavenLocal()
    mavenCentral()
}

kotlin {
    jvm()

    sourceSets {
        all {
            languageSettings.optIn("foo.bar.MyOptIn")
        }
    }
}

