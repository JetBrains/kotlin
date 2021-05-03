plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    ios()

    val commonMain by sourceSets.getting
    commonMain.dependencies {
        implementation(project(":p1"))
    }
}