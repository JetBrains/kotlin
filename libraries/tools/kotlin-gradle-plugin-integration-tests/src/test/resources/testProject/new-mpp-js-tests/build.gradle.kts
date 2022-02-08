plugins {
    kotlin("multiplatform").version("<pluginMarkerVersion>")
}

repositories {
    mavenLocal()
    mavenCentral()
    mavenCentral()
}

kotlin {
    dependencies {
        commonMainImplementation(kotlin("stdlib-common"))
        commonTestApi(kotlin("test-common"))
    }

    val jsCommon = js("jsCommon") {
        dependencies {
            commonMainImplementation(kotlin("stdlib-js"))
            commonTestApi(kotlin("test-js"))
        }
    }

    js("server")
    js("client")
}