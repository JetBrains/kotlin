plugins {
    kotlin("js") version "1.8.255-SNAPSHOT"
}

group = "me.user"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    compileOnly(npm("lit", "2.3.1"))
}

kotlin {
    js(IR) {
        binaries.executable()
        browser {
        }
    }
}
