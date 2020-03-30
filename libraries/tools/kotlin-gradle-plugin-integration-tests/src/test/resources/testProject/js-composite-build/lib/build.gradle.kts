group = "com.example"

plugins {
    kotlin("js") version "<pluginMarkerVersion>"
}

repositories {
    jcenter()
    mavenCentral()
    mavenLocal()
}

kotlin.target.nodejs()

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation(npm("async", "2.6.2"))
}