plugins {
    id("kotlin")
}

description = "Contains a dependency module for compiler:integration-tests"

kotlin.jvmToolchain(11)

dependencies {
    implementation(project(":kotlin-stdlib"))

    implementation(platform(libs.junit.bom))
    implementation(libs.junit4)
}