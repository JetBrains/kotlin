plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    sourceSets.getByName("jvmMain").dependencies {
        implementation(project(":jvm"))
    }
}
