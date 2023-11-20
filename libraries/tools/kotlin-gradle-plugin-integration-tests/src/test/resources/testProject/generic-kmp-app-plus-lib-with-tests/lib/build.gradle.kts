plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    js()
    <SingleNativeTarget>("native")
}

kotlin {
    js {
        nodejs()
    }

    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
