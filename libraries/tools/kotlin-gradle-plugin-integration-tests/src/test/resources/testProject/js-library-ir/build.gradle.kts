plugins {
    kotlin("multiplatform")
    `maven-publish`
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport.enabled = true
            }
        }
    }
}

dependencies {
    "jsMainImplementation"(npm("decamelize", "4.0.0", generateExternals = true))
}