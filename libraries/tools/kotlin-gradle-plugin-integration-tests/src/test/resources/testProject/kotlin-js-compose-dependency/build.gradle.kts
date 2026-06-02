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
        val commonMain = getByName("commonMain") {
            dependencies {
                implementation("org.jetbrains.compose.runtime:runtime:1.4.3") // commenting this out and uncommenting in jsMain fixes the issue
            }
        }
        val jsMain = getByName("jsMain") {
            dependencies {
                implementation("org.jetbrains.compose.html:html-core:1.4.3")
            }
        }
    }
}