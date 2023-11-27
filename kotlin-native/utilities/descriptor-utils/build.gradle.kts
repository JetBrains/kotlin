plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":kotlin-native:utilities:annotations"))
    implementation(project(":core:descriptors"))
}