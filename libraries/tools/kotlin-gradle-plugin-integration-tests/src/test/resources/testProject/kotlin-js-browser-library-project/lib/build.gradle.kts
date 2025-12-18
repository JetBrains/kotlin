plugins {
    kotlin("multiplatform")
}

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
                implementation(project(":base"))
            }
        }
    }
}