plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

description = "Shared utilities for ArchUnit-based architecture tests of the Kotlin Gradle Plugin modules"

dependencies {
    api(libs.archunit)
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$bootstrapKotlinVersion")
}
