buildscript {
    apply(from = "$rootDir/kotlin-native/gradle/kotlinGradlePlugin.gradle")
}

plugins {
    kotlin("jvm")
}

repositories {
    extra["bootstrapKotlinRepo"]?.let {
        maven(url = it)
    }
}

dependencies {
    implementation(kotlinStdlib())
    implementation(project(":kotlin-native:backend.native", "cli_bcApiElements"))
    implementation(project(":kotlin-native:utilities:basic-utils"))
    implementation(project(":kotlin-util-klib-abi"))
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions {
        freeCompilerArgs += "-Xskip-prerelease-check"
    }
}
