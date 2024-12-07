plugins {
    kotlin("multiplatform")
}

kotlin {

    wasmWasi {
        nodejs {}
    }

    wasmJs {
        nodejs {}
    }
}
