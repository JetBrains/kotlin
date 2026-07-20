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
        val jsMain = getByName("jsMain") {
            dependencies {
                implementation(project(":base"))
            }
        }
    }
}