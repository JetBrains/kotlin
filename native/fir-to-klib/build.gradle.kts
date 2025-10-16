plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(intellijCore())
    implementation(project(":core:compiler.common.native"))
    implementation(project(":compiler:cli"))
    implementation(project(":compiler:cli-common"))
    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:ir.backend.native"))
    implementation(project(":compiler:ir.serialization.native"))
    implementation(project(":compiler:util"))
    implementation(project(":compiler:fir:fir-serialization"))
    implementation(project(":compiler:fir:fir-native"))
    implementation(project(":compiler:ir.objcinterop"))
    implementation(project(":native:frontend.native"))
}

optInToUnsafeDuringIrConstructionAPI()