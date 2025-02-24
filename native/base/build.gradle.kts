plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":compiler:cli-base"))
    implementation(project(":compiler:cli-common"))
    implementation(project(":core:compiler.common.native"))
    implementation(project(":core:descriptors"))
    implementation(project(":native:frontend.native"))
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi")
    }
}
