plugins {
    kotlin("multiplatform")
}

kotlin {
    js {
        useCommonJs()
        browser()
        binaries.library()
    }

    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(project(":base"))
            }
        }
    }
}