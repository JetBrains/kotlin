plugins {
    id("com.android.library")
    kotlin("android")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

android {
    compileSdkVersion(30)
    namespace = "com.example.plainAndroidConsumer"
}

repositories {
    maven("<localRepo>")
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk7"))
    implementation("com.example:producer:1.0.0-SNAPSHOT")
}
