plugins {
    kotlin("jvm")
}

dependencies {
    implementation(intellijCore())
    implementation(project(":compiler:cli-base"))
    implementation(project(":compiler:cli-common"))
    implementation(project(":core:compiler.common.native"))
    implementation(project(":core:descriptors"))
    implementation(project(":native:base"))
    implementation(project(":native:kotlin-native-utils"))

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(project(":compiler:tests-common", "tests-jar"))

    testRuntimeOnly(libs.junit.jupiter.engine)
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi")
    }
}