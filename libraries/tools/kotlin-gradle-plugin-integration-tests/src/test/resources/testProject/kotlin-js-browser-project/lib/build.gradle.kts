plugins {
    kotlin("multiplatform")
}

kotlin {
    sourceSets {
        jsMain {
            dependencies {
                implementation(kotlin("stdlib-js"))
                implementation(project(":base"))
            }
        }
    }
}

kotlin {
    js {
        useCommonJs()
        browser {
        }
    }
}