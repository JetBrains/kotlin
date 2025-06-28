plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":native:kotlin-native-utils"))
    implementation(project(":native:base"))
    implementation(project(":compiler:cli-base"))
    implementation(project(":compiler:cli-common"))
    implementation(project(":compiler:cli"))
    implementation(project(":kotlin-util-klib"))
    implementation(project(":kotlin-util-klib-metadata"))
    api(project(":compiler:ir.backend.common"))
    api(project(":compiler:config"))
}

