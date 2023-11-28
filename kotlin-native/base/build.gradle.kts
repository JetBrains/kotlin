import org.jetbrains.kotlin.gradle.utils.extendsFrom

plugins {
    kotlin("jvm")
    id("native-interop-plugin")
}

dependencies {
    implementation(intellijCore())
    implementation(project(":compiler:cli-base"))
    implementation(project(":compiler:cli-common"))
    implementation(project(":compiler:ir.tree"))
    implementation(project(":core:compiler.common.native"))
    implementation(project(":core:descriptors"))
    implementation(project(":kotlin-compiler"))
    implementation(project(":kotlin-native:backend.native", "llvmInteropStubs"))
    implementation(project(":kotlin-native:utilities:basic-utils"))
    implementation(project(":native:kotlin-native-utils"))
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi")
        optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
        optIn.add("org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI")
    }
}
