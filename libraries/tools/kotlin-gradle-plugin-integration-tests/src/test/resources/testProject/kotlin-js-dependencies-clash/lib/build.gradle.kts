plugins {
    kotlin("js")
}

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation(project(":base"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.3.7")
}

kotlin {
    target {
        useCommonJs()
        nodejs {
        }
    }
}