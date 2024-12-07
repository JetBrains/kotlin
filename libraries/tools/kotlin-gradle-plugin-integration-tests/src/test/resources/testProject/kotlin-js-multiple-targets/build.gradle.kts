plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    js("clientSide") {
        attributes {
            attribute(Attribute.of("js.target", String::class.java), "clientSide")
        }
        browser {

        }
        binaries.executable()
    }

    js("serverSide") {
        attributes {
            attribute(Attribute.of("js.target", String::class.java), "serverSide")
        }
        nodejs {

        }
        binaries.executable()
    }
}
