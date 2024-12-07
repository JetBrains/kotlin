plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("<localRepo>") }
}

kotlin {
    jvm()
    js {
        nodejs()
        binaries.executable()
    }
    linuxX64 {
        binaries.executable()
    }

    sourceSets {
        commonMain {
            dependencies {
                api("org.sample.kt62515.foo:libraryWithNonUniqueName:1.0")
                api(project(":libraryWithNonUniqueName"))
            }
        }
    }
}
