plugins {
    kotlin("multiplatform")
}

@Suppress("DEPRECATION")
kotlin {
    js {
        useCommonJs()
        browser {
        }
    }

    sourceSets {
        jsMain {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }
    }
}