import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":native:kotlin-native-utils"))
    implementation(project(":native:base"))
    implementation(project(":compiler:cli-base"))
    implementation(project(":compiler:cli-common"))
    implementation(project(":compiler:cli"))
    implementation(project(":compiler:ir.backend.native"))
    implementation(project(":compiler:ir.serialization.native"))
    implementation(project(":kotlin-util-klib"))
    implementation(project(":kotlin-util-klib-metadata"))
    implementation(project(":compiler:ir.objcinterop"))
    implementation(project(":core:compiler.common.native"))
    implementation(intellijCore())
    api(project(":native:driver-core"))
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.optIn.addAll(
        listOf(
            "org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi",
            "org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI"
        )
    )
}