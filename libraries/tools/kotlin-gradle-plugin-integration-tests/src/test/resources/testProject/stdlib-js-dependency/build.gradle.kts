plugins {
    kotlin("multiplatform")
}

group = "com.example"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    js()
    <otherTarget>

    sourceSets.getByName("<sourceSetWithStdlibJsDependency>") {
        dependencies {
            implementation(kotlin("stdlib-js"))
        }
    }
}
