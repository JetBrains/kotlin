plugins {
    id("com.android.library")
    kotlin("multiplatform")
}

repositories {
    maven("<localRepo>")
    mavenCentral()
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

android {
    compileSdkVersion(30)
    namespace = "com.example.plainAndroidConsumer"
}

kotlin {
    android()

    sourceSets.commonMain.get().dependencies {
        implementation("com.example:producer:1.0.0-SNAPSHOT")
    }
}
