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
    implementation(project(":core:descriptors"))
    implementation(project(":native:frontend.native"))
    implementation(project(":compiler:ir.psi2ir"))
    implementation(project(":compiler:ir.serialization.native"))
    implementation(project(":kotlin-util-klib-abi"))
    implementation(project(":tools:kotlinp-klib"))
    implementation(project(":kotlinx-metadata-klib")) { isTransitive = false }
    implementation(project(":kotlin-metadata")) { isTransitive = false }
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions {
        freeCompilerArgs += "-Xskip-prerelease-check"
    }
}
