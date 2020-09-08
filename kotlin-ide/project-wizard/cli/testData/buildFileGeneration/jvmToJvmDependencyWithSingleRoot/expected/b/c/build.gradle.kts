import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}
group = "testGroupId"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://dl.bintray.com/kotlin/kotlin-dev")
    }
}
dependencies {
    implementation(project(":b"))
    testImplementation(kotlin("test-junit"))
}
tasks.test {
    useJUnit()
}
tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}