plugins {
    kotlin("multiplatform")
}

kotlin {
    <SingleNativeTarget>("host")

    sourceSets {
        val commonMain by getting
    }
}
