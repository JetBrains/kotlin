plugins {
    kotlin("js") version "1.3.61"
}
group = "testGroupId"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}
dependencies {
    testImplementation(kotlin("test-js"))
    implementation(kotlin("stdlib-js"))
}
kotlin.target.browser { }