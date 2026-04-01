plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    wasmJs {
        d8 {}
    }
}
