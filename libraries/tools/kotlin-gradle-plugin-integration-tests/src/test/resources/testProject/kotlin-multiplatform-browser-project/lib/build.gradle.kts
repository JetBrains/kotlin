plugins {
    kotlin("multiplatform")
}

kotlin {
    js {
        useCommonJs()
        browser()
    }

    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(project(":base"))
            }
        }
    }
}