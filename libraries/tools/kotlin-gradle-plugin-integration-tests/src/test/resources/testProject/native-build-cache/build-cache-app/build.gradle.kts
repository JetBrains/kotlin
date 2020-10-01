plugins {
    id("org.jetbrains.kotlin.multiplatform").version("<pluginMarkerVersion>")
}

repositories {
    mavenLocal()
    jcenter()
}

kotlin {
    <SingleNativeTarget>("host")

    sourceSets {
        val hostMain by getting {
            dependencies {
                implementation("com.example:build-cache-lib:1.0")
                api(project(":lib-module"))
            }
        }
    }
}
