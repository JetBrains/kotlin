plugins {
    kotlin("js")
}

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation(project(":lib"))
    implementation(npm(projectDir.resolve("src/main/css")))
}

kotlin {
    target {
        browser {
            webpackTask {
                cssSupport.enabled = true
            }
        }
        binaries.executable()
    }
}