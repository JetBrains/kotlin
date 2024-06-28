plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

android {
    compileSdk = 30
    namespace = "foo.bar"
}

kotlin {
    android()
    linuxX64()

    sourceSets {
        getByName("commonMain") {
            dependencies {
                implementation("com.squareup.okio:okio:3.2.0")
            }
        }
        getByName("commonTest") {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
            }
        }
    }
}
