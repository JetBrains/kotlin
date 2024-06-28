plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    js {
        useCommonJs()
        binaries.executable()
        nodejs {
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.compose.runtime:runtime:1.4.3") // commenting this out and uncommenting in jsMain fixes the issue
            }
        }
        val jsMain by getting {
            dependencies {
                implementation("org.jetbrains.compose.html:html-core:1.4.3")
            }
        }
    }
}