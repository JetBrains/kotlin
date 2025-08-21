plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":compiler:cli-base"))
    implementation(project(":compiler:cli-common"))
    implementation(project(":core:compiler.common.native"))
    implementation(project(":core:descriptors"))
    implementation(project(":native:frontend.native"))
    // Some binary options are leaking via module API surface
    api(project(":native:binary-options"))
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi")
    }
}
