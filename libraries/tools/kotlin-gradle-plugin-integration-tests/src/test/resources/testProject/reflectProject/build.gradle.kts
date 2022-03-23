import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.reflekt.plugin.reflekt

plugins {
//    id("tanvd.kosogor") version "1.0.10" apply true
    id("org.jetbrains.reflekt") version "1.5.31" apply true
    kotlin("jvm") version "1.5.31" apply true

}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {

    mavenCentral()
    google()
    mavenLocal()
    maven(url = uri("https://packages.jetbrains.team/maven/p/reflekt/reflekt"))

}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    implementation("org.jetbrains.reflekt", "reflekt-dsl", "1.5.31")
    implementation("com.github.gumtreediff", "core", "2.1.2")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
reflekt {
    // Enable or disable Reflekt plugin
    enabled = true
    // List of external libraries for dependencies search
    // Use only DependencyHandlers which have canBeResolve = True
    // Note: Reflekt works only with kt files from libraries
    librariesToIntrospect = listOf("io.kotless:kotless-dsl:0.1.6")
}
tasks.withType<KotlinCompile> {
    kotlinOptions {
        useIR = true
        languageVersion = "1.5"
        apiVersion = "1.5"
        jvmTarget = "11"
        // Current Reflekt version does not support incremental compilation process
        incremental = true
    }
}