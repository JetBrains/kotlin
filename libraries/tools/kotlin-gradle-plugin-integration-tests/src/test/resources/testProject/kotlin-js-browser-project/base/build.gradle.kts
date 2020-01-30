plugins {
    kotlin("js")
}

dependencies {
    implementation(kotlin("stdlib-js"))
}

kotlin {
    target {
        useCommonJs()
        produceKotlinLibrary()
        browser {
        }
    }
}