import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:1.7.10")
}

kotlin {
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_0) // this build produces gradle build logic that should be consumable by old Gradle versions
    }
}
