plugins {
    id("org.jetbrains.kotlin.js")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-js")
    testImplementation("org.jetbrains.kotlin:kotlin-test-js")
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
}