plugins {
    kotlin("jvm")
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
}

dependencies {
    api(project(":compiler:ir.serialization.common"))
    api(project(":compiler:ir.tree"))
    api(project(":core:names"))
    api(project(":kotlin-stdlib"))
    implementation(project(":compiler:util"))
    implementation(project(":core:compiler.common"))
    implementation(project(":core:language.model"))
    implementation(project(":core:util.runtime"))
    implementation(project(":core:compiler.common.native"))
    implementation(project(":compiler:ir.serialization.native"))
    implementation(project(":core:descriptors"))
    implementation(project(":kotlin-util-klib-metadata"))
    api(project(":kotlinx-metadata-klib"))
    api(project(":kotlin-metadata"))
    implementation(project(":native:kotlin-native-utils"))
    compileOnly(libs.kotlinx.coroutines.core.jvm)
}

optInToUnsafeDuringIrConstructionAPI()
